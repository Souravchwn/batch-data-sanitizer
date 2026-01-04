package com.sourav.enterprise.sanitizer.strategy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomizeStrategyTest {
    private final RandomizeStrategy strategy = new RandomizeStrategy();

    @Test
    void shouldGenerateFakeEmail() {
        String result = strategy.apply("john@example.com");
        assertTrue(result.contains("@"));
        assertNotEquals("john@example.com", result);
    }

    @Test
    void shouldBeDeterministic() {
        String input = "test@example.com";
        assertEquals(strategy.apply(input), strategy.apply(input));
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
