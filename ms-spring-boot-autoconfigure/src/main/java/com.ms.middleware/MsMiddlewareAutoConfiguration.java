package com.ms.middleware;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsMiddlewareAutoConfiguration {
    public MsMiddlewareAutoConfiguration() {
        System.out.println("MsMiddlewareAutoConfiguration");
    }
}
