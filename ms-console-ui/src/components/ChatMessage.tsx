import { formatChatText } from '../utils/chatFormat'

export function ChatMessage({
  role,
  text,
  toolsUsed,
}: {
  role: 'user' | 'assistant'
  text: string
  toolsUsed?: string[]
}) {
  const isTechnical =
    role === 'assistant' &&
    (text.length > 80 || /[=:|#*`]/.test(text) || text.includes('\n'))

  return (
    <div className={`chat-bubble ${role}`}>
      <div className="chat-bubble-head">
        <span className="chat-avatar">{role === 'user' ? '我' : 'AI'}</span>
        <span className="chat-role">{role === 'user' ? '运维提问' : '助手回复'}</span>
      </div>
      {isTechnical ? (
        <pre className="chat-pre">{formatChatText(text)}</pre>
      ) : (
        <p className="chat-text">{formatChatText(text)}</p>
      )}
      {role === 'assistant' && toolsUsed && toolsUsed.length > 0 ? (
        <p className="chat-tools-used">数据来源：{toolsUsed.join('、')}</p>
      ) : null}
    </div>
  )
}
