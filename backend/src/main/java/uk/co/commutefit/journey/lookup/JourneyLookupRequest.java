package uk.co.commutefit.journey.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JourneyLookupRequest(
    @NotBlank(message = "Enter an origin postcode or place / 请输入出发 postcode 或地点")
    @Size(max = 120, message = "Keep the origin location under 120 characters / 出发地点不能超过 120 个字符") String originPostcode,
    @NotBlank(message = "Enter a destination postcode or place / 请输入目的 postcode 或地点")
    @Size(max = 120, message = "Keep the destination location under 120 characters / 目的地点不能超过 120 个字符") String destinationPostcode,
    String timeMode, LocalDate travelDate,
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use 24-hour HH:mm / 请使用 24 小时制 HH:mm") String travelTime,
    String selectedOriginCandidateId, String selectedDestinationCandidateId
 ) {}
