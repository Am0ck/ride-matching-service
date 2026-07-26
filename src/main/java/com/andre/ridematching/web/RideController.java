package com.andre.ridematching.web;

import com.andre.ridematching.service.RideResult;
import com.andre.ridematching.service.RideMatchingService;
import com.andre.ridematching.web.dto.RideRequest;
import com.andre.ridematching.web.dto.RideResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rides")
@Validated
public class RideController {

    private final RideMatchingService rideMatchingService;

    public RideController(RideMatchingService rideMatchingService) {
        this.rideMatchingService = rideMatchingService;
    }

    @PostMapping
    public ResponseEntity<RideResponse> requestRide(
            @Valid @RequestBody RideRequest request
    ) {
        RideResult result = rideMatchingService.requestRide(
                request.pickupLocation().toLocation());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RideResponse.from(result));
    }

    @PostMapping("/{rideId}/complete")
    public RideResponse completeRide(@PathVariable @Positive long rideId) {
        return RideResponse.from(rideMatchingService.completeRide(rideId));
    }
}
