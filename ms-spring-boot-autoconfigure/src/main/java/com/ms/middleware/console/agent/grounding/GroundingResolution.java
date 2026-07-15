package com.ms.middleware.console.agent.grounding;

import java.util.List;

/**
 * 用户意图解析结果：运维类问题映射到必调 Insight Tool。
 */
public record GroundingResolution(
        GroundingIntent intent,
        List<InsightToolInvocation> requiredTools) {

    public boolean opsQuestion() {
        return intent != GroundingIntent.CHITCHAT;
    }
}
