package uk.co.commutefit.location;

import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** Public World Geocoding endpoint used only by the provider comparison demo. */
@Component
public class ArcGisClient {
    private final RestClient client;
    public ArcGisClient() {
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(12));
        client=RestClient.builder().baseUrl("https://geocode.arcgis.com").requestFactory(factory).build();
    }
    public JsonNode search(String query) {
        var body=client.get().uri(b->b.path("/arcgis/rest/services/World/GeocodeServer/findAddressCandidates")
            .queryParam("SingleLine",query).queryParam("sourceCountry","GBR")
            .queryParam("outFields","*").queryParam("maxLocations",8)
            .queryParam("forStorage",false).queryParam("f","json").build()).retrieve().body(JsonNode.class);
        if(body==null||body.has("error"))throw new IllegalStateException("ArcGIS returned an empty or error response.");
        return body;
    }
}
