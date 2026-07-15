import { useCallback, useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'

interface UseDragResizeOptions {
  axis: 'x' | 'y'
  initial: number
  min: number
  max: number
  storageKey?: string
  /** 水平拖拽时：从右侧面板左缘拖动，delta 取反 */
  invert?: boolean
}

function readStored(key: string | undefined, fallback: number): number {
  if (!key) return fallback
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    const n = Number(raw)
    return Number.isFinite(n) ? n : fallback
  } catch {
    return fallback
  }
}

export function useDragResize({
  axis,
  initial,
  min,
  max,
  storageKey,
  invert = false,
}: UseDragResizeOptions) {
  const [size, setSize] = useState(() => readStored(storageKey, initial))
  const dragging = useRef(false)
  const startPos = useRef(0)
  const startSize = useRef(0)

  const clamp = useCallback((v: number) => Math.min(max, Math.max(min, v)), [min, max])

  const onPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragging.current = true
      startPos.current = axis === 'x' ? e.clientX : e.clientY
      startSize.current = size
      ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
      document.body.classList.add('ms-console-resizing')
    },
    [axis, size],
  )

  useEffect(() => {
    const onMove = (e: PointerEvent) => {
      if (!dragging.current) return
      const pos = axis === 'x' ? e.clientX : e.clientY
      let delta = pos - startPos.current
      if (invert) delta = -delta
      setSize(clamp(startSize.current + delta))
    }

    const onUp = () => {
      if (!dragging.current) return
      dragging.current = false
      document.body.classList.remove('ms-console-resizing')
    }

    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    window.addEventListener('pointercancel', onUp)
    return () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      window.removeEventListener('pointercancel', onUp)
      document.body.classList.remove('ms-console-resizing')
    }
  }, [axis, clamp, invert])

  useEffect(() => {
    if (!storageKey) return
    try {
      localStorage.setItem(storageKey, String(size))
    } catch {
      /* ignore */
    }
  }, [size, storageKey])

  return { size, setSize, onPointerDown, clamp }
}
