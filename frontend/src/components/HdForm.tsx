import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createHd, updateHd, HD_STATUS_LABEL, type HdDto, type HdStatus } from '../api/hds'
import { Button } from './Button'
import { FormField } from './FormField'

export interface HdFormProps {
  hd?: HdDto
  onSaved: (hd: HdDto) => void
  onCancel: () => void
}

interface FormState {
  name: string
  capacityGb: string
  usedSpaceGb: string
  physicalLocation: string
  serialNumber: string
  acquisitionDate: string
  status: HdStatus
}

function toFormState(hd?: HdDto): FormState {
  return {
    name: hd?.name ?? '',
    capacityGb: hd?.capacityGb != null ? String(hd.capacityGb) : '',
    usedSpaceGb: hd?.usedSpaceGb != null ? String(hd.usedSpaceGb) : '',
    physicalLocation: hd?.physicalLocation ?? '',
    serialNumber: hd?.serialNumber ?? '',
    acquisitionDate: hd?.acquisitionDate ?? '',
    status: hd?.status ?? 'ACTIVE',
  }
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível salvar o HD. Tente novamente.'

export function HdForm({ hd, onSaved, onCancel }: HdFormProps) {
  const [form, setForm] = useState<FormState>(() => toFormState(hd))
  const [nameError, setNameError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.name.trim()) {
      setNameError('Informe o nome do HD.')
      return
    }
    setNameError(null)
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      const dto = {
        name: form.name.trim(),
        capacityGb: form.capacityGb ? Number(form.capacityGb) : null,
        usedSpaceGb: form.usedSpaceGb ? Number(form.usedSpaceGb) : null,
        physicalLocation: form.physicalLocation.trim() || null,
        serialNumber: form.serialNumber.trim() || null,
        acquisitionDate: form.acquisitionDate || null,
        status: form.status,
      }
      const saved = hd ? await updateHd(hd.id, dto) : await createHd(dto)
      onSaved(saved)
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <FormField label="Nome" htmlFor="hd-name" error={nameError ?? undefined}>
        <input id="hd-name" value={form.name} onChange={(event) => updateField('name', event.target.value)} disabled={isSubmitting} />
      </FormField>
      <FormField label="Capacidade (GB)" htmlFor="hd-capacity">
        <input
          id="hd-capacity"
          type="number"
          min="0"
          value={form.capacityGb}
          onChange={(event) => updateField('capacityGb', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Espaço usado (GB)" htmlFor="hd-used-space">
        <input
          id="hd-used-space"
          type="number"
          min="0"
          value={form.usedSpaceGb}
          onChange={(event) => updateField('usedSpaceGb', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Localização física" htmlFor="hd-location">
        <input
          id="hd-location"
          value={form.physicalLocation}
          onChange={(event) => updateField('physicalLocation', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Número de série" htmlFor="hd-serial">
        <input
          id="hd-serial"
          value={form.serialNumber}
          onChange={(event) => updateField('serialNumber', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Data de aquisição" htmlFor="hd-acquisition-date">
        <input
          id="hd-acquisition-date"
          type="date"
          value={form.acquisitionDate}
          onChange={(event) => updateField('acquisitionDate', event.target.value)}
          disabled={isSubmitting}
        />
      </FormField>
      <FormField label="Status" htmlFor="hd-status">
        <select id="hd-status" value={form.status} onChange={(event) => updateField('status', event.target.value as HdStatus)} disabled={isSubmitting}>
          {Object.entries(HD_STATUS_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
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
          {isSubmitting ? 'Salvando...' : 'Salvar HD'}
        </Button>
      </div>
    </form>
  )
}
