import { useState, type FormEvent } from 'react'
import { createCustomer, type CustomerDto } from '../api/customers'
import { ApiError } from '../api/client'
import { Button } from './Button'
import { FormField } from './FormField'

export interface CustomerQuickCreateFormProps {
  onCreated: (customer: CustomerDto) => void
  onCancel: () => void
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível criar o cliente. Tente novamente.'

export function CustomerQuickCreateForm({ onCreated, onCancel }: CustomerQuickCreateFormProps) {
  const [name, setName] = useState('')
  const [contact, setContact] = useState('')
  const [address, setAddress] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Informe o nome do cliente.')
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      const customer = await createCustomer({
        name: name.trim(),
        contact: contact.trim() || null,
        address: address.trim() || null,
        notes: notes.trim() || null,
      })
      onCreated(customer)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="qc-customer-name">
        <input id="qc-customer-name" value={name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Contato" htmlFor="qc-customer-contact">
        <input id="qc-customer-contact" value={contact} onChange={(event) => setContact(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Endereço" htmlFor="qc-customer-address">
        <input id="qc-customer-address" value={address} onChange={(event) => setAddress(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Observações" htmlFor="qc-customer-notes">
        <textarea id="qc-customer-notes" value={notes} onChange={(event) => setNotes(event.target.value)} disabled={isSubmitting} />
      </FormField>
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
          {isSubmitting ? 'Salvando...' : 'Salvar cliente'}
        </Button>
      </div>
    </form>
  )
}
