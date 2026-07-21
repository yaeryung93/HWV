package com.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> aiError(IllegalStateException exception) {
        log.error("요청 처리 중 서버 상태 오류: {}", exception.getMessage(), exception);
        return Map.of("message", exception.getMessage() == null ? "AI 응답을 처리하지 못했습니다. 다시 시도해 주세요." : exception.getMessage());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, String>> geminiError(WebClientResponseException exception) {
        int upstreamStatus = exception.getStatusCode().value();
        log.warn("Gemini API 오류 응답: status={}", upstreamStatus);
        String message = switch (upstreamStatus) {
            case 400 -> "Gemini 요청 형식 또는 생성 응답 설정이 올바르지 않습니다.";
            case 401, 403 -> "Gemini API 키 또는 프로젝트 권한을 확인해 주세요.";
            case 404 -> "설정된 Gemini 모델을 사용할 수 없습니다.";
            case 429 -> "Gemini 요청 한도(RPM, TPM 또는 RPD)를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            case 500, 502, 503, 504 -> "Gemini 서버가 일시적으로 혼잡합니다. 잠시 후 다시 시도해 주세요.";
            default -> "Gemini 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        };
        HttpStatus responseStatus = upstreamStatus == 429 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(responseStatus).body(Map.of("message", message));
    }
}
