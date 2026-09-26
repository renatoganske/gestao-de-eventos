import { useState, type FormEvent } from 'react'
import { createEventVenue, type EventVenueDto } from '../api/eventVenues'
import { ApiError } from '../api/client'
import { Button } from './Button'
import { FormField } from './FormField'

export interface EventVenueQuickCreateFormProps {
  onCreated: (venue: EventVenueDto) => void
  onCancel: () => void
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível criar o local. Tente novamente.'

export function EventVenueQuickCreateForm({ onCreated, onCancel }: EventVenueQuickCreateFormProps) {
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [city, setCity] = useState('')
  const [state, setState] = useState('')
  const [type, setType] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Informe o nome do local.')
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      const venue = await createEventVenue({
        name: name.trim(),
        address: address.trim() || null,
        city: city.trim() || null,
        state: state.trim() || null,
        type: type.trim() || null,
      })
      onCreated(venue)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="qc-venue-name">
        <input id="qc-venue-name" value={name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Endereço" htmlFor="qc-venue-address">
        <input id="qc-venue-address" value={address} onChange={(event) => setAddress(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Cidade" htmlFor="qc-venue-city">
        <input id="qc-venue-city" value={city} onChange={(event) => setCity(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Estado" htmlFor="qc-venue-state">
        <input id="qc-venue-state" value={state} onChange={(event) => setState(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Tipo" htmlFor="qc-venue-type">
        <input id="qc-venue-type" value={type} onChange={(event) => setType(event.target.value)} disabled={isSubmitting} />
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
          {isSubmitting ? 'Salvando...' : 'Salvar local'}
        </Button>
      </div>
    </form>
  )
}
