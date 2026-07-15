import type { AdoptionMode, AutonomyRecommendation, AutonomyRun, PlannedAction } from '../api/types'
import type { ReactNode } from 'react'
import { useConsole } from '../context/ConsoleContext'
import { computeMttr, isMqRun, isNacosDraftMode, isWartime } from '../utils/runHelpers'
import { TimelineView } from './TimelineView'
import { PanelHeader } from './PanelHeader'
import { HealthyDashboard } from './HealthyDashboard'

function recommendationStatusBadge(
  status: string,
  rec: AutonomyRecommendation | undefined,
  adoptionMode: AdoptionMode,
) {
  if (status === 'ACCEPTED') {
    if (isNacosDraftMode(adoptionMode) && rec?.nacosPublished) {
      return <span className="badge badge-published">已发布 Nacos 配置</span>
    }
    if (isNacosDraftMode(adoptionMode) && rec?.draftId) {
      return <span className="badge badge-draft">草稿待发布</span>
    }
    return isNacosDraftMode(adoptionMode) ? (
      <span className="badge badge-draft">已生成草稿</span>
    ) : (
      <span className="badge badge-accepted">已记录采纳（未改配置）</span>
    )
  }
  if (status === 'REJECTED') {
    return <span className="badge badge-rejected">已拒绝</span>
  }
  return null
}

function RecoveryEvidence({ run }: { run: AutonomyRun }) {
  const ev = run.recoveryEvidence
  if (!ev) return null
  return (
    <div className="recovery-evidence">
      <strong>恢复依据</strong> · {ev.summary || '—'}
      {(ev.metrics || []).map((m, idx) => (
        <div className="recovery-metric" key={`${m.key || m.label}-${idx}`}>
          <span className="label">{m.label || m.key}</span>
          <span>
            {m.beforeValue} → {m.afterValue}
            {m.threshold ? ` （判定 ${m.threshold}）` : ''}
          </span>
        </div>
      ))}
      {ev.resolutionRule ? <div className="rule">判定条件：{ev.resolutionRule}</div> : null}
    </div>
  )
}

function PrimaryAction({ action }: { action: PlannedAction }) {
  const executed = action.executionStatus === 'SUCCESS' || action.policyDecision === 'AUTO'
  const bannerCls = executed ? 'action-primary' : 'action-primary pending'

  let statusText: ReactNode
  if (action.executionStatus === 'SUCCESS') {
    statusText = (
      <>
        <span className="badge badge-auto">自动执行</span>{' '}
        <span style={{ color: 'var(--ok)' }}>✓ 已完成</span>
        {action.executionDetail ? ` — ${action.executionDetail}` : ''}
      </>
    )
  } else if (action.policyDecision === 'AUTO') {
    statusText = (
      <>
        <span className="badge badge-auto">自动执行</span>{' '}
        <span style={{ color: 'var(--ok)' }}>已调度</span>
      </>
    )
  } else {
    statusText = (
      <span style={{ color: 'var(--warn)' }}>未自动执行（证据或风险未达门槛，需人工确认）</span>
    )
  }

  return (
    <div className={bannerCls}>
      <strong>
        #1 {action.actionType}
      </strong>
      <br />
      {action.reason || ''}
      <br />
      <span style={{ color: 'var(--muted)', fontSize: '0.8rem' }}>
        证据 {(action.confidence || 0).toFixed(2)} · {statusText}
      </span>
    </div>
  )
}

