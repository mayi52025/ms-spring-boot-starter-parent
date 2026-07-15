import { useEffect } from 'react'
import { useConsole } from '../context/ConsoleContext'
import { IconAlert, IconShield } from './icons'
import { PanelHeader } from './PanelHeader'

export function IssuesPanel() {
  const {
    activeRuns,
    issuesError,
    bootstrapError,
    selectedRunId,
    viewingHistory,
    selectRun,
    showHealthyView,
  } = useConsole()

  useEffect(() => {
    if (!activeRuns.length) {
      showHealthyView()
    }
  }, [activeRuns.length, showHealthyView])

  if (bootstrapError) {
    return (
      <aside className="panel panel-nav">
        <PanelHeader title="故障队列" subtitle="连接失败" icon="alert" />
        <p className="error-text">无法连接 API：{bootstrapError}</p>
      </aside>
    )
  }

  return (
    <aside className="panel panel-nav">
      <PanelHeader
        title="故障队列"
        subtitle={activeRuns.length ? '点击切换详情' : '巡检正常'}
        count={activeRuns.length || undefined}
        icon="shield"
      />

      {issuesError ? (
        <p className="error-text">{issuesError}</p>
      ) : !activeRuns.length ? (
        <div className="nav-idle">
          <div className="nav-idle-icon">
            <IconShield />
          </div>
          <strong>零活跃故障</strong>
          <p>自治模块 10s 周期扫描</p>
        </div>
      ) : (
        <div className="issue-list">
          {activeRuns.map((run, idx) => (
            <div
              key={run.runId}
              className={`issue-card wartime ${!viewingHistory && selectedRunId === run.runId ? 'active' : ''}`}
              style={{ animationDelay: `${idx * 60}ms` }}
              onClick={() => void selectRun(run.runId)}
              onKeyDown={(e) => e.key === 'Enter' && void selectRun(run.runId)}
              role="button"
              tabIndex={0}
            >
              <div className="issue-accent" />
              <div className="card-top">
                <span className="run-id">#{run.runId.slice(-8)}</span>
                <span className={`status-tag tag-${run.status.toLowerCase()}`}>{run.status}</span>
              </div>
              <div className="run-summary">{(run.issues || []).join(' · ') || '—'}</div>
            </div>
          ))}
        </div>
      )}

      {activeRuns.length > 0 ? (
        <div className="nav-footnote">
          <IconAlert />
          <span>战时态优先展示止血动作</span>
        </div>
      ) : null}
    </aside>
  )
}
