import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchCustomers, type CustomerDto } from '../api/customers'
import {
  DELIVERY_STATUS_LABEL,
  createEvent,
  deleteEvent,
  fetchEventById,
  updateEvent,
  type CreateEventDto,
  type DeliveryStatus,
  type EventDto,
} from '../api/events'
import { createEventType, fetchEventTypes, type EventTypeDto } from '../api/eventTypes'
import { fetchEventVenues, type EventVenueDto } from '../api/eventVenues'
import { fetchHds, type HdDto } from '../api/hds'
import { fetchProfessionals, type ProfessionalDto } from '../api/professionals'
import { Autocomplete } from '../components/Autocomplete'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { Checkbox } from '../components/Checkbox'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { CustomerQuickCreateForm } from '../components/CustomerQuickCreateForm'
import { EventCustomersSection } from '../components/EventCustomersSection'
import { EventTeamSection } from '../components/EventTeamSection'
import { EventVenuesSection } from '../components/EventVenuesSection'
import { EventVenueQuickCreateForm } from '../components/EventVenueQuickCreateForm'
import { FormField } from '../components/FormField'
import { HdQuickCreateForm } from '../components/HdQuickCreateForm'
import { Modal } from '../components/Modal'
import { ProfessionalQuickCreateForm } from '../components/ProfessionalQuickCreateForm'
import { SimpleNameQuickCreateForm } from '../components/SimpleNameQuickCreateForm'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useFormDraft } from '../hooks/useFormDraft'
import { useToast } from '../hooks/useToast'
import { customerRowsFromEvent, customerRowsToAssignments, type CustomerRow } from '../utils/eventCustomers'
import { hasRoleWithoutProfessional, teamRowsFromEvent, teamRowsToAssignments, type TeamRow } from '../utils/eventTeam'
import { deletableEvent, deleteConfirmationMessage } from '../utils/eventDeletion'
import { hasRoleWithoutVenue, venueRowsFromEvent, venueRowsToAssignments, type VenueRow } from '../utils/eventVenues'
import './EventFormPage.css'

const GENERIC_LOAD_ERROR = 'Não foi possível carregar os dados do formulário. Tente novamente em instantes.'
const GENERIC_SUBMIT_ERROR = 'Não foi possível salvar o evento. Tente novamente.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o evento. Tente novamente.'

interface EventFormState {
  eventCode: string
  eventTypeId: string
  name: string
  eventDate: string
  daytimeWedding: boolean
  outdoorWedding: boolean
  guestCount: string
  description: string
  amount: string
  sizeGb: string
  deliveryStatus: DeliveryStatus
  hdId: string
  venues: VenueRow[]
  customers: CustomerRow[]
  professionals: TeamRow[]
}

const EMPTY_FORM_STATE: EventFormState = {
  eventCode: '',
  eventTypeId: '',
  name: '',
  eventDate: '',
  daytimeWedding: false,
  outdoorWedding: false,
  guestCount: '',
  description: '',
  amount: '',
  sizeGb: '',
  deliveryStatus: 'PENDING',
  hdId: '',
  venues: [],
  customers: [],
  professionals: [],
}

type FormErrors = Partial<Record<'eventCode' | 'eventTypeId' | 'name' | 'eventDate' | 'professionals' | 'venues', string>>

interface ReferenceData {
  eventTypes: EventTypeDto[]
  venues: EventVenueDto[]
  hds: HdDto[]
  customers: CustomerDto[]
  professionals: ProfessionalDto[]
}

type QuickCreateTarget = 'customer' | 'venue' | 'hd' | 'eventType' | 'professional' | null

function eventToFormState(event: EventDto): EventFormState {
  return {
    eventCode: event.eventCode,
    eventTypeId: event.type?.id ?? '',
    name: event.name,
    eventDate: event.eventDate ?? '',
    daytimeWedding: event.daytimeWedding ?? false,
    outdoorWedding: event.outdoorWedding ?? false,
    guestCount: event.guestCount != null ? String(event.guestCount) : '',
    description: event.description ?? '',
    amount: event.amount != null ? String(event.amount) : '',
    sizeGb: event.sizeGb != null ? String(event.sizeGb) : '',
    deliveryStatus: event.deliveryStatus,
    hdId: event.hdId ?? '',
    venues: venueRowsFromEvent(event.venues),
    customers: customerRowsFromEvent(event.customers),
    professionals: teamRowsFromEvent(event.eventProfessionals),
  }
}

