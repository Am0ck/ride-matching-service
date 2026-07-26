package com.andre.ridematching.service;

import com.andre.ridematching.domain.Location;
import com.andre.ridematching.error.ErrorCode;
import com.andre.ridematching.error.RideMatchingException;
import com.andre.ridematching.repository.DriverRepository;
import com.andre.ridematching.repository.RideIdGenerator;
import com.andre.ridematching.repository.RideRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RideMatchingServiceConcurrencyTest {

    @Test
    void concurrentRequestsCannotAllocateOneDriverTwice() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            DriverRepository driverRepository = new DriverRepository();
            RideMatchingService service = new RideMatchingService(
                    driverRepository,
                    new RideRepository(),
                    new RideIdGenerator());
            service.upsertDriver(1, new Location(0, 0), true);

            int requestCount = 12;
            ExecutorService executor = Executors.newFixedThreadPool(requestCount);
            CountDownLatch ready = new CountDownLatch(requestCount);
            CountDownLatch start = new CountDownLatch(1);

            try {
                List<Future<Boolean>> futures = new ArrayList<>();
                for (int index = 0; index < requestCount; index++) {
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            service.requestRide(new Location(1, 1));
                            return true;
                        } catch (RideMatchingException exception) {
                            if (exception.getErrorCode() != ErrorCode.NO_DRIVER_AVAILABLE) {
                                throw exception;
                            }
                            return false;
                        }
                    }));
                }

                ready.await();
                start.countDown();

                long successfulAllocations = 0;
                for (Future<Boolean> future : futures) {
                    if (future.get()) {
                        successfulAllocations++;
                    }
                }

                assertEquals(1, successfulAllocations);
                assertFalse(driverRepository.findById(1).orElseThrow().isAvailable());
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        });
    }
}
