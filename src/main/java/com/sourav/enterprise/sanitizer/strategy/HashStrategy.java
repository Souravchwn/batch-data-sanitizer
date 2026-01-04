package com.sourav.enterprise.sanitizer.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashStrategy implements SanitizationStrategy {
    private final String algorithm;
    private final ThreadLocal<MessageDigest> digestHolder;

    public HashStrategy(@Value("${sanitizer.defaults.hash-algorithm:SHA-256}") String algorithm) {
        this.algorithm = algorithm;
        this.digestHolder = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Hash algorithm not available: " + algorithm, e);
            }
        });
    }

    @Override
    public String apply(String value) {
        if (value == null || value.isEmpty())
            return value;
        MessageDigest digest = digestHolder.get();
        digest.reset();
        byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }

    @Override
    public String getStrategyName() {
        return "HASH(" + algorithm + ")";
    }
}
