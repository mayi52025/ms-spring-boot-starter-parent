package com.ms.middleware.console.agent.context;

import java.util.Optional;

/**
 * 测试用空检索 Provider。
 */
public final class TestRetrievalContextProviders {

    private TestRetrievalContextProviders() {
    }

    public static RetrievalContextProvider empty() {
        return new RetrievalContextProvider() {
            @Override
            public Optional<String> retrieve(RetrievalQuery query, int budgetChars) {
                return Optional.empty();
            }

            @Override
            public String sourceLabel() {
                return "TEST_EMPTY";
            }
        };
    }
}
