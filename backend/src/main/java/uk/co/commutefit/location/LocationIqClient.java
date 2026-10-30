package uk.co.commutefit.location;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class LocationIqClient {
    private final RestClient client;
    private final String apiKey;

    public LocationIqClient(@Value("${commute-fit.geocoding.locationiq-url:https://eu1.locationiq.com}") String baseUrl,
                            @Value("${commute-fit.geocoding.locationiq-key:}") String apiKey) {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(12));
        this.client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey=apiKey;
    }

    public boolean configured(){return !apiKey.isBlank();}

    public JsonNode search(String query,boolean postcode) {
        if(!configured())throw new IllegalStateException("LocationIQ API key is not configured.");
        JsonNode body=client.get().uri(b->{
            var uri=postcode?b.path("/v1/search/postalcode").queryParam("postalcode",query):b.path("/v1/search").queryParam("q",query);
            return uri.queryParam("key",apiKey).queryParam("format","json").queryParam("countrycodes","gb")
                    .queryParam("addressdetails",1).queryParam("normalizeaddress",1).queryParam("limit",8).build();
        }).retrieve().body(JsonNode.class);
        if(body==null)throw new IllegalStateException("LocationIQ returned an empty response.");
        return body;
    }
}
