package com.andre.ridematching.web.dto;

import com.andre.ridematching.domain.RideStatus;
import com.andre.ridematching.service.RideResult;

public record RideResponse(
        long rideId,
        RideStatus status,
        LocationResponse pickupLocation,
        DriverResponse driver
) {

    public static RideResponse from(RideResult result) {
        return new RideResponse(
                result.rideId(),
                result.status(),
                LocationResponse.from(result.pickupLocation()),
                new DriverResponse(
                        result.driverId(),
                        LocationResponse.from(result.driverLocation()),
                        result.driverAvailable()));
    }
}
