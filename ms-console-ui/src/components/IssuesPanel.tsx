import { useConsole } from '../hooks/useConsole'
import { IconAlert, IconShield } from './icons'
import { PanelHeader } from './PanelHeader'

export function IssuesPanel() {
  const {
    activeRuns,
    issuesError,
    bootstrapError,
    navFocus,
    selectedRunId,
    viewingHistory,
    selectRun,
    showHealthyView,
  } = useConsole()

  const onHealthy = navFocus.mode === 'home'

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
        title="导航"
        subtitle={activeRuns.length ? `${activeRuns.length} 个活跃故障` : '巡检正常'}
        count={activeRuns.length || undefined}
        icon="shield"
      />

      {/* 主页面入口：可随时点回；选中态清晰 */}
      <button
        type="button"
        className={`home-nav-btn ${onHealthy ? 'active' : ''}`}
        onClick={showHealthyView}
        title="健康总览主页面"
      >
        <IconShield />
        <span>健康总览（主页）</span>
      </button>

      {issuesError ? (
        <p className="error-text">{issuesError}</p>
      ) : !activeRuns.length ? (
        <div className="nav-idle">
          <div className="nav-idle-icon">
            <IconShield />
          </div>
          <strong>零活跃故障</strong>
          <p>点上方回主页，或右侧打开恢复历史（不会被自动拽回）</p>
        </div>
      ) : (
        <div className="issue-list">
          {activeRuns.map((run, idx) => (
            <div
              key={run.runId}
              className={`issue-card wartime ${!viewingHistory && selectedRunId === run.runId ? 'active' : ''}`}
              style={{ animationDelay: `${idx * 60}ms` }}
              onClick={() => void selectRun(run.runId, false)}
              onKeyDown={(e) => e.key === 'Enter' && void selectRun(run.runId, false)}
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
          <span>点故障卡查看 · 点主页可随时返回</span>
        </div>
      ) : null}
    </aside>
  )
}
