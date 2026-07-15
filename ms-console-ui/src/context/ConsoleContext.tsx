import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import {
  adoptAdvisedAction,
  adoptRecommendation,
  fetchAuthStatus,
  fetchFailedTraces,
  fetchHistory,
  fetchIssues,
  fetchMetrics,
  fetchRun,
  getOrCreateChatSessionId,
  getStoredToken,
  publishRecommendationDraft,
  sendChat,
  setStoredToken,
  streamUrl,
  UnauthorizedError,
} from '../api/consoleApi'
import type { AdoptionMode, AutonomyRun, FailedTrace, MetricsSnapshot, TimelineEvent } from '../api/types'

type SseStatus = 'connecting' | 'connected' | 'error'

interface ChatLine {
  role: 'user' | 'assistant'
  text: string
  toolsUsed?: string[]
  contextHints?: string[]
}

export interface ConsoleContextValue {
  authRequired: boolean
  adoptionMode: AdoptionMode
  llmEnabled: boolean
  token: string
  showAuthGate: boolean
  authMessage: string
  sseStatus: SseStatus
  activeRuns: AutonomyRun[]
  historyRuns: AutonomyRun[]
  selectedRunId: string | null
  viewingHistory: boolean
  currentRun: AutonomyRun | null
  failedTraces: FailedTrace[]
  liveTimeline: TimelineEvent[]
  issuesError: string | null
  historyError: string | null
  bootstrapError: string | null
  metrics: MetricsSnapshot | null
  chatLines: ChatLine[]
  assistantOverlayOpen: boolean
  openAssistantOverlay: () => void
  closeAssistantOverlay: () => void
  login: (token: string) => void
  openAuthGate: (message?: string) => void
  selectRun: (runId: string, fromHistory?: boolean) => Promise<void>
  selectHistoryRun: (runId: string) => Promise<void>
  showHealthyView: () => void
  handleAdoptRecommendation: (recommendationId: string, runId: string, accept: boolean) => Promise<void>
  handlePublishDraft: (recommendationId: string, runId: string) => Promise<void>
  handleAdoptAction: (runId: string, rank: number) => Promise<void>
  handleSendChat: (message: string) => Promise<void>
  queryTraceInChat: (messageId: string) => Promise<void>
  clearChat: () => void
  refreshLists: () => Promise<void>
}

export const ConsoleContext = createContext<ConsoleContextValue | null>(null)

