package uk.co.commutefit.profile;

import java.time.Instant;
import java.util.List;

public record ProfileDto(
    boolean configured,
    String departurePostcode,
    Integer maxOneWayMinutes,
    int arrivalBufferMinutes,
    List<String> selectedConcessions,
    String preferredPaymentMethod,
    String otherPaymentMethodLabel,
    Instant updatedAt
) {
    public static ProfileDto defaults() {
        return new ProfileDto(false, "", null, 30, List.of(), null, null, null);
    }
}
