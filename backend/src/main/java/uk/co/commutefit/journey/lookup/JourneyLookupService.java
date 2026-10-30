package uk.co.commutefit.journey.lookup;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.assessment.AssessmentTimePolicy;
import uk.co.commutefit.common.ProviderUnavailableException;
import uk.co.commutefit.common.LocationDisambiguationException;
import uk.co.commutefit.journey.tfl.TflJourneyService;

@Service
public class JourneyLookupService {
    private final TflJourneyService journeys;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public JourneyLookupService(TflJourneyService journeys) { this(journeys, Clock.systemUTC()); }
    JourneyLookupService(TflJourneyService journeys, Clock clock) { this.journeys = journeys; this.clock = clock; }

    public record Route(String id, LocalDateTime departure, LocalDateTime arrival, Integer durationMinutes,
                        int transfers, List<JourneyLeg> legs, BigDecimal totalFare, String fareStatus,
                        List<String> fareChargeLevels, String currency) {}
    public record Result(String status, String originPostcode, String destinationPostcode, Instant queriedAt,
                         String timezone, String timeMode, LocalDateTime requestedDateTime, List<Route> routes) {}

    public Result lookup(JourneyLookupRequest request) {
        String origin = normalize(request.originPostcode());
        String destination = normalize(request.destinationPostcode());
        Instant queriedAt = clock.instant();
        QueryTime queryTime = queryTime(request, queriedAt);
        String from = journeys.resolveOrigin(origin, request.selectedOriginCandidateId());
        String to = journeys.resolveDestination(destination, request.selectedDestinationCandidateId());
        JsonNode response = journeys.journeys(from, to, origin, destination, "origin", "destination",
            queryTime.dateTime().toLocalDate(), queryTime.dateTime().toLocalTime(), queryTime.timeIs());
        if (response == null || !response.path("journeys").isArray()) {
            if (response != null && (response.has("fromLocationDisambiguation") || response.has("toLocationDisambiguation")))
                throw new LocationDisambiguationException(response.has("fromLocationDisambiguation") ? "origin" : "destination", List.of());
            throw new ProviderUnavailableException(null);
        }
        List<Route> routes = new ArrayList<>();
        for (JsonNode value : response.path("journeys")) {
            JsonNode legValues = value.path("legs");
            if (!legValues.isArray() || legValues.isEmpty()) throw new ProviderUnavailableException(null);
            List<JourneyLeg> legs = new ArrayList<>();
            int transitLegs = 0;
            for (JsonNode leg : legValues) {
                String mode = leg.path("mode").path("name").asText("");
                if (mode.isBlank()) throw new ProviderUnavailableException(null);
                if (!List.of("walking", "walk").contains(mode.toLowerCase(Locale.ROOT))) transitLegs++;
                legs.add(new JourneyLeg(mode, leg.path("routeOptions").path(0).path("name").asText(""),
                    leg.path("departurePoint").path("commonName").asText(""),
                    leg.path("arrivalPoint").path("commonName").asText(""),
                    time(leg.path("departureTime")), time(leg.path("arrivalTime")), integer(leg.path("duration")),
                    leg.path("instruction").path("summary").asText(""), stopCount(leg)));
            }
            LocalDateTime departure = time(value.path("startDateTime"));
            LocalDateTime arrival = time(value.path("arrivalDateTime"));
            if (departure == null) departure = legs.getFirst().departureDateTime();
            if (arrival == null) arrival = legs.getLast().arrivalDateTime();
            if (departure == null || arrival == null) throw new ProviderUnavailableException(null);
            JsonNode cost = value.path("fare").path("totalCost");
            BigDecimal total = cost.isIntegralNumber() && cost.canConvertToLong() && cost.asLong() >= 0
                ? BigDecimal.valueOf(cost.asLong(), 2) : null;
            List<String> chargeLevels = new ArrayList<>();
            JsonNode fares = value.path("fare").path("fares");
            if (fares.isArray()) {
                for (JsonNode fare : fares) {
                    String chargeLevel = fare.path("chargeLevel").asText("").trim();
                    if (!chargeLevel.isBlank() && chargeLevels.stream().noneMatch(existing -> existing.equalsIgnoreCase(chargeLevel))) {
                        chargeLevels.add(chargeLevel);
                    }
                }
            }
            routes.add(new Route("route-" + routes.size(), departure, arrival, integer(value.path("duration")),
                Math.max(0, transitLegs - 1), legs, total, total == null ? "unavailable" : "available", chargeLevels, "GBP"));
        }
        return new Result(routes.isEmpty() ? "no-routes" : "available", origin, destination, queriedAt,
            AssessmentTimePolicy.LONDON.getId(), queryTime.mode(), queryTime.dateTime(), routes);
    }

    private record QueryTime(String mode, String timeIs, LocalDateTime dateTime) {}

    private static QueryTime queryTime(JourneyLookupRequest request, Instant now) {
        String mode = request.timeMode() == null || request.timeMode().isBlank() ? "now" : request.timeMode().toLowerCase(Locale.ROOT);
        if ("now".equals(mode)) {
            LocalDateTime dateTime = now.atZone(AssessmentTimePolicy.LONDON).toLocalDateTime().withSecond(0).withNano(0);
            return new QueryTime(mode, "Departing", dateTime);
        }
        if (!List.of("leave", "arrive").contains(mode) || request.travelDate() == null || request.travelTime() == null) {
            throw new IllegalArgumentException("Choose a date and time for leave or arrive searches.");
        }
        LocalDateTime dateTime;
        try { dateTime = LocalDateTime.of(request.travelDate(), LocalTime.parse(request.travelTime())); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Use a valid date and 24-hour time."); }
        AssessmentTimePolicy.requireUnambiguousLondonTime(dateTime);
        return new QueryTime(mode, "leave".equals(mode) ? "Departing" : "Arriving", dateTime);
    }

    static String normalize(String postcode) {
        String location = postcode.replaceAll("\\s+", " ").trim();
        String compact = location.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (compact.matches("GIR0AA|[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}")) {
            return compact.substring(0, compact.length() - 3) + " " + compact.substring(compact.length() - 3);
        }
        return location;
    }
    private static Integer integer(JsonNode value) { return value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= 0 ? value.asInt() : null; }
    private static Integer stopCount(JsonNode leg) {
        JsonNode stops = leg.path("path").path("stopPoints");
        if (!stops.isArray() || stops.isEmpty()) return null;
        return stops.size();
    }
    private static LocalDateTime time(JsonNode value) {
        String text = value.asText("");
        try { return OffsetDateTime.parse(text).atZoneSameInstant(AssessmentTimePolicy.LONDON).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {
            try { return LocalDateTime.parse(text); } catch (DateTimeParseException ex) { return null; }
        }
    }
}
