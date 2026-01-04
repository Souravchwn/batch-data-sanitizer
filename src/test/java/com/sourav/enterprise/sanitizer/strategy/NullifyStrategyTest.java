package com.sourav.enterprise.sanitizer.strategy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NullifyStrategyTest {
    @Test
    void shouldReturnEmpty() {
        NullifyStrategy strategy = new NullifyStrategy("");
        assertEquals("", strategy.apply("any value"));
    }

    @Test
    void shouldReturnConfiguredReplacement() {
        NullifyStrategy strategy = new NullifyStrategy("[REDACTED]");
        assertEquals("[REDACTED]", strategy.apply("any value"));
    }
}
