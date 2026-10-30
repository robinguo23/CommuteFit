package uk.co.commutefit.fare.policy;

import java.util.List;

public record FarePolicy(
    String id, String titleEn, String titleZh, List<String> supportedModes, String paymentBasis,
    String timeBasis, String qualifyingEvent, String fareScope, String eligibilityTextEn,
    String eligibilityTextZh, String effectiveDate, String source, boolean paygDiscount
) {}
