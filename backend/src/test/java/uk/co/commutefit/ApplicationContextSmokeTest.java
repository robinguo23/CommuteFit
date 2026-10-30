package uk.co.commutefit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ApplicationContextSmokeTest {
    @Autowired
    private RestClient tflRestClient;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    void createsTheTflRestClientInTheApplicationContext() {
        assertNotNull(tflRestClient);
        assertNotNull(context.getBean(uk.co.commutefit.journey.lookup.JourneyLookupService.class));
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
    }
}
