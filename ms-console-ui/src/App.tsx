import { AssistantChatOverlay } from './components/AssistantChatOverlay'
import { AuthGate } from './components/AuthGate'
import { ChatFooter } from './components/ChatFooter'
import { Header } from './components/Header'
import { HistoryPanel } from './components/HistoryPanel'
import { IssuesPanel } from './components/IssuesPanel'
import { TimelinePanel } from './components/TimelinePanel'
import { ConsoleProvider } from './context/ConsoleContext'
import { useDragResize } from './hooks/useDragResize'

function App() {
  const sideResize = useDragResize({
    axis: 'x',
    initial: 380,
    min: 280,
    max: 720,
    storageKey: 'ms-console-side-width',
    invert: true,
  })

  return (
    <ConsoleProvider>
      <div className="app-shell">
        <div className="ambient ambient-a" />
        <div className="ambient ambient-b" />
        <div className="ambient ambient-c" />
        <div className="grid-overlay" />
        <div className="app-content">
          <AuthGate />
          <Header />
          <main
            className="console-main"
            style={{ gridTemplateColumns: `252px minmax(0, 1fr) ${sideResize.size}px` }}
          >
            <IssuesPanel />
            <TimelinePanel />
            <HistoryPanel
              sideWidth={sideResize.size}
              onSideResizePointerDown={sideResize.onPointerDown}
            />
          </main>
          <ChatFooter />
          <AssistantChatOverlay />
        </div>
      </div>
    </ConsoleProvider>
  )
}

export default App
