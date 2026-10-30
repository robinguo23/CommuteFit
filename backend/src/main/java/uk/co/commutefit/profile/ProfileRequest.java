package uk.co.commutefit.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileRequest(
    @NotBlank(message = "Enter a UK postcode / 请输入英国 postcode")
    @Size(max = 16, message = "Postcode is too long / postcode 太长")
    @Pattern(regexp = "(?i)^(GIR 0AA|(?:[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}))$", message = "Enter a valid UK postcode / 请输入有效的英国 postcode")
    String departurePostcode,
    @Min(value = 1, message = "Use a positive number / 请输入正整数") @Max(value = 1440, message = "Maximum is 1440 minutes / 最多 1440 分钟") Integer maxOneWayMinutes,
    @Min(value = 0, message = "Use zero or more / 请输入 0 或更大的数字") @Max(value = 240, message = "Maximum is 240 minutes / 最多 240 分钟") Integer arrivalBufferMinutes,
    List<@Size(max = 60) String> selectedConcessions,
    @Pattern(regexp = "^(apple-pay-contactless|bank-card-contactless|oyster|other)?$", message = "Choose a supported payment method / 请选择支持的付款方式") String preferredPaymentMethod,
    @Size(max = 120) String otherPaymentMethodLabel
) {
}
