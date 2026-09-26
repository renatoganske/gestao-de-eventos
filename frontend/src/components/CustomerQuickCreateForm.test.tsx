import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as customersApi from '../api/customers'
import { CustomerQuickCreateForm } from './CustomerQuickCreateForm'

vi.mock('../api/customers')

describe('CustomerQuickCreateForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exige o nome antes de submeter', async () => {
    const user = userEvent.setup()
    render(<CustomerQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(screen.getByText('Informe o nome do cliente.')).toBeInTheDocument()
    expect(customersApi.createCustomer).not.toHaveBeenCalled()
  })

  it('chama onCancel ao clicar em Cancelar sem submeter', async () => {
    const onCancel = vi.fn()
    const user = userEvent.setup()
    render(<CustomerQuickCreateForm onCreated={vi.fn()} onCancel={onCancel} />)

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onCancel).toHaveBeenCalled()
    expect(customersApi.createCustomer).not.toHaveBeenCalled()
  })

  it('mostra a mensagem de erro da API quando a criação falha', async () => {
    vi.mocked(customersApi.createCustomer).mockRejectedValue(new ApiError(500, 'Falha ao criar cliente'))
    const user = userEvent.setup()
    render(<CustomerQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Cliente X')
    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(await screen.findByText('Falha ao criar cliente')).toBeInTheDocument()
  })

  it('envia campos em branco como null', async () => {
    const created = { id: 'customer-new', name: 'Cliente X', contact: null, address: null, notes: null }
    vi.mocked(customersApi.createCustomer).mockResolvedValue(created)
    const onCreated = vi.fn()
    const user = userEvent.setup()
    render(<CustomerQuickCreateForm onCreated={onCreated} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Cliente X')
    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(customersApi.createCustomer).toHaveBeenCalledWith({ name: 'Cliente X', contact: null, address: null, notes: null })
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created))
  })
})
