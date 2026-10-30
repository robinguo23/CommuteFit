package uk.co.commutefit.workcommute;

import jakarta.validation.constraints.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import uk.co.commutefit.assessment.AssessmentTimePolicy;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.journey.lookup.JourneyLookupRequest;
import uk.co.commutefit.journey.lookup.JourneyLookupService;

@Service
public class WorkCommuteService {
    public record Request(@NotBlank @Size(max=120) String originLocation, String selectedOriginCandidateId,
                          @NotNull @Min(1) @Max(1440) Integer maxOneWayMinutes,
                          List<DayOfWeek> availabilityDays, String availableFrom, String availableUntil,
                          @NotBlank @Size(max=120) String workplaceLocation, String selectedWorkplaceCandidateId,
                          List<DayOfWeek> workDays, String shiftStart, String shiftEnd) {}
    public record Route(String id, LocalDateTime departure, LocalDateTime arrival, Integer durationMinutes,
                        int transfers, List<JourneyLeg> legs) {}
    public record Direction(String status, LocalDateTime targetTime, Integer bestDurationMinutes,
                            List<Route> routes, String reason) {}
    public record DayResult(LocalDate date, DayOfWeek day, Direction outbound, Direction returning) {}
    public record Result(String status, String scheduleStatus, String scheduleReason, int maxOneWayMinutes,
                         List<DayResult> days, List<String> reasons, String timezone) {}
    public record ScenarioRequest(@jakarta.validation.Valid @NotNull Request journey, @NotNull LocalDate date) {}

    private final JourneyLookupService journeys;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public WorkCommuteService(JourneyLookupService journeys) { this(journeys, Clock.system(AssessmentTimePolicy.LONDON)); }
    WorkCommuteService(JourneyLookupService journeys, Clock clock) { this.journeys=journeys; this.clock=clock; }

    public DayResult assessScenario(ScenarioRequest scenario) {
        Request request = scenario.journey();
        validate(request);
        if (scenario.date() == null || blankToNull(request.shiftStart()) == null || blankToNull(request.shiftEnd()) == null)
            throw new IllegalArgumentException("Enter a date and both shift times.");
        LocalTime start = LocalTime.parse(request.shiftStart());
        LocalTime end = LocalTime.parse(request.shiftEnd());
        LocalDate date = scenario.date();
        LocalDate endDate = end.isBefore(start) ? date.plusDays(1) : date;
        return new DayResult(date, date.getDayOfWeek(), scenarioDirection(request, date, start, "arrive", false),
            scenarioDirection(request, endDate, end, "leave", true));
    }

    private Direction scenarioDirection(Request request, LocalDate date, LocalTime time, String mode, boolean returning) {
        LocalDateTime target = date.atTime(time);
        try {
            return direction(lookup(request, date, time, mode, returning), target, request.maxOneWayMinutes());
        } catch (uk.co.commutefit.common.ProviderUnavailableException | uk.co.commutefit.common.RateLimitedException ex) {
            return new Direction("unknown", target, null, List.of(), "Journey service unavailable; please retry.");
        }
    }

    public Result assess(Request request) {
        validate(request);
        List<DayOfWeek> workDays = distinct(request.workDays());
        List<DayOfWeek> availableDays = distinct(request.availabilityDays());
        String shiftStart = blankToNull(request.shiftStart());
        String shiftEnd = blankToNull(request.shiftEnd());
        String availableFrom = blankToNull(request.availableFrom());
        String availableUntil = blankToNull(request.availableUntil());
        String scheduleStatus = scheduleStatus(workDays, shiftStart, shiftEnd, availableDays, availableFrom, availableUntil);
        String scheduleReason = scheduleReason(scheduleStatus);
        LocalDateTime nowLondon = LocalDateTime.now(clock);
        LocalDate today = nowLondon.toLocalDate();
        List<DayResult> days = new ArrayList<>();
        for (DayOfWeek day : workDays) {
            int offset = Math.floorMod(day.getValue() - today.getDayOfWeek().getValue(), 7);
            LocalDate date = today.plusDays(offset);
            if (shiftStart == null || shiftEnd == null) continue;
            LocalTime start = LocalTime.parse(shiftStart);
            LocalTime end = LocalTime.parse(shiftEnd);
            if (date.equals(today) && !start.isAfter(nowLondon.toLocalTime())) date = date.plusDays(7);
            LocalDate endDate = end.isBefore(start) ? date.plusDays(1) : date;
            var out = lookup(request, date, start, "arrive", false);
            var back = lookup(request, endDate, end, "leave", true);
            days.add(new DayResult(date, day, direction(out, date.atTime(start), request.maxOneWayMinutes()), direction(back, endDate.atTime(end), request.maxOneWayMinutes())));
        }
        List<String> reasons = new ArrayList<>();
        if ("incompatible".equals(scheduleStatus)) reasons.add(scheduleReason);
        if (days.stream().anyMatch(d -> "over-limit".equals(d.outbound().status()) || "over-limit".equals(d.returning().status()))) reasons.add("At least one confirmed route exceeds the one-way commute limit.");
        if (days.isEmpty()) reasons.add("Add the job's work days and shift start/end times to check routes.");
        else if (days.stream().anyMatch(d -> "unknown".equals(d.outbound().status()) || "unknown".equals(d.returning().status()))) reasons.add("TfL did not provide enough route information for every direction and work day.");
        if ("unknown".equals(scheduleStatus)) reasons.add(scheduleReason);
        boolean failed = days.stream().anyMatch(d -> "over-limit".equals(d.outbound().status()) || "over-limit".equals(d.returning().status()));
        boolean complete = !days.isEmpty() && "compatible".equals(scheduleStatus) && days.stream().allMatch(d -> "within-limit".equals(d.outbound().status()) && "within-limit".equals(d.returning().status()));
        if (complete) reasons.add("Both commute directions and the job schedule fit the limits you entered.");
        return new Result(failed ? "unsuitable" : complete ? "suitable" : "needs-review", scheduleStatus, scheduleReason,
            request.maxOneWayMinutes(), List.copyOf(days), List.copyOf(reasons), AssessmentTimePolicy.LONDON.getId());
    }

