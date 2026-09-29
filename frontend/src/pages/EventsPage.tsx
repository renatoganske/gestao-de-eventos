import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
  DELIVERY_STATUS_LABEL,
  DELIVERY_STATUS_TO_PILL,
  searchEvents,
  type DeliveryStatus,
  type EventDto,
  type SearchEventsParams,
} from '../api/events'
import { fetchCustomers, type CustomerDto } from '../api/customers'
import { fetchEventTypes, type EventTypeDto } from '../api/eventTypes'
import { fetchEventVenues, type EventVenueDto } from '../api/eventVenues'
import { fetchHds, type HdDto } from '../api/hds'
import { fetchProfessionals, type ProfessionalDto } from '../api/professionals'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { FormField } from '../components/FormField'
import { Pill } from '../components/Pill'
import { Table } from '../components/Table'
import { TopBar } from '../components/TopBar'
import { discardFormDraft } from '../hooks/useFormDraft'
import './EventsPage.css'

interface FilterState {
  eventTypeId: string
  venueId: string
  professionalId: string
  from: string
  to: string
  hdId: string
  deliveryStatus: '' | DeliveryStatus
}

const EMPTY_FILTERS: FilterState = {
  eventTypeId: '',
  venueId: '',
  professionalId: '',
  from: '',
  to: '',
  hdId: '',
  deliveryStatus: '',
}

