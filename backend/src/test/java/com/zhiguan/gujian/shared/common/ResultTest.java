package com.zhiguan.gujian.shared.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 统一响应格式测试
 */
@DisplayName("统一 API 响应格式测试")
class ResultTest {

    @Test
    @DisplayName("ok() 返回 code=200, message=success")
    void ok_noArgs_returnsSuccess() {
        Result<Void> result = Result.ok();
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("ok(data) 返回正确数据")
    void ok_withData_returnsData() {
        String testData = "测试数据";
        Result<String> result = Result.ok(testData);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(testData, result.getData());
    }

    @Test
    @DisplayName("ok(data) 支持复杂对象")
    void ok_withComplexData_returnsData() {
        Result<java.util.Map<String, Object>> result = Result.ok(
                java.util.Map.of("key", "value", "count", 42)
        );
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("value", result.getData().get("key"));
    }

    @Test
    @DisplayName("fail(message) 返回 code=500")
    void fail_message_returns500() {
        Result<Void> result = Result.fail("服务器错误");
        assertEquals(500, result.getCode());
        assertEquals("服务器错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail(code, message) 返回指定 code")
    void fail_codeMessage_returnsSpecifiedCode() {
        Result<Void> result = Result.fail(404, "资源不存在");
        assertEquals(404, result.getCode());
        assertEquals("资源不存在", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail 支持各种错误码")
    void fail_variousCodes() {
        assertEquals(400, Result.fail(400, "参数错误").getCode());
        assertEquals(401, Result.fail(401, "未授权").getCode());
        assertEquals(403, Result.fail(403, "禁止访问").getCode());
        assertEquals(429, Result.fail(429, "请求过多").getCode());
        assertEquals(503, Result.fail(503, "服务不可用").getCode());
    }

    @Test
    @DisplayName("Result 支持泛型类型")
    void result_supportsGenerics() {
        Result<Integer> intResult = Result.ok(42);
        assertEquals(42, intResult.getData());

        Result<Boolean> boolResult = Result.ok(true);
        assertTrue(boolResult.getData());

        Result<String> strResult = Result.ok("test");
        assertEquals("test", strResult.getData());
    }
}
