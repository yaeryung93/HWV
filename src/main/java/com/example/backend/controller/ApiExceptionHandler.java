package com.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> aiError(IllegalStateException exception) {
        return Map.of("message", exception.getMessage() == null ? "AI 응답을 처리하지 못했습니다. 다시 시도해 주세요." : exception.getMessage());
    }

    @ExceptionHandler(WebClientResponseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> geminiError(WebClientResponseException exception) {
        String message = exception.getStatusCode().value() == 429
            ? "Gemini 무료 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."
            : "Gemini 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        return Map.of("message", message);
    }
}
