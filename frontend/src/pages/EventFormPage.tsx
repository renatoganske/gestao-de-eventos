import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchCustomers, type CustomerDto } from '../api/customers'
import {
  DELIVERY_STATUS_LABEL,
  createEvent,
  fetchEventById,
  updateEvent,
  type CreateEventDto,
  type DeliveryStatus,
  type EventDto,
} from '../api/events'
import { fetchEventTypes, type EventTypeDto } from '../api/eventTypes'
import { fetchEventVenues, type EventVenueDto } from '../api/eventVenues'
import { fetchHds, type HdDto } from '../api/hds'
import { Autocomplete } from '../components/Autocomplete'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { CustomerQuickCreateForm } from '../components/CustomerQuickCreateForm'
import { EventVenueQuickCreateForm } from '../components/EventVenueQuickCreateForm'
import { FormField } from '../components/FormField'
import { HdQuickCreateForm } from '../components/HdQuickCreateForm'
import { Modal } from '../components/Modal'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useFormDraft } from '../hooks/useFormDraft'
import { useToast } from '../hooks/useToast'
import './EventFormPage.css'

const WEDDING_TYPE_NAME = 'WEDDING'
const GENERIC_LOAD_ERROR = 'Não foi possível carregar os dados do formulário. Tente novamente em instantes.'
const GENERIC_SUBMIT_ERROR = 'Não foi possível salvar o evento. Tente novamente.'

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
  eventVenueId: string
  customerId: string
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
  eventVenueId: '',
  customerId: '',
}

type FormErrors = Partial<Record<'eventCode' | 'eventTypeId' | 'name' | 'eventDate', string>>

interface ReferenceData {
  eventTypes: EventTypeDto[]
  venues: EventVenueDto[]
  hds: HdDto[]
  customers: CustomerDto[]
}

type QuickCreateTarget = 'customer' | 'venue' | 'hd' | null

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
    eventVenueId: event.eventVenueId ?? '',
    customerId: event.customerId ?? '',
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
    eventVenueId: form.eventVenueId || null,
    customerId: form.customerId || null,
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

  useEffect(() => {
    let cancelled = false

    async function init() {
      setIsLoading(true)
      setLoadError(null)
      try {
        const [eventTypes, venues, hds, customers] = await Promise.all([
          fetchEventTypes(),
          fetchEventVenues(),
          fetchHds(),
          fetchCustomers(),
        ])
        const loadedEvent = isEditMode && id ? await fetchEventById(id) : null
        if (cancelled) {
          return
        }
        setReferenceData({ eventTypes, venues, hds, customers })
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
  const isWedding = selectedType?.name === WEDDING_TYPE_NAME

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
        const updated = await updateEvent(id, dto)
        clearDraft()
        setForm(eventToFormState(updated))
        setToast({ kind: 'success', text: 'Evento atualizado com sucesso.' })
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

  function handleCancel() {
    clearDraft()
    navigate('/eventos')
  }

  function closeQuickCreate() {
    setQuickCreateTarget(null)
  }

  function handleCustomerCreated(customer: CustomerDto) {
    setReferenceData((current) => (current ? { ...current, customers: [...current.customers, customer] } : current))
    updateField('customerId', customer.id)
    setQuickCreateTarget(null)
    setToast({ kind: 'success', text: 'Cliente criado e selecionado.' })
  }

  function handleVenueCreated(venue: EventVenueDto) {
    setReferenceData((current) => (current ? { ...current, venues: [...current.venues, venue] } : current))
    updateField('eventVenueId', venue.id)
    setQuickCreateTarget(null)
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
          <Link to="/eventos" className="btn">
            Voltar para eventos
          </Link>
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
                    <FormField label="Casamento diurno?" htmlFor="event-daytime-wedding">
                      <input
                        id="event-daytime-wedding"
                        type="checkbox"
                        checked={form.daytimeWedding}
                        onChange={(event) => updateField('daytimeWedding', event.target.checked)}
                        disabled={isSubmitting}
                      />
                    </FormField>

                    <FormField label="Casamento ao ar livre?" htmlFor="event-outdoor-wedding">
                      <input
                        id="event-outdoor-wedding"
                        type="checkbox"
                        checked={form.outdoorWedding}
                        onChange={(event) => updateField('outdoorWedding', event.target.checked)}
                        disabled={isSubmitting}
                      />
                    </FormField>
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

            <Card title="Cliente e local">
              <div className="event-form-grid">
                <FormField label="Cliente" htmlFor="event-customer">
                  <Autocomplete
                    id="event-customer"
                    options={referenceData?.customers.map((customer) => ({ id: customer.id, label: customer.name })) ?? []}
                    value={form.customerId}
                    onChange={(customerId) => updateField('customerId', customerId)}
                    placeholder="Buscar cliente..."
                    onCreateNew={() => setQuickCreateTarget('customer')}
                  />
                </FormField>

                <FormField label="Local do evento" htmlFor="event-venue">
                  <Autocomplete
                    id="event-venue"
                    options={referenceData?.venues.map((venue) => ({ id: venue.id, label: venue.name })) ?? []}
                    value={form.eventVenueId}
                    onChange={(venueId) => updateField('eventVenueId', venueId)}
                    placeholder="Buscar local..."
                    onCreateNew={() => setQuickCreateTarget('venue')}
                  />
                </FormField>
              </div>
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
      {quickCreateTarget === 'hd' && (
        <Modal title="Novo HD" onClose={closeQuickCreate}>
          <HdQuickCreateForm onCreated={handleHdCreated} onCancel={closeQuickCreate} />
        </Modal>
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
