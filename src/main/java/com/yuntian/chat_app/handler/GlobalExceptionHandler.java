package com.yuntian.chat_app.handler;


import com.yuntian.chat_app.exception.BaseException;
import com.yuntian.chat_app.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 处理自定义业务异常 (BaseException及其子类)
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException ex, HttpServletRequest request) {
        // 1. 打印业务异常日志 (WARN级别，包含请求路径)
        log.warn("[业务异常] 请求路径: {}, 错误码: {}, 异常信息: {}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        // 2. 返回给前端明确的错误信息
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常 (JSR 303 - 使用@Validated/@Valid在Controller参数上)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置HTTP状态码为400
    public Result<List<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        // 获取所有字段错误信息
        List<String> errorMessages = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        log.warn("[参数校验异常] - {}", errorMessages);
        return Result.error(400, "参数校验失败", errorMessages); // 可以返回具体的错误字段信息
    }



    /**
     * 处理数据绑定异常 (如类型转换错误)
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<String>> handleBindException(BindException ex) {
        List<String> errorMessages = ex.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        log.warn("[数据绑定异常] - {}", errorMessages);
        return Result.error(400, "数据绑定错误", errorMessages);
    }

    /**
     * 处理其他所有未捕获的异常 (兜底处理)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 设置HTTP状态码为500
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        // 1. 打印详细的错误堆栈到日志 (ERROR级别，包含请求路径)
        log.error("[系统异常] 请求路径: {}，异常信息：", request.getRequestURI(), ex);
        // 2. 返回给前端一个通用的错误信息 (避免泄露敏感信息)
        return Result.error(500, "系统繁忙，请稍后再试");
    }

}
