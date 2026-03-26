package com.ms.middleware;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 中间件自动配置类
 * 用于自动配置 Redis、RabbitMQ、缓存、AI 等中间件
 */
@Configuration
@EnableConfigurationProperties(MsMiddlewareProperties.class)
public class MsMiddlewareAutoConfiguration {
    public MsMiddlewareAutoConfiguration() {
        System.out.println("MsMiddlewareAutoConfiguration");
    }
}