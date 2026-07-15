import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { useConsole } from '../hooks/useConsole'

interface AssistantChatComposerProps {
  className?: string
  autoFocus?: boolean
  onSent?: () => void
}

export function AssistantChatComposer({ className = '', autoFocus = false, onSent }: AssistantChatComposerProps) {
  const { handleSendChat, selectedRunId } = useConsole()
  const [input, setInput] = useState('')
  const [focused, setFocused] = useState(false)

  const submit = async () => {
    const msg = input.trim()
    if (!msg) return
    setInput('')
    await handleSendChat(msg)
    onSent?.()
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    void submit()
  }

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void submit()
    }
  }

  return (
    <div className={`assistant-composer ${focused ? 'composer-focused' : ''} ${className}`.trim()}>
      <form className="footer-form" onSubmit={onSubmit}>
        <div className="footer-input-wrap">
          <span className="footer-prefix">›</span>
          <input
            type="text"
            placeholder="当前有什么问题？最近 run？trace messageId…"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            autoFocus={autoFocus}
          />
        </div>
        <button type="submit" disabled={!input.trim()}>
          发送
        </button>
      </form>
      <div className="footer-meta">
        {selectedRunId ? (
          <span>分桶展示 · run #{selectedRunId.slice(-8)}</span>
        ) : (
          <span>分桶展示 · 健康总览主页</span>
        )}
        <span className="footer-meta-sep">·</span>
        <span className="footer-meta-hint" title="气泡为界面可读历史；下一轮推理依据后端 L0/L1 工作上下文与 Tool，不是整段气泡回灌">
          展示历史 ≠ 全量推理上下文
        </span>
      </div>
    </div>
  )
}
