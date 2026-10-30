package uk.co.commutefit.fare.policy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;
import uk.co.commutefit.fare.FareSummary;
import uk.co.commutefit.fare.FareSummary.FareItem;

@Component
public class FarePolicyMatcher {
    private static final Set<String> RAIL = Set.of("rail", "tube", "dlr", "london-overground", "elizabeth-line", "national-rail");
    private static final Set<String> BUS_DISCOUNT_POLICIES = Set.of("jobcentre-plus", "bus-tram-discount", "care-leaver-18-25");
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private final FarePolicyCatalogue catalogue;
    public FarePolicyMatcher(FarePolicyCatalogue catalogue) { this.catalogue = catalogue; }

    public List<FareSummary.ConcessionScenario> match(List<FareItem> fareItems, List<String> routeModes, String paymentMethod, List<String> selectedIds, LocalDateTime fareDateTime) {
        List<FareSummary.ConcessionScenario> result = new ArrayList<>();
        Set<String> selected = selectedIds == null ? Set.of() : Set.copyOf(selectedIds);
        for (FarePolicy policy : catalogue.policies()) {
            List<FareItem> rawScope = fareItems.stream().filter(item -> !Boolean.TRUE.equals(item.hopperFare())).filter(item -> supportsItem(policy, item)).toList();
            boolean routeMatches = routeModes.stream().anyMatch(mode -> supportsMode(policy, mode));
            if (!routeMatches && rawScope.isEmpty()) continue;

            List<FareItem> evaluatedScope = rawScope;
            boolean offPeakAvailable = false;
            boolean levelsComplete = true;
            if (policy.id().equals("railcard-offpeak") && !rawScope.isEmpty()) {
                levelsComplete = rawScope.stream().allMatch(item -> item.chargeLevel() != null && !item.chargeLevel().isBlank());
                if (levelsComplete) {
                    evaluatedScope = rawScope.stream().filter(item -> "off peak".equalsIgnoreCase(item.chargeLevel())).toList();
                    offPeakAvailable = !evaluatedScope.isEmpty();
                    if (evaluatedScope.isEmpty()) evaluatedScope = rawScope;
                }
            }
            List<Integer> sequenceNumbers = evaluatedScope.stream().map(FareItem::sequence).toList();
            boolean isSelected = selected.contains(policy.id());
            String eligibility = eligibility(policy, rawScope, paymentMethod, isSelected, levelsComplete, offPeakAvailable);
            DiscountMatch discount = publishedBusFareItems(policy, rawScope, fareDateTime, eligibility);
            List<Integer> amountSequences = discount == null ? List.of() : discount.items().stream().map(FareItem::sequence).toList();
            FareSummary.Amount amount = discount == null
                ? new FareSummary.Amount("unable-to-confirm", null, "GBP", null)
                : new FareSummary.Amount("available", discount.amount(), "GBP", "Per listed eligible bus/tram fare item; not a route total.");
            String effectiveDate = discount != null && discount.effectiveDate() != null ? discount.effectiveDate() : policy.effectiveDate();
            String timeBasis = policy.timeBasis();
            if (policy.id().equals("railcard-offpeak") || policy.id().equals("disabled-persons-railcard")) {
                List<String> levels = rawScope.stream().map(FareItem::chargeLevel).filter(value -> value != null && !value.isBlank()).distinct().toList();
                timeBasis = levels.isEmpty() ? "Journey Planner fare-item chargeLevel not returned" : "Journey Planner fare-item chargeLevel: " + String.join(", ", levels);
            }
            result.add(new FareSummary.ConcessionScenario(
                policy.id(), policy.titleEn(), policy.titleZh(), eligibility, timeBasis,
                policy.qualifyingEvent(), policy.eligibilityTextEn(), policy.fareScope(), sequenceNumbers,
                amountSequences, policy.paymentBasis(), isSelected ? "User selected this concession; card validity and setup follow the scheme rules." : "Eligibility has not been confirmed by the user.",
                amount, effectiveDate, policy.source(), isSelected
            ));
        }
        return result;
    }

