package uk.co.commutefit.fare.policy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublishedFarePeriod(
    LocalDateTime effectiveFrom,
    LocalDateTime effectiveUntil,
    BigDecimal adultSingleFare,
    BigDecimal discountedSingleFare,
    BigDecimal adultDailyCap,
    BigDecimal discountedDailyCap,
    BigDecimal adultWeeklyCap
) {
    public boolean covers(LocalDateTime dateTime) {
        return !dateTime.isBefore(effectiveFrom) && (effectiveUntil == null || dateTime.isBefore(effectiveUntil));
    }
}
