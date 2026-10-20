import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { deleteHd, fetchHds, fetchHdsNearCapacity, HD_STATUS_LABEL, type HdDto } from '../api/hds'
import { DELIVERY_STATUS_LABEL, DELIVERY_STATUS_TO_PILL, searchEvents, type EventDto } from '../api/events'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { HdForm } from '../components/HdForm'
import { Modal } from '../components/Modal'
import { Pill } from '../components/Pill'
import { Table } from '../components/Table'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useToast } from '../hooks/useToast'
import { effectiveCapacityGb, freeSpaceGb, usagePercent } from '../utils/hdCapacity'
import { codeNumber, sortRows, type SortDirection, type SortValue } from '../utils/sort'
import './HdsPage.css'

type ModalState = { type: 'create' } | { type: 'events'; hd: HdDto } | { type: 'edit'; hd: HdDto } | { type: 'delete'; hd: HdDto } | null

const GENERIC_LOAD_ERROR = 'Não foi possível carregar os HDs. Tente novamente em instantes.'
const GENERIC_EVENTS_ERROR = 'Não foi possível carregar os eventos deste HD. Tente novamente em instantes.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o HD. Tente novamente.'

type HdSortKey = 'name' | 'free' | 'usage' | 'status' | 'location' | 'eventCount'

const HD_SORT_OPTIONS: { key: HdSortKey; label: string; value: (hd: HdDto) => SortValue }[] = [
  { key: 'name', label: 'Nome', value: (hd) => hd.name },
  { key: 'free', label: 'Espaço livre', value: (hd) => freeSpaceGb(hd) },
  { key: 'usage', label: '% de uso', value: (hd) => (effectiveCapacityGb(hd) ? usagePercent(hd) : null) },
  { key: 'status', label: 'Status', value: (hd) => HD_STATUS_LABEL[hd.status] },
  { key: 'location', label: 'Localização', value: (hd) => hd.physicalLocation },
  { key: 'eventCount', label: 'Nº de eventos', value: (hd) => hd.eventCount ?? 0 },
]

function HdEventsList({ hd }: { hd: HdDto }) {
  const navigate = useNavigate()
  const [events, setEvents] = useState<EventDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    searchEvents({ hdId: hd.id })
      .then((result) => {
        if (!cancelled) {
          setEvents(result)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : GENERIC_EVENTS_ERROR)
        }
      })
    return () => {
      cancelled = true
    }
  }, [hd.id])

  if (error) {
    return (
      <p role="alert" className="events-error">
        {error}
      </p>
    )
  }
  if (!events) {
    return <p className="events-empty">Carregando...</p>
  }
  if (events.length === 0) {
    return <p className="events-empty">Nenhum evento neste HD.</p>
  }
  return (
    <Table
      columns={[
        {
          key: 'code',
          header: 'Código',
          render: (row) => <span className="mono">{row.eventCode}</span>,
          sortValue: (row) => codeNumber(row.eventCode) ?? row.eventCode,
        },
        { key: 'name', header: 'Nome', render: (row) => row.name, sortValue: (row) => row.name },
        { key: 'date', header: 'Data', render: (row) => <span className="mono">{row.eventDate}</span>, sortValue: (row) => row.eventDate },
        {
          key: 'status',
          header: 'Status',
          render: (row) => <Pill status={DELIVERY_STATUS_TO_PILL[row.deliveryStatus]}>{DELIVERY_STATUS_LABEL[row.deliveryStatus]}</Pill>,
          sortValue: (row) => DELIVERY_STATUS_LABEL[row.deliveryStatus],
        },
      ]}
      rows={events}
      rowKey={(row) => row.id}
      onRowClick={(row) => navigate(`/eventos/${row.id}`)}
      defaultSort={{ key: 'date', direction: 'asc', thenBy: 'code' }}
    />
  )
}

