package com.andre.ridematching.repository;

import com.andre.ridematching.domain.Driver;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class DriverRepository {

    private final Map<Long, Driver> drivers = new HashMap<>();

    public void save(Driver driver) {
        Driver nonNullDriver = Objects.requireNonNull(driver, "Driver must not be null");
        drivers.put(nonNullDriver.getId(), nonNullDriver);
    }

    public Optional<Driver> findById(long driverId) {
        requirePositiveId(driverId);
        return Optional.ofNullable(drivers.get(driverId));
    }

    public List<Driver> findAll() {
        return List.copyOf(drivers.values());
    }

    private static void requirePositiveId(long driverId) {
        if (driverId <= 0) {
            throw new IllegalArgumentException("Driver ID must be positive");
        }
    }
}
