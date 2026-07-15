export type AdoptionMode = 'audit-only' | 'nacos-draft'

export interface AuthStatus {
  authRequired: boolean
  adoptionMode: AdoptionMode
  llmEnabled?: boolean
}

export interface TimelineEvent {
  at: string
  runId?: string
  phase: string
  message: string
  level?: string
  recommendationId?: string
  operator?: string
  clientIp?: string
}

export interface RecoveryMetric {
  key?: string
  label?: string
  beforeValue?: string
  afterValue?: string
  threshold?: string
}

export interface RecoveryEvidence {
  summary?: string
  metrics?: RecoveryMetric[]
  resolutionRule?: string
}

export interface PlannedAction {
  rank: number
  actionType: string
  reason?: string
  confidence?: number
  executionStatus?: string
  policyDecision?: string
  executionDetail?: string
  humanAccepted?: boolean
}

export interface AutonomyRecommendation {
  recommendationId: string
  title: string
  description?: string
  suggestedConfig?: string
  status?: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  draftId?: string
  nacosPublished?: boolean
  diffSummary?: string
}

export interface AutonomyPlan {
  incidentType?: string
  recommendations?: AutonomyRecommendation[]
  actions?: PlannedAction[]
}

export interface AutonomyRun {
  runId: string
  status: string
  startedAt?: string
  stabilizedAt?: string
  issues?: string[]
  timeline?: TimelineEvent[]
  plan?: AutonomyPlan
  recoveryEvidence?: RecoveryEvidence
  mttrSeconds?: number
}

export interface IssuesResponse {
  count: number
  runs: AutonomyRun[]
  healthy: boolean
}

export interface HistoryResponse {
  count: number
  runs: AutonomyRun[]
}

export interface FailedTrace {
  messageId: string
  queue?: string
  errorMessage?: string
}

export interface FailedTracesResponse {
  count: number
  traces: FailedTrace[]
}

export interface ChatResponse {
  reply?: string
  message?: string
  toolsUsed?: string[]
  grounded?: boolean
  contextHints?: string[]
  boundRunId?: string | null
}

export interface MetricsSnapshot {
  cacheHitRate?: number
  mqFailedCount?: number
  globalFailureCount?: number
  activeRunCount?: number
  lastMttrSeconds?: number
  completedAutonomyRuns?: number
}

export interface ActionResult {
  success?: boolean
  message?: string
}