export function HdsPage() {
  const [hds, setHds] = useState<HdDto[] | null>(null)
  const [nearCapacityIds, setNearCapacityIds] = useState<Set<string>>(new Set())
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [modal, setModal] = useState<ModalState>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [toast, setToast] = useToast()
  const [sortKey, setSortKey] = useState<HdSortKey>('name')
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc')

  const sortedHds = useMemo(() => {
    const option = HD_SORT_OPTIONS.find((item) => item.key === sortKey) ?? HD_SORT_OPTIONS[0]
    return sortRows(hds ?? [], option.value, sortDirection)
  }, [hds, sortKey, sortDirection])

  useEffect(() => {
    let cancelled = false

    async function load() {
      setIsLoading(true)
      setLoadError(null)
      try {
        const [allHds, nearCapacity] = await Promise.all([fetchHds(), fetchHdsNearCapacity()])
        if (cancelled) {
          return
        }
        setHds(allHds)
        setNearCapacityIds(new Set(nearCapacity.map((hd) => hd.id)))
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

    load()
    return () => {
      cancelled = true
    }
  }, [])

  function closeModal() {
    setModal(null)
    setDeleteError(null)
  }

  async function refreshNearCapacity() {
    try {
      const nearCapacity = await fetchHdsNearCapacity()
      setNearCapacityIds(new Set(nearCapacity.map((hd) => hd.id)))
    } catch {
      // destaque de "perto da capacidade" é um enriquecimento, não crítico o suficiente
      // para interromper o fluxo de criação/edição se essa segunda chamada falhar
    }
  }

  function handleCreated(hd: HdDto) {
    setHds((current) => [...(current ?? []), hd])
    closeModal()
    setToast({ kind: 'success', text: 'HD criado com sucesso.' })
    refreshNearCapacity()
  }

  function handleUpdated(hd: HdDto) {
    setHds((current) => (current ?? []).map((item) => (item.id === hd.id ? hd : item)))
    closeModal()
    setToast({ kind: 'success', text: 'HD atualizado com sucesso.' })
    refreshNearCapacity()
  }

  async function handleConfirmDelete(hd: HdDto) {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteHd(hd.id)
      setHds((current) => (current ?? []).filter((item) => item.id !== hd.id))
      setNearCapacityIds((current) => {
        const next = new Set(current)
        next.delete(hd.id)
        return next
      })
      closeModal()
      setToast({ kind: 'success', text: 'HD excluído com sucesso.' })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <section>
      <TopBar
        title="HDs"
        action={
          <Button type="button" variant="primary" onClick={() => setModal({ type: 'create' })}>
            + Novo HD
          </Button>
        }
      />
      <div className="hds-content">
        {isLoading && <p className="events-empty">Carregando...</p>}

        {!isLoading && loadError && (
          <p role="alert" className="events-error">
            {loadError}
          </p>
        )}

        {!isLoading && !loadError && hds && hds.length === 0 && <p className="events-empty">Nenhum HD cadastrado.</p>}

        {!isLoading && !loadError && hds && hds.length > 0 && (
          <div className="hds-sort">
            <label htmlFor="hds-sort-key">Ordenar por</label>
            <select id="hds-sort-key" value={sortKey} onChange={(event) => setSortKey(event.target.value as HdSortKey)}>
              {HD_SORT_OPTIONS.map((option) => (
                <option key={option.key} value={option.key}>
                  {option.label}
                </option>
              ))}
            </select>
            <Button
              type="button"
              variant="ghost"
              onClick={() => setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))}
              aria-label={sortDirection === 'asc' ? 'Direção: crescente' : 'Direção: decrescente'}
            >
              {sortDirection === 'asc' ? '↑ Crescente' : '↓ Decrescente'}
            </Button>
          </div>
        )}

        {!isLoading && !loadError && hds && hds.length > 0 && (
          <div className="hds-grid">
            {sortedHds.map((hd) => {
              const percent = usagePercent(hd)
              const free = freeSpaceGb(hd)
              const freeText = free === null ? '—' : `${free} GB`
              const isNearCapacity = nearCapacityIds.has(hd.id)
              return (
                <Card key={hd.id} className={isNearCapacity ? 'hd-card hd-card-near-capacity' : 'hd-card'}>
                  <div
                    className="hd-card-body"
                    role="button"
                    tabIndex={0}
                    aria-label={`Ver eventos em ${hd.name}`}
                    onClick={() => setModal({ type: 'events', hd })}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        setModal({ type: 'events', hd })
                      }
                    }}
                  >
                  <div className="hd-card-head">
                    <h3>{hd.name}</h3>
                    {isNearCapacity && <Pill status="critical">Perto da capacidade</Pill>}
                  </div>

                  <div className="hd-usage">
                    <div className="hd-usage-head">
                      <span className="mono">
                        {hd.usedSpaceGb} / {effectiveCapacityGb(hd) ?? '—'} GB
                      </span>
                      <span className="mono">{percent}%</span>
                    </div>
                    <p className="hd-free mono">Livre: {freeText}</p>
                    <div className="meter">
                      <div className={percent >= 95 ? 'meter-fill crit' : percent >= 80 ? 'meter-fill warn' : 'meter-fill'} style={{ width: `${percent}%` }} />
                    </div>
                  </div>

                  <dl className="hd-meta">
                    <dt>Status</dt>
                    <dd>{HD_STATUS_LABEL[hd.status]}</dd>
                    <dt>Localização</dt>
                    <dd>{hd.physicalLocation ?? '—'}</dd>
                    <dt>Nº de série</dt>
                    <dd className="mono">{hd.serialNumber ?? '—'}</dd>
                    <dt>Eventos</dt>
                    <dd className="mono">{hd.eventCount ?? 0}</dd>
                  </dl>
                  </div>

                  <div className="hd-card-actions" onClick={(event) => event.stopPropagation()}>
                    <Button type="button" variant="ghost" onClick={() => setModal({ type: 'edit', hd })}>
                      Editar
                    </Button>
                    <Button type="button" variant="ghost" onClick={() => setModal({ type: 'delete', hd })}>
                      Excluir
                    </Button>
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      {modal?.type === 'events' && (
        <Modal title={`Eventos em ${modal.hd.name}`} onClose={closeModal}>
          <HdEventsList hd={modal.hd} />
        </Modal>
      )}
      {modal?.type === 'create' && (
        <Modal title="Novo HD" onClose={closeModal}>
          <HdForm onSaved={handleCreated} onCancel={closeModal} />
        </Modal>
      )}
      {modal?.type === 'edit' && (
        <Modal title="Editar HD" onClose={closeModal}>
          <HdForm hd={modal.hd} onSaved={handleUpdated} onCancel={closeModal} />
        </Modal>
      )}
      {modal?.type === 'delete' && (
        <ConfirmDialog
          title="Excluir HD"
          message={`Tem certeza que deseja excluir o HD "${modal.hd.name}"? Essa ação não pode ser desfeita.`}
          confirmLabel="Excluir"
          isConfirming={isDeleting}
          error={deleteError}
          onConfirm={() => handleConfirmDelete(modal.hd)}
          onCancel={closeModal}
        />
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
