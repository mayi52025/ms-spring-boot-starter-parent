import type { AdoptionMode, AutonomyRun } from '../api/types'

export function computeMttr(run: AutonomyRun | null | undefined): number | null {
  if (!run?.startedAt || !run.stabilizedAt) return null
  const start = new Date(run.startedAt).getTime()
  const end = new Date(run.stabilizedAt).getTime()
  if (Number.isNaN(start) || Number.isNaN(end)) return null
  return Math.max(0, Math.floor((end - start) / 1000))
}

export function isMqRun(run: AutonomyRun): boolean {
  const incidentType = run.plan?.incidentType
  if (incidentType === 'MQ_DEGRADED') return true
  return (run.issues || []).some((i) => /MQ|消息|Rabbit/i.test(i))
}

export function isWartime(run: AutonomyRun): boolean {
  return run.status === 'EXECUTING' || run.status === 'PLANNED' || run.status === 'DETECTED'
}

export function isNacosDraftMode(mode: AdoptionMode): boolean {
  return mode === 'nacos-draft'
}

export function escapeHtml(text: string | undefined | null): string {
  if (!text) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
