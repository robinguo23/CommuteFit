package uk.co.commutefit.location;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;

@Component
public class PostcodesIoClient {
    private final RestClient client;
    public PostcodesIoClient(){
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(http);factory.setReadTimeout(Duration.ofSeconds(10));
        this.client=RestClient.builder().baseUrl("https://api.postcodes.io").requestFactory(factory).build();
    }
    public JsonNode lookup(String postcode){
        String encoded=UriUtils.encodePathSegment(postcode, StandardCharsets.UTF_8);
        return client.get().uri("/postcodes/"+encoded).retrieve().body(JsonNode.class);
    }
    public JsonNode search(String query){
        return client.get().uri(uri->uri.path("/postcodes").queryParam("query",query).queryParam("limit",8).build()).retrieve().body(JsonNode.class);
    }
    public JsonNode nearby(double lat,double lon){
        return client.get().uri(uri->uri.path("/postcodes").queryParam("lat",lat).queryParam("lon",lon).queryParam("limit",8).build()).retrieve().body(JsonNode.class);
    }
}
