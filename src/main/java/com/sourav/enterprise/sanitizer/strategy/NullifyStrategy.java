package com.sourav.enterprise.sanitizer.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NullifyStrategy implements SanitizationStrategy {
    private final String replacement;

    public NullifyStrategy(@Value("${sanitizer.defaults.null-replacement:}") String replacement) {
        this.replacement = replacement;
    }

    @Override
    public String apply(String value) {
        return replacement;
    }

    @Override
    public String getStrategyName() {
        return "NULLIFY";
    }
}
