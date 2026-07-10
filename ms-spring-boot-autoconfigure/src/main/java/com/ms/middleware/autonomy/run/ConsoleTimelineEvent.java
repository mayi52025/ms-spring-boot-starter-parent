package com.ms.middleware.autonomy.run;

import org.springframework.context.ApplicationEvent;

/**
 * Spring 事件：账本写入时间线时发布，{@link com.ms.middleware.console.stream.ConsoleStreamHub} 监听并 SSE 广播。
 */
public class ConsoleTimelineEvent extends ApplicationEvent {

    private final TimelineEvent timelineEvent;

    public ConsoleTimelineEvent(Object source, TimelineEvent timelineEvent) {
        super(source);
        this.timelineEvent = timelineEvent;
    }

    public TimelineEvent getTimelineEvent() {
        return timelineEvent;
    }
}
