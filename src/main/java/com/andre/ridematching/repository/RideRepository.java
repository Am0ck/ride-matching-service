package com.andre.ridematching.repository;

import com.andre.ridematching.domain.Ride;
import com.andre.ridematching.domain.RideStatus;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class RideRepository {

    private final Map<Long, Ride> rides = new HashMap<>();

    public void save(Ride ride) {
        Ride nonNullRide = Objects.requireNonNull(ride, "Ride must not be null");
        rides.put(nonNullRide.getId(), nonNullRide);
    }

    public Optional<Ride> findById(long rideId) {
        requirePositiveId(rideId, "Ride ID must be positive");
        return Optional.ofNullable(rides.get(rideId));
    }

    public boolean hasActiveRideForDriver(long driverId) {
        requirePositiveId(driverId, "Driver ID must be positive");
        return rides.values().stream()
                .anyMatch(ride -> ride.getDriverId() == driverId
                        && ride.getStatus() == RideStatus.ACTIVE);
    }

    private static void requirePositiveId(long id, String message) {
        if (id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
