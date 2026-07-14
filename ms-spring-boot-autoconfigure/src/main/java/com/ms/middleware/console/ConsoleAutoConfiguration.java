package com.ms.middleware.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.console.auth.ConsoleAuthFilter;
import com.ms.middleware.console.auth.ConsoleAuthSupport;
import com.ms.middleware.console.stream.ConsoleStreamHub;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 控制台模块自动配置。依赖业务应用引入 spring-boot-starter-web。
 * 开关：{@code ms.middleware.console.enabled=true}
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "ms.middleware.console", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MsMiddlewareProperties.class)
@ComponentScan(basePackages = "com.ms.middleware.console")
public class ConsoleAutoConfiguration {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConsoleAutoConfiguration.class);

    public ConsoleAutoConfiguration() {
        log.info(">>> ConsoleAutoConfiguration initialized - console module loaded");
    }

    @Bean
    public ConsoleStreamHub consoleStreamHub(ObjectMapper objectMapper) {
        return new ConsoleStreamHub(objectMapper);
    }

    @Bean
    public ConsoleAuthSupport consoleAuthSupport() {
        return new ConsoleAuthSupport();
    }

    @Bean
    public FilterRegistrationBean<ConsoleAuthFilter> consoleAuthFilterRegistration(
            MsMiddlewareProperties properties,
            ConsoleAuthSupport consoleAuthSupport) {
        FilterRegistrationBean<ConsoleAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ConsoleAuthFilter(properties, consoleAuthSupport));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.setName("consoleAuthFilter");
        return registration;
    }
}
