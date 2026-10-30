package uk.co.commutefit.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
    @Bean
    RestClient tflRestClient(
        RestClient.Builder builder,
        @Value("${commute-fit.tfl.base-url}") String baseUrl,
        @Value("${commute-fit.tfl.connect-timeout:4s}") Duration connectTimeout,
        @Value("${commute-fit.tfl.read-timeout:12s}") Duration readTimeout
    ) {
        var httpClient = java.net.http.HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
