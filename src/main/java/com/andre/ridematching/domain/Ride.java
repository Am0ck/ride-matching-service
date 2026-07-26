package com.andre.ridematching.domain;

import java.util.Objects;

public final class Ride {

    private final long id;
    private final long driverId;
    private final Location pickupLocation;
    private RideStatus status;

    public Ride(long id, long driverId, Location pickupLocation) {
        if (id <= 0) {
            throw new IllegalArgumentException("Ride ID must be positive");
        }
        if (driverId <= 0) {
            throw new IllegalArgumentException("Driver ID must be positive");
        }
        this.id = id;
        this.driverId = driverId;
        this.pickupLocation = Objects.requireNonNull(
                pickupLocation, "Pickup location must not be null");
        this.status = RideStatus.ACTIVE;
    }

    public long getId() {
        return id;
    }

    public long getDriverId() {
        return driverId;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void complete() {
        status = RideStatus.COMPLETED;
    }
}
