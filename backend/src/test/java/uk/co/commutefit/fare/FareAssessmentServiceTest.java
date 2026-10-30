package uk.co.commutefit.fare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.fare.policy.FarePolicyCatalogue;
import uk.co.commutefit.fare.policy.FarePolicyMatcher;

class FareAssessmentServiceTest {
    private FareAssessmentService service;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        FarePolicyCatalogue catalogue = new FarePolicyCatalogue();
        service = new FareAssessmentService(new FarePolicyMatcher(catalogue), catalogue);
        jsonMapper = JsonMapper.builder().build();
    }

    @Test
    void usesProviderRouteTotalAndFareItemsInsteadOfChargingEveryLeg() throws Exception {
        JsonNode journey = jsonMapper.readTree("""
            {"fare":{"totalCost":485,"fares":[
              {"cost":175,"isHopperFare":false,"taps":[{"tapDetails":{"modeType":"Bus","busRouteId":"11","tapTimestamp":"2026-10-27T09:07:00+01:00"}}]},
              {"cost":310,"chargeLevel":"Peak","isHopperFare":false,"taps":[{"tapDetails":{"modeType":"Rail","tapTimestamp":"2026-10-27T09:18:00+01:00"}}]}
            ]}}
            """);
        List<JourneyLeg> legs = List.of(
            new JourneyLeg("bus", "11", "Stop A", "Stop B", null, null, 5, null),
            new JourneyLeg("tube", "Victoria line", "Victoria", "Oxford Circus", null, null, 4, null),
            new JourneyLeg("tube", "Bakerloo line", "Oxford Circus", "Baker Street", null, null, 6, null));

        FareSummary result = service.summarize(journey, LocalDateTime.of(2026, 10, 27, 9, 0), null, List.of(), legs);

        assertEquals("available", result.routeFare().status());
        assertEquals("4.85", result.routeFare().amount().toPlainString());
        assertEquals(2, result.fareItems().size());
        assertEquals("1.75", result.fareItems().get(0).amount().toPlainString());
        assertEquals("3.10", result.fareItems().get(1).amount().toPlainString());
    }

    @Test
    void leavesMissingRouteFareUnknownInsteadOfReportingZero() throws Exception {
        JsonNode journey = jsonMapper.readTree("{\"legs\":[{\"mode\":\"bus\"}]}");

        FareSummary result = service.summarize(journey, LocalDateTime.of(2026, 10, 27, 9, 0), null, List.of(), List.of());

        assertEquals("unable-to-confirm", result.routeFare().status());
        assertNull(result.routeFare().amount());
        assertEquals(List.of(), result.fareItems());
    }

    @Test
    void keepsTenArchivedTflScenariosConsistentWithProviderFareComponents() throws Exception {
        try (InputStream archive = getClass().getResourceAsStream("/archived-journey-fares.json")) {
            assertNotNull(archive);
            JsonNode scenarios = jsonMapper.readTree(archive);
            assertEquals(10, scenarios.size());
            for (JsonNode scenario : scenarios) {
                JsonNode providerFare = scenario.path("fare");
                JsonNode journey = jsonMapper.readTree("{\"fare\":" + providerFare + "}");
                List<JourneyLeg> legs = new java.util.ArrayList<>();
                scenario.path("legModes").forEach(mode -> legs.add(
                    new JourneyLeg(mode.asText(), null, null, null, null, null, null, null)));
                LocalDate date = LocalDate.parse(scenario.path("date").asText(), DateTimeFormatter.BASIC_ISO_DATE);
                LocalTime time = LocalTime.parse(scenario.path("queryTime").asText(), DateTimeFormatter.ofPattern("HHmm"));

                FareSummary summary = service.summarize(journey, LocalDateTime.of(date, time), null, List.of(), legs);
                String caseName = scenario.path("archiveRequest").asText();
                if (!providerFare.isObject() || !providerFare.hasNonNull("totalCost")) {
                    assertEquals("unable-to-confirm", summary.routeFare().status(), caseName);
                    assertNull(summary.routeFare().amount(), caseName);
                    continue;
                }

                BigDecimal expectedTotal = BigDecimal.valueOf(providerFare.path("totalCost").asLong(), 2);
                assertEquals("available", summary.routeFare().status(), caseName);
                assertEquals(expectedTotal, summary.routeFare().amount(), caseName);
                assertEquals(providerFare.path("fares").size(), summary.fareItems().size(), caseName);
                if (summary.fareItems().stream().allMatch(item -> item.amount() != null)) {
                    BigDecimal itemTotal = summary.fareItems().stream().map(FareSummary.FareItem::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    assertEquals(expectedTotal, itemTotal, caseName);
                }
            }
        }
    }
}
