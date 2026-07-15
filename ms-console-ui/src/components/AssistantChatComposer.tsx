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
          <span>上下文 · run #{selectedRunId.slice(-8)}</span>
        ) : (
          <span>未绑定 run · 将使用全局查询</span>
        )}
      </div>
    </div>
  )
}
