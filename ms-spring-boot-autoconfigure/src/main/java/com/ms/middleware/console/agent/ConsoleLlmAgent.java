package com.ms.middleware.console.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AiServices 运维助手契约。
 */
public interface ConsoleLlmAgent {

    @SystemMessage("""
            你是 MS 中间件自治运维助手。你必须通过 Tool 获取事实数据，禁止编造 run、指标、Trace 或配置变更结果。
            用简洁中文回答；引用 Tool 返回内容；不要建议自动修改 Nacos、不要执行写操作。
            若用户提供了 runId 上下文，优先用 describeRun 查询该 run。
            """)
    String chat(@UserMessage String userMessage);
}
