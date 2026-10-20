import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
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
import { Toast, type ToastMessage } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { discardFormDraft } from '../hooks/useFormDraft'
import { useToast } from '../hooks/useToast'
import { codeNumber } from '../utils/sort'
import { customerNames } from '../utils/eventCustomers'
import { venueLabels } from '../utils/eventVenues'
import './EventsPage.css'

// '' = do not filter; 'true' / 'false' = only events where the flag is explicitly that value.
type WeddingFlagFilter = '' | 'true' | 'false'

interface FilterState {
  eventTypeId: string
  venueId: string
  professionalId: string
  customerId: string
  from: string
  to: string
  hdId: string
  deliveryStatus: '' | DeliveryStatus
  daytimeWedding: WeddingFlagFilter
  outdoorWedding: WeddingFlagFilter
}

const EMPTY_FILTERS: FilterState = {
  eventTypeId: '',
  venueId: '',
  professionalId: '',
  customerId: '',
  from: '',
  to: '',
  hdId: '',
  deliveryStatus: '',
  daytimeWedding: '',
  outdoorWedding: '',
}

function toBooleanParam(value: WeddingFlagFilter): boolean | undefined {
  return value === '' ? undefined : value === 'true'
}

interface ReferenceData {
  eventTypes: EventTypeDto[]
  venues: EventVenueDto[]
  professionals: ProfessionalDto[]
  customers: CustomerDto[]
  hds: HdDto[]
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível buscar os eventos. Tente novamente em instantes.'

function toSearchParams(filters: FilterState): SearchEventsParams {
  return {
    eventTypeId: filters.eventTypeId || undefined,
    venueId: filters.venueId || undefined,
    professionalId: filters.professionalId || undefined,
    customerId: filters.customerId || undefined,
    from: filters.from || undefined,
    to: filters.to || undefined,
    hdId: filters.hdId || undefined,
    deliveryStatus: filters.deliveryStatus || undefined,
    daytimeWedding: toBooleanParam(filters.daytimeWedding),
    outdoorWedding: toBooleanParam(filters.outdoorWedding),
  }
}

// Filters that other pages can pre-set through the URL (e.g. /eventos?customerId=...).
function filtersFromSearchParams(searchParams: URLSearchParams): FilterState {
  return {
    ...EMPTY_FILTERS,
    customerId: searchParams.get('customerId') ?? '',
    professionalId: searchParams.get('professionalId') ?? '',
    hdId: searchParams.get('hdId') ?? '',
    venueId: searchParams.get('venueId') ?? '',
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
  const location = useLocation()
  const [toast, setToast] = useToast()

  // Pages that send the user here after a write (e.g. deleting an event) hand over a toast as router state.
  // The state is cleared right away so a reload does not show it again.
  useEffect(() => {
    const incoming = (location.state as { toast?: ToastMessage } | null)?.toast
    if (incoming) {
      setToast(incoming)
      navigate(location.pathname, { replace: true, state: null })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const [freeText, setFreeText] = useState('')
  const [searchParams, setSearchParams] = useSearchParams()
  // Read once, on mount: typed filters are not synced back to the URL.
  const [initialFilters] = useState(() => filtersFromSearchParams(searchParams))
  const [filters, setFilters] = useState<FilterState>(initialFilters)
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
        const [eventTypes, venues, professionals, customers, hds] = await Promise.all([
          fetchEventTypes(),
          fetchEventVenues(),
          fetchProfessionals(),
          fetchCustomers(),
          fetchHds(),
        ])
        const initialEvents = await performSearch('', initialFilters)
        if (cancelled) {
          return
        }
        setReferenceData({ eventTypes, venues, professionals, customers, hds })
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
    // Drops the filters that came from the URL too, so a reload does not bring them back.
    setSearchParams({}, { replace: true })
  }

  function updateFilter<K extends keyof FilterState>(key: K, value: FilterState[K]) {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  const activeCustomer = referenceData?.customers.find((customer) => customer.id === filters.customerId)
  const activeProfessional = referenceData?.professionals.find((professional) => professional.id === filters.professionalId)

  const selectedEventType = referenceData?.eventTypes.find((type) => type.id === filters.eventTypeId)
  const isWeddingSelected = selectedEventType?.hasWeddingFields === true

  // The wedding flags only make sense for weddings. Changing to another type clears them, so no
  // hidden filter stays active and silently empties the results.
  function handleEventTypeChange(eventTypeId: string) {
    const nextType = referenceData?.eventTypes.find((type) => type.id === eventTypeId)
    const keepsWeddingFlags = nextType?.hasWeddingFields === true
    setFilters((current) => ({
      ...current,
      eventTypeId,
      daytimeWedding: keepsWeddingFlags ? current.daytimeWedding : '',
      outdoorWedding: keepsWeddingFlags ? current.outdoorWedding : '',
    }))
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
                    onChange={(event) => handleEventTypeChange(event.target.value)}
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

                {isWeddingSelected && (
                  <>
                    <FormField label="Casamento diurno" htmlFor="filter-daytime-wedding">
                      <select
                        id="filter-daytime-wedding"
                        value={filters.daytimeWedding}
                        onChange={(event) => updateFilter('daytimeWedding', event.target.value as WeddingFlagFilter)}
                      >
                        <option value="">Todos</option>
                        <option value="true">Sim</option>
                        <option value="false">Não</option>
                      </select>
                    </FormField>

                    <FormField label="Casamento ao ar livre" htmlFor="filter-outdoor-wedding">
                      <select
                        id="filter-outdoor-wedding"
                        value={filters.outdoorWedding}
                        onChange={(event) => updateFilter('outdoorWedding', event.target.value as WeddingFlagFilter)}
                      >
                        <option value="">Todos</option>
                        <option value="true">Sim</option>
                        <option value="false">Não</option>
                      </select>
                    </FormField>
                  </>
                )}

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

        {(filters.customerId || filters.professionalId) && (
          <p className="result-count">
            {filters.customerId && `Cliente: ${activeCustomer?.name ?? '...'}`}
            {filters.customerId && filters.professionalId && ' · '}
            {filters.professionalId && `Profissional: ${activeProfessional?.name ?? '...'}`}
          </p>
        )}

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
                {
                  key: 'code',
                  header: 'Código',
                  render: (row) => <span className="mono">{row.eventCode}</span>,
                  // Numeric suffix, so RG2 comes before RG10.
                  sortValue: (row) => codeNumber(row.eventCode) ?? row.eventCode,
                },
                { key: 'customer', header: 'Clientes', render: (row) => customerNames(row.customers) || '—',
                  sortValue: (row) => customerNames(row.customers) || null,
                },
                { key: 'venues', header: 'Locais', render: (row) => venueLabels(row.venues) || '—',
                  sortValue: (row) => venueLabels(row.venues) || null,
                },
                { key: 'type', header: 'Tipo', render: (row) => row.type?.name ?? '—', sortValue: (row) => row.type?.name },
                { key: 'date', header: 'Data', render: (row) => <span className="mono">{row.eventDate}</span>,
                  sortValue: (row) => row.eventDate,
                },
                {
                  key: 'status',
                  header: 'Status',
                  sortValue: (row) => DELIVERY_STATUS_LABEL[row.deliveryStatus],
                  render: (row) => (
                    <Pill status={DELIVERY_STATUS_TO_PILL[row.deliveryStatus]}>
                      {DELIVERY_STATUS_LABEL[row.deliveryStatus]}
                    </Pill>
                  ),
                },
              ]}
              rows={events}
              rowKey={(row) => row.id}
              defaultSort={{ key: 'date', direction: 'asc', thenBy: 'code' }}
              onRowClick={(row) => {
                discardFormDraft(`event-edit:${row.id}`)
                navigate(`/eventos/${row.id}`)
              }}
            />
          )}
        </Card>
      </div>

      {toast && <Toast toast={toast} />}
    </section>
  )
}
