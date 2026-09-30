import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { Button } from './Button'
import { Checkbox } from './Checkbox'
import { FormField } from './FormField'

export interface SimpleNameQuickCreateFormProps<T> {
  fieldId: string
  emptyError: string
  genericError: string
  submitLabel: string
  /** When set, an extra checkbox with this label is shown and its value is passed as the second arg of onCreate. */
  checkboxLabel?: string
  onCreate: (name: string, checked: boolean) => Promise<T>
  onCreated: (item: T) => void
  onCancel: () => void
}

export function SimpleNameQuickCreateForm<T>({
  fieldId,
  emptyError,
  genericError,
  submitLabel,
  checkboxLabel,
  onCreate,
  onCreated,
  onCancel,
}: SimpleNameQuickCreateFormProps<T>) {
  const [name, setName] = useState('')
  const [checked, setChecked] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setError(emptyError)
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      const created = await onCreate(name.trim(), checked)
      onCreated(created)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : genericError)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor={fieldId}>
        <input id={fieldId} value={name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting} />
      </FormField>
      {checkboxLabel && (
        <Checkbox label={checkboxLabel} checked={checked} onChange={setChecked} disabled={isSubmitting} />
      )}
      {error && (
        <p role="alert" className="events-error">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <Button type="button" variant="ghost" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {isSubmitting ? 'Salvando...' : submitLabel}
        </Button>
      </div>
    </form>
  )
}
