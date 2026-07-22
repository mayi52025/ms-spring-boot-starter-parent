package com.ms.middleware.console.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagIndexerChunkTest {

    @Test
    void shortTextSingleChunk() {
        List<String> chunks = TextChunker.chunk("hello rag", 800, 0);
        assertEquals(1, chunks.size());
        assertEquals("hello rag", chunks.get(0));
    }

    @Test
    void chineseSentencesPackIntoWindows() {
        String text = "第一句说明 tick 锁。第二句说明多实例跳过 AUTO。第三句说明鉴权 token。第四句说明 Agent 只读。";
        List<String> chunks = TextChunker.chunk(text, 40, 10);
        assertTrue(chunks.size() >= 2, "expected multiple chunks, got " + chunks);
        assertTrue(chunks.get(0).contains("tick") || chunks.get(0).contains("第一句"));
    }

    @Test
    void overlapKeepsBoundaryContext() {
        String text = "AAAA。BBBB。CCCC。DDDD。EEEE。FFFF。";
        List<String> chunks = TextChunker.chunk(text, 12, 4);
        assertTrue(chunks.size() >= 2);
        // 后一块应带上前一块尾部重叠（可能截断在句中，但长度关系成立）
        assertTrue(chunks.get(1).length() <= 12 + 4);
    }

    @Test
    void prefersNewlineAsBoundary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("line-").append(i).append('\n');
        }
        List<String> chunks = TextChunker.chunk(sb.toString(), 40, 0);
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
