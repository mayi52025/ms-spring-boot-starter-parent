package com.ms.middleware.autonomy.run;

import org.springframework.context.ApplicationEvent;

/**
 * 时间线事件，供控制台 SSE 订阅
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
