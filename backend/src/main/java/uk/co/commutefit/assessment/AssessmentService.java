package uk.co.commutefit.assessment;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import uk.co.commutefit.assessment.AssessmentResponse.DataBasis;
import uk.co.commutefit.assessment.AssessmentResponse.DayAssessment;
import uk.co.commutefit.assessment.AssessmentResponse.DirectionAssessment;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyOption;
import uk.co.commutefit.common.ProfileValidationException;
import uk.co.commutefit.fare.FareAssessmentService;
import uk.co.commutefit.fare.FareSummary;
import uk.co.commutefit.journey.tfl.TflJourneyService;
import uk.co.commutefit.profile.ProfileDto;
import uk.co.commutefit.profile.ProfileService;

@Service
@org.springframework.context.annotation.Profile("legacy")
public class AssessmentService {
    private static final String PROVIDER = "Transport for London Journey Planner";
    private static final String PROVIDER_SOURCE = "https://tfl.gov.uk/plan-a-journey/";
    private final ProfileService profiles;
    private final TflJourneyService journeys;
    private final FareAssessmentService fares;

    public AssessmentService(ProfileService profiles, TflJourneyService journeys, FareAssessmentService fares) {
        this.profiles = profiles;
        this.journeys = journeys;
        this.fares = fares;
    }

