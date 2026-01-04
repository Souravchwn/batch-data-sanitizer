package com.sourav.enterprise.sanitizer.strategy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashStrategyTest {
    private final HashStrategy strategy = new HashStrategy("SHA-256");

    @Test
    void shouldBeDeterministic() {
        String input = "test@example.com";
        assertEquals(strategy.apply(input), strategy.apply(input));
    }

    @Test
    void shouldProduceDifferentHashes() {
        assertNotEquals(strategy.apply("input1"), strategy.apply("input2"));
    }

    @Test
    void shouldProduceFixedLength() {
        assertEquals(64, strategy.apply("test").length());
    }

    @Test
    void shouldHandleNull() {
        assertNull(strategy.apply(null));
    }

    @Test
    void shouldHandleEmpty() {
        assertEquals("", strategy.apply(""));
    }
}
