package com.andre.ridematching.web.dto;

import com.andre.ridematching.domain.Location;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull Double x,
        @NotNull Double y
) {

    public Location toLocation() {
        return new Location(x, y);
    }
}
