package uk.co.commutefit.assessment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.co.commutefit.fare.FareSummary;

public record AssessmentResponse(String status, List<DayAssessment> assessments, DataBasis dataBasis, List<String> limitations) {
    public record DataBasis(String provider, Instant queriedAt, String timezone) {}
    public record DayAssessment(LocalDate travelDate, String dayLabel, DirectionAssessment outbound, @JsonProperty("return") DirectionAssessment returning) {}
    public record DirectionAssessment(LocalDateTime targetTime, List<JourneyOption> options, String status, String messageEn, String messageZh) {}
    public record JourneyOption(
        String id, LocalDateTime startDateTime, LocalDateTime arrivalDateTime, Integer durationMinutes,
        Boolean withinLimit, List<JourneyLeg> legs, FareSummary fareSummary, String source
    ) {}
    public record JourneyLeg(
        String mode, String route, String from, String to, LocalDateTime departureDateTime,
        LocalDateTime arrivalDateTime, Integer durationMinutes, String summary, Integer stops
    ) {
        public JourneyLeg(String mode, String route, String from, String to, LocalDateTime departureDateTime,
                          LocalDateTime arrivalDateTime, Integer durationMinutes, String summary) {
            this(mode, route, from, to, departureDateTime, arrivalDateTime, durationMinutes, summary, null);
        }
    }
}
