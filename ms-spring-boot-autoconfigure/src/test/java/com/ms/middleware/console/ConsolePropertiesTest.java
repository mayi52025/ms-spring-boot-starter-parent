package com.ms.middleware.console;

import com.ms.middleware.MsMiddlewareProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 控制台配置绑定：llm-enabled 与废弃 chat-enabled 兼容。
 */
class ConsolePropertiesTest {

    @Test
    void llmEnabledBindsDirectly() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("ms.middleware.console.llm-enabled", "true");
        MsMiddlewareProperties.ConsoleProperties console = bind(env);
        assertTrue(console.isLlmEnabled());
    }

    @Test
    void legacyChatEnabledStillEnablesLlm() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("ms.middleware.console.chat-enabled", "true");
        MsMiddlewareProperties.ConsoleProperties console = bind(env);
        assertTrue(console.isLlmEnabled());
    }

    @Test
    void llmEnabledTakesPrecedenceOverLegacyChatEnabled() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("ms.middleware.console.chat-enabled", "true")
                .withProperty("ms.middleware.console.llm-enabled", "false");
        MsMiddlewareProperties.ConsoleProperties console = bind(env);
        assertFalse(console.isLlmEnabled());
    }

    @Test
    void groundingModeBindsUnderLlm() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("ms.middleware.console.llm.grounding-mode", "strict");
        MsMiddlewareProperties props = Binder.get(env)
                .bind("ms.middleware", MsMiddlewareProperties.class)
                .get();
        assertTrue("strict".equalsIgnoreCase(props.getConsole().getLlm().getGroundingMode()));
    }

    private MsMiddlewareProperties.ConsoleProperties bind(MockEnvironment env) {
        return Binder.get(env)
                .bind("ms.middleware.console", MsMiddlewareProperties.ConsoleProperties.class)
                .get();
    }
}
