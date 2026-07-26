package com.andre.ridematching.service;

import com.andre.ridematching.domain.Driver;
import com.andre.ridematching.domain.Location;
import com.andre.ridematching.domain.Ride;
import com.andre.ridematching.domain.RideStatus;
import com.andre.ridematching.error.ErrorCode;
import com.andre.ridematching.error.RideMatchingException;
import com.andre.ridematching.repository.DriverRepository;
import com.andre.ridematching.repository.RideIdGenerator;
import com.andre.ridematching.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RideMatchingService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final RideIdGenerator rideIdGenerator;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public RideMatchingService(
            DriverRepository driverRepository,
            RideRepository rideRepository,
            RideIdGenerator rideIdGenerator
    ) {
        this.driverRepository = Objects.requireNonNull(
                driverRepository, "Driver repository must not be null");
        this.rideRepository = Objects.requireNonNull(
                rideRepository, "Ride repository must not be null");
        this.rideIdGenerator = Objects.requireNonNull(
                rideIdGenerator, "Ride ID generator must not be null");
    }

    public DriverResult upsertDriver(long driverId, Location location, boolean available) {
        requirePositiveId(driverId, "Driver ID must be positive");
        Objects.requireNonNull(location, "Location must not be null");

        lifecycleLock.lock();
        try {
            return driverRepository.findById(driverId)
                    .map(driver -> updateDriver(driver, location, available))
                    .orElseGet(() -> createDriver(driverId, location, available));
        } finally {
            lifecycleLock.unlock();
        }
    }

    public RideResult requestRide(Location pickupLocation) {
        Objects.requireNonNull(pickupLocation, "Pickup location must not be null");

        lifecycleLock.lock();
        try {
            Driver selectedDriver = driverRepository.findAll().stream()
                    .filter(Driver::isAvailable)
                    .min(Comparator
                            .comparingDouble((Driver driver) ->
                                    driver.getLocation().distanceTo(pickupLocation))
                            .thenComparingLong(Driver::getId))
                    .orElseThrow(() -> new RideMatchingException(
                            ErrorCode.NO_DRIVER_AVAILABLE,
                            "No driver is currently available"));

            if (rideRepository.hasActiveRideForDriver(selectedDriver.getId())) {
                throw new IllegalStateException(
                        "An available driver cannot already have an active ride");
            }

            selectedDriver.markUnavailable();
            Ride ride = new Ride(
                    rideIdGenerator.nextId(),
                    selectedDriver.getId(),
                    pickupLocation);
            rideRepository.save(ride);
            return toRideResult(ride, selectedDriver);
        } finally {
            lifecycleLock.unlock();
        }
    }

    public RideResult completeRide(long rideId) {
        requirePositiveId(rideId, "Ride ID must be positive");

        lifecycleLock.lock();
        try {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new RideMatchingException(
                            ErrorCode.RIDE_NOT_FOUND,
                            "Ride " + rideId + " was not found"));

            if (ride.getStatus() == RideStatus.COMPLETED) {
                throw new RideMatchingException(
                        ErrorCode.RIDE_ALREADY_COMPLETED,
                        "Ride " + rideId + " is already completed");
            }

            Driver driver = driverRepository.findById(ride.getDriverId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Assigned driver " + ride.getDriverId() + " was not found"));

            ride.complete();
            driver.markAvailable();
            return toRideResult(ride, driver);
        } finally {
            lifecycleLock.unlock();
        }
    }

    public List<NearestDriverResult> findNearestDrivers(
            Location suppliedLocation,
            int limit
    ) {
        Objects.requireNonNull(suppliedLocation, "Location must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        List<DriverSnapshot> availableDrivers;
        lifecycleLock.lock();
        try {
            availableDrivers = driverRepository.findAll().stream()
                    .filter(Driver::isAvailable)
                    .map(driver -> new DriverSnapshot(
                            driver.getId(),
                            driver.getLocation()))
                    .toList();
        } finally {
            lifecycleLock.unlock();
        }

        return availableDrivers.stream()
                .map(driver -> new NearestDriverResult(
                        driver.driverId(),
                        driver.location(),
                        driver.location().distanceTo(suppliedLocation)))
                .sorted(Comparator
                        .comparingDouble(NearestDriverResult::distance)
                        .thenComparingLong(NearestDriverResult::driverId))
                .limit(limit)
                .toList();
    }

    private DriverResult createDriver(long driverId, Location location, boolean available) {
        Driver driver = new Driver(driverId, location, available);
        driverRepository.save(driver);
        return toDriverResult(driver, true);
    }

    private DriverResult updateDriver(
            Driver driver,
            Location location,
            boolean available
    ) {
        if (rideRepository.hasActiveRideForDriver(driver.getId()) && available) {
            throw new RideMatchingException(
                    ErrorCode.DRIVER_HAS_ACTIVE_RIDE,
                    "Driver " + driver.getId() + " has an active ride");
        }

        driver.updateLocation(location);
        if (available) {
            driver.markAvailable();
        } else {
            driver.markUnavailable();
        }
        return toDriverResult(driver, false);
    }

    private static DriverResult toDriverResult(Driver driver, boolean created) {
        return new DriverResult(
                driver.getId(),
                driver.getLocation(),
                driver.isAvailable(),
                created);
    }

    private static RideResult toRideResult(Ride ride, Driver driver) {
        return new RideResult(
                ride.getId(),
                ride.getStatus(),
                ride.getPickupLocation(),
                driver.getId(),
                driver.getLocation(),
                driver.isAvailable());
    }

    private static void requirePositiveId(long id, String message) {
        if (id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private record DriverSnapshot(long driverId, Location location) {
    }
}
