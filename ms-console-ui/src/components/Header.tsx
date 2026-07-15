import { useConsole } from '../hooks/useConsole'
import { isNacosDraftMode } from '../utils/runHelpers'
import { IconPulse, IconSpark } from './icons'

export function Header() {
  const { sseStatus, openAuthGate, token, activeRuns, adoptionMode } = useConsole()

  const isHealthy = activeRuns.length === 0

  return (
    <header className="console-header">
      <div className="header-brand">
        <div className="brand-mark">
          <IconSpark />
        </div>
        <div>
          <h1>
            MS <span className="text-gradient-sm">Autonomy</span>
          </h1>
          <p className="brand-sub">Middleware Operations Console</p>
        </div>
      </div>

      <nav className="header-nav">
        <div className={`nav-pill ${isHealthy ? 'nav-ok' : 'nav-alert'}`}>
          <span className="nav-dot" />
          {isHealthy ? '全部正常' : `${activeRuns.length} 故障处置中`}
        </div>
        <div className="nav-pill nav-mode">
          {isNacosDraftMode(adoptionMode) ? '◆ Nacos Draft' : '◇ Audit Only'}
        </div>
        <div className={`nav-pill nav-sse ${sseStatus}`}>
          <IconPulse />
          {sseStatus === 'connected' ? 'Live' : sseStatus === 'error' ? 'Offline' : '…'}
        </div>
      </nav>

      <button type="button" className="btn-auth" onClick={() => openAuthGate('请输入控制台 token')}>
        <span className="btn-auth-dot" data-on={!!token} />
        {token ? '已认证' : '登录'}
      </button>
    </header>
  )
}
