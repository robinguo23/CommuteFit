package uk.co.commutefit.fare.policy;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@Component
public class FarePolicyCatalogue {
    private final List<FarePolicy> policies;
    private final List<PublishedFarePeriod> busTramFarePeriods;
    private final String reviewedOn;

    @SuppressWarnings("unchecked")
    public FarePolicyCatalogue() {
        try (InputStream input = new ClassPathResource("fare-policies/concessions.yml").getInputStream()) {
            Map<String, Object> document = new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
            this.reviewedOn = String.valueOf(document.get("reviewedOn"));
            Map<String, Object> rates = (Map<String, Object>) document.get("busTramFareSchedule");
            List<Map<String, Object>> periods = (List<Map<String, Object>>) rates.get("periods");
            this.busTramFarePeriods = periods.stream().map(value -> new PublishedFarePeriod(
                LocalDateTime.parse(string(value, "effectiveFrom")), dateTime(value, "effectiveUntil"),
                decimal(value, "adultSingleFare"), decimal(value, "discountedSingleFare"),
                decimal(value, "adultDailyCap"), decimal(value, "discountedDailyCap"), decimal(value, "adultWeeklyCap")
            )).toList();
            List<Map<String, Object>> definitions = (List<Map<String, Object>>) document.get("policies");
            this.policies = definitions.stream().map(value -> new FarePolicy(
                string(value, "id"), string(value, "titleEn"), string(value, "titleZh"), strings(value, "modes"),
                string(value, "paymentBasis"), string(value, "timeBasis"), string(value, "qualifyingEvent"),
                string(value, "fareScope"), string(value, "eligibilityEn"), string(value, "eligibilityZh"),
                optionalString(value, "effectiveDate"), string(value, "source"), Boolean.parseBoolean(string(value, "paygDiscount"))
            )).toList();
        } catch (Exception ex) {
            throw new IllegalStateException("The local TfL concession catalogue could not be loaded.", ex);
        }
    }

    public List<FarePolicy> policies() { return policies; }
    public PublishedFarePeriod busTramFarePeriod(LocalDateTime dateTime) { return busTramFarePeriods.stream().filter(period -> period.covers(dateTime)).findFirst().orElse(null); }
    private static String string(Map<String, Object> map, String key) { return String.valueOf(map.get(key)); }
    private static String optionalString(Map<String, Object> map, String key) { return map.get(key) == null ? null : String.valueOf(map.get(key)); }
    private static LocalDateTime dateTime(Map<String, Object> map, String key) { return map.get(key) == null ? null : LocalDateTime.parse(String.valueOf(map.get(key))); }
    private static BigDecimal decimal(Map<String, Object> map, String key) { return new BigDecimal(String.valueOf(map.get(key))); }
    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> map, String key) { return ((List<Object>) map.get(key)).stream().map(String::valueOf).toList(); }
}
