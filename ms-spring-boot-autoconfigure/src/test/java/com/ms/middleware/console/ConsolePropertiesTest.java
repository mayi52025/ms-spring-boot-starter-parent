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

    @Test
    void ragDefaultsDisabled() {
        MsMiddlewareProperties.ConsoleProperties console = new MsMiddlewareProperties.ConsoleProperties();
        assertFalse(console.getRag().isEnabled());
        assertTrue(console.getRag().getEmbedding().getDimensions() > 0);
    }

    @Test
    void ragBindsNestedEmbedding() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("ms.middleware.console.rag.enabled", "true")
                .withProperty("ms.middleware.console.rag.jdbc-url", "jdbc:postgresql://192.168.100.102:5432/ms_rag")
                .withProperty("ms.middleware.console.rag.embedding.model", "text-embedding-v3")
                .withProperty("ms.middleware.console.rag.embedding.dimensions", "1024");
        MsMiddlewareProperties.ConsoleProperties console = bind(env);
        assertTrue(console.getRag().isEnabled());
        assertTrue(console.getRag().getJdbcUrl().contains("ms_rag"));
        assertTrue("text-embedding-v3".equals(console.getRag().getEmbedding().getModel()));
        assertTrue(console.getRag().getEmbedding().getDimensions() == 1024);
    }

    private MsMiddlewareProperties.ConsoleProperties bind(MockEnvironment env) {
        return Binder.get(env)
                .bind("ms.middleware.console", MsMiddlewareProperties.ConsoleProperties.class)
                .get();
    }
}
