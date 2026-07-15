package com.ms.middleware.console.agent.context;

import com.ms.middleware.autonomy.run.AutonomyTimelinePhase;
import com.ms.middleware.autonomy.run.TimelineEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * L1 时间线压缩：按关键事件标记打分，再截断到预算条数（token 预算的前置过滤）。
 *
 * <p>优先级：DETECT / PLAN / AUTO / STABLE / ACCEPTED / PUBLISH 等关键 phase 高于普通 INFO。</p>
 */
public final class TimelineEventScorer {

    private TimelineEventScorer() {
    }

    /**
     * 从完整时间线选出最多 {@code limit} 条：先按分数，同分取更靠后（更新），再按时间顺序输出。
     */
    public static List<TimelineEvent> selectCritical(List<TimelineEvent> timeline, int limit) {
        if (timeline == null || timeline.isEmpty() || limit <= 0) {
            return List.of();
        }
        if (timeline.size() <= limit) {
            return List.copyOf(timeline);
        }

        List<Scored> scored = new ArrayList<>(timeline.size());
        for (int i = 0; i < timeline.size(); i++) {
            TimelineEvent event = timeline.get(i);
            scored.add(new Scored(i, score(event), event));
        }

        // 注意：thenComparing 用独立 reverse，避免整体 reversed() 把分数序颠倒回来
        scored.sort(Comparator
                .comparingInt(Scored::getScore).reversed()
                .thenComparing(Comparator.comparingInt(Scored::getIndex).reversed()));

        List<Scored> picked = new ArrayList<>(scored.subList(0, Math.min(limit, scored.size())));
        picked.sort(Comparator.comparingInt(Scored::getIndex));

        List<TimelineEvent> result = new ArrayList<>(picked.size());
        for (Scored item : picked) {
            result.add(item.getEvent());
        }
        return result;
    }

    /** 单条事件评分：关键 phase / 高严重级别优先 */
    static int score(TimelineEvent event) {
        if (event == null) {
            return 0;
        }
        int phaseScore = scorePhase(event.getPhase());
        int levelScore = scoreLevel(event.getLevel());
        return phaseScore * 10 + levelScore;
    }

    private static int scorePhase(String phase) {
        if (phase == null || phase.isBlank()) {
            return 1;
        }
        String p = phase.trim().toUpperCase(Locale.ROOT);
        if (AutonomyTimelinePhase.DETECT.code().equals(p)) {
            return 10;
        }
        if (AutonomyTimelinePhase.STABLE.code().equals(p)) {
            return 10;
        }
        if (AutonomyTimelinePhase.AUTO.code().equals(p) || "ACTION".equals(p)) {
            return 9;
        }
        if (AutonomyTimelinePhase.PLAN.code().equals(p)) {
            return 8;
        }
        if (AutonomyTimelinePhase.ACCEPTED.code().equals(p) || AutonomyTimelinePhase.PUBLISH.code().equals(p)) {
            return 8;
        }
        if (AutonomyTimelinePhase.ADVISE.code().equals(p) || AutonomyTimelinePhase.RECOMMEND.code().equals(p)) {
            return 7;
        }
        return 2;
    }

    private static int scoreLevel(String level) {
        if (level == null) {
            return 0;
        }
        String l = level.trim().toUpperCase(Locale.ROOT);
        if ("ERROR".equals(l)) {
            return 3;
        }
        if ("WARN".equals(l) || "WARNING".equals(l)) {
            return 2;
        }
        return 0;
    }

    /** 普通静态内部类：避免方法内 local record 触发 IDE 误报 */
    private static final class Scored {
        private final int index;
        private final int score;
        private final TimelineEvent event;

        private Scored(int index, int score, TimelineEvent event) {
            this.index = index;
            this.score = score;
            this.event = event;
        }

        private int getIndex() {
            return index;
        }

        private int getScore() {
            return score;
        }

        private TimelineEvent getEvent() {
            return event;
        }
    }
}
