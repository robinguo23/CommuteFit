package uk.co.commutefit.journey.lookup;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.time.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.co.commutefit.common.*;
import uk.co.commutefit.journey.tfl.*;
import jakarta.validation.Validation;

class JourneyLookupServiceTest {
    private JourneyLookupRequest request() { return new JourneyLookupRequest(" sw1a1aa ", "w1d 3qf", null, null, null, null, null); }
    private JourneyLookupService service(String response) {
        return service(response, "20261027", "0900", "Departing");
    }
    private JourneyLookupService service(String response, String date, String time, String timeIs) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tfl.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.tfl.test/Journey/JourneyResults/SW1A%201AA/to/W1D%203QF?date=" + date + "&time=" + time + "&timeIs=" + timeIs + "&calcOneDirection=true&mode=bus,overground,national-rail,elizabeth-line,tube,dlr,tram,river-bus,cable-car,walking"))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        return new JourneyLookupService(new TflJourneyService(new TflClient(builder.build(), ""), new CandidateTokenStore()),
            Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC));
    }
    private String journey(String fare) {
        return """
            {"journeys":[{"startDateTime":"2026-10-27T09:01:00","arrivalDateTime":"2026-10-27T09:23:00","duration":22,
            "legs":[{"mode":{"name":"bus"},"departurePoint":{"commonName":"Origin"},"arrivalPoint":{"commonName":"Interchange"}},
                    {"mode":{"name":"tube"},"departurePoint":{"commonName":"Interchange"},"arrivalPoint":{"commonName":"Destination"}}]
            %s}]}
            """.formatted(fare);
    }
    @Test void returnsWholeRouteFareAndAllLegsUsingLondonNowAndNormalizedPostcodes() {
        var result = service(journey(",\"fare\":{\"totalCost\":485,\"fares\":[{\"cost\":175},{\"cost\":310}]}")).lookup(request());
        assertEquals("SW1A 1AA", result.originPostcode());
        assertEquals(new BigDecimal("4.85"), result.routes().getFirst().totalFare());
        assertEquals(2, result.routes().getFirst().legs().size());
        assertEquals(1, result.routes().getFirst().transfers());
        assertEquals(22, result.routes().getFirst().durationMinutes());
    }
    @Test void preservesEverySupportedTransportModeAndDoesNotCountWalkingAsAChange() {
        String response = """
            {"journeys":[{"startDateTime":"2026-10-27T09:00:00","arrivalDateTime":"2026-10-27T10:00:00","duration":60,
            "legs":[
              {"mode":{"name":"bus"}},
              {"mode":{"name":"overground"}},
              {"mode":{"name":"national-rail"}},
              {"mode":{"name":"elizabeth-line"}},
              {"mode":{"name":"tube"}},
              {"mode":{"name":"dlr"}},
              {"mode":{"name":"tram"}},
              {"mode":{"name":"river-bus"}},
              {"mode":{"name":"cable-car"}},
              {"mode":{"name":"walking"}}
            ]}]}
            """;

        var route = service(response).lookup(request()).routes().getFirst();

        assertEquals(java.util.List.of("bus", "overground", "national-rail", "elizabeth-line", "tube", "dlr", "tram", "river-bus", "cable-car", "walking"),
            route.legs().stream().map(leg -> leg.mode()).toList());
        assertEquals(8, route.transfers());
    }

    @Test void doesNotSumFareItemsWhenTotalIsMissing() {
        var route = service(journey(",\"fare\":{\"fares\":[{\"cost\":175},{\"cost\":310}]}")).lookup(request()).routes().getFirst();
        assertNull(route.totalFare()); assertEquals("unavailable", route.fareStatus());
    }
    @Test void distinguishesNoRoutesFromMalformedProviderResponse() {
        assertEquals("no-routes", service("{\"journeys\":[]}").lookup(request()).status());
        assertThrows(ProviderUnavailableException.class, () -> service("{}").lookup(request()));
        assertThrows(ProviderUnavailableException.class, () -> service("{\"journeys\":[{}]}").lookup(request()));
    }
    @Test void neverConvertsInvalidTotalToAFreeFare() {
        for (String cost : new String[]{"-1", "\"175\"", "175.5", "null"}) {
            assertNull(service(journey(",\"fare\":{\"totalCost\":" + cost + "}")).lookup(request()).routes().getFirst().totalFare());
        }
        assertEquals(new BigDecimal("0.00"), service(journey(",\"fare\":{\"totalCost\":0}")).lookup(request()).routes().getFirst().totalFare());
    }
    @Test void handlesLocationCandidatesBeforeMappingRoutes() {
        var error = assertThrows(LocationDisambiguationException.class, () -> service("""
            {"toLocationDisambiguation":{"disambiguationOptions":[{"place":{"id":"123","commonName":"Destination"}}]}}
            """).lookup(request()));
        assertEquals("destination", error.candidateField()); assertEquals(1, error.candidates().size());
    }
    @Test void validatesBothLocationsWithoutRequiringAProfile() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(request()).isEmpty());
            assertEquals(2, validator.validate(new JourneyLookupRequest("", "", null, null, null, null, null)).stream().map(v -> v.getPropertyPath().toString()).distinct().count());
        }
    }
    @Test void usesExplicitLeaveOrArriveDateTimeAndRejectsIncompleteSelection() {
        var leave = new JourneyLookupRequest("SW1A 1AA", "W1D 3QF", "leave", LocalDate.of(2026, 10, 28), "08:15", null, null);
        var arrive = new JourneyLookupRequest("SW1A 1AA", "W1D 3QF", "arrive", LocalDate.of(2026, 10, 28), "08:15", null, null);
        assertEquals("leave", service(journey(",\"fare\":{\"totalCost\":485}"), "20261028", "0815", "Departing").lookup(leave).timeMode());
        assertEquals(LocalDateTime.of(2026, 10, 28, 8, 15), service(journey(",\"fare\":{\"totalCost\":485}"), "20261028", "0815", "Arriving").lookup(arrive).requestedDateTime());
        assertThrows(IllegalArgumentException.class, () -> service("{\"journeys\":[]}").lookup(new JourneyLookupRequest("SW1A 1AA", "W1D 3QF", "leave", null, null, null, null)));
    }
    @Test void endpointAcceptsOnlyTwoPostcodesAndReturnsTheRouteTotal() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(new JourneyLookupController(service(journey(",\"fare\":{\"totalCost\":485}"))))
            .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/journeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"originPostcode\":\"sw1a1aa\",\"destinationPostcode\":\"W1D 3QF\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.routes[0].totalFare").value(4.85))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.routes[0].legs.length()").value(2));
    }
    @Test void endpointRejectsMissingLocationsBeforeCallingProvider() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(new JourneyLookupController(service("{\"journeys\":[]}")))
            .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/journeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"originPostcode\":\"\",\"destinationPostcode\":\"\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("invalid_request"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.fields.originPostcode").exists())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.fields.destinationPostcode").exists());
    }

}
