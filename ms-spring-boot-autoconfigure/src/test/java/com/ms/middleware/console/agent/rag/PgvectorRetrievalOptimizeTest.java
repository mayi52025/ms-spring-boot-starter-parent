package com.ms.middleware.console.agent.rag;

import com.ms.middleware.console.agent.context.RetrievalQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PgvectorRetrievalOptimizeTest {

    @Test
    void primingPrefixesDocumentQueries() {
        String primed = PgvectorRetrievalContextProvider.primingForEmbed(
                RetrievalQuery.RetrievalKind.DOCUMENT, "tick 锁多实例要注意什么");
        assertTrue(primed.startsWith("中间件自治运维手册"));
        assertTrue(primed.contains("tick 锁"));
    }

    @Test
    void lexicalTermsExtractOpsAnchors() {
        List<String> terms = RagVectorStore.extractLexicalTerms("手册里 MQ 自治怎么止血");
        assertTrue(terms.contains("手册"));
        assertTrue(terms.contains("mq") || terms.stream().anyMatch(t -> t.equalsIgnoreCase("mq")));
        assertTrue(terms.contains("止血"));
    }

    @Test
    void lexicalTermsEmptyWithoutAnchor() {
        assertEquals(List.of(), RagVectorStore.extractLexicalTerms("今天天气怎么样"));
    }
}
