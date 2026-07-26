package com.andre.ridematching.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideTest {

    @Test
    void startsActiveAndTransitionsToCompletedWithoutChangingIds() {
        Ride ride = new Ride(11, 7, new Location(1, 2));

        assertEquals(RideStatus.ACTIVE, ride.getStatus());

        ride.complete();

        assertEquals(RideStatus.COMPLETED, ride.getStatus());
        assertEquals(11, ride.getId());
        assertEquals(7, ride.getDriverId());
    }

    @Test
    void rejectsNonPositiveRideId() {
        Location pickupLocation = new Location(1, 2);

        assertThrows(IllegalArgumentException.class,
                () -> new Ride(0, 1, pickupLocation));
        assertThrows(IllegalArgumentException.class,
                () -> new Ride(-1, 1, pickupLocation));
    }

    @Test
    void rejectsNonPositiveDriverId() {
        Location pickupLocation = new Location(1, 2);

        assertThrows(IllegalArgumentException.class,
                () -> new Ride(1, 0, pickupLocation));
        assertThrows(IllegalArgumentException.class,
                () -> new Ride(1, -1, pickupLocation));
    }
}
