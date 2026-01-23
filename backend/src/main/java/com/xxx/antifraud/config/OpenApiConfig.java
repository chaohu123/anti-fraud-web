package com.xxx.antifraud.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 基础配置
 *
 * 集成 springdoc-openapi，启动后可通过 /swagger-ui.html 进行接口调试，
 * 便于毕业设计答辩展示后端能力。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI antiFraudOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("反诈信息识别与风险自测平台 API")
                        .description("基于 Spring Boot 3 + MyBatis-Plus 的后端服务")
                        .version("1.0.0")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("毕业设计文档")
                        .url("https://example.com/antifraud-docs"));
    }
}

