package uk.co.commutefit.jobextraction;

import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.net.*;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

class JobExtractionTest {
    private final JobDocumentParser parser=new JobDocumentParser();
    private JobExtractionService service(String html){
        return new JobExtractionService(new PublicPageFetcher(){
            @Override public Page fetch(String url){return new Page(html,"https://jobs.example/role");}
        },parser);
    }
    @Test void jsonLdPreservesWorkplaceAndExcludesCompanyHeadquarters(){
        var result=service("""
            <html><script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[{"@type":"JobPosting","title":"Barista",
             "hiringOrganization":{"address":{"postalCode":"SW1A 1AA"}},
             "jobLocation":{"address":{"postalCode":"e16an"}},
             "description":"Shifts: Monday to Friday 09:00-17:00."}]}
            </script><footer>Registered office SW1A 1AA</footer></html>
            """).extract(new JobExtractionService.Request("url","https://jobs.example/role"));
        assertThat(result.title()).isEqualTo("Barista");
        assertThat(result.workplaceLocation()).isEqualTo("E1 6AN");
        assertThat(result.locations()).hasSize(1);
        assertThat(result.startTime()).isEqualTo("09:00");
        assertThat(result.workDays()).hasSize(5);
        assertThat(result.locations().getFirst().sourcePath()).contains("jobLocation");
    }
    @Test void doesNotChooseBetweenMultipleJobPostings(){
        var p=parser.parse("""
            <script type="application/ld+json">[{"@type":"JobPosting","title":"One"},{"@type":"JobPosting","title":"Two"}]</script>
            <p>Workplace E1 6AN Monday 09:00-17:00</p>
            """,true);
        assertThat(p.locations()).isEmpty();assertThat(p.schedules()).isEmpty();assertThat(p.warnings()).isNotEmpty();
    }
    @Test void employerWindowNeverBecomesAnActualShift(){
        var result=service("""
            <main><h1>Store colleague</h1><p>Workplace E1 6AN</p>
            <h2>Availability Window</h2><table>
            <tr><th>Days</th><th>From time</th><th>To time</th></tr>
            <tr><td>Wed</td><td>15:30:00</td><td>24:00:00</td></tr>
            </table></main>
            """).extract(new JobExtractionService.Request("url","https://jobs.example/role"));
        assertThat(result.startTime()).isNull();
        assertThat(result.schedules()).hasSize(1);
        assertThat(result.schedules().getFirst().kind()).isEqualTo("availability-window");
        assertThat(result.schedules().getFirst().days()).containsExactly(DayOfWeek.WEDNESDAY);
        assertThat(result.schedules().getFirst().end()).isEqualTo("00:00");
        assertThat(result.schedules().getFirst().endsNextDay()).isTrue();
    }
    @Test void retainsTwoRotatingShiftsRatherThanPickingTheFirst(){
        var p=parser.parse("Contract details: Rotating shifts 06:00-14:00 and 14:00-22:00. Five days from seven.\nAvailability Window\nMon 06:00-22:00",false);
        assertThat(p.schedules().stream().filter(s->s.kind().equals("exact-shift"))).hasSize(2);
        assertThat(p.schedules().getFirst().days()).isEmpty();
        assertThat(JobExtractionService.result(null,"unknown",null,p.locations(),p.schedules(),p.warnings()).startTime()).isNull();
    }
    @Test void preservesDailyTimeAssociation(){
        var p=parser.parse("Shifts:\nMonday 09:00-17:00\nTuesday 10:00-18:00",false);
        assertThat(p.schedules()).hasSize(2);
        assertThat(p.schedules().get(0).days()).containsExactly(DayOfWeek.MONDAY);
        assertThat(p.schedules().get(1).start()).isEqualTo("10:00");
    }
    @Test void suppliedMarksAndSpencerChineseRotaPreservesSixRowsAndStore()throws Exception{
        String text=new String(getClass().getResourceAsStream("/jobextraction/marks-woolwich.txt").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        var p=parser.parse(text,false);
        assertThat(p.locations()).hasSize(1);
        assertThat(p.locations().getFirst().value()).isEqualTo("Woolwich Simply Food, London, Greater London");
        assertThat(p.schedules()).hasSize(6);
        assertThat(p.schedules()).extracting(s->s.cycleWeek()).containsExactly(1,1,1,2,2,2);
        assertThat(p.schedules()).extracting(s->s.start()).containsExactly("08:00","04:00","06:00","04:00","04:00","04:00");
        assertThat(p.schedules()).extracting(s->s.days().getFirst()).containsExactly(DayOfWeek.SUNDAY,DayOfWeek.MONDAY,DayOfWeek.SATURDAY,DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY,DayOfWeek.FRIDAY);
    }
    @Test void chineseWeekdaysAndRangeAreSupported(){
        assertThat(JobDocumentParser.days("周一至周五")).containsExactly(DayOfWeek.MONDAY,DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY,DayOfWeek.THURSDAY,DayOfWeek.FRIDAY);
        assertThat(parser.parse("工作时间：星期三 04:00-08:00",false).schedules().getFirst().kind()).isEqualTo("exact-shift");
    }
    @Test void exactShiftsSuppressEmployerWindowsInPublicResult(){
        var p=parser.parse("Shifts: 06:00-14:00 and 14:00-22:00\nAvailability Window\nMonday 06:00-22:00",false);
        var result=JobExtractionService.result(null,"unknown",null,p.locations(),p.schedules(),p.warnings());
        assertThat(result.schedules()).hasSize(2).allMatch(s->s.kind().equals("exact-shift"));
    }
    @Test void fiveDaysFromSevenIncludingWeekendsDoesNotDefineFixedWorkDays(){
        var p=parser.parse("Rotating Shifts (6:00-14:00 and 14:00-22:00)- 5 days from 7 (including weekends)",false);
        assertThat(p.schedules()).hasSize(2);
        assertThat(p.schedules()).allMatch(s->s.days().isEmpty());
    }
    @Test void weeklyHoursAndOpeningHoursAreNotExactShifts(){
        var p=parser.parse("Working hours: 20 hours per week\nOpening hours: Monday 08:00-22:00",false);
        assertThat(p.schedules()).extracting(s->s.kind()).containsExactly("weekly-hours","opening-hours");
    }
    @Test void meridiemAndOvernightNormalization(){
        assertThat(JobDocumentParser.time("12am",false)).isEqualTo("00:00");
        assertThat(JobDocumentParser.time("12pm",false)).isEqualTo("12:00");
        assertThat(JobDocumentParser.time("24:00",false)).isNull();
        assertThat(JobDocumentParser.time("24:01",true)).isNull();
        var p=parser.parse("Shift Monday 10pm-6am",false);
        assertThat(p.schedules().getFirst().endsNextDay()).isTrue();
    }
    @Test void refusesPartialMeridiemAndEqualEndpoints(){
        assertThat(parser.parse("Shift Monday 9-5pm\nShift Tuesday 00:00-00:00",false).schedules()).isEmpty();
    }
    @Test void expiredJobAndRemoteFlagsAreVisible(){
        var p=parser.parse("""
            <script type="application/ld+json">{"@type":"JobPosting","validThrough":"2000-01-01","jobLocationType":"TELECOMMUTE"}</script>
            <h1>Remote job</h1>
            """,true);
        assertThat(p.jobStatus()).isEqualTo("expired");
        assertThat(p.warnings()).anyMatch(s->s.contains("remote"));
    }
    @Test void multiplePostcodesRemainCandidates(){
        var p=parser.parse("Work location E1 6AN or DA18 4DE",false);
        var result=JobExtractionService.result(null,"unknown",null,p.locations(),p.schedules(),p.warnings());
        assertThat(result.locations()).hasSize(2);assertThat(result.workplaceLocation()).isNull();
    }
    @Test void ambiguousLocationArrayCannotSilentlyUseItsOnlyCompletePostcode(){
        var r=service("""
            <script type="application/ld+json">{"@type":"JobPosting","jobLocation":[{"address":{"postalCode":"E1 6AN"}},{"address":{"addressLocality":"London"}}]}</script>
            <h1>Several sites</h1>
            """).extract(new JobExtractionService.Request("url","https://jobs.example/role"));
        assertThat(r.workplaceLocation()).isNull();assertThat(r.warnings()).anyMatch(s->s.startsWith("Multiple workplace"));
    }
    @Test void accessBarrierDoesNotInventFields(){
        var p=parser.parse("<p>Please enable JavaScript to continue</p>",true);
        assertThat(p.locations()).isEmpty();assertThat(p.schedules()).isEmpty();assertThat(p.warnings()).isNotEmpty();
    }
    @Test void missingUrlAndFailedFetchAreRecoverable(){
        var svc=new JobExtractionService(new PublicPageFetcher(){public Page fetch(String raw)throws IOException{throw new IOException("blocked");}},parser);
        var r=svc.extract(new JobExtractionService.Request("url","https://example.com/job?token=secret"));
        assertThat(r.status()).isEqualTo("unavailable");
        assertThat(r.source().url()).isEqualTo("https://example.com/job");
        assertThat(r.notes()).anyMatch(s->s.contains("Paste"));
    }
    @Test void publicUrlValidationRefusesCredentialsAndUnexpectedPorts(){
        assertThatThrownBy(()->PublicPageFetcher.validate("file:///etc/passwd")).isInstanceOf(IOException.class);
        assertThatThrownBy(()->PublicPageFetcher.validate("http://user:pass@example.com/")).isInstanceOf(IOException.class);
        assertThatThrownBy(()->PublicPageFetcher.validate("http://example.com:8080/")).isInstanceOf(IOException.class);
    }
    @Test void addressPolicyRejectsPrivateAndMetadataTargets()throws Exception{
        for(String ip:new String[]{"127.0.0.1","10.0.0.1","169.254.169.254","192.168.1.1","100.64.0.1","::1","fc00::1","fe80::1"})
            assertThat(PublicPageFetcher.isPublic(InetAddress.getByName(ip))).as(ip).isFalse();
        assertThat(PublicPageFetcher.isPublic(InetAddress.getByName("8.8.8.8"))).isTrue();
    }
    @Test void extractionHttpContractAndLegacyProjection()throws Exception{
        var svc=service("<main><h1>Assistant</h1><p>Work location E1 6AN</p><p>Shifts Monday 09:00-17:00</p></main>");
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new JobExtractionController(svc))
            .setControllerAdvice(new uk.co.commutefit.common.ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/job-extractions")
            .contentType("application/json").content("{\"sourceType\":\"url\",\"content\":\"https://jobs.example/role\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.workplaceLocation").value("E1 6AN"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.schedules[0].kind").value("exact-shift"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.locations[0].evidence").exists());
    }
}
