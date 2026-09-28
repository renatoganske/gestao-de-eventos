import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { deleteEventVenue, fetchEventVenues, type EventVenueDto } from '../api/eventVenues'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EventVenueForm } from '../components/EventVenueForm'
import { Modal } from '../components/Modal'
import { Table } from '../components/Table'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useToast } from '../hooks/useToast'
import './ListPage.css'

type ModalState = { type: 'create' } | { type: 'edit'; venue: EventVenueDto } | { type: 'delete'; venue: EventVenueDto } | null

const GENERIC_LOAD_ERROR = 'Não foi possível carregar os locais. Tente novamente em instantes.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o local. Tente novamente.'

export function EventVenuesPage() {
  const [venues, setVenues] = useState<EventVenueDto[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [modal, setModal] = useState<ModalState>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [toast, setToast] = useToast()

  useEffect(() => {
    let cancelled = false

    async function load() {
      setIsLoading(true)
      setLoadError(null)
      try {
        const data = await fetchEventVenues()
        if (!cancelled) {
          setVenues(data)
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err instanceof ApiError ? err.message : GENERIC_LOAD_ERROR)
        }
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

  function handleCreated(venue: EventVenueDto) {
    setVenues((current) => [...(current ?? []), venue])
    closeModal()
    setToast({ kind: 'success', text: 'Local criado com sucesso.' })
  }

  function handleUpdated(venue: EventVenueDto) {
    setVenues((current) => (current ?? []).map((item) => (item.id === venue.id ? venue : item)))
    closeModal()
    setToast({ kind: 'success', text: 'Local atualizado com sucesso.' })
  }

  async function handleConfirmDelete(venue: EventVenueDto) {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteEventVenue(venue.id)
      setVenues((current) => (current ?? []).filter((item) => item.id !== venue.id))
      closeModal()
      setToast({ kind: 'success', text: 'Local excluído com sucesso.' })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <section>
      <TopBar
        title="Locais de evento"
        action={
          <Button type="button" variant="primary" onClick={() => setModal({ type: 'create' })}>
            + Novo local
          </Button>
        }
      />
      <div className="list-page-content">
        <Card>
          {isLoading && <p className="events-empty">Carregando...</p>}

          {!isLoading && loadError && (
            <p role="alert" className="events-error">
              {loadError}
            </p>
          )}

          {!isLoading && !loadError && venues && venues.length === 0 && (
            <p className="events-empty">Nenhum local cadastrado.</p>
          )}

          {!isLoading && !loadError && venues && venues.length > 0 && (
            <Table
              columns={[
                { key: 'name', header: 'Nome', render: (row) => row.name },
                { key: 'city', header: 'Cidade', render: (row) => row.city ?? '—' },
                { key: 'state', header: 'Estado', render: (row) => row.state ?? '—' },
                { key: 'type', header: 'Tipo', render: (row) => row.type ?? '—' },
                {
                  key: 'actions',
                  header: 'Ações',
                  render: (row) => (
                    <div className="table-actions">
                      <Button type="button" variant="ghost" onClick={() => setModal({ type: 'edit', venue: row })}>
                        Editar
                      </Button>
                      <Button type="button" variant="ghost" onClick={() => setModal({ type: 'delete', venue: row })}>
                        Excluir
                      </Button>
                    </div>
                  ),
                },
              ]}
              rows={venues}
              rowKey={(row) => row.id}
            />
          )}
        </Card>
      </div>

      {modal?.type === 'create' && (
        <Modal title="Novo local" onClose={closeModal}>
          <EventVenueForm onSaved={handleCreated} onCancel={closeModal} />
        </Modal>
      )}
      {modal?.type === 'edit' && (
        <Modal title="Editar local" onClose={closeModal}>
          <EventVenueForm venue={modal.venue} onSaved={handleUpdated} onCancel={closeModal} />
        </Modal>
      )}
      {modal?.type === 'delete' && (
        <ConfirmDialog
          title="Excluir local"
          message={`Tem certeza que deseja excluir o local "${modal.venue.name}"? Essa ação não pode ser desfeita.`}
          confirmLabel="Excluir"
          isConfirming={isDeleting}
          error={deleteError}
          onConfirm={() => handleConfirmDelete(modal.venue)}
          onCancel={closeModal}
        />
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
