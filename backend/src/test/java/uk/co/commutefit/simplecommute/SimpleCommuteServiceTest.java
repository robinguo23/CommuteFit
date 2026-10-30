package uk.co.commutefit.simplecommute;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import uk.co.commutefit.common.ProviderUnavailableException;
import uk.co.commutefit.common.RateLimitedException;
import uk.co.commutefit.journey.lookup.*;
import uk.co.commutefit.simplecommute.SimpleCommuteService.*;

class SimpleCommuteServiceTest {
    static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"),ZoneOffset.UTC);
    static final LocalDate FIRST=LocalDate.of(2026,11,2);
    static Request request(String...types){return new Request("da184de","se186fl",60,List.of(types),List.of(),null,null);}
    static JourneyLookupService.Route route(JourneyLookupRequest r,int duration){
        var target=r.travelDate().atTime(LocalTime.parse(r.travelTime()));
        var depart="arrive".equals(r.timeMode())?target.minusMinutes(duration):target;
        return new JourneyLookupService.Route("r",depart,depart.plusMinutes(duration),duration,0,List.of(),null,"unavailable",List.of(),"GBP");
    }
    static class Planner extends JourneyLookupService {
        final List<JourneyLookupRequest> calls=new ArrayList<>();
        Function<JourneyLookupRequest,List<JourneyLookupService.Route>> answer=r->List.of(route(r,20));
        Planner(){super(null);}
        public Result lookup(JourneyLookupRequest r){calls.add(r);return new Result("available",r.originPostcode(),r.destinationPostcode(),CLOCK.instant(),"Europe/London",r.timeMode(),r.travelDate().atTime(LocalTime.parse(r.travelTime())),answer.apply(r));}
    }
    SimpleCommuteService service(Planner p){return new SimpleCommuteService(p,CLOCK);}
    @Test void firstHitStopsEachGroupAndOnlySelectedTypeIsQueried(){
        var p=new Planner();p.answer=r->List.of(route(r,"02:30".equals(r.travelTime())?90:20));
        var result=service(p).assess(request("early"));
        assertThat(result.days()).hasSize(7);assertThat(result.status()).isEqualTo("suitable");
        assertThat(p.calls).hasSize(21);
        assertThat(p.calls.stream().map(JourneyLookupRequest::travelTime).distinct()).containsExactly("02:30","03:30","06:10");
    }
    @Test void firstMorningAndLastNightKeepRealDatesAndReuseWholeMorningGroup(){
        var p=new Planner();var result=service(p).assess(request("early","night"));
        var returns=p.calls.stream().filter(r->r.timeMode().equals("leave")).toList();
        assertThat(returns).hasSize(8);
        assertThat(returns.getFirst().travelDate()).isEqualTo(FIRST);
        assertThat(returns.getLast().travelDate()).isEqualTo(FIRST.plusDays(7));
        assertThat(p.calls.stream().filter(r->r.travelDate().equals(FIRST.plusDays(7)))).allMatch(r->r.timeMode().equals("leave"));
        assertThat(result.days()).hasSize(7);assertThat(result.reusedPoints()).isGreaterThanOrEqualTo(6);
    }
    @Test void earlyStoppingDoesNotPretend0630WasQueriedAndDayIsStillChecked(){
        var p=new Planner();service(p).assess(request("early","day"));
        assertThat(p.calls.stream().filter(r->r.travelDate().equals(FIRST)&&r.timeMode().equals("arrive")).map(JourneyLookupRequest::travelTime))
            .containsExactly("02:30","06:30");
    }
    @Test void anActuallyQueried0630IsReusedBeforeDay0730(){
        var p=new Planner();p.answer=r->List.of(route(r,r.timeMode().equals("arrive")&&!r.travelTime().equals("07:30")?90:20));
        var result=service(p).assess(request("early","day"));
        assertThat(p.calls.stream().filter(r->r.travelDate().equals(FIRST)&&r.timeMode().equals("arrive")).map(JourneyLookupRequest::travelTime))
            .containsExactly("02:30","03:30","04:30","05:30","06:30","07:30");
        assertThat(result.status()).isEqualTo("suitable");assertThat(result.issues()).anyMatch(i->i.type().equals("early")&&i.direction().equals("outbound"));
    }
    @Test void dayOnlyDoesNotGenerateEarlyNightOrDayEight(){
        var p=new Planner();var result=service(p).assess(request("day"));
        assertThat(p.calls).hasSize(14);assertThat(p.calls).allMatch(r->!r.travelDate().isAfter(FIRST.plusDays(6)));
        assertThat(result.positiveScopes()).hasSize(7);
    }
    @Test void fastestQualifiedRouteIsChosenAfterTimeFiltering(){
        var p=new Planner();p.answer=r->{var t=r.travelDate().atTime(LocalTime.parse(r.travelTime()));
            var invalid=new JourneyLookupService.Route("invalid",r.timeMode().equals("arrive")?t:t.minusMinutes(1),r.timeMode().equals("arrive")?t.plusMinutes(5):t.plusMinutes(4),5,0,List.of(),null,"unavailable",List.of(),"GBP");
            return List.of(route(r,90),invalid,route(r,25),route(r,15));};
        var result=service(p).assess(request("day"));
        assertThat(result.days().getFirst().checks().getFirst().outbound().route().durationMinutes()).isEqualTo(15);
        assertThat(result.days().getFirst().checks().getFirst().returning().route().durationMinutes()).isEqualTo(15);
    }
    @Test void noMinimumWorkDurationIsImposedOnReferenceCoverage(){
        var p=new Planner();p.answer=r->List.of(route(r,r.timeMode().equals("arrive")&&!r.travelTime().equals("06:30")?90:20));
        var c=service(p).assess(request("early")).days().getFirst().checks().getFirst();
        assertThat(c.status()).isEqualTo("suitable");assertThat(c.outbound().targetTime().toLocalTime()).isEqualTo(LocalTime.of(6,30));
        assertThat(c.returning().targetTime().toLocalTime()).isEqualTo(LocalTime.of(6,10));
    }
    @Test void onlyOneFeasibleDayStillGivesScopedPositiveVerdict(){
        var p=new Planner();p.answer=r->List.of(route(r,r.travelDate().equals(FIRST.plusDays(2))?20:90));
        var result=service(p).assess(request("day"));assertThat(result.status()).isEqualTo("suitable");
        assertThat(result.positiveScopes()).extracting(Scope::date).containsExactly(FIRST.plusDays(2));
    }
    @Test void allConfirmedFailuresAreNegativeAndNetworkFailuresAreUnknown(){
        var p=new Planner();p.answer=r->List.of(route(r,90));assertThat(service(p).assess(request("day")).status()).isEqualTo("unsuitable");
        p.answer=r->{throw new ProviderUnavailableException(null);};p.calls.clear();
        assertThat(service(p).assess(request("day")).status()).isEqualTo("needs-review");assertThat(p.calls.size()).isLessThanOrEqualTo(3);
    }
    @Test void providerFailureInOutboundDoesNotEraseAWorkingReturn(){
        var p=new Planner();p.answer=r->{if(r.timeMode().equals("arrive"))throw new ProviderUnavailableException(null);return List.of(route(r,20));};
        var result=service(p).assess(request("day"));var c=result.days().getFirst().checks().getFirst();
        assertThat(c.outbound().status()).isEqualTo("unknown");assertThat(c.returning().status()).isEqualTo("within-limit");
    }
    @Test void rateLimitStopsFurtherProviderRequests(){
        var p=new Planner();p.answer=r->{throw new RateLimitedException();};
        assertThat(service(p).assess(request("early","day","night")).status()).isEqualTo("needs-review");assertThat(p.calls).hasSize(1);
    }
    @Test void explicitOvernightShiftOverridesReferencesAndKeepsSevenSchedules(){
        var p=new Planner();var input=new Request("DA18 4DE","SE18 6FL",60,List.of("day"),List.of(new Shift("Job","22:00","06:00",List.of(DayOfWeek.MONDAY),null)),null,null);
        var result=service(p).assess(input);assertThat(result.mode()).isEqualTo("exact");assertThat(result.status()).isEqualTo("suitable");
        assertThat(p.calls).hasSize(2);assertThat(p.calls.get(1).travelDate()).isEqualTo(FIRST.plusDays(1));assertThat(p.calls.get(1).travelTime()).isEqualTo("06:00");
    }
    @Test void pastPointsTodayRemainUnknownRatherThanMovingToNextWeek(){
        var p=new Planner();var c=Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"),ZoneOffset.UTC);
        var result=new SimpleCommuteService(p,c).assess(request("day"));
        assertThat(result.scopeStart()).isEqualTo(FIRST);assertThat(result.days().getFirst().checks().getFirst().outbound().status()).isEqualTo("unknown");
        assertThat(p.calls).noneMatch(r->r.travelDate().equals(FIRST)&&r.timeMode().equals("arrive"));
    }
    @Test void missingDurationThenLaterCandidateCanStillMatch(){
        var p=new Planner();p.answer=r->{var good=route(r,20);return List.of(new JourneyLookupService.Route("r",good.departure(),good.arrival(),r.travelTime().equals("06:30")?null:20,0,List.of(),null,"unavailable",List.of(),"GBP"));};
        assertThat(service(p).assess(request("day")).days().getFirst().checks().getFirst().outbound().targetTime().toLocalTime()).isEqualTo(LocalTime.of(7,30));
    }
    @Test void validationRejectsInvalidInputAndDoesNotRequireAvailability(){
        assertThat(service(new Planner()).validate(request("day")).originPostcode()).isEqualTo("DA18 4DE");
        assertThatThrownBy(()->service(new Planner()).validate(request())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service(new Planner()).validate(new Request("London","SE18 6FL",60,List.of("day"),null,null,null))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void cancellationAndBudgetDoNotBecomeNegativeResults(){
        var p=new Planner();assertThatThrownBy(()->service(p).assess(request("day"),(a,b)->{},()->true)).isInstanceOf(CancellationException.class);
        assertThat(new SimpleCommuteService(p,CLOCK,0).assess(request("day")).status()).isEqualTo("needs-review");assertThat(p.calls).isEmpty();
    }
    @Test void nightSearchesLatestFirstAndMorningCanFallBackTo1010Only(){
        var p=new Planner();p.answer=r->List.of(route(r,r.timeMode().equals("arrive")?(r.travelTime().equals("21:30")?20:90):(r.travelTime().equals("10:10")?20:90)));
        var result=service(p).assess(request("night"));
        assertThat(p.calls.stream().filter(r->r.travelDate().equals(FIRST)).map(JourneyLookupRequest::travelTime)).containsExactly("22:30","21:30");
        assertThat(p.calls.stream().filter(r->r.travelDate().equals(FIRST.plusDays(7)))).extracting(JourneyLookupRequest::travelTime)
            .containsExactly("06:10","07:10","08:10","09:10","10:10");
        assertThat(result.status()).isEqualTo("suitable");
    }
    @Test void aMissingDurationDoesNotTurnOtherOverLimitOptionsIntoAConfirmedFailure(){
        var p=new Planner();p.answer=r->{var good=route(r,20);return List.of(route(r,90),new JourneyLookupService.Route("missing",good.departure(),good.arrival(),null,0,List.of(),null,"unavailable",List.of(),"GBP"));};
        assertThat(service(p).assess(request("day")).status()).isEqualTo("needs-review");
    }
    @Test void knownRequiredShiftsAreNotApprovedJustBecauseOneOtherShiftFits(){
        var p=new Planner();p.answer=r->List.of(route(r,r.travelDate().getDayOfWeek()==DayOfWeek.TUESDAY?90:20));
        var input=new Request("DA18 4DE","SE18 6FL",60,List.of("day"),List.of(
            new Shift("Mon","09:00","17:00",List.of(DayOfWeek.MONDAY),null),new Shift("Tue","09:00","17:00",List.of(DayOfWeek.TUESDAY),null)),null,null);
        var result=service(p).assess(input);assertThat(result.status()).isEqualTo("unsuitable");assertThat(result.positiveScopes()).hasSize(1);
    }
    @Test void availabilityWindowFiltersReferencesWithoutBecomingAnExactShift(){
        var p=new Planner();var input=new Request("DA18 4DE","SE18 6FL",60,List.of("early","day"),List.of(),new Window("07:00","18:00",List.of(DayOfWeek.TUESDAY)),null);
        var result=service(p).assess(input);assertThat(result.mode()).isEqualTo("reference");assertThat(result.status()).isEqualTo("suitable");
        assertThat(result.positiveScopes()).hasSize(1);assertThat(result.positiveScopes().getFirst().type()).isEqualTo("day");
        assertThat(p.calls).allMatch(r->r.travelDate().getDayOfWeek()==DayOfWeek.TUESDAY);
    }
    @Test void anOvernightWindowCoversTheNextMorningWithoutAddingExtraSchedules(){
        var p=new Planner();var input=new Request("DA18 4DE","SE18 6FL",60,List.of("night"),List.of(),new Window("21:00","08:00",List.of(DayOfWeek.MONDAY)),null);
        var result=service(p).assess(input);assertThat(result.positiveScopes()).hasSize(1);assertThat(p.calls).hasSize(2);
        assertThat(p.calls.getLast().travelDate()).isEqualTo(FIRST.plusDays(1));assertThat(result.days()).hasSize(7);
    }
    @Test void exactShiftStillShowsRoutesWhenItConflictsWithPersonalWindow(){
        var p=new Planner();var input=new Request("DA18 4DE","SE18 6FL",60,List.of(),List.of(new Shift("Job","07:00","15:00",List.of(DayOfWeek.MONDAY),null)),new Window("08:00","18:00",List.of()),null);
        var result=service(p).assess(input);assertThat(result.status()).isEqualTo("unsuitable");assertThat(result.issues()).anyMatch(i->i.direction().equals("schedule"));
        assertThat(result.days().getFirst().checks().getFirst().outbound().status()).isEqualTo("within-limit");
    }
    @Test void rotaWeeksAreNotFlattenedOrGuessed(){
        var p=new Planner();var input=new Request("DA18 4DE","SE18 6FL",60,List.of(),List.of(
            new Shift("Week 1","04:00","10:00",List.of(DayOfWeek.MONDAY),1),new Shift("Week 2","04:00","08:00",List.of(DayOfWeek.MONDAY),2)),null,FIRST);
        var result=service(p).assess(input);assertThat(p.calls).hasSize(2);assertThat(p.calls.getLast().travelTime()).isEqualTo("10:00");
        assertThat(result.days().getFirst().checks().getFirst().label()).isEqualTo("Week 1");
        p.calls.clear();new SimpleCommuteService(p,Clock.offset(CLOCK,Duration.ofDays(7))).assess(input);
        assertThat(p.calls).hasSize(2);assertThat(p.calls.getLast().travelTime()).isEqualTo("08:00");
        assertThatThrownBy(()->service(p).validate(new Request(input.originPostcode(),input.workPostcode(),60,List.of(),input.shifts(),null,null))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deselectingWeekTwoDoesNotCollapseTheWholeRotaToOneWeek(){
        var p=new Planner();var input=new Request("DA18 4DE","SE18 6FL",60,List.of(),List.of(new Shift("Week 1","04:00","08:00",List.of(DayOfWeek.MONDAY),1)),null,FIRST,2);
        var result=new SimpleCommuteService(p,Clock.offset(CLOCK,Duration.ofDays(7))).assess(input);
        assertThat(result.status()).isEqualTo("needs-review");assertThat(p.calls).isEmpty();
    }
    @Test void unspecifiedWorkdaysDoNotTurnEveryCheckedDateIntoAMandatoryJobDay(){
        var p=new Planner();p.answer=r->List.of(route(r,r.travelDate().equals(FIRST)?20:90));
        var input=new Request("DA18 4DE","SE18 6FL",60,List.of(),List.of(new Shift("Unknown days","09:00","17:00",List.of(),null)),null,null);
        var result=service(p).assess(input);assertThat(result.status()).isEqualTo("suitable");assertThat(result.positiveScopes()).hasSize(1);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"2026-10-24","2027-03-27"})
    void clocksChangingDoNotTurnAmbiguousOrMissingLocalTimesIntoFailures(String date){
        var p=new Planner();p.answer=r->{uk.co.commutefit.assessment.AssessmentTimePolicy.requireUnambiguousLondonTime(r.travelDate().atTime(LocalTime.parse(r.travelTime())));return List.of(route(r,20));};
        var c=Clock.fixed(Instant.parse(date+"T00:00:00Z"),ZoneOffset.UTC);
        var input=new Request("DA18 4DE","SE18 6FL",60,List.of(),List.of(new Shift("Sunday","01:30","10:00",List.of(DayOfWeek.SUNDAY),null)),null,null);
        var result=new SimpleCommuteService(p,c).assess(input);assertThat(result.status()).isEqualTo("needs-review");
        assertThat(result.issues()).anyMatch(i->i.reason().equals("time-unavailable"));
    }
}
