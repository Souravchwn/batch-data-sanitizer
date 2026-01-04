package com.sourav.enterprise.sanitizer.strategy;

import com.sourav.enterprise.sanitizer.domain.enums.SanitizationOperation;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Map;

@Component
public class SanitizationStrategyFactory {
    private final Map<SanitizationOperation, SanitizationStrategy> strategies;

    public SanitizationStrategyFactory(
            MaskStrategy maskStrategy,
            HashStrategy hashStrategy,
            NullifyStrategy nullifyStrategy,
            RandomizeStrategy randomizeStrategy) {
        this.strategies = new EnumMap<>(SanitizationOperation.class);
        strategies.put(SanitizationOperation.MASK, maskStrategy);
        strategies.put(SanitizationOperation.HASH, hashStrategy);
        strategies.put(SanitizationOperation.NULLIFY, nullifyStrategy);
        strategies.put(SanitizationOperation.RANDOMIZE, randomizeStrategy);
    }

    public SanitizationStrategy getStrategy(SanitizationOperation operation) {
        SanitizationStrategy strategy = strategies.get(operation);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for operation: " + operation);
        }
        return strategy;
    }
}
