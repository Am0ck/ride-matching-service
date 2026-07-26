package com.andre.ridematching.service;

import com.andre.ridematching.domain.Location;
import com.andre.ridematching.domain.RideStatus;

public record RideResult(
        long rideId,
        RideStatus status,
        Location pickupLocation,
        long driverId,
        Location driverLocation,
        boolean driverAvailable
) {
}
