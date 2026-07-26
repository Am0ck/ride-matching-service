package com.andre.ridematching.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DriverUpsertRequest(
        @NotNull @Valid LocationRequest location,
        @NotNull Boolean available
) {
}
