package com.zhiguan.gujian;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhiguan.gujian.mapper")
public class ZhiguanApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhiguanApplication.class, args);
    }
}
