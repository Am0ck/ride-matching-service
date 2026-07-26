package com.andre.ridematching.web.dto;

import com.andre.ridematching.service.NearestDriverResult;

import java.util.List;

public record NearestDriversResponse(List<NearestDriverResponse> drivers) {

    public static NearestDriversResponse from(List<NearestDriverResult> results) {
        return new NearestDriversResponse(results.stream()
                .map(NearestDriverResponse::from)
                .toList());
    }
}
