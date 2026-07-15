import { useEffect, useRef } from 'react'
import { useConsole } from '../hooks/useConsole'
import { ChatMessage } from './ChatMessage'

export function AssistantChatMessages({ className = '' }: { className?: string }) {
  const { chatLines } = useConsole()
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [chatLines.length])

  if (!chatLines.length) {
    return (
      <div className={`chat-empty-wrap ${className}`.trim()}>
        <div className="chat-empty-icon">💬</div>
        <p>输入运维问题，或从 Trace 发起查询</p>
      </div>
    )
  }

  return (
    <div className={`chat-log chat-log-resizable ${className}`.trim()}>
      {chatLines.map((line, idx) => (
        <ChatMessage
          key={idx}
          role={line.role}
          text={line.text}
          toolsUsed={line.toolsUsed}
          contextHints={line.contextHints}
        />
      ))}
      <div ref={bottomRef} />
    </div>
  )
}
