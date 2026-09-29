import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { deleteHd, fetchHds, fetchHdsNearCapacity, HD_STATUS_LABEL, type HdDto } from '../api/hds'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { HdForm } from '../components/HdForm'
import { Modal } from '../components/Modal'
import { Pill } from '../components/Pill'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useToast } from '../hooks/useToast'
import { effectiveCapacityGb, usagePercent } from '../utils/hdCapacity'
import './HdsPage.css'

type ModalState = { type: 'create' } | { type: 'edit'; hd: HdDto } | { type: 'delete'; hd: HdDto } | null

const GENERIC_LOAD_ERROR = 'Não foi possível carregar os HDs. Tente novamente em instantes.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o HD. Tente novamente.'

export function HdsPage() {
  const [hds, setHds] = useState<HdDto[] | null>(null)
  const [nearCapacityIds, setNearCapacityIds] = useState<Set<string>>(new Set())
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
          <div className="hds-grid">
            {hds.map((hd) => {
              const percent = usagePercent(hd)
              const isNearCapacity = nearCapacityIds.has(hd.id)
              return (
                <Card key={hd.id} className={isNearCapacity ? 'hd-card hd-card-near-capacity' : 'hd-card'}>
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
                  </dl>

                  <div className="hd-card-actions">
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
