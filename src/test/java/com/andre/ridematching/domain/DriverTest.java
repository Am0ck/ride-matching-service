package com.andre.ridematching.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverTest {

    @Test
    void changesLocationAndAvailabilityWithoutChangingId() {
        Driver driver = new Driver(7, new Location(1, 2), true);
        Location updatedLocation = new Location(3, 4);

        driver.updateLocation(updatedLocation);
        driver.markUnavailable();

        assertEquals(7, driver.getId());
        assertEquals(updatedLocation, driver.getLocation());
        assertFalse(driver.isAvailable());

        driver.markAvailable();

        assertEquals(7, driver.getId());
        assertTrue(driver.isAvailable());
    }

    @Test
    void rejectsNonPositiveId() {
        Location location = new Location(1, 2);

        assertThrows(IllegalArgumentException.class, () -> new Driver(0, location, true));
        assertThrows(IllegalArgumentException.class, () -> new Driver(-1, location, true));
    }
}
