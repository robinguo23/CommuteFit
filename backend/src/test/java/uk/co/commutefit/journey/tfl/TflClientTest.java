package uk.co.commutefit.journey.tfl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TflClientTest {
    @Test
    void sendsJourneyTimeInTfLRequiredHourMinuteFormat() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tfl.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TflClient client = new TflClient(builder.build(), "");

        server.expect(requestTo("https://api.tfl.test/Journey/JourneyResults/DA18%204DE/to/Starbucks%20Oxford%20Street?date=20261028&time=0230&timeIs=Arriving&calcOneDirection=true&mode=bus,overground,national-rail,elizabeth-line,tube,dlr,tram,river-bus,cable-car,walking"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"journeys\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(client.journey("DA18 4DE", "Starbucks Oxford Street", LocalDate.of(2026, 10, 28), LocalTime.of(2, 30), "Arriving")
            .path("journeys").isArray());
        server.verify();
    }
}
