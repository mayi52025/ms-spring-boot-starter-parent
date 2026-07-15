import { useState } from 'react'
import { useConsole } from '../context/ConsoleContext'
import { computeMttr } from '../utils/runHelpers'
import { ChatMessage } from './ChatMessage'
import { PanelHeader } from './PanelHeader'

const HISTORY_DEFAULT_VISIBLE = 5

function incidentColor(issues: string[] = []): string {
  const text = issues.join(' ')
  if (/Rabbit|MQ|消息/i.test(text)) return 'dot-amber'
  if (/Redis|缓存/i.test(text)) return 'dot-rose'
  if (/Nacos|配置/i.test(text)) return 'dot-violet'
  return 'dot-cyan'
}

export function HistoryPanel() {
  const { historyRuns, historyError, selectedRunId, viewingHistory, selectHistoryRun, chatLines, clearChat } =
    useConsole()
  const [historyCollapsed, setHistoryCollapsed] = useState(false)
  const [historyExpanded, setHistoryExpanded] = useState(false)

  const hasMore = historyRuns.length > HISTORY_DEFAULT_VISIBLE
  const visibleRuns = historyExpanded ? historyRuns : historyRuns.slice(0, HISTORY_DEFAULT_VISIBLE)
  const hiddenCount = Math.max(0, historyRuns.length - HISTORY_DEFAULT_VISIBLE)

  return (
    <aside className="panel panel-side">
      <PanelHeader
        title="恢复历史"
        subtitle="STABLE 归档"
        count={historyRuns.length}
        collapsed={historyCollapsed}
        onToggleCollapse={() => setHistoryCollapsed((v) => !v)}
        icon="history"
      />

      {!historyCollapsed ? (
        <div className="panel-body">
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

      <div className="panel-divider" />

      <PanelHeader
        title="运维助手"
        subtitle="规则 · Phase 5.1 LLM"
        count={chatLines.length || undefined}
        actions={
          chatLines.length ? (
            <button type="button" className="btn-ghost-sm" onClick={clearChat}>
              清空
            </button>
          ) : null
        }
      />

      {chatLines.length ? (
        <div className="chat-log">
          {chatLines.map((line, idx) => (
            <ChatMessage key={idx} role={line.role} text={line.text} />
          ))}
        </div>
      ) : (
        <div className="chat-empty-wrap">
          <div className="chat-empty-icon">💬</div>
          <p>底部输入或 Trace 查询</p>
        </div>
      )}
    </aside>
  )
}
