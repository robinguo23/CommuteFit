package uk.co.commutefit.fare;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import uk.co.commutefit.fare.policy.FarePolicyCatalogue;
import uk.co.commutefit.fare.policy.FarePolicyMatcher;
import uk.co.commutefit.fare.policy.PublishedFarePeriod;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.fare.FareSummary.FareItem;
import uk.co.commutefit.fare.FareSummary.FareTap;

@Service
public class FareAssessmentService {
    private final FarePolicyMatcher matcher;
    private final FarePolicyCatalogue catalogue;
    public FareAssessmentService(FarePolicyMatcher matcher, FarePolicyCatalogue catalogue) { this.matcher = matcher; this.catalogue = catalogue; }

    public FareSummary summarize(JsonNode journey, LocalDateTime fareDateTime, String paymentMethod, List<String> selectedConcessions, List<JourneyLeg> legs) {
        LocalDate travelDate = fareDateTime.toLocalDate();
        JsonNode fare = journey.path("fare");
        JsonNode itemsNode = fare.path("fares");
        List<FareItem> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            int sequence = 1;
            for (JsonNode item : itemsNode) {
                List<String> modes = new ArrayList<>();
                List<FareTap> taps = new ArrayList<>();
                JsonNode tapsNode = item.path("taps");
                if (tapsNode.isArray()) for (JsonNode tap : tapsNode) {
                    JsonNode detail = tap.path("tapDetails");
                    String mode = detail.path("modeType").asText("");
                    if (!mode.isBlank() && !modes.contains(mode.toLowerCase(Locale.ROOT))) modes.add(mode.toLowerCase(Locale.ROOT));
                    taps.add(new FareTap(mode, detail.path("tapTimestamp").asText(null), detail.path("busRouteId").asText(detail.path("routeId").asText(null)), detail.path("nationalLocationCode").asText(null)));
                }
                // Journey Planner's fare.totalCost and fare item cost are integer pence in the captured API responses.
                BigDecimal amount = item.hasNonNull("cost") && item.path("cost").canConvertToInt()
                    ? BigDecimal.valueOf(item.path("cost").asLong(), 2) : null;
                items.add(new FareItem(sequence++, amount, "GBP", item.path("chargeLevel").asText(null), integerOrNull(item, "lowZone"), integerOrNull(item, "highZone"), item.has("isHopperFare") ? item.path("isHopperFare").asBoolean() : null, modes, taps));
            }
        }
        BigDecimal total = fare.hasNonNull("totalCost") && fare.path("totalCost").canConvertToInt()
            ? BigDecimal.valueOf(fare.path("totalCost").asLong(), 2) : null;
        FareSummary.RouteFare routeFare = new FareSummary.RouteFare(
            total == null ? "unable-to-confirm" : "available", total, "GBP",
            paymentMethod == null
                ? "Adult pay as you go (Journey Planner)"
                : "Adult PAYG quote from Journey Planner; selected payment: " + paymentLabel(paymentMethod) + "; concession not applied",
            paymentMethod == null
                ? "Journey Planner 返回的成人 PAYG 票价"
                : "Journey Planner 返回的成人 PAYG 票价；付款方式假设：" + paymentLabelZh(paymentMethod) + "；未应用优惠",
            "Complete Journey Planner route; fare items are provider fare components, not itinerary legs.",
            "Journey Planner 完整路线；票价项是提供方返回的收费组成，不等于行程 leg。", travelDate,
            "https://tfl.gov.uk/fares/find-fares/single-fare-finder"
        );
        List<String> journeyModes = legs.stream().map(JourneyLeg::mode).filter(value -> value != null).toList();
        List<FareSummary.ConcessionScenario> scenarios = matcher.match(items, journeyModes, paymentMethod, selectedConcessions, fareDateTime);
        boolean includesBusOrTram = journeyModes.stream().map(FareAssessmentService::canonicalMode).anyMatch(mode -> mode.equals("bus") || mode.equals("tram"))
            || items.stream().anyMatch(item -> item.modes().stream().map(FareAssessmentService::canonicalMode).anyMatch(mode -> mode.equals("bus") || mode.equals("tram")));
        PublishedFarePeriod period = includesBusOrTram ? catalogue.busTramFarePeriod(fareDateTime) : null;
        boolean discountCardMatched = scenarios.stream().anyMatch(scenario -> scenario.selectedByUser() && "eligible".equals(scenario.eligibilityStatus())
            && List.of("jobcentre-plus", "bus-tram-discount", "care-leaver-18-25").contains(scenario.policyId()));
        FareSummary.CapGuidance caps = new FareSummary.CapGuidance(
            "guidance-only",
            period == null ? null : period.adultDailyCap(), period == null ? null : period.adultWeeklyCap(),
            period == null || !discountCardMatched ? null : period.discountedDailyCap(), "GBP",
            period == null ? null : period.effectiveFrom().toString(),
            period == null
                ? "Bus and tram cap amounts are unavailable for this route date or mode. Caps depend on all travel on the same card or device; this commute is not a full-day or full-week total."
                : "Published adult bus/tram PAYG caps; a qualifying Oyster discount holder may have the conditional reduced daily cap. Caps depend on all travel on the same card or device and do not price this commute by themselves.",
            period == null
                ? "当前路线日期或交通方式没有可用的公交/电车封顶金额。封顶取决于同一张卡或设备的全部行程；本次通勤不是全天或全周总额。"
                : "TfL 公布的成人公交/电车 PAYG 封顶；符合条件且已设置 Oyster 优惠者可能适用条件性折扣日封顶。封顶取决于同一张卡或设备的全部行程，不能单独代表本次通勤费用。",
            "https://tfl.gov.uk/fares/find-fares/caps-and-travelcard-prices"
        );
        return new FareSummary(routeFare, items, scenarios, caps);
    }

    private static Integer integerOrNull(JsonNode node, String field) { return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null; }
    private static String canonicalMode(String mode) { return mode.toLowerCase(Locale.ROOT).trim().replace(' ', '-'); }
    private static String paymentLabel(String method) {
        return switch (method) {
            case "oyster" -> "Oyster PAYG";
            case "apple-pay-contactless" -> "Contactless device";
            case "bank-card-contactless" -> "Contactless bank card";
            case "other" -> "User-selected payment method";
            default -> "Adult pay as you go (Journey Planner)";
        };
    }
    private static String paymentLabelZh(String method) {
        return switch (method) {
            case "oyster" -> "Oyster PAYG";
            case "apple-pay-contactless" -> "Apple Pay／设备感应支付";
            case "bank-card-contactless" -> "银行卡感应支付";
            case "other" -> "用户选择的其他付款方式";
            default -> "Journey Planner 成人 PAYG";
        };
    }
}
