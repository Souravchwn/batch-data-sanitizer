package com.sourav.enterprise.sanitizer.strategy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaskStrategyTest {
    private final MaskStrategy strategy = new MaskStrategy('*', 4);

    @Test
    void shouldMaskEmail() {
        String result = strategy.apply("john.doe@example.com");
        assertTrue(result.contains("@"));
        assertTrue(result.contains(".com"));
        assertTrue(result.contains("*"));
    }

    @Test
    void shouldMaskPhone() {
        String result = strategy.apply("123-456-7890");
        assertTrue(result.endsWith("7890"));
        assertTrue(result.contains("*"));
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
