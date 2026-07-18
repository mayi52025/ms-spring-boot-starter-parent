package com.ms.middleware.console.agent.rag;

import com.ms.middleware.autonomy.run.RunStabilizedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * STABLE 事件异步索引；失败只打日志，不回灌 tick。
 */
public class RagIndexEventListener {

    private static final Logger log = LoggerFactory.getLogger(RagIndexEventListener.class);

    private final RagIndexer indexer;

    public RagIndexEventListener(RagIndexer indexer) {
        this.indexer = indexer;
    }

    @Async("ragIndexExecutor")
    @EventListener
    public void onRunStabilized(RunStabilizedEvent event) {
        if (event == null || event.getRun() == null) {
            return;
        }
        try {
            indexer.indexStableRun(event.getRun());
        } catch (Exception ex) {
            log.warn("RAG async indexStableRun failed runId={}: {}",
                    event.getRun().getRunId(), ex.getMessage());
        }
    }
}
