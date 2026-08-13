package com.zhiguan.gujian.shared.common;

import com.zhiguan.gujian.shared.common.Result;
import com.zhiguan.gujian.auth.interfaces.AuthController;
import com.zhiguan.gujian.auth.interfaces.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试 — 校验/解析/上传超限三类异常映射
 */
@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("参数校验失败 - 返回 400 与字段错误信息")
    void validationFailure_returns400() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new RegisterRequest(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "password", "密码长度不能少于6位"));

        MethodParameter parameter = new MethodParameter(
                AuthController.class.getMethod("register", RegisterRequest.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Result<Void>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("password"));
        assertTrue(response.getBody().getMessage().contains("密码长度不能少于6位"));
    }

    @Test
    @DisplayName("请求体解析失败 - 返回 400")
    void messageNotReadable_returns400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Could not read JSON: broken", new RuntimeException("broken json"));

        ResponseEntity<Result<Void>> response = handler.handleMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    @DisplayName("上传超出大小限制 - 返回 413")
    void maxUploadSize_returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5 * 1024 * 1024);

        ResponseEntity<Result<Void>> response = handler.handleMaxUploadSize(ex);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(413, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("5MB"));
    }
}
