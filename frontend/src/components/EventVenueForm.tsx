import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createEventVenue, updateEventVenue, type EventVenueDto } from '../api/eventVenues'
import { Button } from './Button'
import { FormField } from './FormField'

export interface EventVenueFormProps {
  venue?: EventVenueDto
  onSaved: (venue: EventVenueDto) => void
  onCancel: () => void
}

interface FormState {
  name: string
  address: string
  city: string
  state: string
  type: string
}

function toFormState(venue?: EventVenueDto): FormState {
  return {
    name: venue?.name ?? '',
    address: venue?.address ?? '',
    city: venue?.city ?? '',
    state: venue?.state ?? '',
    type: venue?.type ?? '',
  }
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível salvar o local. Tente novamente.'

export function EventVenueForm({ venue, onSaved, onCancel }: EventVenueFormProps) {
  const [form, setForm] = useState<FormState>(() => toFormState(venue))
  const [nameError, setNameError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.name.trim()) {
      setNameError('Informe o nome do local.')
      return
    }
    setNameError(null)
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      const dto = {
        name: form.name.trim(),
        address: form.address.trim() || null,
        city: form.city.trim() || null,
        state: form.state.trim() || null,
        type: form.type.trim() || null,
      }
      const saved = venue ? await updateEventVenue(venue.id, dto) : await createEventVenue(dto)
      onSaved(saved)
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="venue-name" error={nameError ?? undefined}>
        <input id="venue-name" value={form.name} onChange={(event) => updateField('name', event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Endereço" htmlFor="venue-address">
        <input id="venue-address" value={form.address} onChange={(event) => updateField('address', event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Cidade" htmlFor="venue-city">
        <input id="venue-city" value={form.city} onChange={(event) => updateField('city', event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Estado" htmlFor="venue-state">
        <input id="venue-state" value={form.state} onChange={(event) => updateField('state', event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Tipo" htmlFor="venue-type">
        <input id="venue-type" value={form.type} onChange={(event) => updateField('type', event.target.value)} disabled={isSubmitting} />
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
          {isSubmitting ? 'Salvando...' : 'Salvar local'}
        </Button>
      </div>
    </form>
  )
}
