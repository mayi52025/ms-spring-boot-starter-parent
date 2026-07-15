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

/** 导航焦点：只由用户点击切换，轮询 / SSE 绝不自动改回 home */
export type NavFocus =
  | { mode: 'home' }
  | { mode: 'run'; runId: string; fromHistory: boolean }

const GLOBAL_CHAT_KEY = '_global_'
const CHAT_STORAGE_KEY = 'ms-console-chat-buckets-v2'
/** run 维度最多保留多少个独立聊天桶（另永久保留主页桶） */
const MAX_RUN_CHAT_BUCKETS = 20

interface ChatLine {
  role: 'user' | 'assistant'
  text: string
  toolsUsed?: string[]
  contextHints?: string[]
}

/** 从 NavFocus 取 runId；避免对 mutable ref 二次访问时联合类型无法收窄 */
function runIdOf(focus: NavFocus): string | null {
  return focus.mode === 'run' ? focus.runId : null
}

function isFocusedRun(focus: NavFocus, runId: string): boolean {
  return focus.mode === 'run' && focus.runId === runId
}

/** buckets + LRU 顺序（MRU 在尾部；不含 _global_） */
interface ChatStore {
  buckets: Record<string, ChatLine[]>
  order: string[]
}

function chatBucketKey(runId: string | null | undefined): string {
  return runId && runId.trim() ? runId.trim() : GLOBAL_CHAT_KEY
}

function loadChatStore(): ChatStore {
  try {
    const raw = sessionStorage.getItem(CHAT_STORAGE_KEY)
    if (!raw) return { buckets: {}, order: [] }
    const parsed = JSON.parse(raw) as Partial<ChatStore>
    return {
      buckets: parsed.buckets && typeof parsed.buckets === 'object' ? parsed.buckets : {},
      order: Array.isArray(parsed.order) ? parsed.order.filter((k) => k && k !== GLOBAL_CHAT_KEY) : [],
    }
  } catch {
    return { buckets: {}, order: [] }
  }
}

function saveChatStore(store: ChatStore) {
  try {
    sessionStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(store))
  } catch {
    // ignore quota
  }
}

