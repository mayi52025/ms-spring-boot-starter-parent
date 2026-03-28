package com.ms.middleware;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用中间件功能的注解
 * 使用此注解可以自动配置缓存、消息队列等中间件组件
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MsMiddlewareAutoConfiguration.class)
public @interface EnableMsMiddleware {
}
