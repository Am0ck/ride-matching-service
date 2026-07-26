package com.andre.ridematching.web.dto;

import com.andre.ridematching.service.DriverResult;

public record DriverResponse(
        long driverId,
        LocationResponse location,
        boolean available
) {

    public static DriverResponse from(DriverResult result) {
        return new DriverResponse(
                result.driverId(),
                LocationResponse.from(result.location()),
                result.available());
    }
}
