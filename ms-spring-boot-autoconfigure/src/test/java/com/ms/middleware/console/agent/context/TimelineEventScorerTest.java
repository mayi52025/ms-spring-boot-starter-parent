package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import com.ms.middleware.autonomy.run.TimelineEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimelineEventScorerTest {

    @Test
    void prefersCriticalPhasesOverNoise() {
        List<TimelineEvent> timeline = List.of(
                event("INFO", "HEARTBEAT", "noise-1"),
                event("INFO", "HEARTBEAT", "noise-2"),
                event("INFO", AutonomyTimelinePhase.DETECT.code(), "检测到故障"),
                event("INFO", "HEARTBEAT", "noise-3"),
                event("INFO", AutonomyTimelinePhase.AUTO.code(), "限流已执行"),
                event("INFO", "HEARTBEAT", "noise-4"),
                event("INFO", AutonomyTimelinePhase.STABLE.code(), "已恢复"));

        List<TimelineEvent> selected = TimelineEventScorer.selectCritical(timeline, 3);

        assertEquals(3, selected.size());
        assertEquals(AutonomyTimelinePhase.DETECT.code(), selected.get(0).getPhase());
        assertEquals(AutonomyTimelinePhase.AUTO.code(), selected.get(1).getPhase());
        assertEquals(AutonomyTimelinePhase.STABLE.code(), selected.get(2).getPhase());
    }

    @Test
    void keepsChronologicalOrderAmongPicked() {
        List<TimelineEvent> timeline = List.of(
                event("INFO", AutonomyTimelinePhase.DETECT.code(), "d"),
                event("INFO", AutonomyTimelinePhase.STABLE.code(), "s"),
                event("INFO", AutonomyTimelinePhase.AUTO.code(), "a"));

        List<TimelineEvent> selected = TimelineEventScorer.selectCritical(timeline, 2);

        // DETECT(100) 与 STABLE(100) 同分时优先更靠后；再按时间序输出
        assertEquals(2, selected.size());
        assertEquals(AutonomyTimelinePhase.DETECT.code(), selected.get(0).getPhase());
        assertEquals(AutonomyTimelinePhase.STABLE.code(), selected.get(1).getPhase());
    }

    @Test
    void underLimitReturnsAll() {
        List<TimelineEvent> timeline = List.of(
                event("INFO", "FOO", "a"),
                event("WARN", "BAR", "b"));
        assertEquals(2, TimelineEventScorer.selectCritical(timeline, 5).size());
        assertTrue(TimelineEventScorer.score(event("ERROR", "X", "e"))
                > TimelineEventScorer.score(event("INFO", "X", "i")));
    }

    private static TimelineEvent event(String level, String phase, String message) {
        TimelineEvent e = new TimelineEvent("run", phase, message);
        e.setLevel(level);
        e.setAt(Instant.parse("2026-07-15T10:00:00Z"));
        return e;
    }
}
