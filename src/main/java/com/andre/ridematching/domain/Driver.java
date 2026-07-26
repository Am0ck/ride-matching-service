package com.andre.ridematching.domain;

import java.util.Objects;

public final class Driver {

    private final long id;
    private Location location;
    private boolean available;

    public Driver(long id, Location location, boolean available) {
        if (id <= 0) {
            throw new IllegalArgumentException("Driver ID must be positive");
        }
        this.id = id;
        this.location = Objects.requireNonNull(location, "Location must not be null");
        this.available = available;
    }

    public long getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isAvailable() {
        return available;
    }

    public void updateLocation(Location location) {
        this.location = Objects.requireNonNull(location, "Location must not be null");
    }

    public void markAvailable() {
        available = true;
    }

    public void markUnavailable() {
        available = false;
    }
}
