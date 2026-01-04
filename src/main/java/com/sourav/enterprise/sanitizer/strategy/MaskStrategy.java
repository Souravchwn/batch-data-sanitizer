package com.sourav.enterprise.sanitizer.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MaskStrategy implements SanitizationStrategy {
    private final char maskChar;
    private final int visibleChars;

    public MaskStrategy(
            @Value("${sanitizer.defaults.mask-char:*}") char maskChar,
            @Value("${sanitizer.defaults.mask-visible-chars:4}") int visibleChars) {
        this.maskChar = maskChar;
        this.visibleChars = visibleChars;
    }

    @Override
    public String apply(String value) {
        if (value == null || value.isEmpty())
            return value;
        int length = value.length();
        if (length <= visibleChars)
            return String.valueOf(maskChar).repeat(length);
        if (value.contains("@"))
            return maskEmail(value);
        if (isPhoneNumber(value))
            return maskPhone(value);
        return maskDefault(value);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0)
            return maskDefault(email);
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        String maskedLocal = localPart.length() > 2
                ? localPart.substring(0, 2) + String.valueOf(maskChar).repeat(localPart.length() - 2)
                : String.valueOf(maskChar).repeat(localPart.length());
        int dotIndex = domainPart.lastIndexOf('.');
        String maskedDomain = dotIndex > 0
                ? String.valueOf(maskChar).repeat(dotIndex) + domainPart.substring(dotIndex)
                : String.valueOf(maskChar).repeat(domainPart.length());
        return maskedLocal + "@" + maskedDomain;
    }

    private String maskPhone(String phone) {
        StringBuilder masked = new StringBuilder();
        int totalDigits = (int) phone.chars().filter(Character::isDigit).count();
        int showLast = Math.min(visibleChars, totalDigits);
        int digitCount = 0;
        for (char c : phone.toCharArray()) {
            if (Character.isDigit(c)) {
                digitCount++;
                masked.append(digitCount <= totalDigits - showLast ? maskChar : c);
            } else {
                masked.append(c);
            }
        }
        return masked.toString();
    }

    private String maskDefault(String value) {
        int show = Math.min(visibleChars, value.length() / 2);
        return value.substring(0, show) + String.valueOf(maskChar).repeat(value.length() - show);
    }

    private boolean isPhoneNumber(String value) {
        String cleaned = value.replaceAll("[\\s\\-().+]", "");
        return cleaned.matches("\\d{7,15}");
    }

    @Override
    public String getStrategyName() {
        return "MASK";
    }
}
