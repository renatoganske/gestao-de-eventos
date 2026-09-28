export type ToastKind = 'success' | 'error'

export interface ToastMessage {
  kind: ToastKind
  text: string
}

export interface ToastProps {
  toast: ToastMessage
}

export function Toast({ toast }: ToastProps) {
  return (
    <div className={`toast toast-${toast.kind}`} role={toast.kind === 'error' ? 'alert' : 'status'}>
      {toast.text}
    </div>
  )
}
