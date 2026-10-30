package uk.co.commutefit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssessmentTimePolicyTest {
    @Test
    void weekendAssessmentAddsSaturdayAndSundayAndDeduplicatesReferenceDate() {
        assertEquals(List.of(LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)),
            AssessmentTimePolicy.assessmentDates(LocalDate.of(2026, 10, 28), true));
        assertEquals(List.of(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)),
            AssessmentTimePolicy.assessmentDates(LocalDate.of(2026, 10, 31), true));
        assertEquals(List.of(LocalDate.of(2026, 10, 31)),
            AssessmentTimePolicy.assessmentDates(LocalDate.of(2026, 10, 31), false));
    }

    @Test
    void overnightFinishUsesFollowingDateAndArrivalBufferMovesTargetEarlier() {
        LocalDate shiftDate = LocalDate.of(2026, 10, 28);
        assertEquals(LocalDateTime.of(2026, 10, 29, 2, 0),
            AssessmentTimePolicy.returnBoundary(shiftDate, LocalTime.of(18, 0), LocalTime.of(2, 0)));
        assertEquals(LocalDateTime.of(2026, 10, 28, 2, 30),
            AssessmentTimePolicy.outboundTarget(shiftDate, LocalTime.of(3, 0), 30));
    }

    @Test
    void rejectsMissingAndRepeatedLondonClockTimes() {
        assertThrows(AssessmentTimeBoundaryException.class,
            () -> AssessmentTimePolicy.requireUnambiguousLondonTime(LocalDateTime.of(2026, 3, 29, 1, 30)));
        assertThrows(AssessmentTimeBoundaryException.class,
            () -> AssessmentTimePolicy.requireUnambiguousLondonTime(LocalDateTime.of(2026, 10, 25, 1, 30)));
        assertDoesNotThrow(() -> AssessmentTimePolicy.requireUnambiguousLondonTime(LocalDateTime.of(2026, 10, 25, 2, 30)));
    }

    @Test
    void acceptsOrdinaryWinterClockTimesUsingGmt() {
        LocalDateTime winterTime = LocalDateTime.of(2026, 1, 15, 2, 30);
        assertDoesNotThrow(() -> AssessmentTimePolicy.requireUnambiguousLondonTime(winterTime));
        assertEquals(0, AssessmentTimePolicy.LONDON.getRules().getOffset(winterTime).getTotalSeconds());
    }
}
