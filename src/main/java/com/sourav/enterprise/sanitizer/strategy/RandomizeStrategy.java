package com.sourav.enterprise.sanitizer.strategy;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class RandomizeStrategy implements SanitizationStrategy {

    public RandomizeStrategy() {
    }

    @Override
    public String apply(String value) {
        if (value == null || value.isEmpty())
            return value;
        long seed = value.hashCode();
        Faker seededFaker = new Faker(new Random(seed));

        if (isEmail(value))
            return seededFaker.internet().emailAddress();
        if (isPhoneNumber(value))
            return seededFaker.phoneNumber().cellPhone();
        if (isName(value))
            return seededFaker.name().fullName();
        if (isAddress(value))
            return seededFaker.address().streetAddress();
        if (isNumeric(value))
            return String.valueOf(seededFaker.number().numberBetween(1000, 9999));

        int wordCount = Math.max(1, value.split("\\s+").length);
        return seededFaker.lorem().words(wordCount).toString().replace("[", "").replace("]", "").replace(",", "");
    }

    private boolean isEmail(String value) {
        return value.contains("@") && value.contains(".");
    }

    private boolean isPhoneNumber(String value) {
        String cleaned = value.replaceAll("[\\s\\-().+]", "");
        return cleaned.matches("\\d{7,15}");
    }

    private boolean isName(String value) {
        if (!value.matches("[a-zA-Z\\s.'\\-]+"))
            return false;
        String[] words = value.trim().split("\\s+");
        if (words.length < 1 || words.length > 4)
            return false;
        for (String word : words) {
            if (word.length() > 0 && Character.isLowerCase(word.charAt(0)))
                return false;
        }
        return true;
    }

    private boolean isAddress(String value) {
        String lower = value.toLowerCase();
        return value.matches(".*\\d+.*") && (lower.contains("street") || lower.contains("st.") ||
                lower.contains("ave") || lower.contains("road") || lower.contains("rd.") ||
                lower.contains("lane") || lower.contains("blvd") || lower.contains("drive") ||
                lower.contains("dr.") || lower.contains("#"));
    }

    private boolean isNumeric(String value) {
        return value.matches("-?\\d+(\\.\\d+)?");
    }

    @Override
    public String getStrategyName() {
        return "RANDOMIZE";
    }
}
