package com.yuntian.chat_app.handler;

import com.yuntian.chat_app.exception.BaseException;
import com.yuntian.chat_app.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException ex, HttpServletRequest request) {
        log.warn("[BusinessException] path={}, code={}, message={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        List<String> errorMessages = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        log.warn("[ValidationException] {}", errorMessages);
        return Result.error(400, "参数校验失败", errorMessages);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<String>> handleBindException(BindException ex) {
        List<String> errorMessages = ex.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        log.warn("[BindException] {}", errorMessages);
        return Result.error(400, "参数绑定错误", errorMessages);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("[HttpMessageNotReadableException] {}", ex.getMessage());
        return Result.error(400, "请求体格式错误");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("[MethodArgumentTypeMismatchException] {}", ex.getMessage());
        return Result.error(400, "参数类型错误");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Result<Void> handleResponseStatusException(ResponseStatusException ex, HttpServletResponse response) {
        if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            log.warn("[BadRequest] {}", ex.getReason());
            return Result.error(400, ex.getReason() == null ? "参数错误" : ex.getReason());
        }
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("[SystemException] path={}", request.getRequestURI(), ex);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
