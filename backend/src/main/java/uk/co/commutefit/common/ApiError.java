package uk.co.commutefit.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(String code, String messageEn, String messageZh, Map<String, String> fields, Instant timestamp) {
    public static ApiError of(String code, String en, String zh) {
        return new ApiError(code, en, zh, Map.of(), Instant.now());
    }
}
