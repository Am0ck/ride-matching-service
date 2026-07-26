package com.andre.ridematching.web.dto;

import com.andre.ridematching.domain.Location;

public record LocationResponse(double x, double y) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(location.x(), location.y());
    }
}
