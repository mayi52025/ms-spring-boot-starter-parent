import type { AdoptionMode, TimelineEvent } from '../api/types'
import { isNacosDraftMode } from '../utils/runHelpers'

function timelinePhaseBadge(phase: string, adoptionMode: AdoptionMode) {
  if (phase === 'ACCEPTED') {
    return isNacosDraftMode(adoptionMode) ? (
      <span className="badge badge-draft">已生成配置草稿</span>
    ) : (
      <span className="badge badge-accepted">已记录采纳（未改配置）</span>
    )
  }
  if (phase === 'PUBLISH') {
    return <span className="badge badge-published">已确认发布 Nacos</span>
  }
  if (phase === 'AUTO') {
    return <span className="badge badge-auto">自动执行</span>
  }
  return <span className="tl-phase">[{phase}]</span>
}

function timelineClass(phase: string): string {
  if (phase === 'STABLE') return 'tl-item tl-stable'
  if (phase === 'AUTO') return 'tl-item tl-auto'
  if (phase === 'ACCEPTED') return 'tl-item tl-accepted'
  return 'tl-item'
}

export function TimelineView({
  events,
  adoptionMode,
}: {
  events: TimelineEvent[]
  adoptionMode: AdoptionMode
}) {
  if (!events.length) {
    return <div className="timeline muted-text">无时间线</div>
  }

  return (
    <div className="timeline">
      {events.map((e, idx) => (
        <div className={timelineClass(e.phase)} key={`${e.at}-${e.phase}-${idx}`}>
          {timelinePhaseBadge(e.phase, adoptionMode)} {e.at} — {e.message}
        </div>
      ))}
    </div>
  )
}
