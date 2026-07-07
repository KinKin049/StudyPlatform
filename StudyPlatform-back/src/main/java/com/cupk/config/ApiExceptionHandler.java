package com.cupk.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * API 全局异常处理器
 * 将框架异常转换为前端友好的紧凑错误响应格式
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 处理参数校验异常
     * 将校验失败的字段和错误信息封装为统一格式返回
     *
     * @param exception 参数校验异常实例
     * @return 包含错误状态码、消息和字段错误详情的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        String message = fields.values().stream()
                .findFirst()
                .orElse("请求参数校验失败");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", message);
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理响应状态异常
     * 将业务逻辑中抛出的自定义状态异常转换为统一格式返回
     *
     * @param exception 响应状态异常实例
     * @return 包含错误状态码和消息的响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", exception.getStatusCode().value());
        body.put("message", exception.getReason());
        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }
}
