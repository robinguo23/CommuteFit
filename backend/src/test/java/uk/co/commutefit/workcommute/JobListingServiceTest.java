package uk.co.commutefit.workcommute;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.util.List;

class JobListingServiceTest {
    private final JobListingService service = new JobListingService();

    @Test void extractsClearPostcodeWeekdaysAndAmPmShiftAsReviewableSuggestions() {
        var result = service.extract(new JobListingService.ExtractionRequest("description",
            "Retail assistant, Monday to Friday, 9am–5:30pm. Workplace: DA18 4DE."));
        assertThat(result.workplaceLocation()).isEqualTo("DA18 4DE");
        assertThat(result.workDays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        assertThat(result.startTime()).isEqualTo("09:00");
        assertThat(result.endTime()).isEqualTo("17:30");
        assertThat(result.notes()).anyMatch(n -> n.contains("suggestions"));
    }

    @Test void refusesToGuessWorkplaceWhenSeveralPostcodesArePresent() {
        var result = service.extract(new JobListingService.ExtractionRequest("description", "Office in DA18 4DE, training at DA17 6FD, 9am-5pm"));
        assertThat(result.workplaceLocation()).isNull();
        assertThat(result.notes()).anyMatch(n -> n.contains("More than one postcode"));
    }

    @Test void reportsUnreadableUrlAsManualFallbackInsteadOfFailingAssessment() {
        var result = service.extract(new JobListingService.ExtractionRequest("url", "http://127.0.0.1/private"));
        assertThat(result.workplaceLocation()).isNull();
        assertThat(result.notes()).anyMatch(n -> n.contains("Could not read this page"));
    }
}
