package com.andre.ridematching.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DriverControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void registersAndUpdatesDriver() throws Exception {
        String createdDriver = """
                {
                  "location": {"x": 1.0, "y": 2.0},
                  "available": true
                }
                """;
        String updatedDriver = """
                {
                  "location": {"x": 3.0, "y": 4.0},
                  "available": false
                }
                """;

        mockMvc.perform(put("/drivers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createdDriver))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value(1))
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(put("/drivers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedDriver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.x").value(3.0))
                .andExpect(jsonPath("$.location.y").value(4.0))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void returnsStructuredValidationErrorForInvalidBody() throws Exception {
        String invalidDriver = """
                {
                  "location": {"x": null, "y": 2.0},
                  "available": true
                }
                """;

        mockMvc.perform(put("/drivers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDriver))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations[0].field")
                        .value("location.x"));
    }

    @Test
    void rejectsNonPositiveDriverId() throws Exception {
        String driver = """
                {
                  "location": {"x": 1.0, "y": 2.0},
                  "available": true
                }
                """;

        mockMvc.perform(put("/drivers/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driver))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsNearestAvailableDrivers() throws Exception {
        String nearestDriver = """
                {
                  "location": {"x": 1000.0, "y": 1000.0},
                  "available": true
                }
                """;
        String fartherDriver = """
                {
                  "location": {"x": 1003.0, "y": 1004.0},
                  "available": true
                }
                """;

        mockMvc.perform(put("/drivers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nearestDriver))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/drivers/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fartherDriver))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/drivers/nearest")
                        .param("x", "1000")
                        .param("y", "1000")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drivers[0].driverId").value(10))
                .andExpect(jsonPath("$.drivers[0].distance").value(0.0));
    }
}
