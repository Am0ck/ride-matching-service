package com.andre.ridematching.service;

import com.andre.ridematching.domain.Location;
import com.andre.ridematching.domain.RideStatus;
import com.andre.ridematching.error.ErrorCode;
import com.andre.ridematching.error.RideMatchingException;
import com.andre.ridematching.repository.DriverRepository;
import com.andre.ridematching.repository.RideIdGenerator;
import com.andre.ridematching.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideMatchingServiceTest {

    private DriverRepository driverRepository;
    private RideRepository rideRepository;
    private RideMatchingService service;

    @BeforeEach
    void setUp() {
        driverRepository = new DriverRepository();
        rideRepository = new RideRepository();
        service = new RideMatchingService(
                driverRepository,
                rideRepository,
                new RideIdGenerator());
    }

    @Test
    void registersAndUpdatesDriver() {
        DriverResult created = service.upsertDriver(
                1, new Location(1, 2), true);
        DriverResult updated = service.upsertDriver(
                1, new Location(3, 4), false);

        assertTrue(created.created());
        assertFalse(updated.created());
        assertEquals(new Location(3, 4), updated.location());
        assertFalse(updated.available());
        assertEquals(1, driverRepository.findAll().size());
    }

    @Test
    void allocatesNearestDriverAndBreaksEqualDistanceByNumericId() {
        service.upsertDriver(20, new Location(1, 0), true);
        service.upsertDriver(10, new Location(-1, 0), true);
        service.upsertDriver(5, new Location(0, 10), true);

        RideResult result = service.requestRide(new Location(0, 0));

        assertEquals(1, result.rideId());
        assertEquals(10, result.driverId());
        assertEquals(RideStatus.ACTIVE, result.status());
        assertFalse(result.driverAvailable());
        assertFalse(driverRepository.findById(10).orElseThrow().isAvailable());
        assertTrue(driverRepository.findById(20).orElseThrow().isAvailable());
        assertTrue(rideRepository.hasActiveRideForDriver(10));
    }

    @Test
    void rejectsRideRequestWhenNoDriverIsAvailable() {
        service.upsertDriver(1, new Location(0, 0), false);

        RideMatchingException exception = assertThrows(
                RideMatchingException.class,
                () -> service.requestRide(new Location(0, 0)));

        assertEquals(ErrorCode.NO_DRIVER_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void completesRideAndRejectsRepeatedCompletionWithoutFurtherStateChange() {
        service.upsertDriver(1, new Location(0, 0), true);
        RideResult allocated = service.requestRide(new Location(1, 1));

        RideResult completed = service.completeRide(allocated.rideId());

        assertEquals(RideStatus.COMPLETED, completed.status());
        assertTrue(completed.driverAvailable());

        RideMatchingException exception = assertThrows(
                RideMatchingException.class,
                () -> service.completeRide(allocated.rideId()));

        assertEquals(ErrorCode.RIDE_ALREADY_COMPLETED, exception.getErrorCode());
        assertEquals(
                RideStatus.COMPLETED,
                rideRepository.findById(allocated.rideId()).orElseThrow().getStatus());
        assertTrue(driverRepository.findById(1).orElseThrow().isAvailable());
    }

    @Test
    void rejectsMissingRideCompletion() {
        RideMatchingException exception = assertThrows(
                RideMatchingException.class,
                () -> service.completeRide(99));

        assertEquals(ErrorCode.RIDE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void allowsLocationOnlyUpdateDuringActiveRide() {
        service.upsertDriver(1, new Location(0, 0), true);
        service.requestRide(new Location(1, 1));

        DriverResult updated = service.upsertDriver(
                1, new Location(5, 6), false);

        assertEquals(new Location(5, 6), updated.location());
        assertFalse(updated.available());

        RideMatchingException exception = assertThrows(
                RideMatchingException.class,
                () -> service.upsertDriver(1, new Location(7, 8), true));

        assertEquals(ErrorCode.DRIVER_HAS_ACTIVE_RIDE, exception.getErrorCode());
        assertEquals(
                new Location(5, 6),
                driverRepository.findById(1).orElseThrow().getLocation());
        assertFalse(driverRepository.findById(1).orElseThrow().isAvailable());
    }

    @Test
    void returnsOnlyNearestAvailableDriversInDeterministicOrder() {
        service.upsertDriver(30, new Location(3, 4), true);
        service.upsertDriver(20, new Location(0, 2), true);
        service.upsertDriver(10, new Location(0, -2), true);
        service.upsertDriver(1, new Location(0, 1), false);

        List<NearestDriverResult> results = service.findNearestDrivers(
                new Location(0, 0), 2);

        assertEquals(List.of(10L, 20L), results.stream()
                .map(NearestDriverResult::driverId)
                .toList());
        assertEquals(List.of(2.0, 2.0), results.stream()
                .map(NearestDriverResult::distance)
                .toList());
    }

    @Test
    void validatesNearestDriverLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findNearestDrivers(new Location(0, 0), 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findNearestDrivers(new Location(0, 0), -1));
    }

    @Test
    void largePositiveLimitReturnsAllAvailableDrivers() {
        service.upsertDriver(1, new Location(1, 0), true);
        service.upsertDriver(2, new Location(-1, 0), true);
        service.upsertDriver(3, new Location(0, 5), false);

        List<NearestDriverResult> results =
                service.findNearestDrivers(new Location(0, 0), 1000);

        assertEquals(
                List.of(1L, 2L),
                results.stream()
                        .map(NearestDriverResult::driverId)
                        .toList());
    }
}
