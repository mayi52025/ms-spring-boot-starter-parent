import { useConsole } from '../hooks/useConsole'
import { computeMttr } from '../utils/runHelpers'
import {
  IconConfig,
  IconHistory,
  IconLayers,
  IconQueue,
  IconShield,
  IconSpark,
  IconTimer,
} from './icons'

function formatPercent(value: number | undefined): string {
  if (value == null || Number.isNaN(value)) return '—'
  return `${(value * 100).toFixed(1)}%`
}

const CARD_STYLES = [
  { key: 'active', icon: IconShield, gradient: 'g-emerald', label: '活跃故障', hint: '当前需处置' },
  { key: 'history', icon: IconHistory, gradient: 'g-violet', label: '历史恢复', hint: '已完成 run' },
  { key: 'mttr', icon: IconTimer, gradient: 'g-cyan', label: '最近 MTTR', hint: '恢复耗时' },
  { key: 'cache', icon: IconLayers, gradient: 'g-blue', label: '缓存命中', hint: '多级缓存' },
  { key: 'mq', icon: IconQueue, gradient: 'g-amber', label: 'MQ 失败', hint: '滑动窗口' },
  { key: 'mode', icon: IconConfig, gradient: 'g-rose', label: '采纳模式', hint: '变更策略' },
] as const

export function HealthyDashboard() {
  const { metrics, historyRuns, adoptionMode } = useConsole()

  const lastHistory = historyRuns[0]
  const lastMttr =
    metrics?.lastMttrSeconds ??
    (lastHistory ? (lastHistory.mttrSeconds ?? computeMttr(lastHistory)) : null)

  const values = [
    String(metrics?.activeRunCount ?? 0),
    String(metrics?.completedAutonomyRuns ?? historyRuns.length),
    lastMttr != null ? `${lastMttr}s` : '—',
    formatPercent(metrics?.cacheHitRate),
    String(metrics?.mqFailedCount ?? 0),
    adoptionMode === 'nacos-draft' ? 'Nacos 草稿' : '审计记录',
  ]

  const recentPreview = historyRuns.slice(0, 4)

  return (
    <div className="dashboard">
      <section className="hero-bento">
        <div className="hero-main-card">
          <div className="hero-badge">
            <span className="pulse-ring" />
            <IconSpark />
            <span>Operational</span>
          </div>
          <h2 className="hero-title">
            系统<span className="text-gradient">运行正常</span>
          </h2>
          <p className="hero-desc">
            自治内核持续巡检 Redis · RabbitMQ · 缓存链路。当前无活跃故障，{historyRuns.length} 条历史事件已归档可供复盘。
          </p>
          <div className="hero-chips">
            <span className="mini-chip chip-green">Ledger 在线</span>
            <span className="mini-chip chip-violet">SSE 推送</span>
            <span className="mini-chip chip-cyan">Tool 就绪</span>
          </div>
        </div>

        <div className="hero-score-card">
          <div className="score-ring-wrap">
            <svg viewBox="0 0 140 140" className="score-svg">
              <defs>
                <linearGradient id="scoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#4ade80" />
                  <stop offset="50%" stopColor="#22d3ee" />
                  <stop offset="100%" stopColor="#a78bfa" />
                </linearGradient>
              </defs>
              <circle cx="70" cy="70" r="58" className="score-track" />
              <circle cx="70" cy="70" r="58" className="score-fill" />
            </svg>
            <div className="score-center">
              <span className="score-num">100</span>
              <span className="score-unit">健康指数</span>
            </div>
          </div>
          <div className="score-bars">
            <div className="score-bar-row">
              <span>自治</span>
              <div className="bar-track"><div className="bar-fill bar-green" style={{ width: '100%' }} /></div>
            </div>
            <div className="score-bar-row">
              <span>中间件</span>
              <div className="bar-track"><div className="bar-fill bar-cyan" style={{ width: '96%' }} /></div>
            </div>
            <div className="score-bar-row">
              <span>可观测</span>
              <div className="bar-track"><div className="bar-fill bar-violet" style={{ width: '92%' }} /></div>
            </div>
          </div>
        </div>
      </section>

      <section className="bento-metrics">
        {CARD_STYLES.map((card, i) => {
          const Icon = card.icon
          return (
            <article key={card.key} className={`bento-card ${card.gradient}`}>
              <div className="bento-icon">
                <Icon />
              </div>
              <div className="bento-body">
                <span className="bento-label">{card.label}</span>
                <span className="bento-value">{values[i]}</span>
                <span className="bento-hint">{card.hint}</span>
              </div>
              <div className="bento-shine" />
            </article>
          )
        })}
      </section>

      {recentPreview.length > 0 ? (
        <section className="recent-panel">
          <div className="recent-head">
            <h3>最近恢复</h3>
            <span>点击右侧历史查看详情</span>
          </div>
          <div className="recent-timeline">
            {recentPreview.map((run, idx) => {
              const mttr = run.mttrSeconds ?? computeMttr(run)
              return (
                <div key={run.runId} className="recent-item" style={{ animationDelay: `${idx * 80}ms` }}>
                  <div className="recent-dot" />
                  <div className="recent-content">
                    <div className="recent-top">
                      <code>#{run.runId.slice(-8)}</code>
                      {mttr != null ? <em>{mttr}s MTTR</em> : null}
                    </div>
                    <p>{(run.issues || []).join(' · ') || '—'}</p>
                  </div>
                </div>
              )
            })}
          </div>
        </section>
      ) : null}
    </div>
  )
}
