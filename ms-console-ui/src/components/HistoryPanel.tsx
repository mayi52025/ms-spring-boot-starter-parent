import { useState, type PointerEventHandler } from 'react'
import { useConsole } from '../hooks/useConsole'
import { useDragResize } from '../hooks/useDragResize'
import { computeMttr } from '../utils/runHelpers'
import { AssistantChatMessages } from './AssistantChatMessages'
import { AssistantPanelActions } from './AssistantPanelActions'
import { PanelHeader } from './PanelHeader'

const HISTORY_DEFAULT_VISIBLE = 5

function incidentColor(issues: string[] = []): string {
  const text = issues.join(' ')
  if (/Rabbit|MQ|消息/i.test(text)) return 'dot-amber'
  if (/Redis|缓存/i.test(text)) return 'dot-rose'
  if (/Nacos|配置/i.test(text)) return 'dot-violet'
  return 'dot-cyan'
}

interface HistoryPanelProps {
  sideWidth: number
  onSideResizePointerDown: PointerEventHandler<HTMLDivElement>
}

export function HistoryPanel({ sideWidth, onSideResizePointerDown }: HistoryPanelProps) {
  const { historyRuns, historyError, selectedRunId, viewingHistory, selectHistoryRun, chatLines, clearChat, llmEnabled, openAssistantOverlay } =
    useConsole()
  const [historyCollapsed, setHistoryCollapsed] = useState(false)
  const [historyExpanded, setHistoryExpanded] = useState(false)

  const chatResize = useDragResize({
    axis: 'y',
    initial: 360,
    min: 200,
    max: 640,
    storageKey: 'ms-console-chat-height',
    invert: true,
  })

  const hasMore = historyRuns.length > HISTORY_DEFAULT_VISIBLE
  const visibleRuns = historyExpanded ? historyRuns : historyRuns.slice(0, HISTORY_DEFAULT_VISIBLE)
  const hiddenCount = Math.max(0, historyRuns.length - HISTORY_DEFAULT_VISIBLE)

  return (
    <aside className="panel panel-side panel-side-resizable" style={{ width: sideWidth }}>
      <div
        className="resize-handle resize-handle-vertical"
        role="separator"
        aria-orientation="vertical"
        aria-label="拖动调整侧栏宽度"
        title="拖动调整侧栏宽度"
        onPointerDown={onSideResizePointerDown}
      />

      <div className="side-history-zone">
        <PanelHeader
          title="恢复历史"
          subtitle="STABLE 归档"
          count={historyRuns.length}
          collapsed={historyCollapsed}
          onToggleCollapse={() => setHistoryCollapsed((v) => !v)}
          icon="history"
        />

        {!historyCollapsed ? (
          <div className="panel-body side-history-body">
            {historyError ? (
              <p className="error-text">{historyError}</p>
            ) : !historyRuns.length ? (
              <p className="empty-state">暂无历史</p>
            ) : (
              <>
                <div className="history-timeline">
                  {visibleRuns.map((run, idx) => {
                    const mttr = run.mttrSeconds != null ? run.mttrSeconds : computeMttr(run)
                    const active = viewingHistory && selectedRunId === run.runId
                    const dotClass = incidentColor(run.issues)
                    return (
                      <button
                        key={run.runId}
                        type="button"
                        className={`history-node ${active ? 'active' : ''}`}
                        style={{ animationDelay: `${idx * 50}ms` }}
                        onClick={() => void selectHistoryRun(run.runId)}
                      >
                        <div className="node-rail">
                          <span className={`node-dot ${dotClass}`} />
                          {idx < visibleRuns.length - 1 ? <span className="node-line" /> : null}
                        </div>
                        <div className="node-body">
                          <div className="node-top">
                            <code>#{run.runId.slice(-8)}</code>
                            {mttr != null ? <em>{mttr}s</em> : null}
                          </div>
                          <p>{(run.issues || []).join(' · ') || '—'}</p>
                        </div>
                      </button>
                    )
                  })}
                </div>
                {hasMore ? (
                  <button type="button" className="btn-expand" onClick={() => setHistoryExpanded((v) => !v)}>
                    {historyExpanded ? '↑ 收起' : `↓ 还有 ${hiddenCount} 条`}
                  </button>
                ) : null}
              </>
            )}
          </div>
        ) : null}
      </div>

      <div
        className="resize-handle resize-handle-horizontal"
        role="separator"
        aria-orientation="horizontal"
        aria-label="拖动调整助手区域高度"
        title="拖动调整助手区域高度"
        onPointerDown={chatResize.onPointerDown}
      />

      <div className="side-chat-zone" style={{ height: chatResize.size }}>
        <PanelHeader
          title="运维助手"
          subtitle={llmEnabled ? 'LLM + Tool Grounding' : '规则模式'}
          count={chatLines.length || undefined}
          actions={
            <AssistantPanelActions
              hasChat={chatLines.length > 0}
              onExpand={openAssistantOverlay}
              onClear={clearChat}
            />
          }
        />

        <AssistantChatMessages />
      </div>
    </aside>
  )
}
