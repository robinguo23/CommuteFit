package uk.co.commutefit.fare.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.commutefit.fare.FareSummary;
import uk.co.commutefit.fare.FareSummary.FareItem;
import uk.co.commutefit.fare.FareSummary.FareTap;

class FarePolicyMatcherTest {
    private FarePolicyMatcher matcher;

    @BeforeEach
    void setUp() { matcher = new FarePolicyMatcher(new FarePolicyCatalogue()); }

    @Test
    void publishesBusDiscountForSelectedOysterPolicyAndMatchingFareItem() {
        FareItem item = busItem(1, "1.75", "2026-10-27T07:00:00+01:00");
        FareSummary.ConcessionScenario result = scenario(List.of(item), "oyster", List.of("bus-tram-discount"));

        assertEquals("eligible", result.eligibilityStatus());
        assertEquals("available", result.amount().status());
        assertEquals(new BigDecimal("0.85"), result.amount().value());
        assertEquals(List.of(1), result.amountFareItemSequences());
        assertEquals("2026-10-27T00:00", result.effectiveDate());
    }

    @Test
    void doesNotInventDiscountWhenAdultFareAmountDoesNotMatchPublishedPeriod() {
        FareSummary.ConcessionScenario result = scenario(
            List.of(busItem(1, "1.75", "2026-11-01T05:00:00Z")), "oyster", List.of("bus-tram-discount"));

        assertEquals("eligible", result.eligibilityStatus());
        assertEquals("unable-to-confirm", result.amount().status());
        assertNull(result.amount().value());
        assertEquals(List.of(), result.amountFareItemSequences());
    }

    @Test
    void requiresSelectedPolicyAndItsPaymentBasis() {
        FareItem item = busItem(1, "1.75", "2026-10-27T07:00:00+01:00");
        FareSummary.ConcessionScenario unselected = scenario(List.of(item), "oyster", List.of());
        FareSummary.ConcessionScenario contactless = scenario(List.of(item), "bank-card-contactless", List.of("bus-tram-discount"));

        assertEquals("unknown", unselected.eligibilityStatus());
        assertEquals("ineligible", contactless.eligibilityStatus());
        assertEquals("unable-to-confirm", contactless.amount().status());
    }

    private FareSummary.ConcessionScenario scenario(List<FareItem> items, String payment, List<String> selected) {
        return matcher.match(items, List.of("bus"), payment, selected, LocalDateTime.of(2026, 10, 27, 7, 0)).stream()
            .filter(value -> value.policyId().equals("bus-tram-discount"))
            .findFirst().orElseThrow();
    }

    private FareItem busItem(int sequence, String amount, String timestamp) {
        return new FareItem(sequence, new BigDecimal(amount), "GBP", null, 0, 0, false,
            List.of("bus"), List.of(new FareTap("Bus", timestamp, "11", "0")));
    }
}
