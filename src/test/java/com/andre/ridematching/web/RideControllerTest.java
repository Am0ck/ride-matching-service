package com.andre.ridematching.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RideControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void allocatesAndCompletesRideThenRejectsRepeatedCompletion() throws Exception {
        String driver = """
                {
                  "location": {"x": 1.0, "y": 1.0},
                  "available": true
                }
                """;
        String rideRequest = """
                {
                  "pickupLocation": {"x": 2.0, "y": 2.0}
                }
                """;

        mockMvc.perform(put("/drivers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driver))
                .andExpect(status().isCreated());

        MvcResult allocation = mockMvc.perform(post("/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rideRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.driver.driverId").value(1))
                .andExpect(jsonPath("$.driver.available").value(false))
                .andReturn();

        String rideId = allocation.getResponse()
                .getContentAsString()
                .replaceAll(".*\"rideId\":(\\d+).*", "$1");

        mockMvc.perform(post("/rides/{rideId}/complete", rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.driver.available").value(true));

        mockMvc.perform(post("/rides/{rideId}/complete", rideId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RIDE_ALREADY_COMPLETED"));
    }

    @Test
    void returnsConflictWhenNoDriverIsAvailable() throws Exception {
        String rideRequest = """
                {
                  "pickupLocation": {"x": 2.0, "y": 2.0}
                }
                """;

        mockMvc.perform(post("/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rideRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_DRIVER_AVAILABLE"));
    }

    @Test
    void returnsNotFoundForMissingRide() throws Exception {
        mockMvc.perform(post("/rides/99/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RIDE_NOT_FOUND"));
    }

    @Test
    void validatesRideRequest() throws Exception {
        String invalidRideRequest = """
                {
                  "pickupLocation": {"x": null, "y": 2.0}
                }
                """;

        mockMvc.perform(post("/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRideRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