/** 访问某个 run 桶：更新 LRU；超出上限淘汰最久未用（永不删主页桶） */
function touchChatBucket(store: ChatStore, key: string): ChatStore {
  const buckets = { ...store.buckets }
  let order = [...store.order]
  if (key === GLOBAL_CHAT_KEY) {
    return { buckets, order }
  }
  order = order.filter((k) => k !== key)
  order.push(key)
  while (order.length > MAX_RUN_CHAT_BUCKETS) {
    const evict = order.shift()
    if (evict) {
      delete buckets[evict]
    }
  }
  return { buckets, order }
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
  /** 当前导航：home=健康总览；run=某个故障/历史 */
  navFocus: NavFocus
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
  const [navFocus, setNavFocus] = useState<NavFocus>({ mode: 'home' })
  const [currentRun, setCurrentRun] = useState<AutonomyRun | null>(null)
  const [failedTraces, setFailedTraces] = useState<FailedTrace[]>([])
  const [liveTimeline, setLiveTimeline] = useState<TimelineEvent[]>([])
  const [issuesError, setIssuesError] = useState<string | null>(null)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [bootstrapError, setBootstrapError] = useState<string | null>(null)
  const [metrics, setMetrics] = useState<MetricsSnapshot | null>(null)
  const [chatLines, setChatLines] = useState<ChatLine[]>(() => loadChatStore().buckets[GLOBAL_CHAT_KEY] ?? [])
  const [assistantOverlayOpen, setAssistantOverlayOpen] = useState(false)
  const [ready, setReady] = useState(false)

  const sseRef = useRef<EventSource | null>(null)
  const navFocusRef = useRef<NavFocus>(navFocus)
  const chatSessionIdRef = useRef(getOrCreateChatSessionId())
  const chatStoreRef = useRef<ChatStore>(loadChatStore())
  const chatLinesRef = useRef<ChatLine[]>(chatLines)
  const refreshListsRef = useRef<() => Promise<void>>(async () => {})

  const selectedRunId = navFocus.mode === 'run' ? navFocus.runId : null
  const viewingHistory = navFocus.mode === 'run' ? navFocus.fromHistory : false

  useEffect(() => {
    navFocusRef.current = navFocus
  }, [navFocus])

  useEffect(() => {
    chatLinesRef.current = chatLines
  }, [chatLines])

  const persistChatStore = useCallback(() => {
    saveChatStore(chatStoreRef.current)
  }, [])

  /** 切换导航前：写入当前分桶；再读出目标分桶（主页/各 run 互不覆盖 + LRU） */
  const applyChatBucket = useCallback(
    (nextRunId: string | null) => {
      const fromKey = chatBucketKey(runIdOf(navFocusRef.current))
      let store = chatStoreRef.current
      store = {
        ...store,
        buckets: { ...store.buckets, [fromKey]: chatLinesRef.current },
      }
      store = touchChatBucket(store, fromKey)

      const toKey = chatBucketKey(nextRunId)
      store = touchChatBucket(store, toKey)
      const nextLines = store.buckets[toKey] ?? []
      chatStoreRef.current = store
      chatLinesRef.current = nextLines
      setChatLines(nextLines)
      persistChatStore()
    },
    [persistChatStore],
  )

  const persistChatLines = useCallback(
    (lines: ChatLine[]) => {
      const key = chatBucketKey(runIdOf(navFocusRef.current))
      let store = {
        ...chatStoreRef.current,
        buckets: { ...chatStoreRef.current.buckets, [key]: lines },
      }
      store = touchChatBucket(store, key)
      chatStoreRef.current = store
      chatLinesRef.current = lines
      setChatLines(lines)
      persistChatStore()
    },
    [persistChatStore],
  )

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
      // 绝不因「无活跃故障」改导航或清空详情——只有用户点「健康总览」才回 home
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
      // metrics optional
    }
  }, [handleUnauthorized, token])

  useEffect(() => {
    refreshListsRef.current = refreshLists
  }, [refreshLists])

  const loadRunDetails = useCallback(
    async (runId: string, fromHistory: boolean) => {
      const focus: NavFocus = { mode: 'run', runId, fromHistory }
      const prev = navFocusRef.current
      const switched = !isFocusedRun(prev, runId)

      // 1) 立刻切换导航（中间区显示加载态，绝不会闪回健康页）
      if (switched) {
        applyChatBucket(runId)
      }
      navFocusRef.current = focus
      setNavFocus(focus)

      try {
        const run = await fetchRun(token, runId)
        // 用户已切走则丢弃
        if (!isFocusedRun(navFocusRef.current, runId)) {
          return
        }
        setCurrentRun(run)
        setLiveTimeline(run.timeline || [])
        const wartime =
          run.status === 'EXECUTING' || run.status === 'PLANNED' || run.status === 'DETECTED'
        if (wartime) {
          const traces = await fetchFailedTraces(token, 10)
          if (!isFocusedRun(navFocusRef.current, runId)) {
            return
          }
          setFailedTraces(traces.traces || [])
        } else {
          setFailedTraces([])
        }
      } catch (err) {
        if (err instanceof UnauthorizedError) {
          handleUnauthorized()
        }
      }
    },
    [applyChatBucket, handleUnauthorized, token],
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
    applyChatBucket(null)
    const home: NavFocus = { mode: 'home' }
    navFocusRef.current = home
    setNavFocus(home)
    setCurrentRun(null)
    setFailedTraces([])
    setLiveTimeline([])
  }, [applyChatBucket])

  const login = useCallback((newToken: string) => {
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
  }, [])

  // SSE：依赖稳定，绝不因 loadRunDetails 重建而重连抢焦点
  useEffect(() => {
    if (!ready) return

    void refreshListsRef.current()

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
        const focus = navFocusRef.current

        // 用户钉在 home 或历史 run：只刷列表，不改中间页
        if (focus.mode === 'home' || (focus.mode === 'run' && focus.fromHistory)) {
          void refreshListsRef.current()
          return
        }

        // 战时看中的活跃 run：仅同 runId 才刷新详情
        const focusedRunId = runIdOf(focus)
        if (focusedRunId && event.runId === focusedRunId && focus.mode === 'run' && !focus.fromHistory) {
          setLiveTimeline((prev) => [event, ...prev])
          void refreshListsRef.current()
          void fetchRun(token, focusedRunId)
            .then((run) => {
              if (isFocusedRun(navFocusRef.current, focusedRunId)) {
                setCurrentRun(run)
                setLiveTimeline(run.timeline || [])
              }
            })
            .catch(() => {
              /* ignore */
            })
          return
        }

        void refreshListsRef.current()
      } catch {
        // ignore malformed SSE
      }
    })

    es.onerror = () => {
      setSseStatus('error')
    }

    const timer = window.setInterval(() => {
      void refreshListsRef.current()
    }, 10000)

    return () => {
      window.clearInterval(timer)
      es.close()
      if (sseRef.current === es) {
        sseRef.current = null
      }
    }
  }, [ready, token])

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

  const handleAdoptRecommendation = useCallback(
    async (recommendationId: string, runId: string, accept: boolean) => {
      const data = await adoptRecommendation(token, recommendationId, runId, accept)
      if (!data.ok) {
        alert(data.message || '操作失败')
        return
      }
      const focus = navFocusRef.current
      await loadRunDetails(runId, focus.mode === 'run' ? focus.fromHistory : false)
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
      const focus = navFocusRef.current
      await loadRunDetails(runId, focus.mode === 'run' ? focus.fromHistory : false)
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
      const focus = navFocusRef.current
      await loadRunDetails(runId, focus.mode === 'run' ? focus.fromHistory : false)
    },
    [loadRunDetails, token],
  )

  const handleSendChat = useCallback(
    async (message: string) => {
      const msg = message.trim()
      if (!msg) return
      const userLine: ChatLine = { role: 'user', text: msg }
      const withUser: ChatLine[] = [...chatLinesRef.current, userLine]
      persistChatLines(withUser)
      const runId = runIdOf(navFocusRef.current)
      try {
        const data = await sendChat(token, msg, runId, chatSessionIdRef.current)
        const reply = data.reply || data.message || '（无回复内容）'
        const assistantLine: ChatLine = {
          role: 'assistant',
          text: reply,
          toolsUsed: data.toolsUsed?.length ? data.toolsUsed : undefined,
          contextHints: data.contextHints?.length ? data.contextHints : undefined,
        }
        persistChatLines([...withUser, assistantLine])
      } catch (err) {
        const text =
          err instanceof UnauthorizedError
            ? '需要重新登录'
            : `请求异常：${err instanceof Error ? err.message : '未知错误'}`
        const errorLine: ChatLine = { role: 'assistant', text }
        persistChatLines([...withUser, errorLine])
        if (err instanceof UnauthorizedError) handleUnauthorized()
      }
    },
    [handleUnauthorized, persistChatLines, token],
  )

  const queryTraceInChat = useCallback(
    async (messageId: string) => {
      await handleSendChat(`trace ${messageId}`)
    },
    [handleSendChat],
  )

  const clearChat = useCallback(() => {
    persistChatLines([])
  }, [persistChatLines])

  const openAssistantOverlay = useCallback(() => setAssistantOverlayOpen(true), [])
  const closeAssistantOverlay = useCallback(() => setAssistantOverlayOpen(false), [])

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
      navFocus,
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
      navFocus,
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
