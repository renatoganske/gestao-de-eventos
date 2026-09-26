import { Button } from './Button'
import { Modal } from './Modal'

export interface ConfirmDialogProps {
  title: string
  message: string
  confirmLabel?: string
  isConfirming?: boolean
  error?: string | null
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Confirmar',
  isConfirming = false,
  error,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p>{message}</p>
      {error && (
        <p role="alert" className="events-error">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <Button type="button" variant="ghost" onClick={onCancel} disabled={isConfirming}>
          Cancelar
        </Button>
        <Button type="button" variant="primary" onClick={onConfirm} disabled={isConfirming}>
          {isConfirming ? 'Excluindo...' : confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
