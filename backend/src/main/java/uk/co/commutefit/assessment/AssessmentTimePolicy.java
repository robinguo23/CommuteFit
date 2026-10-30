package uk.co.commutefit.assessment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.util.LinkedHashSet;
import java.util.List;

public final class AssessmentTimePolicy {
    public static final ZoneId LONDON = ZoneId.of("Europe/London");
    private AssessmentTimePolicy() {}

    public static List<LocalDate> assessmentDates(LocalDate reference, boolean weekends) {
        LinkedHashSet<LocalDate> dates = new LinkedHashSet<>();
        dates.add(reference);
        if (weekends) {
            LocalDate monday = reference.with(DayOfWeek.MONDAY);
            dates.add(monday.plusDays(5));
            dates.add(monday.plusDays(6));
        }
        return dates.stream().sorted().toList();
    }

    public static LocalDateTime outboundTarget(LocalDate shiftDate, LocalTime start, int bufferMinutes) {
        return LocalDateTime.of(shiftDate, start).minusMinutes(bufferMinutes);
    }

    public static LocalDateTime returnBoundary(LocalDate shiftDate, LocalTime start, LocalTime finish) {
        LocalDate finishDate = finish.isBefore(start) ? shiftDate.plusDays(1) : shiftDate;
        return LocalDateTime.of(finishDate, finish);
    }

    public static void requireUnambiguousLondonTime(LocalDateTime localDateTime) {
        ZoneRules rules = LONDON.getRules();
        int validOffsets = rules.getValidOffsets(localDateTime).size();
        if (validOffsets == 0) throw new AssessmentTimeBoundaryException(false);
        if (validOffsets > 1) throw new AssessmentTimeBoundaryException(true);
    }
}