    private JourneyLookupService.Result lookup(Request request, LocalDate date, LocalTime time, String mode, boolean returning) {
        String from = returning ? request.workplaceLocation() : request.originLocation();
        String to = returning ? request.originLocation() : request.workplaceLocation();
        String fromToken = returning ? request.selectedWorkplaceCandidateId() : request.selectedOriginCandidateId();
        String toToken = returning ? request.selectedOriginCandidateId() : request.selectedWorkplaceCandidateId();
        try {
            return journeys.lookup(new JourneyLookupRequest(from, to, mode, date, time.format(DateTimeFormatter.ofPattern("HH:mm")), fromToken, toToken));
        } catch (uk.co.commutefit.common.LocationDisambiguationException ex) {
            String field = returning
                ? ("origin".equals(ex.candidateField()) ? "workplace" : "origin")
                : ("origin".equals(ex.candidateField()) ? "origin" : "workplace");
            throw new uk.co.commutefit.common.LocationDisambiguationException(field, ex.candidates());
        }
    }

    private static Direction direction(JourneyLookupService.Result result, LocalDateTime target, int limit) {
        if (result.routes().isEmpty()) return new Direction("unknown", target, null, List.of(), "TfL returned no route; this does not confirm that the commute is impossible.");
        List<Route> routes = result.routes().stream().map(r -> new Route(r.id(), r.departure(), r.arrival(), r.durationMinutes(), r.transfers(), r.legs())).toList();
        Integer best = routes.stream().map(Route::durationMinutes).filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
        if (best == null) return new Direction("unknown", target, null, routes, "Route duration was not available.");
        if (best > limit && routes.stream().anyMatch(r -> r.durationMinutes() == null)) return new Direction("unknown", target, best, routes, "At least one returned route has no duration, so the limit could not be confirmed.");
        return new Direction(best <= limit ? "within-limit" : "over-limit", target, best, routes,
            best <= limit ? "A returned option is within the one-way limit." : "Every returned option is longer than the one-way limit.");
    }
    private static String scheduleStatus(List<DayOfWeek> work, String start, String end, List<DayOfWeek> available, String from, String until) {
        if (work.isEmpty() || start == null || end == null || available.isEmpty() || from == null || until == null) return "unknown";
        if (!available.containsAll(work)) return "incompatible";
        int s=minutes(start), e=minutes(end), a=minutes(from), b=minutes(until);
        if (e < s && work.stream().anyMatch(day -> !available.contains(day.plus(1)))) return "incompatible";
        if (e < s) e += 1440;
        if (b <= a) b += 1440;
        if (s < a && e <= b) { s += 1440; e += 1440; }
        return s >= a && e <= b ? "compatible" : "incompatible";
    }
    private static String scheduleReason(String status) {
        return switch(status) {
            case "compatible" -> "The job days and full shift fit the availability you entered.";
            case "incompatible" -> "At least one job day or shift time is outside the availability you entered.";
            default -> "Add the job days, shift times, and your weekly availability to confirm schedule compatibility.";
        };
    }
    private static int minutes(String t) { LocalTime time=LocalTime.parse(t); return time.getHour()*60+time.getMinute(); }
    private static List<DayOfWeek> distinct(List<DayOfWeek> days) { return days == null ? List.of() : days.stream().filter(Objects::nonNull).distinct().toList(); }
    private static void validate(Request r) {
        if (r == null || r.originLocation() == null || r.originLocation().isBlank() || r.workplaceLocation() == null || r.workplaceLocation().isBlank()) throw new IllegalArgumentException("Enter both locations.");
        if (r.maxOneWayMinutes() == null || r.maxOneWayMinutes() < 1 || r.maxOneWayMinutes() > 1440) throw new IllegalArgumentException("Enter a one-way limit between 1 and 1440 minutes.");
        validTime(r.availableFrom()); validTime(r.availableUntil()); validTime(r.shiftStart()); validTime(r.shiftEnd());
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static void validTime(String value) { if (value != null && !value.isBlank()) try { if (!value.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) throw new IllegalArgumentException(); LocalTime.parse(value); } catch (RuntimeException ex) { throw new IllegalArgumentException("Use 24-hour HH:mm times."); } }
}
