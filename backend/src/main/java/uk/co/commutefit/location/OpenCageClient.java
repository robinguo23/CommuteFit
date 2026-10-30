package uk.co.commutefit.location;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class OpenCageClient {
    private final RestClient client;
    private final String apiKey;

    public OpenCageClient(@Value("${commute-fit.geocoding.opencage-url:https://api.opencagedata.com}") String baseUrl,
                          @Value("${commute-fit.geocoding.opencage-key:}") String apiKey) {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(12));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey=apiKey;
    }

    public boolean configured(){return !apiKey.isBlank();}

    public JsonNode search(String query) {
        if(!configured())throw new IllegalStateException("OpenCage API key is not configured.");
        JsonNode body=client.get().uri(b->b.path("/geocode/v1/json")
                .queryParam("q",query).queryParam("key",apiKey).queryParam("countrycode","gb")
                .queryParam("limit",8).queryParam("no_annotations",1).queryParam("no_record",1).build())
                .retrieve().body(JsonNode.class);
        if(body==null)throw new IllegalStateException("OpenCage returned an empty response.");
        return body;
    }
}
