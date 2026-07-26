package com.andre.ridematching.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RideRequest(
        @NotNull @Valid LocationRequest pickupLocation
) {
}