function formStateToDto(form: EventFormState, isWedding: boolean): CreateEventDto {
  return {
    eventCode: form.eventCode.trim(),
    eventTypeId: form.eventTypeId || null,
    name: form.name.trim(),
    eventDate: form.eventDate || null,
    daytimeWedding: isWedding ? form.daytimeWedding : null,
    outdoorWedding: isWedding ? form.outdoorWedding : null,
    guestCount: form.guestCount ? Number(form.guestCount) : null,
    description: form.description.trim() || null,
    amount: form.amount ? Number(form.amount) : null,
    sizeGb: form.sizeGb ? Number(form.sizeGb) : null,
    deliveryStatus: form.deliveryStatus,
    hdId: form.hdId || null,
    // Same rule as the team: always an array; "?? []" covers a draft saved before venues became a list.
    venues: venueRowsToAssignments(form.venues ?? []),
    // Same rule as the team: always an array; "?? []" covers a draft saved before customers became a list.
    customers: customerRowsToAssignments(form.customers ?? []),
    // Always an array (never omitted): omitting it on update would keep the old team instead of replacing it.
    // "?? []" covers a draft saved before the team existed.
    professionals: teamRowsToAssignments(form.professionals ?? []),
  }
}

function validate(form: EventFormState): FormErrors {
  const errors: FormErrors = {}
  if (!form.eventCode.trim()) {
    errors.eventCode = 'Informe o código do evento.'
  }
  if (!form.eventTypeId) {
    errors.eventTypeId = 'Selecione o tipo do evento.'
  }
  if (!form.name.trim()) {
    errors.name = 'Informe o nome do evento.'
  }
  if (!form.eventDate) {
    errors.eventDate = 'Informe a data do evento.'
  }
  if (hasRoleWithoutProfessional(form.professionals ?? [])) {
    errors.professionals = 'Selecione o profissional em cada linha que tem um papel, ou remova a linha.'
  }
  if (hasRoleWithoutVenue(form.venues ?? [])) {
    errors.venues = 'Selecione o local em cada linha que tem um papel, ou remova a linha.'
  }
  return errors
}

