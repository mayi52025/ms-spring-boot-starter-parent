package com.ms.middleware.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.console.stream.ConsoleStreamHub;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
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
}
