import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createProfessional, updateProfessional, type ProfessionalDto } from '../api/professionals'
import { createProfessionalType, type ProfessionalTypeDto } from '../api/professionalTypes'
import { createSpecialtyTag, type SpecialtyTagDto } from '../api/specialtyTags'
import { Autocomplete } from './Autocomplete'
import { Button } from './Button'
import { FormField } from './FormField'
import { Modal } from './Modal'
import { SimpleNameQuickCreateForm } from './SimpleNameQuickCreateForm'
import { TagMultiSelect } from './TagMultiSelect'

export interface ProfessionalFormProps {
  professional?: ProfessionalDto
  professionalTypes: ProfessionalTypeDto[]
  specialtyTags: SpecialtyTagDto[]
  onProfessionalTypeCreated: (type: ProfessionalTypeDto) => void
  onSpecialtyTagCreated: (tag: SpecialtyTagDto) => void
  onSaved: (professional: ProfessionalDto) => void
  onCancel: () => void
}

interface FormState {
  name: string
  typeId: string
  contact: string
  specialtyTagIds: string[]
  otherInfo: string
}

function toFormState(professional?: ProfessionalDto): FormState {
  return {
    name: professional?.name ?? '',
    typeId: professional?.type?.id ?? '',
    contact: professional?.contact ?? '',
    specialtyTagIds: professional?.specialtyTags.map((tag) => tag.id) ?? [],
    otherInfo: professional?.otherInfo ?? '',
  }
}

type QuickCreateTarget = 'type' | 'tag' | null

const GENERIC_ERROR_MESSAGE = 'Não foi possível salvar o profissional. Tente novamente.'

export function ProfessionalForm({
  professional,
  professionalTypes,
  specialtyTags,
  onProfessionalTypeCreated,
  onSpecialtyTagCreated,
  onSaved,
  onCancel,
}: ProfessionalFormProps) {
  const [form, setForm] = useState<FormState>(() => toFormState(professional))
  const [nameError, setNameError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [quickCreateTarget, setQuickCreateTarget] = useState<QuickCreateTarget>(null)

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function closeQuickCreate() {
    setQuickCreateTarget(null)
  }

  function handleTypeCreated(type: ProfessionalTypeDto) {
    onProfessionalTypeCreated(type)
    updateField('typeId', type.id)
    setQuickCreateTarget(null)
  }

  function handleTagCreated(tag: SpecialtyTagDto) {
    onSpecialtyTagCreated(tag)
    setForm((current) => ({ ...current, specialtyTagIds: [...current.specialtyTagIds, tag.id] }))
    setQuickCreateTarget(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.name.trim()) {
      setNameError('Informe o nome do profissional.')
      return
    }
    setNameError(null)
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      const dto = {
        name: form.name.trim(),
        typeId: form.typeId || null,
        contact: form.contact.trim() || null,
        specialtyTagIds: form.specialtyTagIds,
        otherInfo: form.otherInfo.trim() || null,
      }
      const saved = professional ? await updateProfessional(professional.id, dto) : await createProfessional(dto)
      onSaved(saved)
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <form onSubmit={handleSubmit}>
        <FormField label="Nome" htmlFor="professional-name" error={nameError ?? undefined}>
          <input
            id="professional-name"
            value={form.name}
            onChange={(event) => updateField('name', event.target.value)}
            disabled={isSubmitting}
          />
        </FormField>
        <FormField label="Tipo" htmlFor="professional-type">
          <Autocomplete
            id="professional-type"
            options={professionalTypes.map((type) => ({ id: type.id, label: type.name }))}
            value={form.typeId}
            onChange={(typeId) => updateField('typeId', typeId)}
            placeholder="Buscar tipo..."
            onCreateNew={() => setQuickCreateTarget('type')}
          />
        </FormField>
        <FormField label="Contato" htmlFor="professional-contact">
          <input
            id="professional-contact"
            value={form.contact}
            onChange={(event) => updateField('contact', event.target.value)}
            disabled={isSubmitting}
          />
        </FormField>
        <FormField label="Especialidades" htmlFor="professional-specialty-tags">
          <TagMultiSelect
            id="professional-specialty-tags"
            options={specialtyTags.map((tag) => ({ id: tag.id, label: tag.name }))}
            selectedIds={form.specialtyTagIds}
            onChange={(ids) => updateField('specialtyTagIds', ids)}
            placeholder="Buscar especialidade..."
            onCreateNew={() => setQuickCreateTarget('tag')}
          />
        </FormField>
        <FormField label="Observações" htmlFor="professional-other-info">
          <textarea
            id="professional-other-info"
            value={form.otherInfo}
            onChange={(event) => updateField('otherInfo', event.target.value)}
            disabled={isSubmitting}
          />
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
            {isSubmitting ? 'Salvando...' : 'Salvar profissional'}
          </Button>
        </div>
      </form>

      {quickCreateTarget === 'type' && (
        <Modal title="Novo tipo de profissional" onClose={closeQuickCreate}>
          <SimpleNameQuickCreateForm
            fieldId="qc-professional-type-name"
            emptyError="Informe o nome do tipo."
            genericError="Não foi possível criar o tipo. Tente novamente."
            submitLabel="Salvar tipo"
            onCreate={(name) => createProfessionalType({ name })}
            onCreated={handleTypeCreated}
            onCancel={closeQuickCreate}
          />
        </Modal>
      )}
      {quickCreateTarget === 'tag' && (
        <Modal title="Nova especialidade" onClose={closeQuickCreate}>
          <SimpleNameQuickCreateForm
            fieldId="qc-specialty-tag-name"
            emptyError="Informe o nome da especialidade."
            genericError="Não foi possível criar a especialidade. Tente novamente."
            submitLabel="Salvar especialidade"
            onCreate={(name) => createSpecialtyTag({ name })}
            onCreated={handleTagCreated}
            onCancel={closeQuickCreate}
          />
        </Modal>
      )}
    </>
  )
}
