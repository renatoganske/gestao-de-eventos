import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
  DELIVERY_STATUS_LABEL,
  DELIVERY_STATUS_TO_PILL,
  deleteEvent,
  searchEvents,
  type DeliveryStatus,
  type EventListItemDto,
  type EventSortKey,
  type PageDto,
  type SearchEventsParams,
} from '../api/events'
import { fetchCustomer, type CustomerDto } from '../api/customers'
import { fetchEventTypes, type EventTypeDto } from '../api/eventTypes'
import {
  fetchEventVenueOptions,
  fetchHdOptions,
  fetchProfessionalOptions,
  type OptionDto,
} from '../api/options'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { FormField } from '../components/FormField'
import { Pill } from '../components/Pill'
import { Table, type TableSort } from '../components/Table'
import { Toast, type ToastMessage } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { discardFormDraft } from '../hooks/useFormDraft'
import { useToast } from '../hooks/useToast'
import { customerNames } from '../utils/eventCustomers'
import { deletableEvent, deleteConfirmationMessage } from '../utils/eventDeletion'
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
  // Only id and name, for the filter selects (ADR-0029).
  venues: OptionDto[]
  professionals: OptionDto[]
  hds: OptionDto[]
  // Only the customer from the URL filter (if any); the full customer list is never loaded here.
  filterCustomer: CustomerDto | null
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível buscar os eventos. Tente novamente em instantes.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o evento. Tente novamente.'

// One page of the list; the server sorts and pages (ADR-0029).
const EVENTS_PAGE_SIZE = 50

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

// Only the shape is checked (ids are UUIDs, but fixtures and older links need not be): it rules out junk, not unknown ids.
const ID_PATTERN = /^[\w-]{1,64}$/
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

// Ties are broken by the server (code, then id), so the table needs no thenBy.
const DEFAULT_SORT: TableSort = { key: 'date', direction: 'desc' }
const SORTABLE_KEYS: EventSortKey[] = ['code', 'customer', 'venues', 'type', 'date', 'hd', 'status']

interface UrlView {
  freeText: string
  filters: FilterState
}

// Each reader returns '' for a missing or invalid value, so a hand-edited URL never breaks the page.
function readId(searchParams: URLSearchParams, name: string): string {
  const value = searchParams.get(name) ?? ''
  return ID_PATTERN.test(value) ? value : ''
}

function readDate(searchParams: URLSearchParams, name: string): string {
  const value = searchParams.get(name) ?? ''
  return DATE_PATTERN.test(value) ? value : ''
}

function readWeddingFlag(searchParams: URLSearchParams, name: string): WeddingFlagFilter {
  const value = searchParams.get(name)
  return value === 'true' || value === 'false' ? value : ''
}

// The applied view (filters + free text) that lives in the URL; other pages can pre-set it too
// (e.g. /eventos?customerId=...). The sort and the page are read separately.
function viewFromSearchParams(searchParams: URLSearchParams): UrlView {
  const deliveryStatus = searchParams.get('deliveryStatus') ?? ''
  const eventTypeId = readId(searchParams, 'eventTypeId')
  return {
    freeText: searchParams.get('q') ?? '',
    filters: {
      eventTypeId,
      venueId: readId(searchParams, 'venueId'),
      professionalId: readId(searchParams, 'professionalId'),
      customerId: readId(searchParams, 'customerId'),
      from: readDate(searchParams, 'from'),
      to: readDate(searchParams, 'to'),
      hdId: readId(searchParams, 'hdId'),
      deliveryStatus: deliveryStatus in DELIVERY_STATUS_LABEL ? (deliveryStatus as DeliveryStatus) : '',
      // The wedding flags only exist together with an event type.
      daytimeWedding: eventTypeId ? readWeddingFlag(searchParams, 'daytimeWedding') : '',
      outdoorWedding: eventTypeId ? readWeddingFlag(searchParams, 'outdoorWedding') : '',
    },
  }
}

function sortFromSearchParams(searchParams: URLSearchParams): TableSort {
  const [key, direction] = (searchParams.get('sort') ?? '').split(':')
  if ((SORTABLE_KEYS as string[]).includes(key) && (direction === 'asc' || direction === 'desc')) {
    return { key, direction }
  }
  return DEFAULT_SORT
}

// 1-based in the URL ("?page=2" is the second page), 0-based for the API; anything else is the first page.
function pageFromSearchParams(searchParams: URLSearchParams): number {
  const value = Number(searchParams.get('page'))
  return Number.isInteger(value) && value > 1 ? value - 1 : 0
}

// Writes only what differs from "no filter" / the default sort / the first page, so the default view has a clean URL.
function toUrlParams(freeText: string, filters: FilterState, sort: TableSort, page = 0): URLSearchParams {
  const params = new URLSearchParams()
  if (freeText.trim()) {
    params.set('q', freeText.trim())
  }
  for (const [name, value] of Object.entries(filters)) {
    if (value) {
      params.set(name, value)
    }
  }
  if (sort.key !== DEFAULT_SORT.key || sort.direction !== DEFAULT_SORT.direction) {
    params.set('sort', `${sort.key}:${sort.direction}`)
  }
  if (page > 0) {
    params.set('page', String(page + 1))
  }
  return params
}

