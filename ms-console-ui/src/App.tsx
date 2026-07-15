import { AuthGate } from './components/AuthGate'
import { ChatFooter } from './components/ChatFooter'
import { Header } from './components/Header'
import { HistoryPanel } from './components/HistoryPanel'
import { IssuesPanel } from './components/IssuesPanel'
import { TimelinePanel } from './components/TimelinePanel'
import { ConsoleProvider } from './context/ConsoleContext'

function App() {
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
          <main className="console-main">
            <IssuesPanel />
            <TimelinePanel />
            <HistoryPanel />
          </main>
          <ChatFooter />
        </div>
      </div>
    </ConsoleProvider>
  )
}

export default App
