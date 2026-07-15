import { useEffect, useState, type FormEvent } from 'react'
import { useConsole } from '../hooks/useConsole'

export function AuthGate() {
  const { showAuthGate, authMessage, login, token } = useConsole()
  const [value, setValue] = useState('')

  useEffect(() => {
    if (showAuthGate) {
      setValue(token)
    }
  }, [showAuthGate, token])

  if (!showAuthGate) return null

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    login(value)
  }

  return (
    <div className={`auth-gate ${showAuthGate ? 'show' : ''}`}>
      <form className="auth-card" onSubmit={onSubmit}>
        <h2>控制台鉴权</h2>
        <p>{authMessage}</p>
        <input
          type="password"
          placeholder="X-MS-Console-Token"
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
        <button type="submit">进入控制台</button>
      </form>
    </div>
  )
}