// Filters and free text only: changing the sort or the page keeps whatever is being typed in the panel.
function viewKey(searchParams: URLSearchParams): string {
  const copy = new URLSearchParams(searchParams)
  copy.delete('sort')
  copy.delete('page')
  return copy.toString()
}

// Fetches just the customer named in the URL filter (works even if they have no events).
// A missing customer must not break the page, so any failure falls back to null.
async function loadFilterCustomer(customerId: string): Promise<CustomerDto | null> {
  if (!customerId) {
    return null
  }
  try {
    return await fetchCustomer(customerId)
  } catch {
    return null
  }
}

// One request for the page the URL describes: the free text goes as "q", which the server matches
// against the event code OR any customer name (ADR-0029).
function performSearch(searchParams: URLSearchParams): Promise<PageDto<EventListItemDto>> {
  const view = viewFromSearchParams(searchParams)
  const sort = sortFromSearchParams(searchParams)
  return searchEvents({
    ...toSearchParams(view.filters),
    q: view.freeText.trim() || undefined,
    page: pageFromSearchParams(searchParams),
    size: EVENTS_PAGE_SIZE,
    sort: { key: sort.key as EventSortKey, direction: sort.direction },
  })
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

  const [searchParams, setSearchParams] = useSearchParams()
  // The URL holds the applied view (filters, free text, sort, page); typing in the panel only changes the
  // draft below, and "Aplicar filtros" copies it to the URL. The first read happens on mount.
  const [initialView] = useState(() => viewFromSearchParams(searchParams))
  const [freeText, setFreeText] = useState(initialView.freeText)
  const [filters, setFilters] = useState<FilterState>(initialView.filters)
  // Key of the URL view the draft was last synced with, to tell our own writes from navigation (back/forward).
  const appliedKey = useRef(viewKey(searchParams))
  // Only the answer to the latest search is shown, however the responses arrive.
  const latestSearch = useRef(0)
  const sort = sortFromSearchParams(searchParams)
  const [isFilterPanelOpen, setIsFilterPanelOpen] = useState(true)

  const [referenceData, setReferenceData] = useState<ReferenceData | null>(null)
  const [events, setEvents] = useState<PageDto<EventListItemDto> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [eventToDelete, setEventToDelete] = useState<EventListItemDto | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  // The filter selects only need id and name; the events come from the search below.
  useEffect(() => {
    let cancelled = false
    Promise.all([
      fetchEventTypes(),
      fetchEventVenueOptions(),
      fetchProfessionalOptions(),
      fetchHdOptions(),
      loadFilterCustomer(initialView.filters.customerId),
    ])
      .then(([eventTypes, venues, professionals, hds, filterCustomer]) => {
        if (!cancelled) {
          setReferenceData({ eventTypes, venues, professionals, hds, filterCustomer })
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
        }
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function runSearch(params: URLSearchParams) {
    const searchId = ++latestSearch.current
    setIsLoading(true)
    setError(null)
    try {
      const result = await performSearch(params)
      if (searchId === latestSearch.current) {
        setEvents(result)
      }
    } catch (err) {
      if (searchId === latestSearch.current) {
        setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
      }
    } finally {
      if (searchId === latestSearch.current) {
        setIsLoading(false)
      }
    }
  }

  // Every URL change (filters, free text, sort, page) is a new search. Back/forward, or any change we
  // did not make, also restores the draft in the panel to the view the URL describes.
  useEffect(() => {
    const key = viewKey(searchParams)
    if (key !== appliedKey.current) {
      appliedKey.current = key
      const view = viewFromSearchParams(searchParams)
      setFreeText(view.freeText)
      setFilters(view.filters)
      if (view.filters.customerId !== referenceData?.filterCustomer?.id) {
        loadFilterCustomer(view.filters.customerId).then((filterCustomer) =>
          setReferenceData((current) => (current ? { ...current, filterCustomer } : current)),
        )
      }
    }
    runSearch(searchParams)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  // Writing the URL triggers the search; the same URL again (re-applying unchanged filters) does not
  // change it, so that case searches directly.
  function applyUrl(next: URLSearchParams) {
    if (next.toString() === searchParams.toString()) {
      runSearch(next)
    } else {
      setSearchParams(next)
    }
  }

  // A new filter or text starts again from the first page.
  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const next = toUrlParams(freeText, filters, sort)
    appliedKey.current = viewKey(next)
    applyUrl(next)
  }

  function handleClearFilters() {
    setFreeText('')
    setFilters(EMPTY_FILTERS)
    appliedKey.current = ''
    // Empties the whole query (filters, free text, sort and page), so a reload does not bring them back.
    applyUrl(new URLSearchParams())
  }

  // The sort is saved over the applied view in the URL, not over filters still being typed; a new
  // order starts again from the first page.
  function handleSortChange(next: TableSort) {
    const applied = viewFromSearchParams(searchParams)
    setSearchParams(toUrlParams(applied.freeText, applied.filters, next))
  }

  function handlePageChange(page: number) {
    const applied = viewFromSearchParams(searchParams)
    setSearchParams(toUrlParams(applied.freeText, applied.filters, sort, page))
  }

  function closeDeleteDialog() {
    setEventToDelete(null)
    setDeleteError(null)
  }

  async function handleConfirmDelete(event: EventListItemDto) {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteEvent(event.id)
      discardFormDraft(`event-edit:${event.id}`)
      closeDeleteDialog()
      setToast({ kind: 'success', text: 'Evento excluído com sucesso.' })
      // Reload the page, so it fills up again from the next one and the total stays right; the last
      // event of a page that is not the first one sends the user back a page instead of to an empty one.
      if (events && events.content.length === 1 && events.page > 0) {
        handlePageChange(events.page - 1)
      } else {
        runSearch(searchParams)
      }
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
    } finally {
      setIsDeleting(false)
    }
  }

  function openEdit(event: EventListItemDto) {
    // Same as "Editar" on the detail page: a stale draft of this event must not replace what is saved.
    discardFormDraft(`event-edit:${event.id}`)
    navigate(`/eventos/${event.id}/editar`)
  }

  function updateFilter<K extends keyof FilterState>(key: K, value: FilterState[K]) {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  const activeCustomer = referenceData?.filterCustomer?.id === filters.customerId ? referenceData.filterCustomer : undefined
  const activeProfessional = referenceData?.professionals.find((professional) => professional.id === filters.professionalId)
  const activeVenue = referenceData?.venues.find((venue) => venue.id === filters.venueId)

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
                {events ? `${events.totalElements} evento(s) encontrado(s)` : ''}
              </span>
              <Button type="submit" variant="primary">
                Aplicar filtros
              </Button>
            </div>
          </div>
        </form>

        {(filters.customerId || filters.professionalId || filters.venueId) && (
          <p className="result-count">
            {[
              filters.customerId && `Cliente: ${activeCustomer?.name ?? 'não encontrado'}`,
              filters.professionalId && `Profissional: ${activeProfessional?.name ?? '...'}`,
              filters.venueId && `Local: ${activeVenue?.name ?? '...'}`,
            ]
              .filter(Boolean)
              .map((label, index) => (
                <span key={label}>
                  {index > 0 && ' · '}
                  <span>{label}</span>
                </span>
              ))}
          </p>
        )}

        <Card>
          {isLoading && <p className="events-empty">Carregando...</p>}

          {!isLoading && error && (
            <p role="alert" className="events-error">
              {error}
            </p>
          )}

          {!isLoading && !error && events && events.content.length === 0 && (
            <p className="events-empty">Nenhum evento encontrado com esses filtros.</p>
          )}

          {!isLoading && !error && events && events.content.length > 0 && (
            <Table
              columns={[
                {
                  key: 'code',
                  header: 'Código',
                  render: (row) => <span className="mono">{row.eventCode}</span>,
                  sortValue: (row) => row.eventCode,
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
                { key: 'hd', header: 'HD', render: (row) => row.hdName ?? '—', sortValue: (row) => row.hdName },
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
                {
                  key: 'actions',
                  header: 'Ações',
                  // Inside a clickable row: stop the click so Editar/Excluir do not also open the detail.
                  render: (row) => (
                    <div className="table-actions" onClick={(event) => event.stopPropagation()}>
                      <Button type="button" variant="ghost" onClick={() => openEdit(row)}>
                        Editar
                      </Button>
                      <Button type="button" variant="ghost" onClick={() => setEventToDelete(row)}>
                        Excluir
                      </Button>
                    </div>
                  ),
                },
              ]}
              rows={events.content}
              rowKey={(row) => row.id}
              sort={sort}
              onSortChange={handleSortChange}
              serverSorted
              onRowClick={(row) => navigate(`/eventos/${row.id}`)}
            />
          )}

          {!error && events && events.totalPages > 1 && (
            <nav className="pagination" aria-label="Paginação">
              <Button
                type="button"
                variant="ghost"
                onClick={() => handlePageChange(events.page - 1)}
                disabled={isLoading || events.page === 0}
              >
                ← Anterior
              </Button>
              <span className="pagination-status">
                Página {events.page + 1} de {events.totalPages}
              </span>
              <Button
                type="button"
                variant="ghost"
                onClick={() => handlePageChange(events.page + 1)}
                disabled={isLoading || events.page + 1 >= events.totalPages}
              >
                Próxima →
              </Button>
            </nav>
          )}
        </Card>
      </div>

      {eventToDelete && (
        <ConfirmDialog
          title="Excluir evento"
          message={deleteConfirmationMessage(deletableEvent(eventToDelete))}
          confirmLabel="Excluir"
          isConfirming={isDeleting}
          error={deleteError}
          onConfirm={() => handleConfirmDelete(eventToDelete)}
          onCancel={closeDeleteDialog}
        />
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
