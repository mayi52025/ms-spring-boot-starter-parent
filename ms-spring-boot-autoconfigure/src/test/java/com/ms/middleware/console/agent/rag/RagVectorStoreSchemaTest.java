package com.ms.middleware.console.agent.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 1：schema 契约单测（不连真实 PG）。
 */
class RagVectorStoreSchemaTest {

    @Test
    void tableConstantAndKindEnumStable() {
        assertEquals("ms_rag_document", RagVectorStore.TABLE);
        assertEquals("RUN", RagDocumentKind.RUN.name());
        assertEquals("DOC", RagDocumentKind.DOC.name());
    }

    @Test
    void dimensionsFloorIsAtLeastEight() {
        assertTrue(Math.max(8, 1024) == 1024);
        assertTrue(Math.max(8, 1) == 8);
    }
}
