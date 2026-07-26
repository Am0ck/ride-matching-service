package com.andre.ridematching.web;

import com.andre.ridematching.web.dto.DriverResponse;
import com.andre.ridematching.web.dto.DriverUpsertRequest;
import com.andre.ridematching.web.dto.LocationRequest;
import com.andre.ridematching.web.dto.NearestDriversResponse;
import com.andre.ridematching.web.dto.RideRequest;
import com.andre.ridematching.web.dto.RideResponse;
import com.andre.ridematching.web.error.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RideMatchingApiEndToEndTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void completesRideMatchingWorkflowThroughRealHttp() throws Exception {
        DriverResponse driver10 = requireBody(putDriver(
                10, 1.0, 0.0, true, DriverResponse.class), HttpStatus.CREATED);
        assertDriver(driver10, 10, 1.0, 0.0, true);

        DriverResponse driver20 = requireBody(putDriver(
                20, -1.0, 0.0, true, DriverResponse.class), HttpStatus.CREATED);
        assertDriver(driver20, 20, -1.0, 0.0, true);

        DriverResponse driver30 = requireBody(putDriver(
                30, 0.0, 5.0, false, DriverResponse.class), HttpStatus.CREATED);
        assertDriver(driver30, 30, 0.0, 5.0, false);

        driver30 = requireBody(putDriver(
                30, 0.0, 4.0, false, DriverResponse.class), HttpStatus.OK);
        assertDriver(driver30, 30, 0.0, 4.0, false);

        assertNearestDriverIds(10, List.of(10L, 20L));

        RideResponse ride1 = requireBody(
                requestRide(0.0, 0.0, RideResponse.class),
                HttpStatus.CREATED);
        assertEquals(1, ride1.rideId());
        assertEquals("ACTIVE", ride1.status().name());
        assertDriver(ride1.driver(), 10, 1.0, 0.0, false);

        assertNearestDriverIds(10, List.of(20L));

        driver10 = requireBody(putDriver(
                10, 2.0, 2.0, false, DriverResponse.class), HttpStatus.OK);
        assertDriver(driver10, 10, 2.0, 2.0, false);

        assertError(
                putDriver(10, 9.0, 9.0, true, ApiErrorResponse.class),
                HttpStatus.CONFLICT,
                "DRIVER_HAS_ACTIVE_RIDE");

        RideResponse ride2 = requireBody(
                requestRide(0.0, 0.0, RideResponse.class),
                HttpStatus.CREATED);
        assertEquals(2, ride2.rideId());
        assertEquals(20, ride2.driver().driverId());
        assertFalse(ride2.driver().available());

        assertError(
                requestRide(0.0, 0.0, ApiErrorResponse.class),
                HttpStatus.CONFLICT,
                "NO_DRIVER_AVAILABLE");

        ride1 = requireBody(completeRide(1, RideResponse.class), HttpStatus.OK);
        assertEquals("COMPLETED", ride1.status().name());
        assertDriver(ride1.driver(), 10, 2.0, 2.0, true);

        assertError(
                completeRide(1, ApiErrorResponse.class),
                HttpStatus.CONFLICT,
                "RIDE_ALREADY_COMPLETED");

        assertError(
                completeRide(999, ApiErrorResponse.class),
                HttpStatus.NOT_FOUND,
                "RIDE_NOT_FOUND");

        assertNearestDriverIds(10, List.of(10L));

        ride2 = requireBody(completeRide(2, RideResponse.class), HttpStatus.OK);
        assertEquals("COMPLETED", ride2.status().name());
        assertTrue(ride2.driver().available());
        assertEquals(20, ride2.driver().driverId());

        assertNearestDriverIds(1000, List.of(20L, 10L));

        assertError(
                putDriver(0, 0.0, 0.0, true, ApiErrorResponse.class),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR");

        assertError(
                putDriver(40, null, 0.0, true, ApiErrorResponse.class),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR");

        assertError(
                send("GET", "/drivers/nearest", null, ApiErrorResponse.class),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST");

        assertError(
                nearestDrivers(0, ApiErrorResponse.class),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR");

        assertError(
                requestRide(null, 0.0, ApiErrorResponse.class),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR");
    }

    private <T> ApiResponse<T> putDriver(
            long driverId,
            Double x,
            Double y,
            boolean available,
            Class<T> responseType
    ) throws Exception {
        DriverUpsertRequest request = new DriverUpsertRequest(
                new LocationRequest(x, y),
                available);
        return send(
                "PUT",
                "/drivers/{driverId}",
                request,
                responseType,
                driverId);
    }

    private <T> ApiResponse<T> requestRide(
            Double x,
            Double y,
            Class<T> responseType
    ) throws Exception {
        RideRequest request = new RideRequest(new LocationRequest(x, y));
        return send("POST", "/rides", request, responseType);
    }

    private <T> ApiResponse<T> completeRide(
            long rideId,
            Class<T> responseType
    ) throws Exception {
        return send(
                "POST",
                "/rides/{rideId}/complete",
                null,
                responseType,
                rideId);
    }

    private <T> ApiResponse<T> nearestDrivers(
            int limit,
            Class<T> responseType
    ) throws Exception {
        return send(
                "GET",
                "/drivers/nearest?x={x}&y={y}&limit={limit}",
                null,
                responseType,
                0,
                0,
                limit);
    }

    private void assertNearestDriverIds(
            int limit,
            List<Long> expectedDriverIds
    ) throws Exception {
        NearestDriversResponse response = requireBody(
                nearestDrivers(limit, NearestDriversResponse.class),
                HttpStatus.OK);
        assertEquals(
                expectedDriverIds,
                response.drivers().stream()
                        .map(driver -> driver.driverId())
                        .toList());
    }

    private void assertDriver(
            DriverResponse driver,
            long expectedId,
            double expectedX,
            double expectedY,
            boolean expectedAvailability
    ) {
        assertEquals(expectedId, driver.driverId());
        assertEquals(expectedX, driver.location().x());
        assertEquals(expectedY, driver.location().y());
        assertEquals(expectedAvailability, driver.available());
    }

    private void assertError(
            ApiResponse<ApiErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedCode
    ) {
        ApiErrorResponse error = requireBody(response, expectedStatus);
        assertEquals(expectedCode, error.code());
    }

    private <T> T requireBody(
            ApiResponse<T> response,
            HttpStatus expectedStatus
    ) {
        assertEquals(expectedStatus.value(), response.status());
        T body = response.body();
        assertNotNull(body);
        return body;
    }

    private <T> ApiResponse<T> send(
            String method,
            String pathTemplate,
            Object requestBody,
            Class<T> responseType,
            Object... uriVariables
    ) throws Exception {
        String path = expandPath(pathTemplate, uriVariables);
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json");

        if (requestBody == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(
                            method,
                            HttpRequest.BodyPublishers.ofString(
                                    jsonMapper.writeValueAsString(requestBody)));
        }

        HttpResponse<String> response = httpClient.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString());
        return new ApiResponse<>(
                response.statusCode(),
                jsonMapper.readValue(response.body(), responseType));
    }

    private String expandPath(String pathTemplate, Object... uriVariables) {
        String expandedPath = pathTemplate;
        for (Object uriVariable : uriVariables) {
            expandedPath = expandedPath.replaceFirst(
                    "\\{[^}]+}",
                    uriVariable.toString());
        }
        return expandedPath;
    }

    private record ApiResponse<T>(int status, T body) {
    }
}
