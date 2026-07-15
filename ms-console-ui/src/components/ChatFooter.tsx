import { useConsole } from '../hooks/useConsole'
import { AssistantChatComposer } from './AssistantChatComposer'

export function ChatFooter() {
  const { openAssistantOverlay } = useConsole()

  return (
    <footer className="console-footer">
      <div className="footer-toolbar">
        <AssistantChatComposer className="footer-composer-grow" />
        <button
          type="button"
          className="btn-expand-assistant"
          onClick={openAssistantOverlay}
          title="放大助手（弹窗）"
        >
          放大
        </button>
      </div>
    </footer>
  )
}
