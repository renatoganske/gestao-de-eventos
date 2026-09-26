import { useEffect, useState } from 'react'
import type { ToastMessage } from '../components/Toast'

const AUTO_DISMISS_MS = 4000

export function useToast(): [ToastMessage | null, (toast: ToastMessage) => void] {
  const [toast, setToast] = useState<ToastMessage | null>(null)

  useEffect(() => {
    if (!toast) {
      return
    }
    const timer = setTimeout(() => setToast(null), AUTO_DISMISS_MS)
    return () => clearTimeout(timer)
  }, [toast])

  return [toast, setToast]
}
