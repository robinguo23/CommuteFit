package uk.co.commutefit.assessment;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class AssessmentRequest {
    @NotBlank(message = "Enter the workplace postcode, address or branch / 请输入工作地点 postcode、地址或门店")
    @Size(max = 180, message = "Location is too long / 地点文字太长")
    private String destinationText;
    @NotNull(message = "Choose a reference date / 请选择参考日期")
    private LocalDate referenceDate;
    @NotBlank(message = "Enter the earliest shift start / 请输入最早开班时间")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use 24-hour HH:mm / 请使用 24 小时制 HH:mm")
    private String earliestShiftStart;
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use 24-hour HH:mm / 请使用 24 小时制 HH:mm")
    @NotBlank(message = "Enter the expected latest finish / 请输入预计最晚下班时间")
    private String expectedLatestFinish;
    private boolean checkWeekend;
    private Integer arrivalBufferMinutes;
    private boolean arrivalBufferSupplied;
    private Integer maxOneWayMinutes;
    private boolean maxOneWaySupplied;
    private String paymentMethod;
    private boolean paymentMethodSupplied;
    private List<String> concessions;
    private boolean concessionsSupplied;
    private String selectedCandidateId;
    private String selectedOriginCandidateId;

    public String getDestinationText() { return destinationText; }
    public void setDestinationText(String destinationText) { this.destinationText = destinationText; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public void setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; }
    public String getEarliestShiftStart() { return earliestShiftStart; }
    public void setEarliestShiftStart(String earliestShiftStart) { this.earliestShiftStart = earliestShiftStart; }
    public String getExpectedLatestFinish() { return expectedLatestFinish; }
    public void setExpectedLatestFinish(String expectedLatestFinish) { this.expectedLatestFinish = expectedLatestFinish; }
    public boolean isCheckWeekend() { return checkWeekend; }
    public void setCheckWeekend(boolean checkWeekend) { this.checkWeekend = checkWeekend; }
    public Integer getArrivalBufferMinutes() { return arrivalBufferMinutes; }
    @JsonSetter("arrivalBufferMinutes") public void setArrivalBufferMinutes(Integer value) { this.arrivalBufferMinutes = value; this.arrivalBufferSupplied = true; }
    public boolean isArrivalBufferSupplied() { return arrivalBufferSupplied; }
    public Integer getMaxOneWayMinutes() { return maxOneWayMinutes; }
    @JsonSetter("maxOneWayMinutes") public void setMaxOneWayMinutes(Integer value) { this.maxOneWayMinutes = value; this.maxOneWaySupplied = true; }
    public boolean isMaxOneWaySupplied() { return maxOneWaySupplied; }
    public String getPaymentMethod() { return paymentMethod; }
    @JsonSetter("paymentMethod") public void setPaymentMethod(String value) { this.paymentMethod = value; this.paymentMethodSupplied = true; }
    public boolean isPaymentMethodSupplied() { return paymentMethodSupplied; }
    public List<String> getConcessions() { return concessions; }
    @JsonSetter("concessions") public void setConcessions(List<String> value) { this.concessions = value; this.concessionsSupplied = true; }
    public boolean isConcessionsSupplied() { return concessionsSupplied; }
    public String getSelectedCandidateId() { return selectedCandidateId; }
    public void setSelectedCandidateId(String selectedCandidateId) { this.selectedCandidateId = selectedCandidateId; }
    public String getSelectedOriginCandidateId() { return selectedOriginCandidateId; }
    public void setSelectedOriginCandidateId(String selectedOriginCandidateId) { this.selectedOriginCandidateId = selectedOriginCandidateId; }
}
