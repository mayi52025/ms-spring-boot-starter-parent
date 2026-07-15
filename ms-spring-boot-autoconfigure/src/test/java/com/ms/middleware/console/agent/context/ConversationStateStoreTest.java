package com.ms.middleware.console.agent.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ConversationStateStoreTest {

    @Test
    void differentRunUsesDifferentBucket() {
        ConversationStateStore store = new ConversationStateStore();
        ConversationState global = store.getOrCreate("sess-1", null);
        ConversationState runA = store.getOrCreate("sess-1", "run-a");

        global.appendUserMessage("全局问题", 2);
        runA.appendUserMessage("run 问题", 2);

        assertEquals(1, global.getRecentUserMessages().size());
        assertEquals("run 问题", runA.getRecentUserMessages().get(0));
        assertNotSame(global, runA);
    }
}