export function EventFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEditMode = Boolean(id)
  const navigate = useNavigate()

  const draftKey = isEditMode ? `event-edit:${id}` : 'event-create'
  const [form, setForm, clearDraft] = useFormDraft<EventFormState>(draftKey, EMPTY_FORM_STATE)
  const [hasRecoveredDraft] = useState(() => form !== EMPTY_FORM_STATE)
  const [errors, setErrors] = useState<FormErrors>({})
  const [toast, setToast] = useToast()

  const [referenceData, setReferenceData] = useState<ReferenceData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [quickCreateTarget, setQuickCreateTarget] = useState<QuickCreateTarget>(null)
  const [quickCreateTeamRowKey, setQuickCreateTeamRowKey] = useState<string | null>(null)
  const [quickCreateCustomerRowKey, setQuickCreateCustomerRowKey] = useState<string | null>(null)
  const [quickCreateVenueRowKey, setQuickCreateVenueRowKey] = useState<string | null>(null)
  // The saved event (not the form, which may hold unsaved edits) is what the delete confirmation describes.
  const [savedEvent, setSavedEvent] = useState<EventDto | null>(null)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function init() {
      setIsLoading(true)
      setLoadError(null)
      try {
        const [eventTypes, venues, hds, customers, professionals] = await Promise.all([
          fetchEventTypes(),
          fetchEventVenues(),
          fetchHds(),
          fetchCustomers(),
          fetchProfessionals(),
        ])
        const loadedEvent = isEditMode && id ? await fetchEventById(id) : null
        if (cancelled) {
          return
        }
        setReferenceData({ eventTypes, venues, hds, customers, professionals })
        setSavedEvent(loadedEvent)
        if (loadedEvent && !hasRecoveredDraft) {
          setForm(eventToFormState(loadedEvent))
        }
      } catch (err) {
        if (cancelled) {
          return
        }
        setLoadError(err instanceof ApiError ? err.message : GENERIC_LOAD_ERROR)
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    init()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const selectedType = referenceData?.eventTypes.find((type) => type.id === form.eventTypeId)
  const isWedding = selectedType?.hasWeddingFields === true

  function updateField<K extends keyof EventFormState>(key: K, value: EventFormState[K]) {
    setForm({ ...form, [key]: value })
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      return
    }

    setIsSubmitting(true)
    try {
      const dto = formStateToDto(form, isWedding)
      if (isEditMode && id) {
        await updateEvent(id, dto)
        clearDraft()
        // Back to the read-only view; this page unmounts, so the confirmation travels as router state.
        navigate(`/eventos/${id}`, { state: { toast: { kind: 'success', text: 'Evento atualizado com sucesso.' } } })
        return
      } else {
        await createEvent(dto)
        clearDraft()
        setToast({ kind: 'success', text: 'Evento criado com sucesso.' })
      }
    } catch (err) {
      setToast({ kind: 'error', text: err instanceof ApiError ? err.message : GENERIC_SUBMIT_ERROR })
    } finally {
      setIsSubmitting(false)
    }
  }

  // Editing comes from the event's detail page, so leaving the form goes back there; creating goes back to the list.
  const backPath = isEditMode && id ? `/eventos/${id}` : '/eventos'

  function handleCancel() {
    clearDraft()
    navigate(backPath)
  }

  function closeDeleteDialog() {
    setIsDeleteDialogOpen(false)
    setDeleteError(null)
  }

  async function handleConfirmDelete() {
    if (!id) {
      return
    }
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteEvent(id)
      clearDraft()
      // This page unmounts on navigation, so the confirmation travels to the list as router state.
      navigate('/eventos', { state: { toast: { kind: 'success', text: 'Evento excluído com sucesso.' } } })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
      setIsDeleting(false)
    }
  }

  function closeQuickCreate() {
    setQuickCreateTarget(null)
    setQuickCreateTeamRowKey(null)
    setQuickCreateCustomerRowKey(null)
    setQuickCreateVenueRowKey(null)
  }

  function openVenueQuickCreate(rowKey: string) {
    setQuickCreateVenueRowKey(rowKey)
    setQuickCreateTarget('venue')
  }

  function openCustomerQuickCreate(rowKey: string) {
    setQuickCreateCustomerRowKey(rowKey)
    setQuickCreateTarget('customer')
  }

  function openProfessionalQuickCreate(rowKey: string) {
    setQuickCreateTeamRowKey(rowKey)
    setQuickCreateTarget('professional')
  }

  function handleProfessionalCreated(professional: ProfessionalDto) {
    setReferenceData((current) =>
      current ? { ...current, professionals: [...current.professionals, professional] } : current,
    )
    const rows = form.professionals ?? []
    updateField(
      'professionals',
      rows.map((row) => (row.key === quickCreateTeamRowKey ? { ...row, professionalId: professional.id } : row)),
    )
    setQuickCreateTarget(null)
    setQuickCreateTeamRowKey(null)
    setToast({ kind: 'success', text: 'Profissional criado e selecionado.' })
  }

  function handleEventTypeCreated(eventType: EventTypeDto) {
    setReferenceData((current) => (current ? { ...current, eventTypes: [...current.eventTypes, eventType] } : current))
    updateField('eventTypeId', eventType.id)
    setQuickCreateTarget(null)
    setToast({ kind: 'success', text: 'Tipo criado e selecionado.' })
  }

  function handleCustomerCreated(customer: CustomerDto) {
    setReferenceData((current) => (current ? { ...current, customers: [...current.customers, customer] } : current))
    const rows = form.customers ?? []
    updateField(
      'customers',
      rows.map((row) => (row.key === quickCreateCustomerRowKey ? { ...row, customerId: customer.id } : row)),
    )
    setQuickCreateTarget(null)
    setQuickCreateCustomerRowKey(null)
    setToast({ kind: 'success', text: 'Cliente criado e selecionado.' })
  }

  function handleVenueCreated(venue: EventVenueDto) {
    setReferenceData((current) => (current ? { ...current, venues: [...current.venues, venue] } : current))
    const rows = form.venues ?? []
    updateField(
      'venues',
      rows.map((row) => (row.key === quickCreateVenueRowKey ? { ...row, venueId: venue.id } : row)),
    )
    setQuickCreateTarget(null)
    setQuickCreateVenueRowKey(null)
    setToast({ kind: 'success', text: 'Local criado e selecionado.' })
  }

  function handleHdCreated(hd: HdDto) {
    setReferenceData((current) => (current ? { ...current, hds: [...current.hds, hd] } : current))
    updateField('hdId', hd.id)
    setQuickCreateTarget(null)
    setToast({ kind: 'success', text: 'HD criado e selecionado.' })
  }

  return (
    <section>
      <TopBar
        title={isEditMode ? 'Editar evento' : 'Novo evento'}
        action={
          <div className="event-form-topbar-actions">
            {isEditMode && savedEvent && (
              <Button type="button" variant="ghost" onClick={() => setIsDeleteDialogOpen(true)}>
                Excluir evento
              </Button>
            )}
            <Link to={backPath} className="btn" onClick={clearDraft}>
              {isEditMode ? 'Voltar para o evento' : 'Voltar para eventos'}
            </Link>
          </div>
        }
      />
      <div className="event-form-content">
        {isLoading && <p className="events-empty">Carregando...</p>}

        {!isLoading && loadError && (
          <p role="alert" className="events-error">
            {loadError}
          </p>
        )}

        {!isLoading && !loadError && (
          <form onSubmit={handleSubmit} noValidate>
            <Card title="Informações básicas">
              <div className="event-form-grid">
                <FormField label="Código do evento" htmlFor="event-code" error={errors.eventCode}>
                  <input
                    id="event-code"
                    value={form.eventCode}
                    onChange={(event) => updateField('eventCode', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>

                <FormField label="Tipo" htmlFor="event-type" error={errors.eventTypeId}>
                  <div className="autocomplete-row event-type-row">
                    <select
                      id="event-type"
                      value={form.eventTypeId}
                      onChange={(event) => updateField('eventTypeId', event.target.value)}
                      disabled={isSubmitting}
                    >
                      <option value="">Selecione...</option>
                      {referenceData?.eventTypes.map((type) => (
                        <option key={type.id} value={type.id}>
                          {type.name}
                        </option>
                      ))}
                    </select>
                    <Button type="button" variant="ghost" onClick={() => setQuickCreateTarget('eventType')} disabled={isSubmitting}>
                      + Novo
                    </Button>
                  </div>
                </FormField>

                <FormField label="Nome do evento" htmlFor="event-name" error={errors.name} className="span-2">
                  <input
                    id="event-name"
                    value={form.name}
                    onChange={(event) => updateField('name', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>

                <FormField label="Data do evento" htmlFor="event-date" error={errors.eventDate}>
                  <input
                    id="event-date"
                    type="date"
                    value={form.eventDate}
                    onChange={(event) => updateField('eventDate', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>

                <FormField label="Nº de convidados" htmlFor="event-guest-count">
                  <input
                    id="event-guest-count"
                    type="number"
                    min="0"
                    value={form.guestCount}
                    onChange={(event) => updateField('guestCount', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>

                <FormField label="Valor (R$)" htmlFor="event-amount">
                  <input
                    id="event-amount"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.amount}
                    onChange={(event) => updateField('amount', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>

                {isWedding && (
                  <>
                    <div className="span-2 event-form-flags">
                      <Checkbox
                        id="event-daytime-wedding"
                        label="Casamento diurno?"
                        checked={form.daytimeWedding}
                        onChange={(checked) => updateField('daytimeWedding', checked)}
                        disabled={isSubmitting}
                      />
                      <Checkbox
                        id="event-outdoor-wedding"
                        label="Casamento ao ar livre?"
                        checked={form.outdoorWedding}
                        onChange={(checked) => updateField('outdoorWedding', checked)}
                        disabled={isSubmitting}
                      />
                    </div>
                  </>
                )}

                <FormField label="Descrição" htmlFor="event-description" className="span-2">
                  <textarea
                    id="event-description"
                    value={form.description}
                    onChange={(event) => updateField('description', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>
              </div>
            </Card>

            <Card title="Clientes">
              <div className="event-form-grid">
                <EventCustomersSection
                  rows={form.customers ?? []}
                  customers={referenceData?.customers ?? []}
                  onChange={(rows) => updateField('customers', rows)}
                  onCreateNew={openCustomerQuickCreate}
                  disabled={isSubmitting}
                />
              </div>
            </Card>

            <Card title="Locais">
              <EventVenuesSection
                rows={form.venues ?? []}
                venues={referenceData?.venues ?? []}
                onChange={(rows) => updateField('venues', rows)}
                onCreateNew={openVenueQuickCreate}
                disabled={isSubmitting}
                error={errors.venues}
              />
            </Card>

            <Card title="Profissionais">
              <EventTeamSection
                rows={form.professionals ?? []}
                professionals={referenceData?.professionals ?? []}
                onChange={(rows) => updateField('professionals', rows)}
                onCreateNew={openProfessionalQuickCreate}
                disabled={isSubmitting}
                error={errors.professionals}
              />
            </Card>

            <Card title="Entrega e armazenamento">
              <div className="event-form-grid">
                <FormField label="HD" htmlFor="event-hd">
                  <Autocomplete
                    id="event-hd"
                    options={referenceData?.hds.map((hd) => ({ id: hd.id, label: hd.name })) ?? []}
                    value={form.hdId}
                    onChange={(hdId) => updateField('hdId', hdId)}
                    placeholder="Buscar HD..."
                    onCreateNew={() => setQuickCreateTarget('hd')}
                  />
                </FormField>

                <FormField label="Tamanho (GB)" htmlFor="event-size-gb">
                  <input
                    id="event-size-gb"
                    type="number"
                    min="0"
                    value={form.sizeGb}
                    onChange={(event) => updateField('sizeGb', event.target.value)}
                    disabled={isSubmitting}
                  />
                </FormField>

                <FormField label="Status de entrega" htmlFor="event-delivery-status">
                  <select
                    id="event-delivery-status"
                    value={form.deliveryStatus}
                    onChange={(event) => updateField('deliveryStatus', event.target.value as DeliveryStatus)}
                    disabled={isSubmitting}
                  >
                    {Object.entries(DELIVERY_STATUS_LABEL).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </FormField>
              </div>
            </Card>

            <div className="event-form-actions">
              <Button type="button" variant="ghost" onClick={handleCancel} disabled={isSubmitting}>
                Cancelar
              </Button>
              <Button type="submit" variant="primary" disabled={isSubmitting}>
                {isSubmitting ? 'Salvando...' : 'Salvar evento'}
              </Button>
            </div>
          </form>
        )}
      </div>

      {quickCreateTarget === 'eventType' && (
        <Modal title="Novo tipo de evento" onClose={closeQuickCreate}>
          <SimpleNameQuickCreateForm
            fieldId="qc-event-type-name"
            emptyError="Informe o nome do tipo."
            genericError="Não foi possível criar o tipo. Tente novamente."
            submitLabel="Salvar tipo"
            checkboxLabel="Usa campos de casamento (diurno e ao ar livre)"
            onCreate={(name, hasWeddingFields) => createEventType({ name, hasWeddingFields })}
            onCreated={handleEventTypeCreated}
            onCancel={closeQuickCreate}
          />
        </Modal>
      )}
      {quickCreateTarget === 'customer' && (
        <Modal title="Novo cliente" onClose={closeQuickCreate}>
          <CustomerQuickCreateForm onCreated={handleCustomerCreated} onCancel={closeQuickCreate} />
        </Modal>
      )}
      {quickCreateTarget === 'venue' && (
        <Modal title="Novo local" onClose={closeQuickCreate}>
          <EventVenueQuickCreateForm onCreated={handleVenueCreated} onCancel={closeQuickCreate} />
        </Modal>
      )}
      {quickCreateTarget === 'professional' && (
        <Modal title="Novo profissional" onClose={closeQuickCreate}>
          <ProfessionalQuickCreateForm onCreated={handleProfessionalCreated} onCancel={closeQuickCreate} />
        </Modal>
      )}
      {quickCreateTarget === 'hd' && (
        <Modal title="Novo HD" onClose={closeQuickCreate}>
          <HdQuickCreateForm onCreated={handleHdCreated} onCancel={closeQuickCreate} />
        </Modal>
      )}

      {isDeleteDialogOpen && savedEvent && (
        <ConfirmDialog
          title="Excluir evento"
          message={deleteConfirmationMessage(deletableEvent(savedEvent))}
          confirmLabel="Excluir"
          isConfirming={isDeleting}
          error={deleteError}
          onConfirm={handleConfirmDelete}
          onCancel={closeDeleteDialog}
        />
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
