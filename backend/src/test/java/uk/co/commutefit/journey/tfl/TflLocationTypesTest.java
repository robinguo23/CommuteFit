package uk.co.commutefit.journey.tfl;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.time.*;
import java.util.stream.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.co.commutefit.common.LocationDisambiguationException;

class TflLocationTypesTest {
    static final String[] TYPES={"51.5178,-0.0823","DA18 4DE","940GZZLULVT","1000138","King's Cross & St Pancras International"};
    static Stream<Arguments> combinations(){return Stream.of(TYPES).flatMap(from->Stream.of(TYPES).map(to->Arguments.of(from,to)));}
    @ParameterizedTest @MethodSource("combinations")
    void allFiveFormatsArePreservedInBothPathEndpoints(String from,String to){
        RestClient.Builder builder=RestClient.builder().baseUrl("https://api.tfl.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(request->{
            assertThat(request.getURI().getPath()).isEqualTo("/Journey/JourneyResults/"+from+"/to/"+to);
            assertThat(request.getURI().getRawPath()).doesNotContain("%2520","%2526","%252C");
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI().getQuery()).contains("date=20261030","time=0900","timeIs=Arriving");
        }).andRespond(withSuccess("{\"journeys\":[]}",MediaType.APPLICATION_JSON));
        new TflClient(builder.build(),"").journey(from,to,LocalDate.of(2026,10,30),LocalTime.of(9,0),"Arriving");
        server.verify();
    }
    @ParameterizedTest @ValueSource(strings={"51.5178,-0.0823","1000138"})
    void http300ParameterValueCanBeSelectedByItsDisplayNameAndRequeried(String parameter){
        var builder=RestClient.builder().baseUrl("https://api.tfl.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(request->{}).andRespond(withStatus(HttpStatus.MULTIPLE_CHOICES).contentType(MediaType.APPLICATION_JSON).body(
            "{\"toLocationDisambiguation\":{\"disambiguationOptions\":[{\"parameterValue\":\""+parameter+"\",\"place\":{\"commonName\":\"Liverpool Street Station\",\"placeType\":\"StopPoint\"}}]}}"));
        server.expect(request->assertThat(request.getURI().getPath()).endsWith("/to/"+parameter))
            .andRespond(withSuccess("{\"journeys\":[]}",MediaType.APPLICATION_JSON));
        var tokens=new CandidateTokenStore();
        var svc=new TflJourneyService(new TflClient(builder.build(),""),tokens);
        LocationDisambiguationException ex=catchThrowableOfType(()->svc.journeys("DA18 4DE","Liverpool Street","DA18 4DE","Liverpool Street","origin","destination",LocalDate.of(2026,10,30),LocalTime.of(9,0),"Arriving"),LocationDisambiguationException.class);
        assertThat(ex.candidates()).hasSize(1);
        var choice=ex.candidates().getFirst();
        String route=svc.resolveDestination(choice.get("name"),choice.get("id"));
        assertThat(route).isEqualTo(parameter);
        assertThatThrownBy(()->svc.resolveDestination("different workplace",choice.get("id"))).isInstanceOf(IllegalArgumentException.class);
        svc.journeys("DA18 4DE",route,"DA18 4DE",choice.get("name"),"origin","destination",LocalDate.of(2026,10,30),LocalTime.of(9,0),"Arriving");
        server.verify();
    }
    @Test void displayNameWhitespaceNormalizationRemainsConsistent(){
        var store=new CandidateTokenStore();
        String token=store.add("Liverpool  Street","1000138","Liverpool  Street","Station");
        assertThat(store.resolve(token,"Liverpool Street").providerId()).isEqualTo("1000138");
    }
}
