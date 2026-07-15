import { useContext } from 'react'
import { ConsoleContext, type ConsoleContextValue } from '../context/ConsoleContext'

export function useConsole(): ConsoleContextValue {
  const ctx = useContext(ConsoleContext)
  if (!ctx) {
    throw new Error('useConsole must be used within ConsoleProvider')
  }
  return ctx
}
