package com.andre.ridematching.service;

import com.andre.ridematching.domain.Location;

public record NearestDriverResult(
        long driverId,
        Location location,
        double distance
) {
}
