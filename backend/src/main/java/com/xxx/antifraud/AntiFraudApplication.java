package com.xxx.antifraud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 反诈信息识别与风险自测平台后端启动类
 *
 * 本项目基于 Spring Boot 3.x，提供诈骗案例管理、训练记录、
 * 风险测评与个性化评估报告、防骗知识库等核心能力，
 * 适合作为毕业设计后端示例。
 */
@SpringBootApplication
@MapperScan("com.xxx.antifraud.mapper")
public class AntiFraudApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntiFraudApplication.class, args);
    }
}


