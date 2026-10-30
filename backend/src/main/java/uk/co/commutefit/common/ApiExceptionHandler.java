package uk.co.commutefit.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import uk.co.commutefit.assessment.AssessmentTimeBoundaryException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("invalid_request", "Check the highlighted fields.", "请检查标出的输入项。", fields, java.time.Instant.now()));
    }

    @ExceptionHandler(ProfileValidationException.class)
    ResponseEntity<ApiError> invalidProfile(ProfileValidationException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("invalid_profile", ex.getMessage(), "个人档案数据无效，请检查 postcode、付款方式和优惠选项。"));
    }

    @ExceptionHandler(AssessmentTimeBoundaryException.class)
    ResponseEntity<ApiError> invalidLondonTime(AssessmentTimeBoundaryException ex) {
        return ResponseEntity.badRequest().body(ex.ambiguous()
            ? ApiError.of("ambiguous_london_time", "This assessment time occurs twice when clocks change in London. Choose a different time to keep the journey lookup unambiguous.", "这个评估时间在伦敦时钟切换时会出现两次。请选择其他时间，以明确路线查询时刻。")
            : ApiError.of("nonexistent_london_time", "This assessment time does not exist when clocks change in London. Choose a different time.", "这个评估时间在伦敦时钟切换时不存在。请选择其他时间。"));
    }

    @ExceptionHandler(DatabaseUnavailableException.class)
    ResponseEntity<ApiError> databaseUnavailable(DatabaseUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiError.of("database_unavailable", "The local profile database is unavailable. Check its connection settings and try again.", "本地档案数据库不可用。请检查连接设置后重试。"));
    }

    @ExceptionHandler(CandidateTokenExpiredException.class)
    ResponseEntity<ApiError> expiredCandidate() {
        return ResponseEntity.badRequest().body(ApiError.of("candidate_expired",
            "The location choice expired. Submit again to choose a matching location.",
            "地点选择已过期。请重新提交，并选择对应地点。"));
    }

    @ExceptionHandler(LocationDisambiguationException.class)
    ResponseEntity<Map<String, Object>> disambiguation(LocationDisambiguationException ex) {
        boolean origin = "origin".equals(ex.candidateField());
        boolean hasChoices = !ex.candidates().isEmpty();
        return ResponseEntity.unprocessableEntity().body(Map.of(
            "code", hasChoices ? "location_disambiguation" : "location_unresolved",
            "candidateField", ex.candidateField(),
            "messageEn", hasChoices
                ? (origin ? "Choose the departure location that matches your input." : "Choose the destination location that matches your input.")
                : (origin ? "TfL could not resolve the departure input to a usable location." : "TfL could not resolve the destination input to a usable location."),
            "messageZh", hasChoices
                ? (origin ? "请选择与出发输入相符的地点。" : "请选择与目的输入相符的地点。")
                : (origin ? "TfL 无法将出发输入解析为可用地点。" : "TfL 无法将目的地输入解析为可用地点。"),
            "candidates", ex.candidates()
        ));
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    ResponseEntity<ApiError> providerUnavailable(ProviderUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiError.of("provider_unavailable", "TfL Journey Planner could not provide this assessment. Please retry.", "TfL Journey Planner 暂时无法提供这次评估，请重试。"));
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ApiError> rateLimited(RateLimitedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiError.of("rate_limited", "TfL has temporarily limited requests. Please try again later.", "TfL 暂时限制了请求，请稍后再试。"));
    }

    @ExceptionHandler({DataAccessException.class})
    ResponseEntity<ApiError> databaseError(DataAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiError.of("database_unavailable", "The local profile database is unavailable. Check its connection settings and try again.", "本地档案数据库不可用。请检查连接设置后重试。"));
    }

    @ExceptionHandler({RestClientResponseException.class, ResourceAccessException.class})
    ResponseEntity<ApiError> upstreamError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiError.of("provider_unavailable", "TfL Journey Planner could not provide this assessment. Please retry.", "TfL Journey Planner 暂时无法提供这次评估，请重试。"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("invalid_request", "The assessment input is invalid.", "通勤评估输入无效。"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("invalid_request", "The request is missing required fields or contains an invalid date/time.", "请求缺少必填项，或日期/时间格式无效。"));
    }
}
