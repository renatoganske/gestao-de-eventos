import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createCustomer, updateCustomer, type CustomerDto } from '../api/customers'
import { Button } from './Button'
import { FormField } from './FormField'

export interface CustomerFormProps {
  customer?: CustomerDto
  onSaved: (customer: CustomerDto) => void
  onCancel: () => void
}

interface FormState {
  name: string
  contact: string
  address: string
  notes: string
}

function toFormState(customer?: CustomerDto): FormState {
  return {
    name: customer?.name ?? '',
    contact: customer?.contact ?? '',
    address: customer?.address ?? '',
    notes: customer?.notes ?? '',
  }
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível salvar o cliente. Tente novamente.'

export function CustomerForm({ customer, onSaved, onCancel }: CustomerFormProps) {
  const [form, setForm] = useState<FormState>(() => toFormState(customer))
  const [nameError, setNameError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.name.trim()) {
      setNameError('Informe o nome do cliente.')
      return
    }
    setNameError(null)
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      const dto = {
        name: form.name.trim(),
        contact: form.contact.trim() || null,
        address: form.address.trim() || null,
        notes: form.notes.trim() || null,
      }
      const saved = customer ? await updateCustomer(customer.id, dto) : await createCustomer(dto)
      onSaved(saved)
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="customer-name" error={nameError ?? undefined}>
        <input id="customer-name" value={form.name} onChange={(event) => updateField('name', event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Contato" htmlFor="customer-contact">
        <input
          id="customer-contact"
          value={form.contact}
          onChange={(event) => updateField('contact', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Endereço" htmlFor="customer-address">
        <input
          id="customer-address"
          value={form.address}
          onChange={(event) => updateField('address', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Observações" htmlFor="customer-notes">
        <textarea id="customer-notes" value={form.notes} onChange={(event) => updateField('notes', event.target.value)} disabled={isSubmitting} />
      </FormField>
      {submitError && (
        <p role="alert" className="events-error">
          {submitError}
        </p>
      )}
      <div className="modal-actions">
        <Button type="button" variant="ghost" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {isSubmitting ? 'Salvando...' : 'Salvar cliente'}
        </Button>
      </div>
    </form>
  )
}
