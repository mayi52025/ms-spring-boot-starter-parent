package com.ms.middleware.console.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagIndexerChunkTest {

    @Test
    void shortTextSingleChunk() {
        List<String> chunks = RagIndexer.chunk("hello rag", 800);
        assertEquals(1, chunks.size());
        assertEquals("hello rag", chunks.get(0));
    }

    @Test
    void prefersNewlineBreak() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("line-").append(i).append('\n');
        }
        List<String> chunks = RagIndexer.chunk(sb.toString(), 40);
        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).length() <= 40);
    }

    @Test
    void vectorLiteralFormat() {
        String lit = RagVectorStore.toVectorLiteral(new float[]{0.1f, -0.2f});
        assertTrue(lit.startsWith("["));
        assertTrue(lit.contains(","));
        assertTrue(lit.endsWith("]"));
    }
}
