import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { deleteProfessional, fetchProfessionals, type ProfessionalDto } from '../api/professionals'
import { fetchProfessionalTypes, type ProfessionalTypeDto } from '../api/professionalTypes'
import { fetchSpecialtyTags, type SpecialtyTagDto } from '../api/specialtyTags'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Modal } from '../components/Modal'
import { ProfessionalForm } from '../components/ProfessionalForm'
import { Table } from '../components/Table'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useToast } from '../hooks/useToast'
import './ListPage.css'

type ModalState = { type: 'create' } | { type: 'edit'; professional: ProfessionalDto } | { type: 'delete'; professional: ProfessionalDto } | null

const GENERIC_LOAD_ERROR = 'Não foi possível carregar os profissionais. Tente novamente em instantes.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o profissional. Tente novamente.'

export function ProfessionalsPage() {
  const [professionals, setProfessionals] = useState<ProfessionalDto[] | null>(null)
  const [professionalTypes, setProfessionalTypes] = useState<ProfessionalTypeDto[]>([])
  const [specialtyTags, setSpecialtyTags] = useState<SpecialtyTagDto[]>([])
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
        const [professionalsData, typesData, tagsData] = await Promise.all([
          fetchProfessionals(),
          fetchProfessionalTypes(),
          fetchSpecialtyTags(),
        ])
        if (!cancelled) {
          setProfessionals(professionalsData)
          setProfessionalTypes(typesData)
          setSpecialtyTags(tagsData)
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

  function handleCreated(professional: ProfessionalDto) {
    setProfessionals((current) => [...(current ?? []), professional])
    closeModal()
    setToast({ kind: 'success', text: 'Profissional criado com sucesso.' })
  }

  function handleUpdated(professional: ProfessionalDto) {
    setProfessionals((current) => (current ?? []).map((item) => (item.id === professional.id ? professional : item)))
    closeModal()
    setToast({ kind: 'success', text: 'Profissional atualizado com sucesso.' })
  }

  async function handleConfirmDelete(professional: ProfessionalDto) {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteProfessional(professional.id)
      setProfessionals((current) => (current ?? []).filter((item) => item.id !== professional.id))
      closeModal()
      setToast({ kind: 'success', text: 'Profissional excluído com sucesso.' })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
    } finally {
      setIsDeleting(false)
    }
  }

  function handleProfessionalTypeCreated(type: ProfessionalTypeDto) {
    setProfessionalTypes((current) => [...current, type])
  }

  function handleSpecialtyTagCreated(tag: SpecialtyTagDto) {
    setSpecialtyTags((current) => [...current, tag])
  }

  return (
    <section>
      <TopBar
        title="Profissionais"
        action={
          <Button type="button" variant="primary" onClick={() => setModal({ type: 'create' })}>
            + Novo profissional
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

          {!isLoading && !loadError && professionals && professionals.length === 0 && (
            <p className="events-empty">Nenhum profissional cadastrado.</p>
          )}

          {!isLoading && !loadError && professionals && professionals.length > 0 && (
            <Table
              columns={[
                { key: 'name', header: 'Nome', render: (row) => row.name },
                { key: 'type', header: 'Tipo', render: (row) => row.type?.name ?? '—' },
                { key: 'contact', header: 'Contato', render: (row) => row.contact ?? '—' },
                {
                  key: 'specialtyTags',
                  header: 'Especialidades',
                  render: (row) => (row.specialtyTags.length > 0 ? row.specialtyTags.map((tag) => tag.name).join(', ') : '—'),
                },
                {
                  key: 'actions',
                  header: 'Ações',
                  render: (row) => (
                    <div className="table-actions">
                      <Button type="button" variant="ghost" onClick={() => setModal({ type: 'edit', professional: row })}>
                        Editar
                      </Button>
                      <Button type="button" variant="ghost" onClick={() => setModal({ type: 'delete', professional: row })}>
                        Excluir
                      </Button>
                    </div>
                  ),
                },
              ]}
              rows={professionals}
              rowKey={(row) => row.id}
            />
          )}
        </Card>
      </div>

      {modal?.type === 'create' && (
        <Modal title="Novo profissional" onClose={closeModal}>
          <ProfessionalForm
            professionalTypes={professionalTypes}
            specialtyTags={specialtyTags}
            onProfessionalTypeCreated={handleProfessionalTypeCreated}
            onSpecialtyTagCreated={handleSpecialtyTagCreated}
            onSaved={handleCreated}
            onCancel={closeModal}
          />
        </Modal>
      )}
      {modal?.type === 'edit' && (
        <Modal title="Editar profissional" onClose={closeModal}>
          <ProfessionalForm
            professional={modal.professional}
            professionalTypes={professionalTypes}
            specialtyTags={specialtyTags}
            onProfessionalTypeCreated={handleProfessionalTypeCreated}
            onSpecialtyTagCreated={handleSpecialtyTagCreated}
            onSaved={handleUpdated}
            onCancel={closeModal}
          />
        </Modal>
      )}
      {modal?.type === 'delete' && (
        <ConfirmDialog
          title="Excluir profissional"
          message={`Tem certeza que deseja excluir o profissional "${modal.professional.name}"? Essa ação não pode ser desfeita.`}
          confirmLabel="Excluir"
          isConfirming={isDeleting}
          error={deleteError}
          onConfirm={() => handleConfirmDelete(modal.professional)}
          onCancel={closeModal}
        />
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
