import { useEffect, useState, type ReactNode } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { DELIVERY_STATUS_LABEL, DELIVERY_STATUS_TO_PILL, deleteEvent, fetchEventById, type EventDto } from '../api/events'
import { fetchHd } from '../api/hds'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Pill } from '../components/Pill'
import { Toast, type ToastMessage } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { discardFormDraft } from '../hooks/useFormDraft'
import { useToast } from '../hooks/useToast'
import { deletableEvent, deleteConfirmationMessage, formatIsoDate } from '../utils/eventDeletion'
import './EventDetailPage.css'

const GENERIC_LOAD_ERROR = 'Não foi possível carregar o evento. Tente novamente em instantes.'
const NOT_FOUND_ERROR = 'Evento não encontrado. Ele pode ter sido excluído.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o evento. Tente novamente.'

const currencyFormat = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

function yesNo(value: boolean): string {
  return value ? 'Sim' : 'Não'
}

function DetailItem({ label, children, wide = false }: { label: string; children: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? 'event-detail-item span-2' : 'event-detail-item'}>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  )
}

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const [toast, setToast] = useToast()

  const [event, setEvent] = useState<EventDto | null>(null)
  // Only the HD of this event is fetched, to show its name instead of the raw id.
  const [hdName, setHdName] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  // The edit form sends the user back here after saving, with the confirmation as router state.
  useEffect(() => {
    const incoming = (location.state as { toast?: ToastMessage } | null)?.toast
    if (incoming) {
      setToast(incoming)
      navigate(location.pathname, { replace: true, state: null })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!id) {
      return
    }
    let cancelled = false

    async function load(eventId: string) {
      setIsLoading(true)
      setLoadError(null)
      try {
        const loaded = await fetchEventById(eventId)
        // A missing HD name must not hide the event, so a failure here just leaves the name out.
        const loadedHdName = loaded.hdId ? await fetchHd(loaded.hdId).then((hd) => hd.name, () => null) : null
        if (cancelled) {
          return
        }
        setEvent(loaded)
        setHdName(loadedHdName)
      } catch (err) {
        if (cancelled) {
          return
        }
        if (err instanceof ApiError && err.status === 404) {
          setLoadError(NOT_FOUND_ERROR)
        } else {
          setLoadError(err instanceof ApiError ? err.message : GENERIC_LOAD_ERROR)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    load(id)
    return () => {
      cancelled = true
    }
  }, [id])

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
      discardFormDraft(`event-edit:${id}`)
      // This page unmounts on navigation, so the confirmation travels to the list as router state.
      navigate('/eventos', { state: { toast: { kind: 'success', text: 'Evento excluído com sucesso.' } } })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
      setIsDeleting(false)
    }
  }

  return (
    <section>
      <TopBar
        title={event ? event.name : 'Evento'}
        action={
          <div className="event-detail-topbar-actions">
            {event && (
              <>
                <Link
                  to={`/eventos/${event.id}/editar`}
                  className="btn btn-primary"
                  onClick={() => discardFormDraft(`event-edit:${event.id}`)}
                >
                  Editar
                </Link>
                <Button type="button" variant="ghost" onClick={() => setIsDeleteDialogOpen(true)}>
                  Excluir
                </Button>
              </>
            )}
            <Link to="/eventos" className="btn">
              Voltar para eventos
            </Link>
          </div>
        }
      />
      <div className="event-detail-content">
        {isLoading && <p className="events-empty">Carregando...</p>}

        {!isLoading && loadError && (
          <p role="alert" className="events-error">
            {loadError}
          </p>
        )}

        {!isLoading && !loadError && event && (
          <>
            <Card title="Informações básicas">
              <dl className="event-detail-grid">
                <DetailItem label="Código">
                  <span className="mono">{event.eventCode}</span>
                </DetailItem>
                <DetailItem label="Tipo">{event.type?.name ?? '—'}</DetailItem>
                <DetailItem label="Nome do evento" wide>
                  {event.name}
                </DetailItem>
                <DetailItem label="Data">{event.eventDate ? formatIsoDate(event.eventDate) : '—'}</DetailItem>
                <DetailItem label="Nº de convidados">{event.guestCount ?? '—'}</DetailItem>
                <DetailItem label="Valor">{event.amount != null ? currencyFormat.format(event.amount) : '—'}</DetailItem>
                {/* The wedding flags are null for event types that do not use them ("not applicable"). */}
                {event.daytimeWedding != null && (
                  <DetailItem label="Casamento diurno?">{yesNo(event.daytimeWedding)}</DetailItem>
                )}
                {event.outdoorWedding != null && (
                  <DetailItem label="Casamento ao ar livre?">{yesNo(event.outdoorWedding)}</DetailItem>
                )}
                <DetailItem label="Descrição" wide>
                  {event.description || '—'}
                </DetailItem>
              </dl>
            </Card>

            <Card title="Clientes">
              {event.customers.length === 0 ? (
                <p className="event-detail-empty">Nenhum cliente vinculado.</p>
              ) : (
                <ul className="event-detail-list">
                  {event.customers.map((customer) => (
                    <li key={customer.customerId}>
                      {customer.customerName}
                      {customer.contracting && <span className="event-detail-tag">Contratante</span>}
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card title="Locais">
              {event.venues.length === 0 ? (
                <p className="event-detail-empty">Nenhum local vinculado.</p>
              ) : (
                <ul className="event-detail-list">
                  {event.venues.map((venue) => (
                    <li key={venue.venueId}>
                      {venue.venueName}
                      {venue.role && <span className="event-detail-role"> — {venue.role}</span>}
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card title="Profissionais">
              {event.eventProfessionals.length === 0 ? (
                <p className="event-detail-empty">Nenhum profissional vinculado.</p>
              ) : (
                <ul className="event-detail-list">
                  {event.eventProfessionals.map((professional) => (
                    <li key={professional.professionalId}>
                      {professional.professionalName}
                      {professional.roleInEvent && (
                        <span className="event-detail-role"> — {professional.roleInEvent}</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card title="Entrega e armazenamento">
              <dl className="event-detail-grid">
                <DetailItem label="HD">{event.hdId ? (hdName ?? '—') : '—'}</DetailItem>
                <DetailItem label="Tamanho">{event.sizeGb != null ? `${event.sizeGb} GB` : '—'}</DetailItem>
                <DetailItem label="Status de entrega">
                  <Pill status={DELIVERY_STATUS_TO_PILL[event.deliveryStatus]}>
                    {DELIVERY_STATUS_LABEL[event.deliveryStatus]}
                  </Pill>
                </DetailItem>
              </dl>
            </Card>
          </>
        )}
      </div>

      {isDeleteDialogOpen && event && (
        <ConfirmDialog
          title="Excluir evento"
          message={deleteConfirmationMessage(deletableEvent(event))}
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