    public AssessmentResponse assess(AssessmentRequest request) {
        validateRequest(request);
        ProfileDto profile = profiles.get();
        if (profile.departurePostcode() == null || profile.departurePostcode().isBlank()) {
            throw new ProfileValidationException("Save your departure postcode before assessing a commute. / 请先保存出发 postcode，再评估通勤。");
        }
        int buffer = request.isArrivalBufferSupplied()
            ? (request.getArrivalBufferMinutes() == null ? 30 : request.getArrivalBufferMinutes())
            : profile.arrivalBufferMinutes();
        Integer maxMinutes = request.isMaxOneWaySupplied() ? request.getMaxOneWayMinutes() : profile.maxOneWayMinutes();
        String payment = request.isPaymentMethodSupplied() ? request.getPaymentMethod() : profile.preferredPaymentMethod();
        List<String> selected = request.isConcessionsSupplied()
            ? (request.getConcessions() == null ? List.of() : request.getConcessions())
            : profile.selectedConcessions();
        if (buffer < 0 || buffer > 240) throw new IllegalArgumentException("Arrival buffer must be between 0 and 240 minutes.");
        if (maxMinutes != null && (maxMinutes < 1 || maxMinutes > 1440)) throw new IllegalArgumentException("Maximum one-way time is out of range.");
        if (payment != null && !List.of("apple-pay-contactless", "bank-card-contactless", "oyster", "other").contains(payment)) throw new IllegalArgumentException("Payment method is not supported.");
        if (selected.stream().anyMatch(id -> !profiles.supportsConcession(id))) throw new IllegalArgumentException("Concession selection is not supported.");

        String departureQuery = profile.departurePostcode();
        String destinationQuery = request.getDestinationText();
        String resolvedOrigin = journeys.resolveOrigin(departureQuery, request.getSelectedOriginCandidateId());
        String resolvedDestination = journeys.resolveDestination(destinationQuery, request.getSelectedCandidateId());
        LocalDate shiftDate = request.getReferenceDate();
        LocalTime earliest = LocalTime.parse(request.getEarliestShiftStart());
        LocalTime finish = LocalTime.parse(request.getExpectedLatestFinish());
        List<LocalDate> dates = AssessmentTimePolicy.assessmentDates(shiftDate, request.isCheckWeekend());
        List<DayAssessment> assessments = new ArrayList<>();
        boolean anyMissingFare = false;
        for (LocalDate date : dates) {
            AssessmentTimePolicy.requireUnambiguousLondonTime(LocalDateTime.of(date, earliest));
            LocalDateTime outboundTarget = AssessmentTimePolicy.outboundTarget(date, earliest, buffer);
            LocalDateTime returnTarget = AssessmentTimePolicy.returnBoundary(date, earliest, finish);
            AssessmentTimePolicy.requireUnambiguousLondonTime(outboundTarget);
            AssessmentTimePolicy.requireUnambiguousLondonTime(returnTarget);
            DirectionAssessment outbound = assessDirection(resolvedOrigin, resolvedDestination, departureQuery, destinationQuery,
                "origin", "destination", date, outboundTarget, "Arriving", maxMinutes, payment, selected);
            DirectionAssessment returning = assessDirection(resolvedDestination, resolvedOrigin, destinationQuery, departureQuery,
                "destination", "origin", returnTarget.toLocalDate(), returnTarget, "Departing", maxMinutes, payment, selected);
            anyMissingFare |= outbound.options().stream().anyMatch(option -> option.fareSummary().routeFare().amount() == null)
                || returning.options().stream().anyMatch(option -> option.fareSummary().routeFare().amount() == null);
            assessments.add(new DayAssessment(date, date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH), outbound, returning));
        }
        boolean anyOptions = assessments.stream().anyMatch(day -> !day.outbound().options().isEmpty() || !day.returning().options().isEmpty());
        String status = !anyOptions ? "no-scheduled-option" : anyMissingFare ? "partial" : "available";
        AssessmentResponse result = new AssessmentResponse(status, assessments, new DataBasis(PROVIDER, Instant.now(), AssessmentTimePolicy.LONDON.getId()), List.of(
            "Scheduled information only; no realtime punctuality prediction or arrival guarantee.",
            "Assessment inputs are sent to TfL for route planning and are not saved by this app."
        ));
        journeys.removeCandidateTokens(request.getSelectedCandidateId(), request.getSelectedOriginCandidateId());
        return result;
    }

    private DirectionAssessment assessDirection(String from, String to, String fromQuery, String toQuery,
                                                String fromCandidateField, String toCandidateField, LocalDate date, LocalDateTime target,
                                                String timeIs, Integer maxMinutes, String payment, List<String> selected) {
        JsonNode response = journeys.journeys(from, to, fromQuery, toQuery, fromCandidateField, toCandidateField,
            date, target.toLocalTime(), timeIs);
        JsonNode values = response.path("journeys");
        List<JourneyOption> options = new ArrayList<>();
        if (values.isArray()) for (int index = 0; index < values.size(); index++) {
            JsonNode value = values.get(index);
            List<JourneyLeg> legs = mapLegs(value.path("legs"));
            Integer duration = integerOrNull(value, "duration");
            LocalDateTime start = parseDateTime(value.path("startDateTime").asText(null));
            LocalDateTime arrival = parseDateTime(value.path("arrivalDateTime").asText(null));
            if (start == null) start = legs.stream().map(JourneyLeg::departureDateTime).filter(v -> v != null).findFirst().orElse(null);
            if (arrival == null) arrival = legs.stream().map(JourneyLeg::arrivalDateTime).filter(v -> v != null).reduce((first, second) -> second).orElse(null);
            FareSummary fare = fares.summarize(value, start == null ? target : start, payment, selected, legs);
            options.add(new JourneyOption(
                Integer.toHexString(value.toString().hashCode()) + "-" + index, start, arrival, duration,
                maxMinutes == null || duration == null ? null : duration <= maxMinutes, legs, fare, PROVIDER_SOURCE
            ));
        }
        String state = options.isEmpty() ? "no-scheduled-option" : "available";
        String en = options.isEmpty() ? "TfL returned no scheduled journey for this query. This does not confirm that no service exists; try another reference time or check TfL directly." : null;
        String zh = options.isEmpty() ? "TfL 对这组日期和时间没有返回计划路线。这不能证明当天完全没有班次；可调整参考时间或直接向 TfL 核实。" : null;
        return new DirectionAssessment(target, options, state, en, zh);
    }

    private List<JourneyLeg> mapLegs(JsonNode input) {
        List<JourneyLeg> legs = new ArrayList<>();
        if (!input.isArray()) return legs;
        for (JsonNode leg : input) {
            String mode = leg.path("mode").path("name").asText("");
            String route = leg.path("routeOptions").path(0).path("name").asText("");
            String summary = leg.path("instruction").path("summary").asText("");
            if (summary.isBlank()) summary = route.isBlank() ? mode : mode + " " + route;
            legs.add(new JourneyLeg(mode, route, leg.path("departurePoint").path("commonName").asText(""), leg.path("arrivalPoint").path("commonName").asText(""),
                parseDateTime(leg.path("departureTime").asText(null)), parseDateTime(leg.path("arrivalTime").asText(null)), integerOrNull(leg, "duration"), summary, stopCount(leg)));
        }
        return legs;
    }

    private static Integer stopCount(JsonNode leg) {
        JsonNode stops = leg.path("path").path("stopPoints");
        return stops.isArray() && !stops.isEmpty() ? stops.size() : null;
    }

    private void validateRequest(AssessmentRequest request) {
        if (request == null || request.getReferenceDate() == null || request.getDestinationText() == null || request.getEarliestShiftStart() == null || request.getExpectedLatestFinish() == null)
            throw new IllegalArgumentException("Required assessment fields are missing.");
        try { LocalTime.parse(request.getEarliestShiftStart()); LocalTime.parse(request.getExpectedLatestFinish()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Times must use 24-hour HH:mm."); }
        if (request.getDestinationText().length() > 180) throw new IllegalArgumentException("Destination is too long.");
    }

    private static Integer integerOrNull(JsonNode node, String field) { return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null; }
    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try { return OffsetDateTime.parse(value).atZoneSameInstant(AssessmentTimePolicy.LONDON).toLocalDateTime(); }
        catch (DateTimeParseException ignored) { try { return LocalDateTime.parse(value); } catch (DateTimeParseException ex) { return null; } }
    }
}
