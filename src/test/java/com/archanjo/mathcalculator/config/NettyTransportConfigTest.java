package com.archanjo.mathcalculator.config;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyTransportConfigTest {

    private static final Set<String> VALID_TRANSPORTS =
            Set.of("io_uring", "epoll", "kqueue", "nio");
    @Test
    void detectTransportReturnsNonNull() {
        final String transport = NettyTransportConfig.detectTransport();
        assertNotNull(transport, "Detected transport should not be null");
    }

    @Test
    void detectTransportReturnsValidName() {
        final String transport = NettyTransportConfig.detectTransport();
        assertTrue(VALID_TRANSPORTS.contains(transport),
                "Transport should be one of " + VALID_TRANSPORTS
                        + " but was: " + transport);
    }
}
