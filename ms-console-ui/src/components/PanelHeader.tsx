import type { JSX } from 'react'
import { IconAlert, IconHistory, IconShield } from './icons'

interface PanelHeaderProps {
  title: string
  subtitle?: string
  count?: number
  collapsed?: boolean
  onToggleCollapse?: () => void
  /** 用 JSX.Element，避免 IDE 里 JSX.Element 与 ReactNode 类型源不一致 */
  actions?: JSX.Element | null
  icon?: 'shield' | 'history' | 'alert'
}

const ICONS = {
  shield: IconShield,
  history: IconHistory,
  alert: IconAlert,
}

export function PanelHeader({
  title,
  subtitle,
  count,
  collapsed,
  onToggleCollapse,
  actions,
  icon,
}: PanelHeaderProps) {
  const Icon = icon ? ICONS[icon] : null

  return (
    <div className="panel-header">
      <div className="panel-header-main">
        {onToggleCollapse ? (
          <button type="button" className="panel-collapse-btn" onClick={onToggleCollapse} aria-expanded={!collapsed}>
            <span className={`panel-collapse-icon ${collapsed ? 'collapsed' : ''}`}>▾</span>
          </button>
        ) : null}
        {Icon ? (
          <span className="panel-icon">
            <Icon />
          </span>
        ) : null}
        <div>
          <h2>{title}</h2>
          {subtitle ? <p className="panel-subtitle">{subtitle}</p> : null}
        </div>
        {count != null ? <span className="panel-count">{count}</span> : null}
      </div>
      {actions ? <div className="panel-header-actions">{actions}</div> : null}
    </div>
  )
}
