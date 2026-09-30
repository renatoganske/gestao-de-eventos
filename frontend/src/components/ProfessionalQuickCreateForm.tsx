import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createProfessional, type ProfessionalDto } from '../api/professionals'
import { fetchProfessionalTypes, type ProfessionalTypeDto } from '../api/professionalTypes'
import { Button } from './Button'
import { FormField } from './FormField'

export interface ProfessionalQuickCreateFormProps {
  onCreated: (professional: ProfessionalDto) => void
  onCancel: () => void
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível criar o profissional. Tente novamente.'

export function ProfessionalQuickCreateForm({ onCreated, onCancel }: ProfessionalQuickCreateFormProps) {
  const [name, setName] = useState('')
  const [typeId, setTypeId] = useState('')
  const [contact, setContact] = useState('')
  const [types, setTypes] = useState<ProfessionalTypeDto[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    // The type is optional here, so a failure to load the list must not block creating the professional.
    fetchProfessionalTypes()
      .then((loaded) => {
        if (!cancelled) {
          setTypes(loaded)
        }
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Informe o nome do profissional.')
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      const professional = await createProfessional({
        name: name.trim(),
        typeId: typeId || null,
        contact: contact.trim() || null,
        specialtyTagIds: [],
        otherInfo: null,
      })
      onCreated(professional)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="qc-professional-name">
        <input id="qc-professional-name" value={name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Tipo" htmlFor="qc-professional-type">
        <select id="qc-professional-type" value={typeId} onChange={(event) => setTypeId(event.target.value)} disabled={isSubmitting}>
          <option value="">Sem tipo</option>
          {types.map((type) => (
            <option key={type.id} value={type.id}>
              {type.name}
            </option>
          ))}
        </select>
      </FormField>
      <FormField label="Contato" htmlFor="qc-professional-contact">
        <input id="qc-professional-contact" value={contact} onChange={(event) => setContact(event.target.value)} disabled={isSubmitting} />
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
          {isSubmitting ? 'Salvando...' : 'Salvar profissional'}
        </Button>
      </div>
    </form>
  )
}
