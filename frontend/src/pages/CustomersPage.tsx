import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { deleteCustomer, fetchCustomers, type CustomerDto } from '../api/customers'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { CustomerForm } from '../components/CustomerForm'
import { Modal } from '../components/Modal'
import { Table } from '../components/Table'
import { Toast } from '../components/Toast'
import { TopBar } from '../components/TopBar'
import { useToast } from '../hooks/useToast'
import './ListPage.css'

type ModalState = { type: 'create' } | { type: 'edit'; customer: CustomerDto } | { type: 'delete'; customer: CustomerDto } | null

const GENERIC_LOAD_ERROR = 'Não foi possível carregar os clientes. Tente novamente em instantes.'
const GENERIC_DELETE_ERROR = 'Não foi possível excluir o cliente. Tente novamente.'

export function CustomersPage() {
  const [customers, setCustomers] = useState<CustomerDto[] | null>(null)
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
        const data = await fetchCustomers()
        if (!cancelled) {
          setCustomers(data)
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

  function handleCreated(customer: CustomerDto) {
    setCustomers((current) => [...(current ?? []), customer])
    closeModal()
    setToast({ kind: 'success', text: 'Cliente criado com sucesso.' })
  }

  function handleUpdated(customer: CustomerDto) {
    setCustomers((current) => (current ?? []).map((item) => (item.id === customer.id ? customer : item)))
    closeModal()
    setToast({ kind: 'success', text: 'Cliente atualizado com sucesso.' })
  }

  async function handleConfirmDelete(customer: CustomerDto) {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteCustomer(customer.id)
      setCustomers((current) => (current ?? []).filter((item) => item.id !== customer.id))
      closeModal()
      setToast({ kind: 'success', text: 'Cliente excluído com sucesso.' })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : GENERIC_DELETE_ERROR)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <section>
      <TopBar
        title="Clientes"
        action={
          <Button type="button" variant="primary" onClick={() => setModal({ type: 'create' })}>
            + Novo cliente
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

          {!isLoading && !loadError && customers && customers.length === 0 && (
            <p className="events-empty">Nenhum cliente cadastrado.</p>
          )}

          {!isLoading && !loadError && customers && customers.length > 0 && (
            <Table
              columns={[
                { key: 'name', header: 'Nome', render: (row) => row.name },
                { key: 'contact', header: 'Contato', render: (row) => row.contact ?? '—' },
                { key: 'address', header: 'Endereço', render: (row) => row.address ?? '—' },
                {
                  key: 'actions',
                  header: 'Ações',
                  render: (row) => (
                    <div className="table-actions">
                      <Button type="button" variant="ghost" onClick={() => setModal({ type: 'edit', customer: row })}>
                        Editar
                      </Button>
                      <Button type="button" variant="ghost" onClick={() => setModal({ type: 'delete', customer: row })}>
                        Excluir
                      </Button>
                    </div>
                  ),
                },
              ]}
              rows={customers}
              rowKey={(row) => row.id}
            />
          )}
        </Card>
      </div>

      {modal?.type === 'create' && (
        <Modal title="Novo cliente" onClose={closeModal}>
          <CustomerForm onSaved={handleCreated} onCancel={closeModal} />
        </Modal>
      )}
      {modal?.type === 'edit' && (
        <Modal title="Editar cliente" onClose={closeModal}>
          <CustomerForm customer={modal.customer} onSaved={handleUpdated} onCancel={closeModal} />
        </Modal>
      )}
      {modal?.type === 'delete' && (
        <ConfirmDialog
          title="Excluir cliente"
          message={`Tem certeza que deseja excluir o cliente "${modal.customer.name}"? Essa ação não pode ser desfeita.`}
          confirmLabel="Excluir"
          isConfirming={isDeleting}
          error={deleteError}
          onConfirm={() => handleConfirmDelete(modal.customer)}
          onCancel={closeModal}
        />
      )}

      {toast && <Toast toast={toast} />}
    </section>
  )
}
