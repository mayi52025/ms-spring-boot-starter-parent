import { useCallback, useEffect, useRef, useState } from 'react'
import { useConsole } from '../hooks/useConsole'
import { AssistantChatComposer } from './AssistantChatComposer'
import { AssistantChatMessages } from './AssistantChatMessages'

export function AssistantChatOverlay() {
  const { assistantOverlayOpen, closeAssistantOverlay, llmEnabled, chatLines, clearChat } = useConsole()
  const panelRef = useRef<HTMLDivElement>(null)
  const [fullscreen, setFullscreen] = useState(false)

  const exitFullscreen = useCallback(() => {
    if (document.fullscreenElement) {
      void document.exitFullscreen()
    }
    setFullscreen(false)
  }, [])

  const toggleFullscreen = useCallback(async () => {
    const el = panelRef.current
    if (!el) return
    if (!document.fullscreenElement) {
      try {
        await el.requestFullscreen()
        setFullscreen(true)
      } catch {
        setFullscreen(false)
      }
    } else {
      exitFullscreen()
    }
  }, [exitFullscreen])

  useEffect(() => {
    const onFullscreenChange = () => {
      setFullscreen(document.fullscreenElement === panelRef.current)
    }
    document.addEventListener('fullscreenchange', onFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange)
  }, [])

  useEffect(() => {
    if (!assistantOverlayOpen) {
      exitFullscreen()
      return
    }
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (document.fullscreenElement) {
          exitFullscreen()
        } else {
          closeAssistantOverlay()
        }
      }
    }
    window.addEventListener('keydown', onKey)
    return () => {
      document.body.style.overflow = prev
      window.removeEventListener('keydown', onKey)
    }
  }, [assistantOverlayOpen, closeAssistantOverlay, exitFullscreen])

  if (!assistantOverlayOpen) return null

  return (
    <div className="assistant-overlay" role="dialog" aria-modal="true" aria-label="运维助手">
      <button
        type="button"
        className="assistant-overlay-backdrop"
        aria-label="关闭助手"
        onClick={closeAssistantOverlay}
      />
      <div ref={panelRef} className={`assistant-overlay-panel ${fullscreen ? 'is-fullscreen' : ''}`}>
        <header className="assistant-overlay-toolbar">
          <div>
            <h2>运维助手</h2>
            <p>{llmEnabled ? 'LLM + Tool Grounding' : '规则模式'}</p>
          </div>
          <div className="assistant-overlay-actions">
            {chatLines.length ? (
              <button type="button" className="btn-ghost-sm" onClick={clearChat}>
                清空
              </button>
            ) : null}
            <button
              type="button"
              className="btn-ghost-sm"
              onClick={() => void toggleFullscreen()}
              title={fullscreen ? '退出全屏' : '全屏'}
            >
              {fullscreen ? '退出全屏' : '全屏'}
            </button>
            <button type="button" className="btn-ghost-sm" onClick={closeAssistantOverlay}>
              关闭
            </button>
          </div>
        </header>

        <AssistantChatMessages className="assistant-overlay-messages" />

        <AssistantChatComposer className="assistant-overlay-composer" autoFocus />
      </div>
    </div>
  )
}