    private DiscountMatch publishedBusFareItems(FarePolicy policy, List<FareItem> scope, LocalDateTime fallbackDateTime, String eligibility) {
        if (!"eligible".equals(eligibility) || !BUS_DISCOUNT_POLICIES.contains(policy.id())) return null;
        List<FareItem> matched = new ArrayList<>();
        Set<LocalDateTime> effectiveDates = new LinkedHashSet<>();
        BigDecimal discountedFare = null;
        for (FareItem item : scope) {
            if (!isBusTramItem(item) || item.amount() == null) continue;
            LocalDateTime itemDateTime = fareItemDateTime(item, fallbackDateTime);
            PublishedFarePeriod period = catalogue.busTramFarePeriod(itemDateTime);
            if (period == null || item.amount().compareTo(period.adultSingleFare()) != 0) continue;
            if (discountedFare != null && discountedFare.compareTo(period.discountedSingleFare()) != 0) return null;
            discountedFare = period.discountedSingleFare();
            effectiveDates.add(period.effectiveFrom());
            matched.add(item);
        }
        String effectiveDate = effectiveDates.isEmpty() ? null : effectiveDates.stream().map(LocalDateTime::toString).collect(Collectors.joining(", "));
        return matched.isEmpty() ? null : new DiscountMatch(List.copyOf(matched), discountedFare, effectiveDate);
    }

    private LocalDateTime fareItemDateTime(FareItem item, LocalDateTime fallbackDateTime) {
        return item.taps().stream()
            .filter(tap -> tap.modeType() != null && ("bus".equalsIgnoreCase(tap.modeType()) || "tram".equalsIgnoreCase(tap.modeType())))
            .map(tap -> parseLondonDateTime(tap.timestamp()))
            .filter(value -> value != null)
            .findFirst()
            .orElse(fallbackDateTime);
    }

    private LocalDateTime parseLondonDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try { return OffsetDateTime.parse(value).atZoneSameInstant(LONDON).toLocalDateTime(); }
        catch (RuntimeException ignored) { return null; }
    }

    private record DiscountMatch(List<FareItem> items, BigDecimal amount, String effectiveDate) {}

    private boolean isBusTramItem(FareItem item) {
        return !item.modes().isEmpty() && item.modes().stream().allMatch(mode -> Set.of("bus", "tram").contains(canonical(mode)));
    }

    private String eligibility(FarePolicy policy, List<FareItem> scope, String payment, boolean selected, boolean levelsComplete, boolean offPeakAvailable) {
        if (!policy.paygDiscount()) return "ineligible";
        if (scope.isEmpty()) return "unknown";
        if (!selected) return "unknown";
        if (requiresOyster(policy) && !"oyster".equals(payment)) return payment == null ? "unknown" : "ineligible";
        if (policy.id().equals("railcard-offpeak")) {
            if (!levelsComplete) return "unknown";
            if (!offPeakAvailable) return "ineligible";
        }
        if (policy.id().equals("disabled-persons-railcard") && scope.stream().anyMatch(item -> item.chargeLevel() == null || item.chargeLevel().isBlank())) return "unknown";
        return "eligible";
    }

    private boolean supportsItem(FarePolicy policy, FareItem item) {
        return item.modes().stream().anyMatch(mode -> supportsMode(policy, mode));
    }
    private boolean supportsMode(FarePolicy policy, String value) {
        String mode = canonical(value);
        if (mode.equals("rail")) return policy.supportedModes().stream().anyMatch(RAIL::contains);
        return policy.supportedModes().contains(mode);
    }
    private String canonical(String value) {
        String mode = value.toLowerCase(Locale.ROOT).trim().replace(' ', '-');
        if (mode.equals("overground")) return "london-overground";
        if (mode.equals("elizabethline")) return "elizabeth-line";
        return mode;
    }
    private boolean requiresOyster(FarePolicy policy) { return policy.paymentBasis().toLowerCase(Locale.ROOT).contains("oyster"); }
}
