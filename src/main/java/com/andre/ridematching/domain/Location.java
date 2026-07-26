package com.andre.ridematching.domain;

import java.util.Objects;

public record Location(double x, double y) {

    public Location {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Coordinates must be finite");
        }
    }

    public double distanceTo(Location other) {
        Objects.requireNonNull(other, "Other location must not be null");
        return Math.hypot(x - other.x, y - other.y);
    }
}
