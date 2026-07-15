package com.ms.middleware.console.agent;

import com.ms.middleware.console.agent.grounding.InsightToolGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 5.1/5.2：LangChain4j Agent + Insight Tool Grounding。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "ms.middleware.console", name = "llm-enabled", havingValue = "true")
public class ConsoleLlmAutoConfiguration {

    @Bean
    public MiddlewareInsightLangChainTools middlewareInsightLangChainTools(InsightToolGateway insightToolGateway) {
        return new MiddlewareInsightLangChainTools(insightToolGateway);
    }
}
