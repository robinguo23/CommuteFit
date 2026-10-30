package uk.co.commutefit.workcommute;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import uk.co.commutefit.assessment.AssessmentResponse.JourneyLeg;
import uk.co.commutefit.journey.lookup.JourneyLookupRequest;
import uk.co.commutefit.journey.lookup.JourneyLookupService;

class WorkCommuteServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T06:00:00Z"), ZoneId.of("Europe/London"));
    private static final LocalDate WEDNESDAY = LocalDate.of(2026,10,28);

    @Test void queriesArrivalForOutboundAndDepartureForReturnAndReturnsSuitableWhenBothFit() {
        FakePlanner planner = new FakePlanner(35, false);
        var result = new WorkCommuteService(planner, CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.WEDNESDAY), "09:00", "17:00", "08:00", "18:00", 45));
        assertThat(result.status()).isEqualTo("suitable");
        assertThat(result.days()).hasSize(1);
        assertThat(result.days().getFirst().outbound().bestDurationMinutes()).isEqualTo(35);
        assertThat(result.days().getFirst().returning().routes().getFirst().transfers()).isEqualTo(1);
        assertThat(planner.requests).extracting(JourneyLookupRequest::timeMode).containsExactly("arrive","leave");
        assertThat(planner.requests).extracting(JourneyLookupRequest::travelDate).containsExactly(WEDNESDAY,WEDNESDAY);
        assertThat(planner.requests.get(0).travelTime()).isEqualTo("09:00");
        assertThat(planner.requests.get(1).travelTime()).isEqualTo("17:00");
        assertThat(planner.requests.get(0).selectedOriginCandidateId()).isEqualTo("home-token");
        assertThat(planner.requests.get(0).selectedDestinationCandidateId()).isEqualTo("work-token");
        assertThat(planner.requests.get(1).selectedOriginCandidateId()).isEqualTo("work-token");
        assertThat(planner.requests.get(1).selectedDestinationCandidateId()).isEqualTo("home-token");
    }

    @Test void aConfirmedOverLimitDirectionIsUnsuitable() {
        var result = new WorkCommuteService(new FakePlanner(61,false), CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.WEDNESDAY), "09:00", "17:00", "08:00", "18:00", 60));
        assertThat(result.status()).isEqualTo("unsuitable");
        assertThat(result.days().getFirst().outbound().status()).isEqualTo("over-limit");
    }

    @Test void unavailableRouteIsUnknownAndNeedsReviewRatherThanUnsuitable() {
        var result = new WorkCommuteService(new FakePlanner(0,true), CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.WEDNESDAY), "09:00", "17:00", "08:00", "18:00", 60));
        assertThat(result.status()).isEqualTo("needs-review");
        assertThat(result.days().getFirst().outbound().status()).isEqualTo("unknown");
        assertThat(result.days().getFirst().outbound().reason()).contains("does not confirm");
    }

    @Test void plannerServiceFailureIsPropagatedForAnExplicitUnavailableResponse() {
        FakePlanner planner = new FakePlanner(0,false) {
            @Override public Result lookup(JourneyLookupRequest request) { throw new uk.co.commutefit.common.ProviderUnavailableException(null); }
        };
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new WorkCommuteService(planner,CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY),List.of(DayOfWeek.WEDNESDAY),"09:00","17:00","08:00","18:00",60)))
            .isInstanceOf(uk.co.commutefit.common.ProviderUnavailableException.class);
    }

    @Test void scheduleMismatchNeedsConfirmation() {
        var result = new WorkCommuteService(new FakePlanner(20,false), CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.MONDAY), "09:00", "17:00", "08:00", "18:00", 60));
        assertThat(result.status()).isEqualTo("needs-review");
        assertThat(result.scheduleStatus()).isEqualTo("incompatible");
    }

    @Test void scenarioUsesExactDateAndNextDayForOvernightReturn() {
        FakePlanner planner = new FakePlanner(20,false);
        var input = request(List.of(), List.of(), "22:00", "06:00", "", "", 60);
        var result = new WorkCommuteService(planner,CLOCK).assessScenario(new WorkCommuteService.ScenarioRequest(input,WEDNESDAY));
        assertThat(result.date()).isEqualTo(WEDNESDAY);
        assertThat(planner.requests).extracting(JourneyLookupRequest::travelDate).containsExactly(WEDNESDAY,WEDNESDAY.plusDays(1));
    }

    @Test void scenarioPreservesReturnWhenOutboundServiceFails() {
        FakePlanner planner = new FakePlanner(20,false) {
            @Override public Result lookup(JourneyLookupRequest request) {
                if ("arrive".equals(request.timeMode())) throw new uk.co.commutefit.common.ProviderUnavailableException(null);
                return super.lookup(request);
            }
        };
        var input = request(List.of(),List.of(),"09:00","17:00","","",60);
        var result = new WorkCommuteService(planner,CLOCK).assessScenario(new WorkCommuteService.ScenarioRequest(input,WEDNESDAY));
        assertThat(result.outbound().status()).isEqualTo("unknown");
        assertThat(result.returning().status()).isEqualTo("within-limit");
    }

    @Test void scenarioHttpEndpointAcceptsExplicitDateAndReturnsFareFreeDirections() throws Exception {
        FakePlanner planner = new FakePlanner(25,false);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(new JobListingController(null,new WorkCommuteService(planner,CLOCK)))
            .setControllerAdvice(new uk.co.commutefit.common.ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/work-commute/scenario")
            .contentType("application/json")
            .content("""
                {"date":"2026-10-28","journey":{"originLocation":"DA18 4DE","workplaceLocation":"E1 6AN","maxOneWayMinutes":60,"shiftStart":"22:00","shiftEnd":"06:00"}}
                """))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.date").value("2026-10-28"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.outbound.status").value("within-limit"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.returning.targetTime").value("2026-10-29T06:00:00"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.outbound.routes[0].totalFare").doesNotExist());
    }

    @Test void missingJobDaysLeavesVerdictPendingWithoutPlannerCall() {
        FakePlanner planner = new FakePlanner(0,false);
        var result = new WorkCommuteService(planner, CLOCK).assess(request(List.of(), List.of(DayOfWeek.MONDAY), "09:00", "17:00", "08:00", "18:00", 60));
        assertThat(result.status()).isEqualTo("needs-review");
        assertThat(result.days()).isEmpty();
        assertThat(planner.requests).isEmpty();
    }

    @Test void blankShiftTimesRemainIncompleteInsteadOfBecomingValidationErrors() {
        FakePlanner planner = new FakePlanner(0,false);
        var input = new WorkCommuteService.Request("DA18 4DE",null,60,List.of(DayOfWeek.MONDAY),"08:00","18:00","King's Cross",null,List.of(DayOfWeek.MONDAY),"","");
        var result = new WorkCommuteService(planner,CLOCK).assess(input);
        assertThat(result.status()).isEqualTo("needs-review");
        assertThat(result.scheduleStatus()).isEqualTo("unknown");
        assertThat(planner.requests).isEmpty();
    }

    @Test void overnightShiftLooksUpReturnOnTheFollowingDate() {
        FakePlanner planner = new FakePlanner(35,false);
        new WorkCommuteService(planner, CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.WEDNESDAY), "22:00", "06:00", "21:00", "07:00", 45));
        assertThat(planner.requests).extracting(JourneyLookupRequest::travelDate).containsExactly(WEDNESDAY,WEDNESDAY.plusDays(1));
    }

    @Test void overnightAvailabilityRequiresTheFollowingDay() {
        var service = new WorkCommuteService(new FakePlanner(20,false),CLOCK);
        var missing = service.assess(request(List.of(DayOfWeek.WEDNESDAY),List.of(DayOfWeek.WEDNESDAY),"22:00","06:00","21:00","07:00",60));
        var complete = service.assess(request(List.of(DayOfWeek.WEDNESDAY),List.of(DayOfWeek.WEDNESDAY,DayOfWeek.THURSDAY),"22:00","06:00","21:00","07:00",60));
        assertThat(missing.scheduleStatus()).isEqualTo("incompatible");
        assertThat(missing.status()).isEqualTo("needs-review");
        assertThat(complete.scheduleStatus()).isEqualTo("compatible");
    }

    @Test void aShiftThatAlreadyStartedTodayIsCheckedNextWeek() {
        FakePlanner planner = new FakePlanner(25,false);
        new WorkCommuteService(planner, CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.WEDNESDAY), "06:00", "07:00", "05:00", "08:00", 45));
        assertThat(planner.requests).extracting(JourneyLookupRequest::travelDate).containsExactly(WEDNESDAY.plusDays(7),WEDNESDAY.plusDays(7));
    }

    @Test void returnDirectionDisambiguationIsReportedAgainstTheWorkplaceField() {
        FakePlanner planner = new FakePlanner(20,false) {
            @Override public Result lookup(JourneyLookupRequest request) {
                if ("leave".equals(request.timeMode())) throw new uk.co.commutefit.common.LocationDisambiguationException("origin", List.of(Map.of("id","candidate","name","Work station")));
                return super.lookup(request);
            }
        };
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new WorkCommuteService(planner, CLOCK).assess(request(List.of(DayOfWeek.WEDNESDAY), List.of(DayOfWeek.WEDNESDAY), "09:00", "17:00", "08:00", "18:00", 60)))
            .isInstanceOf(uk.co.commutefit.common.LocationDisambiguationException.class)
            .satisfies(error -> assertThat(((uk.co.commutefit.common.LocationDisambiguationException) error).candidateField()).isEqualTo("workplace"));
    }

    private static WorkCommuteService.Request request(List<DayOfWeek> jobDays,List<DayOfWeek> available,String start,String end,String from,String until,int limit) {
        return new WorkCommuteService.Request("DA18 4DE","home-token",limit,available,from,until,"King's Cross","work-token",jobDays,start,end);
    }
    private static class FakePlanner extends JourneyLookupService {
        private final int duration; private final boolean noRoutes; private final List<JourneyLookupRequest> requests = new ArrayList<>();
        FakePlanner(int duration, boolean noRoutes) { super(null); this.duration=duration; this.noRoutes=noRoutes; }
        @Override public Result lookup(JourneyLookupRequest request) {
            requests.add(request);
            if(noRoutes) return new Result("no-routes","A","B",Instant.now(),"Europe/London",request.timeMode(),request.travelDate().atTime(LocalTime.parse(request.travelTime())),List.of());
            var legs=List.of(new JourneyLeg("tube","Jubilee","Home station","Work station",request.travelDate().atTime(8,30),request.travelDate().atTime(9,0),30,"Tube",4));
            var route=new Route("r1",request.travelDate().atTime(8,25),request.travelDate().atTime(9,0),duration,1,legs,BigDecimal.valueOf(3.10),"available",List.of("Peak"),"GBP");
            return new Result("available",request.originPostcode(),request.destinationPostcode(),Instant.now(),"Europe/London",request.timeMode(),request.travelDate().atTime(LocalTime.parse(request.travelTime())),List.of(route));
        }
    }
}
