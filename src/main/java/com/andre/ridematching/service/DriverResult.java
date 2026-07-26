package com.andre.ridematching.service;

import com.andre.ridematching.domain.Location;

public record DriverResult(
        long driverId,
        Location location,
        boolean available,
        boolean created
) {
}
