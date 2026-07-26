package com.andre.ridematching.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocationTest {

    @Test
    void rejectsNonFiniteCoordinates() {
        double[] nonFiniteValues = {
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double value : nonFiniteValues) {
            assertThrows(IllegalArgumentException.class, () -> new Location(value, 0));
            assertThrows(IllegalArgumentException.class, () -> new Location(0, value));
        }
    }

    @Test
    void calculatesEuclideanDistance() {
        Location origin = new Location(0, 0);
        Location destination = new Location(3, 4);

        assertEquals(5.0, origin.distanceTo(destination));
        assertEquals(5.0, destination.distanceTo(origin));
    }
}
