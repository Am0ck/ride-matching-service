package com.andre.ridematching.web;

import com.andre.ridematching.domain.Location;
import com.andre.ridematching.service.DriverResult;
import com.andre.ridematching.service.RideMatchingService;
import com.andre.ridematching.web.dto.DriverResponse;
import com.andre.ridematching.web.dto.DriverUpsertRequest;
import com.andre.ridematching.web.dto.NearestDriversResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drivers")
@Validated
public class DriverController {

    private final RideMatchingService rideMatchingService;

    public DriverController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverResponse> upsertDriver(
            @PathVariable @Positive long driverId,
            @Valid @RequestBody DriverUpsertRequest request
    ) {
        DriverResult result = rideMatchingService.upsertDriver(
                driverId,
                request.location().toLocation(),
                request.available());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DriverResponse.from(result));
    }

    @GetMapping("/nearest")
    public NearestDriversResponse findNearestDrivers(
            @RequestParam double x,
            @RequestParam double y,
            @RequestParam @Min(1) int limit
    ) {
        return NearestDriversResponse.from(
                rideMatchingService.findNearestDrivers(new Location(x, y), limit));
    }
}
