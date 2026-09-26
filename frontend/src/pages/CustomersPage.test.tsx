import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as customersApi from '../api/customers'
import type { CustomerDto } from '../api/customers'
import { CustomersPage } from './CustomersPage'

vi.mock('../api/customers')

function makeCustomer(overrides: Partial<CustomerDto> = {}): CustomerDto {
  return {
    id: 'customer-1',
    name: 'Maria Silva',
    contact: '(11) 99999-0000',
    address: 'Rua das Flores, 123',
    notes: null,
    ...overrides,
  }
}

describe('CustomersPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('mostra a lista de clientes numa tabela', async () => {
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([makeCustomer({})])

    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument())
    expect(screen.getByText('(11) 99999-0000')).toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há clientes cadastrados', async () => {
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([])

    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByText('Nenhum cliente cadastrado.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(customersApi.fetchCustomers).mockRejectedValue(new ApiError(500, 'Falha ao carregar clientes'))

    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao carregar clientes'))
  })

  it('cria um novo cliente pelo modal', async () => {
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([])
    vi.mocked(customersApi.createCustomer).mockResolvedValue(makeCustomer({ id: 'customer-new', name: 'Cliente Novo' }))
    const user = userEvent.setup()
    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByText('Nenhum cliente cadastrado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Novo cliente' }))
    const modal = await screen.findByRole('dialog', { name: 'Novo cliente' })
    await user.type(within(modal).getByLabelText('Nome'), 'Cliente Novo')
    await user.click(within(modal).getByRole('button', { name: 'Salvar cliente' }))

    await waitFor(() => expect(screen.getByText('Cliente Novo')).toBeInTheDocument())
    expect(screen.queryByRole('dialog', { name: 'Novo cliente' })).not.toBeInTheDocument()
  })

  it('edita um cliente existente pelo modal', async () => {
    const customer = makeCustomer()
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([customer])
    vi.mocked(customersApi.updateCustomer).mockResolvedValue({ ...customer, name: 'Maria Renomeada' })
    const user = userEvent.setup()
    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument())

    const row = screen.getByText('Maria Silva').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Editar' }))

    const modal = await screen.findByRole('dialog', { name: 'Editar cliente' })
    expect(within(modal).getByLabelText('Nome')).toHaveValue('Maria Silva')
    await user.clear(within(modal).getByLabelText('Nome'))
    await user.type(within(modal).getByLabelText('Nome'), 'Maria Renomeada')
    await user.click(within(modal).getByRole('button', { name: 'Salvar cliente' }))

    expect(customersApi.updateCustomer).toHaveBeenCalledWith('customer-1', expect.objectContaining({ name: 'Maria Renomeada' }))
    await waitFor(() => expect(screen.getByText('Maria Renomeada')).toBeInTheDocument())
  })

  it('exclui um cliente após confirmação, e não exclui quando cancelado', async () => {
    const customer = makeCustomer()
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([customer])
    vi.mocked(customersApi.deleteCustomer).mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument())

    const row = screen.getByText('Maria Silva').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    const dialog = await screen.findByRole('dialog', { name: 'Excluir cliente' })
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }))
    expect(customersApi.deleteCustomer).not.toHaveBeenCalled()
    expect(screen.getByText('Maria Silva')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialogAgain = await screen.findByRole('dialog', { name: 'Excluir cliente' })
    await user.click(within(dialogAgain).getByRole('button', { name: 'Excluir' }))

    expect(customersApi.deleteCustomer).toHaveBeenCalledWith('customer-1')
    await waitFor(() => expect(screen.queryByText('Maria Silva')).not.toBeInTheDocument())
  })

  it('mantém o modal de exclusão aberto com erro inline quando a exclusão falha', async () => {
    const customer = makeCustomer()
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([customer])
    vi.mocked(customersApi.deleteCustomer).mockRejectedValue(new ApiError(409, 'Cliente vinculado a eventos'))
    const user = userEvent.setup()
    render(<CustomersPage />)

    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument())

    const row = screen.getByText('Maria Silva').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir cliente' })
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('Cliente vinculado a eventos')).toBeInTheDocument()
    expect(screen.getByText('Maria Silva')).toBeInTheDocument()
  })
})