function Recommendations({ run }: { run: AutonomyRun }) {
  const {
    adoptionMode,
    handleAdoptRecommendation,
    handlePublishDraft,
    handleAdoptAction,
  } = useConsole()

  const recs = run.plan?.recommendations || []
  const actions = run.plan?.actions || []
  const wartime = isWartime(run)
  const nacosDraft = isNacosDraftMode(adoptionMode)

  const primary = actions.find((a) => a.rank === 1)
  const alternates = actions.filter((a) => a.rank > 1)

  const recBody = recs.map((r) => {
    const st = r.status || 'PENDING'
    const cls = st === 'ACCEPTED' ? 'rec accepted' : st === 'REJECTED' ? 'rec rejected' : 'rec'
    const acceptLabel = nacosDraft ? '生成配置草稿' : '记录采纳'
    const pendingHint = nacosDraft ? (
      <p className="section-hint" style={{ margin: '6px 0 0' }}>
        采纳将生成 Nacos 草稿与 diff，不会直接改生产配置
      </p>
    ) : (
      <p className="section-hint" style={{ margin: '6px 0 0' }}>
        采纳仅记录审计，不会自动修改 Nacos/应用配置
      </p>
    )

    return (
      <div className={cls} key={r.recommendationId}>
        <strong>{r.title}</strong> {recommendationStatusBadge(st, r, adoptionMode)}
        <br />
        {r.description}
        {r.suggestedConfig ? (
          <>
            <br />
            <code>{r.suggestedConfig}</code>
          </>
        ) : null}
        {st === 'PENDING' ? (
          <>
            <div className="rec-actions">
              <button
                type="button"
                className="btn-accept"
                onClick={() => void handleAdoptRecommendation(r.recommendationId, run.runId, true)}
              >
                {acceptLabel}
              </button>
              <button
                type="button"
                className="btn-reject"
                onClick={() => void handleAdoptRecommendation(r.recommendationId, run.runId, false)}
              >
                拒绝
              </button>
            </div>
            {pendingHint}
          </>
        ) : null}
        {st === 'ACCEPTED' && nacosDraft && r.diffSummary && !r.nacosPublished ? (
          <>
            <div className="config-diff">{r.diffSummary}</div>
            <div className="rec-actions">
              <button
                type="button"
                className="btn-publish"
                onClick={() => void handlePublishDraft(r.recommendationId, run.runId)}
              >
                确认发布到 Nacos
              </button>
            </div>
          </>
        ) : null}
      </div>
    )
  })

  const sectionHint = nacosDraft
    ? '战后复盘：配置级建议，「生成配置草稿」后展示 diff，须二次「确认发布」'
    : '战后复盘：配置级建议，「记录采纳」仅审计，不自动改配置'

  return (
    <div>
      {primary ? (
        <>
          <h2>自治处置</h2>
          <p className="section-hint">系统首选动作，LOW 风险止血优先自动执行</p>
          <PrimaryAction action={primary} />
        </>
      ) : null}

      {alternates.length ? (
        <>
          <h2 style={{ marginTop: 12 }}>备选方案</h2>
          <p className="section-hint">非首选动作，默认不自动执行，可人工采纳</p>
          {alternates.map((a) => (
            <div className="advise" key={a.rank}>
              <strong>
                #{a.rank} {a.actionType}
              </strong>{' '}
              — {a.reason || ''}
              <br />
              <span style={{ color: 'var(--muted)', fontSize: '0.75rem' }}>
                证据 {(a.confidence || 0).toFixed(2)} · 风险较高或顺位靠后
              </span>
              {!a.humanAccepted ? (
                <div className="rec-actions">
                  <button
                    type="button"
                    className="btn-adopt"
                    onClick={() => void handleAdoptAction(run.runId, a.rank)}
                  >
                    人工采纳并执行
                  </button>
                </div>
              ) : (
                <div style={{ marginTop: 4 }}>
                  <span className="badge badge-manual">人工触发</span>{' '}
                  <span style={{ color: 'var(--ok)', fontSize: '0.75rem' }}>已执行</span>
                </div>
              )}
            </div>
          ))}
        </>
      ) : null}

      {recs.length ? (
        wartime ? (
          <div className="rec-section-collapsed">
            <h2 style={{ marginTop: 12 }}>优化建议</h2>
            <p className="section-hint">战时默认折叠，不阻塞止血；恢复后可展开处理</p>
            <details>
              <summary>展开 {recs.length} 条配置建议</summary>
              {recBody}
            </details>
          </div>
        ) : (
          <>
            <h2 style={{ marginTop: 12 }}>优化建议</h2>
            <p className="section-hint">{sectionHint}</p>
            {recBody}
          </>
        )
      ) : null}
    </div>
  )
}

function FailedTracesPanel() {
  const { failedTraces, queryTraceInChat } = useConsole()
  if (!failedTraces.length) return null

  return (
    <div className="failed-traces">
      <h2 style={{ margin: '0 0 8px' }}>最近失败消息</h2>
      <p className="section-hint" style={{ margin: '0 0 8px' }}>
        无需翻日志，可复制 messageId 或在对话输入 trace &lt;id&gt;
      </p>
      {failedTraces.map((t) => (
        <div className="failed-trace-item" key={t.messageId}>
          <div>
            <strong>{t.messageId}</strong>
            {t.queue ? ` · ${t.queue}` : ''}
          </div>
          <div style={{ color: 'var(--muted)', marginTop: 4 }}>{t.errorMessage || '—'}</div>
          <div style={{ marginTop: 6 }}>
            <button
              type="button"
              className="btn-copy"
              onClick={() => {
                void navigator.clipboard.writeText(t.messageId).catch(() => alert(t.messageId))
              }}
            >
              复制 ID
            </button>
            <button type="button" className="btn-trace" onClick={() => void queryTraceInChat(t.messageId)}>
              查 Trace
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

export function TimelinePanel() {
  const { activeRuns, currentRun, liveTimeline, adoptionMode } = useConsole()

  if (!activeRuns.length && !currentRun) {
    return (
      <section className="panel panel-main">
        <HealthyDashboard />
      </section>
    )
  }

  if (!currentRun) {
    return (
      <section className="panel panel-main">
        <PanelHeader title="自治时间线" subtitle="等待选择事件" />
        <p className="empty-state">选择左侧事件或等待新事件…</p>
      </section>
    )
  }

  const mttr = computeMttr(currentRun)
  const wartime = isWartime(currentRun)

  return (
    <section className="panel panel-main">
      <PanelHeader
        title="自治时间线"
        subtitle={`run #${currentRun.runId.slice(-8)} · ${currentRun.status}`}
        count={liveTimeline.length || undefined}
      />
      {wartime ? (
        <>
          <div className="war-banner">
            <strong>战时态</strong> · 自治处置进行中 · run {currentRun.runId} · {currentRun.status}
          </div>
          {isMqRun(currentRun) ? (
            <div className="executing-hint">
              <strong>提示</strong> · 限流/自愈已启用，根因未消除时可能无法 STABLE（需停止故障注入或等待滑动窗口清零）
            </div>
          ) : null}
        </>
      ) : null}
      {currentRun.status === 'STABLE' && mttr != null ? (
        <div className="mttr-banner">
          <strong>战后态 · 已恢复</strong> · MTTR = <strong>{mttr}s</strong> · run {currentRun.runId}
        </div>
      ) : null}
      {currentRun.status === 'STABLE' ? <RecoveryEvidence run={currentRun} /> : null}
      <TimelineView events={liveTimeline} adoptionMode={adoptionMode} />
      <FailedTracesPanel />
      <Recommendations run={currentRun} />
    </section>
  )
}
