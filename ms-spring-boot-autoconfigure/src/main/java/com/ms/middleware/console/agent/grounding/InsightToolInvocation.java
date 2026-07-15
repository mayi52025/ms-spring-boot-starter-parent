package com.ms.middleware.console.agent.grounding;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 待执行的 Insight Tool 调用（含参数）。
 */
public record InsightToolInvocation(InsightToolName tool, List<String> args) {

    public InsightToolInvocation {
        Objects.requireNonNull(tool, "tool");
        args = args == null || args.isEmpty() ? List.of() : List.copyOf(args);
    }

    public static InsightToolInvocation of(InsightToolName tool, String... values) {
        if (values == null || values.length == 0) {
            return new InsightToolInvocation(tool, List.of());
        }
        return new InsightToolInvocation(tool, Arrays.asList(values));
    }

    public String arg(int index) {
        return index >= 0 && index < args.size() ? args.get(index) : null;
    }
}
