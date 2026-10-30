package uk.co.commutefit.fare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FareSummary(RouteFare routeFare, List<FareItem> fareItems, List<ConcessionScenario> conditionalConcessionScenarios, CapGuidance capGuidance) {
    public record RouteFare(String status, BigDecimal amount, String currency, String paymentBasis, String paymentBasisZh,
                            String scope, String scopeZh, LocalDate travelDate, String source) {}
    public record FareItem(int sequence, BigDecimal amount, String currency, String chargeLevel, Integer lowZone, Integer highZone, Boolean hopperFare, List<String> modes, List<FareTap> taps) {}
    public record FareTap(String modeType, String timestamp, String routeId, String locationCode) {}
    public record ConcessionScenario(
        String policyId, String titleEn, String titleZh, String eligibilityStatus, String timeBasis,
        String qualifyingEvent, String eligibilityRule, String fareScope, List<Integer> fareItemSequences,
        List<Integer> amountFareItemSequences, String paymentAssumption,
        String eligibilityAssumption, Amount amount, String effectiveDate, String source, boolean selectedByUser
    ) {}
    public record Amount(String status, BigDecimal value, String currency, String basis) {}
    public record CapGuidance(
        String status, BigDecimal adultDailyCap, BigDecimal adultWeeklyCap,
        BigDecimal conditionalDiscountDailyCap, String currency, String effectiveFrom,
        String messageEn, String messageZh, String source
    ) {}
}
