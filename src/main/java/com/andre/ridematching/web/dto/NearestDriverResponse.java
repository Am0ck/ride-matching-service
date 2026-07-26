package com.andre.ridematching.web.dto;

import com.andre.ridematching.service.NearestDriverResult;

public record NearestDriverResponse(
        long driverId,
        LocationResponse location,
        double distance
) {

    public static NearestDriverResponse from(NearestDriverResult result) {
        return new NearestDriverResponse(
                result.driverId(),
                LocationResponse.from(result.location()),
                result.distance());
    }
}
