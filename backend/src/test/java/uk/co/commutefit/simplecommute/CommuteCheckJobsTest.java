package uk.co.commutefit.simplecommute;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import tools.jackson.databind.ObjectMapper;
import uk.co.commutefit.common.ApiExceptionHandler;
import uk.co.commutefit.simplecommute.SimpleCommuteService.*;

class CommuteCheckJobsTest {
    static class MutableClock extends Clock {
        final AtomicReference<Instant> now=new AtomicReference<>(Instant.parse("2026-11-02T00:00:00Z"));
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now.get();}
    }
    static class FastService extends SimpleCommuteService {
        FastService(){super(null,SimpleCommuteServiceTest.CLOCK);}
        public Result assess(Request r,BiConsumer<Integer,Integer> progress,BooleanSupplier cancelled){progress.accept(7,7);return new Result("needs-review","reference",r.originPostcode(),r.workPostcode(),60,SimpleCommuteServiceTest.FIRST,SimpleCommuteServiceTest.FIRST.plusDays(6),List.of(),List.of(),List.of(),0,0,"Europe/London",SimpleCommuteServiceTest.CLOCK.instant());}
    }
    static CommuteCheckJobs.Snapshot finished(CommuteCheckJobs jobs,String id)throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(System.nanoTime()<until){var s=jobs.find(id).orElseThrow();if(!s.status().equals("running"))return s;Thread.sleep(5);}
        throw new AssertionError("Background check did not finish");
    }
    @Test void resultsExpireAndCancelIsIdempotent()throws Exception{
        var clock=new MutableClock();try(var jobs=new CommuteCheckJobs(new FastService(),clock,Executors.newSingleThreadExecutor())){
            var id=jobs.start(SimpleCommuteServiceTest.request("day")).id();
            assertThat(finished(jobs,id).completed()).isEqualTo(7);clock.now.updateAndGet(t->t.plusSeconds(121));
            assertThat(jobs.find(id)).isEmpty();jobs.cancel(id);jobs.cancel(id);
        }
    }
    @Test void atMostTwoChecksRunAndCancellationFreesTheSlot()throws Exception{
        CountDownLatch entered=new CountDownLatch(2),gate=new CountDownLatch(1);
        var service=new FastService(){public Result assess(Request r,BiConsumer<Integer,Integer> progress,BooleanSupplier cancelled){
            entered.countDown();try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}
            return super.assess(r,progress,cancelled);
        }};
        try(var jobs=new CommuteCheckJobs(service,new MutableClock(),Executors.newCachedThreadPool())){
            String first=jobs.start(SimpleCommuteServiceTest.request("day")).id();jobs.start(SimpleCommuteServiceTest.request("day"));assertThat(entered.await(2,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(()->jobs.start(SimpleCommuteServiceTest.request("day"))).isInstanceOf(CommuteCheckJobs.Busy.class);
            jobs.cancel(first);assertThat(jobs.find(first)).isEmpty();assertThat(jobs.start(SimpleCommuteServiceTest.request("day")).id()).isNotBlank();gate.countDown();
        }
    }
    @Test void invalidInputsDoNotCreateBackgroundTasks(){
        try(var jobs=new CommuteCheckJobs(new FastService(),new MutableClock(),Executors.newSingleThreadExecutor())){
            assertThatThrownBy(()->jobs.start(SimpleCommuteServiceTest.request())).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void createPollCancelContractAndMissingCheckErrorsAreExplicit()throws Exception{
        try(var jobs=new CommuteCheckJobs(new FastService(),new MutableClock(),Executors.newSingleThreadExecutor())){
            var mvc=MockMvcBuilders.standaloneSetup(new SimpleCommuteController(jobs)).setControllerAdvice(new ApiExceptionHandler()).build();
            var body=mvc.perform(post("/api/commute-checks").contentType("application/json").content("{\"originPostcode\":\"DA18 4DE\",\"workPostcode\":\"SE18 6FL\",\"maxOneWayMinutes\":60,\"referenceTypes\":[\"day\"]}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            String id=new ObjectMapper().readTree(body).path("id").asText();finished(jobs,id);
            mvc.perform(get("/api/commute-checks/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("complete")).andExpect(jsonPath("$.result.totalFare").doesNotExist());
            mvc.perform(delete("/api/commute-checks/"+id)).andExpect(status().isNoContent());
            mvc.perform(get("/api/commute-checks/"+id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("check_expired"));
            mvc.perform(post("/api/commute-checks").contentType("application/json").content("{\"originPostcode\":\"London\"}"))
                .andExpect(status().isBadRequest());
        }
    }
    @Test void httpJobRunsTheRealEngineLookupAndTflParserWithoutLeakingFares()throws Exception{
        var builder=org.springframework.web.client.RestClient.builder().baseUrl("https://api.tfl.test");
        var server=org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.ExpectedCount.times(14),request->{
            assertThat(request.getURI().getPath()).startsWith("/Journey/JourneyResults/");
            assertThat(request.getURI().getQuery()).contains("date=2026110");
        }).andRespond(request->{
            var query=org.springframework.web.util.UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            var d=LocalDate.parse(query.getFirst("date"),java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            var t=LocalTime.parse(query.getFirst("time"),java.time.format.DateTimeFormatter.ofPattern("HHmm"));
            var target=d.atTime(t);boolean out="Arriving".equals(query.getFirst("timeIs"));
            var departure=out?target.minusMinutes(25):target;var arrival=departure.plusMinutes(25);
            String body="{\"journeys\":[{\"startDateTime\":\""+departure+"\",\"arrivalDateTime\":\""+arrival+"\",\"duration\":25,\"fare\":{\"totalCost\":99999},\"legs\":[{\"mode\":{\"name\":\"bus\"},\"departureTime\":\""+departure+"\",\"arrivalTime\":\""+arrival+"\",\"duration\":25}]}]}";
            var response=new org.springframework.mock.http.client.MockClientHttpResponse(body.getBytes(java.nio.charset.StandardCharsets.UTF_8),org.springframework.http.HttpStatus.OK);
            response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);return response;
        });
        var tfl=new uk.co.commutefit.journey.tfl.TflClient(builder.build(),"");
        var lookup=new uk.co.commutefit.journey.lookup.JourneyLookupService(new uk.co.commutefit.journey.tfl.TflJourneyService(tfl,new uk.co.commutefit.journey.tfl.CandidateTokenStore()));
        try(var jobs=new CommuteCheckJobs(new SimpleCommuteService(lookup,SimpleCommuteServiceTest.CLOCK),new MutableClock(),Executors.newSingleThreadExecutor())){
            var mvc=MockMvcBuilders.standaloneSetup(new SimpleCommuteController(jobs)).setControllerAdvice(new ApiExceptionHandler()).build();
            String body=mvc.perform(post("/api/commute-checks").contentType("application/json").content("{\"originPostcode\":\"DA18 4DE\",\"workPostcode\":\"SE18 6FL\",\"maxOneWayMinutes\":60,\"referenceTypes\":[\"day\"]}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            String id=new ObjectMapper().readTree(body).path("id").asText();var result=finished(jobs,id).result();
            assertThat(result.status()).isEqualTo("suitable");assertThat(result.days()).hasSize(7);assertThat(result.positiveScopes()).hasSize(7);
            String output=mvc.perform(get("/api/commute-checks/"+id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(output).doesNotContain("totalFare","totalCost","fareStatus","99999");server.verify();
        }
    }
}