interface ReferenceData {
  eventTypes: EventTypeDto[]
  venues: EventVenueDto[]
  professionals: ProfessionalDto[]
  hds: HdDto[]
  customers: CustomerDto[]
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível buscar os eventos. Tente novamente em instantes.'

function toSearchParams(filters: FilterState): SearchEventsParams {
  return {
    eventTypeId: filters.eventTypeId || undefined,
    venueId: filters.venueId || undefined,
    professionalId: filters.professionalId || undefined,
    from: filters.from || undefined,
    to: filters.to || undefined,
    hdId: filters.hdId || undefined,
    deliveryStatus: filters.deliveryStatus || undefined,
  }
}

async function performSearch(freeText: string, filters: FilterState): Promise<EventDto[]> {
  const baseParams = toSearchParams(filters)
  const trimmedText = freeText.trim()

  if (!trimmedText) {
    return searchEvents(baseParams)
  }

  const [byCustomerName, byEventCode] = await Promise.all([
    searchEvents({ ...baseParams, customerName: trimmedText }),
    searchEvents({ ...baseParams, eventCode: trimmedText }),
  ])

  const merged = new Map<string, EventDto>()
  for (const event of [...byCustomerName, ...byEventCode]) {
    merged.set(event.id, event)
  }
  return Array.from(merged.values())
}

export function EventsPage() {
  const navigate = useNavigate()

  const [freeText, setFreeText] = useState('')
  const [filters, setFilters] = useState<FilterState>(EMPTY_FILTERS)
  const [isFilterPanelOpen, setIsFilterPanelOpen] = useState(true)

  const [referenceData, setReferenceData] = useState<ReferenceData | null>(null)
  const [events, setEvents] = useState<EventDto[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function init() {
      setIsLoading(true)
      setError(null)
      try {
        const [eventTypes, venues, professionals, hds, customers] = await Promise.all([
          fetchEventTypes(),
          fetchEventVenues(),
          fetchProfessionals(),
          fetchHds(),
          fetchCustomers(),
        ])
        const initialEvents = await performSearch('', EMPTY_FILTERS)
        if (cancelled) {
          return
        }
        setReferenceData({ eventTypes, venues, professionals, hds, customers })
        setEvents(initialEvents)
      } catch (err) {
        if (cancelled) {
          return
        }
        setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
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
  }, [])

  async function runSearch(text: string, currentFilters: FilterState) {
    setIsLoading(true)
    setError(null)
    try {
      const result = await performSearch(text, currentFilters)
      setEvents(result)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
    } finally {
      setIsLoading(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    runSearch(freeText, filters)
  }

  function handleClearFilters() {
    setFreeText('')
    setFilters(EMPTY_FILTERS)
    runSearch('', EMPTY_FILTERS)
  }

  function updateFilter<K extends keyof FilterState>(key: K, value: FilterState[K]) {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  function customerName(customerId: string | null): string {
    if (!customerId) {
      return '—'
    }
    return referenceData?.customers.find((customer) => customer.id === customerId)?.name ?? '—'
  }

  return (
    <section>
      <TopBar
        title="Eventos"
        action={
          <Link to="/eventos/novo" className="btn btn-primary" onClick={() => discardFormDraft('event-create')}>
            + Novo evento
          </Link>
        }
      />
      <div className="events-content">
        <form onSubmit={handleSubmit}>
          <div className="search-bar">
            <input
              type="text"
              value={freeText}
              onChange={(event) => setFreeText(event.target.value)}
              placeholder='Buscar por nome do cliente ou código do evento — ex: "Maria" ou "EVT-018"'
              aria-label="Buscar por nome do cliente ou código do evento"
            />
          </div>

          <div className="filter-panel">
            <div className="filter-panel-head">
              <button
                type="button"
                className="filter-toggle"
                onClick={() => setIsFilterPanelOpen((open) => !open)}
                aria-expanded={isFilterPanelOpen}
              >
                Filtros
              </button>
              <Button type="button" variant="ghost" onClick={handleClearFilters}>
                Limpar filtros
              </Button>
            </div>

            {isFilterPanelOpen && (
              <div className="filter-grid">
                <FormField label="Tipo" htmlFor="filter-event-type">
                  <select
                    id="filter-event-type"
                    value={filters.eventTypeId}
                    onChange={(event) => updateFilter('eventTypeId', event.target.value)}
                  >
                    <option value="">Todos</option>
                    {referenceData?.eventTypes.map((type) => (
                      <option key={type.id} value={type.id}>
                        {type.name}
                      </option>
                    ))}
                  </select>
                </FormField>

                <FormField label="Local" htmlFor="filter-venue">
                  <select
                    id="filter-venue"
                    value={filters.venueId}
                    onChange={(event) => updateFilter('venueId', event.target.value)}
                  >
                    <option value="">Todos</option>
                    {referenceData?.venues.map((venue) => (
                      <option key={venue.id} value={venue.id}>
                        {venue.name}
                      </option>
                    ))}
                  </select>
                </FormField>

                <FormField label="Profissional" htmlFor="filter-professional">
                  <select
                    id="filter-professional"
                    value={filters.professionalId}
                    onChange={(event) => updateFilter('professionalId', event.target.value)}
                  >
                    <option value="">Todos</option>
                    {referenceData?.professionals.map((professional) => (
                      <option key={professional.id} value={professional.id}>
                        {professional.name}
                      </option>
                    ))}
                  </select>
                </FormField>

                <FormField label="HD" htmlFor="filter-hd">
                  <select id="filter-hd" value={filters.hdId} onChange={(event) => updateFilter('hdId', event.target.value)}>
                    <option value="">Todos</option>
                    {referenceData?.hds.map((hd) => (
                      <option key={hd.id} value={hd.id}>
                        {hd.name}
                      </option>
                    ))}
                  </select>
                </FormField>

                <FormField label="Status de entrega" htmlFor="filter-delivery-status">
                  <select
                    id="filter-delivery-status"
                    value={filters.deliveryStatus}
                    onChange={(event) => updateFilter('deliveryStatus', event.target.value as '' | DeliveryStatus)}
                  >
                    <option value="">Todos</option>
                    {Object.entries(DELIVERY_STATUS_LABEL).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </FormField>

                <FormField label="Data inicial" htmlFor="filter-from">
                  <input
                    id="filter-from"
                    type="date"
                    value={filters.from}
                    onChange={(event) => updateFilter('from', event.target.value)}
                  />
                </FormField>

                <FormField label="Data final" htmlFor="filter-to">
                  <input id="filter-to" type="date" value={filters.to} onChange={(event) => updateFilter('to', event.target.value)} />
                </FormField>
              </div>
            )}

            <div className="filter-footer">
              <span className="result-count">
                {events ? `${events.length} evento(s) encontrado(s)` : ''}
              </span>
              <Button type="submit" variant="primary">
                Aplicar filtros
              </Button>
            </div>
          </div>
        </form>

        <Card>
          {isLoading && <p className="events-empty">Carregando...</p>}

          {!isLoading && error && (
            <p role="alert" className="events-error">
              {error}
            </p>
          )}

          {!isLoading && !error && events && events.length === 0 && (
            <p className="events-empty">Nenhum evento encontrado com esses filtros.</p>
          )}

          {!isLoading && !error && events && events.length > 0 && (
            <Table
              columns={[
                { key: 'code', header: 'Código', render: (row) => <span className="mono">{row.eventCode}</span> },
                { key: 'customer', header: 'Cliente', render: (row) => customerName(row.customerId) },
                { key: 'type', header: 'Tipo', render: (row) => row.type?.name ?? '—' },
                { key: 'date', header: 'Data', render: (row) => <span className="mono">{row.eventDate}</span> },
                {
                  key: 'status',
                  header: 'Status',
                  render: (row) => (
                    <Pill status={DELIVERY_STATUS_TO_PILL[row.deliveryStatus]}>
                      {DELIVERY_STATUS_LABEL[row.deliveryStatus]}
                    </Pill>
                  ),
                },
              ]}
              rows={events}
              rowKey={(row) => row.id}
              onRowClick={(row) => {
                discardFormDraft(`event-edit:${row.id}`)
                navigate(`/eventos/${row.id}`)
              }}
            />
          )}
        </Card>
      </div>
    </section>
  )
}
