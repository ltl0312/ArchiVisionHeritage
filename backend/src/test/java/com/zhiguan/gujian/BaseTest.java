package com.zhiguan.gujian;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 测试基类 — 提供通用配置和工具方法
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {

    /**
     * 创建测试用户数据
     */
    protected static final String TEST_USERNAME = "testuser";
    protected static final String TEST_PASSWORD = "test123";
    protected static final String TEST_NICKNAME = "测试用户";
    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin123";
}