export function ConsoleProvider({ children }: { children: ReactNode }) {
  const [authRequired, setAuthRequired] = useState(false)
  const [adoptionMode, setAdoptionMode] = useState<AdoptionMode>('audit-only')
  const [llmEnabled, setLlmEnabled] = useState(false)
  const [token, setToken] = useState(getStoredToken)
  const [showAuthGate, setShowAuthGate] = useState(false)
  const [authMessage, setAuthMessage] = useState('请输入运维 token（Demo 默认 demo-secret）')
  const [sseStatus, setSseStatus] = useState<SseStatus>('connecting')
  const [activeRuns, setActiveRuns] = useState<AutonomyRun[]>([])
  const [historyRuns, setHistoryRuns] = useState<AutonomyRun[]>([])
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null)
  const [viewingHistory, setViewingHistory] = useState(false)
  const [currentRun, setCurrentRun] = useState<AutonomyRun | null>(null)
  const [failedTraces, setFailedTraces] = useState<FailedTrace[]>([])
  const [liveTimeline, setLiveTimeline] = useState<TimelineEvent[]>([])
  const [issuesError, setIssuesError] = useState<string | null>(null)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [bootstrapError, setBootstrapError] = useState<string | null>(null)
  const [metrics, setMetrics] = useState<MetricsSnapshot | null>(null)
  const [chatLines, setChatLines] = useState<ChatLine[]>([])
  const [assistantOverlayOpen, setAssistantOverlayOpen] = useState(false)
  const [ready, setReady] = useState(false)

  const sseRef = useRef<EventSource | null>(null)
  const viewingHistoryRef = useRef(viewingHistory)
  const selectedRunIdRef = useRef(selectedRunId)
  const chatSessionIdRef = useRef(getOrCreateChatSessionId())
  const previousRunIdRef = useRef<string | null>(null)

  useEffect(() => {
    viewingHistoryRef.current = viewingHistory
  }, [viewingHistory])

  useEffect(() => {
    selectedRunIdRef.current = selectedRunId
  }, [selectedRunId])

  const openAuthGate = useCallback((message?: string) => {
    if (message) setAuthMessage(message)
    setShowAuthGate(true)
  }, [])

  const handleUnauthorized = useCallback(() => {
    setAuthRequired(true)
    setToken('')
    setStoredToken('')
    openAuthGate('需要控制台 token（请在弹窗输入，不是底部「对话」框）。Demo 默认：demo-secret')
  }, [openAuthGate])

  const refreshLists = useCallback(async () => {
    try {
      const data = await fetchIssues(token)
      setIssuesError(null)
      setActiveRuns(data.runs || [])
      if (!data.runs?.length) {
        setSelectedRunId(null)
        setViewingHistory(false)
        setCurrentRun(null)
        setFailedTraces([])
        setLiveTimeline([])
      }
    } catch (err) {
      if (err instanceof UnauthorizedError) {
        handleUnauthorized()
        return
      }
      setIssuesError(err instanceof Error ? err.message : '加载失败')
    }

    try {
      const data = await fetchHistory(token)
      setHistoryError(null)
      setHistoryRuns(data.runs || [])
    } catch (err) {
      if (err instanceof UnauthorizedError) {
        handleUnauthorized()
        return
      }
      setHistoryError(err instanceof Error ? err.message : '加载失败')
    }

    try {
      const data = await fetchMetrics(token)
      setMetrics(data)
    } catch {
      // metrics optional for dashboard
    }
  }, [handleUnauthorized, token])

  const loadRunDetails = useCallback(
    async (runId: string, fromHistory: boolean) => {
      try {
        if (previousRunIdRef.current !== runId) {
          setChatLines([])
          previousRunIdRef.current = runId
        }
        const run = await fetchRun(token, runId)
        setCurrentRun(run)
        setLiveTimeline(run.timeline || [])
        const wartime =
          run.status === 'EXECUTING' || run.status === 'PLANNED' || run.status === 'DETECTED'
        if (wartime) {
          const traces = await fetchFailedTraces(token, 10)
          setFailedTraces(traces.traces || [])
        } else {
          setFailedTraces([])
        }
        setSelectedRunId(runId)
        setViewingHistory(fromHistory)
      } catch (err) {
        if (err instanceof UnauthorizedError) {
          handleUnauthorized()
        }
      }
    },
    [handleUnauthorized, token],
  )

  const selectRun = useCallback(
    async (runId: string, fromHistory = false) => {
      await loadRunDetails(runId, fromHistory)
    },
    [loadRunDetails],
  )

  const selectHistoryRun = useCallback(
    async (runId: string) => {
      await loadRunDetails(runId, true)
    },
    [loadRunDetails],
  )

  const showHealthyView = useCallback(() => {
    setSelectedRunId(null)
    setViewingHistory(false)
    setCurrentRun(null)
    setFailedTraces([])
    setLiveTimeline([])
    setChatLines([])
    previousRunIdRef.current = null
  }, [])

  const login = useCallback(
    (newToken: string) => {
      const trimmed = newToken.trim()
      if (!trimmed) return
      setToken(trimmed)
      setStoredToken(trimmed)
      setShowAuthGate(false)
      if (sseRef.current) {
        sseRef.current.close()
        sseRef.current = null
      }
      setReady(true)
    },
    [],
  )

  const connectSse = useCallback(() => {
    if (sseRef.current) {
      sseRef.current.close()
    }
    setSseStatus('connecting')
    const es = new EventSource(streamUrl(token))
    sseRef.current = es

    es.addEventListener('connected', () => {
      setSseStatus('connected')
    })

    es.addEventListener('timeline', (ev) => {
      try {
        const event = JSON.parse(ev.data) as TimelineEvent
        setLiveTimeline((prev) => [event, ...prev])
        if (event.phase === 'STABLE') {
          void refreshLists()
          if (event.runId) {
            void loadRunDetails(event.runId, true)
          }
        } else {
          void refreshLists()
          if (event.runId && !viewingHistoryRef.current) {
            void loadRunDetails(event.runId, false)
          }
        }
      } catch {
        // ignore malformed SSE payload
      }
    })

    es.onerror = () => {
      setSseStatus('error')
    }
  }, [loadRunDetails, refreshLists, token])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const status = await fetchAuthStatus()
        if (cancelled) return
        setAuthRequired(status.authRequired)
        setAdoptionMode(status.adoptionMode)
        setLlmEnabled(!!status.llmEnabled)
        if (status.authRequired && !token) {
          openAuthGate('请输入运维 token（Demo 默认 demo-secret）')
          return
        }
        setShowAuthGate(false)
        setReady(true)
      } catch (err) {
        if (!cancelled) {
          setBootstrapError(err instanceof Error ? err.message : '无法连接控制台 API')
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [openAuthGate, token])

  useEffect(() => {
    if (!ready) return
    void refreshLists()
    connectSse()
    const timer = window.setInterval(() => {
      void refreshLists()
    }, 10000)
    return () => {
      window.clearInterval(timer)
      if (sseRef.current) {
        sseRef.current.close()
        sseRef.current = null
      }
    }
  }, [connectSse, ready, refreshLists])

  const handleAdoptRecommendation = useCallback(
    async (recommendationId: string, runId: string, accept: boolean) => {
      const data = await adoptRecommendation(token, recommendationId, runId, accept)
      if (!data.ok) {
        alert(data.message || '操作失败')
        return
      }
      await loadRunDetails(runId, viewingHistoryRef.current)
    },
    [loadRunDetails, token],
  )

  const handlePublishDraft = useCallback(
    async (recommendationId: string, runId: string) => {
      const data = await publishRecommendationDraft(token, recommendationId, runId)
      if (!data.ok) {
        alert(data.message || '发布失败')
        return
      }
      await loadRunDetails(runId, viewingHistoryRef.current)
    },
    [loadRunDetails, token],
  )

  const handleAdoptAction = useCallback(
    async (runId: string, rank: number) => {
      const data = await adoptAdvisedAction(token, runId, rank)
      if (!data.ok) {
        alert(data.message || '执行失败')
        return
      }
      await loadRunDetails(runId, viewingHistoryRef.current)
    },
    [loadRunDetails, token],
  )

  const handleSendChat = useCallback(
    async (message: string) => {
      const msg = message.trim()
      if (!msg) return
      setChatLines((prev) => [...prev, { role: 'user', text: msg }])
      try {
        const data = await sendChat(token, msg, selectedRunIdRef.current, chatSessionIdRef.current)
        const reply = data.reply || data.message || '（无回复内容）'
        setChatLines((prev) => [
          ...prev,
          {
            role: 'assistant',
            text: reply,
            toolsUsed: data.toolsUsed?.length ? data.toolsUsed : undefined,
            contextHints: data.contextHints?.length ? data.contextHints : undefined,
          },
        ])
      } catch (err) {
        const text =
          err instanceof UnauthorizedError
            ? '需要重新登录'
            : `请求异常：${err instanceof Error ? err.message : '未知错误'}`
        setChatLines((prev) => [...prev, { role: 'assistant', text }])
        if (err instanceof UnauthorizedError) handleUnauthorized()
      }
    },
    [handleUnauthorized, token],
  )

  const queryTraceInChat = useCallback(
    async (messageId: string) => {
      await handleSendChat(`trace ${messageId}`)
    },
    [handleSendChat],
  )

  const clearChat = useCallback(() => {
    setChatLines([])
  }, [])

  const openAssistantOverlay = useCallback(() => {
    setAssistantOverlayOpen(true)
  }, [])

  const closeAssistantOverlay = useCallback(() => {
    setAssistantOverlayOpen(false)
  }, [])

  const value = useMemo<ConsoleContextValue>(
    () => ({
      authRequired,
      adoptionMode,
      llmEnabled,
      token,
      showAuthGate,
      authMessage,
      sseStatus,
      activeRuns,
      historyRuns,
      selectedRunId,
      viewingHistory,
      currentRun,
      failedTraces,
      liveTimeline,
      issuesError,
      historyError,
      bootstrapError,
      metrics,
      chatLines,
      assistantOverlayOpen,
      openAssistantOverlay,
      closeAssistantOverlay,
      login,
      openAuthGate,
      selectRun,
      selectHistoryRun,
      showHealthyView,
      handleAdoptRecommendation,
      handlePublishDraft,
      handleAdoptAction,
      handleSendChat,
      queryTraceInChat,
      clearChat,
      refreshLists,
    }),
    [
      authRequired,
      adoptionMode,
      llmEnabled,
      token,
      showAuthGate,
      authMessage,
      sseStatus,
      activeRuns,
      historyRuns,
      selectedRunId,
      viewingHistory,
      currentRun,
      failedTraces,
      liveTimeline,
      issuesError,
      historyError,
      bootstrapError,
      metrics,
      chatLines,
      assistantOverlayOpen,
      openAssistantOverlay,
      closeAssistantOverlay,
      login,
      openAuthGate,
      selectRun,
      selectHistoryRun,
      showHealthyView,
      handleAdoptRecommendation,
      handlePublishDraft,
      handleAdoptAction,
      handleSendChat,
      queryTraceInChat,
      clearChat,
      refreshLists,
    ],
  )

  return <ConsoleContext.Provider value={value}>{children}</ConsoleContext.Provider>
}
