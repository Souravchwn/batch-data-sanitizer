package com.sourav.enterprise.sanitizer.strategy;

public interface SanitizationStrategy {
    String apply(String value);

    String getStrategyName();
}
