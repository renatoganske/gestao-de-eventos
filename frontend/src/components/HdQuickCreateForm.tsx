import { useState, type FormEvent } from 'react'
import { createHd, type HdDto } from '../api/hds'
import { ApiError } from '../api/client'
import { validateRealCapacity } from './hdCapacityValidation'
import { Button } from './Button'
import { FormField } from './FormField'

export interface HdQuickCreateFormProps {
  onCreated: (hd: HdDto) => void
  onCancel: () => void
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível criar o HD. Tente novamente.'

export function HdQuickCreateForm({ onCreated, onCancel }: HdQuickCreateFormProps) {
  const [name, setName] = useState('')
  const [capacityGb, setCapacityGb] = useState('')
  const [realCapacityGb, setRealCapacityGb] = useState('')
  const [physicalLocation, setPhysicalLocation] = useState('')
  const [serialNumber, setSerialNumber] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Informe o nome do HD.')
      return
    }
    const realCapacityMessage = validateRealCapacity(capacityGb, realCapacityGb)
    if (realCapacityMessage) {
      setError(realCapacityMessage)
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      const hd = await createHd({
        name: name.trim(),
        capacityGb: capacityGb ? Number(capacityGb) : null,
        realCapacityGb: realCapacityGb ? Number(realCapacityGb) : null,
        usedSpaceGb: null,
        physicalLocation: physicalLocation.trim() || null,
        serialNumber: serialNumber.trim() || null,
        acquisitionDate: null,
        status: 'ACTIVE',
      })
      onCreated(hd)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="qc-hd-name">
        <input id="qc-hd-name" value={name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Capacidade nominal (GB)" htmlFor="qc-hd-capacity">
        <input
          id="qc-hd-capacity"
          type="number"
          min="0"
          value={capacityGb}
          onChange={(event) => setCapacityGb(event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Capacidade real (GB)" htmlFor="qc-hd-real-capacity">
        <input
          id="qc-hd-real-capacity"
          type="number"
          min="1"
          value={realCapacityGb}
          onChange={(event) => setRealCapacityGb(event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Localização física" htmlFor="qc-hd-location">
        <input
          id="qc-hd-location"
          value={physicalLocation}
          onChange={(event) => setPhysicalLocation(event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Número de série" htmlFor="qc-hd-serial">
        <input id="qc-hd-serial" value={serialNumber} onChange={(event) => setSerialNumber(event.target.value)} disabled={isSubmitting} />
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
          {isSubmitting ? 'Salvando...' : 'Salvar HD'}
        </Button>
      </div>
    </form>
  )
}
