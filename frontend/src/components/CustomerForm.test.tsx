import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as customersApi from '../api/customers'
import type { CustomerDto } from '../api/customers'
import { CustomerForm } from './CustomerForm'

vi.mock('../api/customers')

function makeCustomer(overrides: Partial<CustomerDto> = {}): CustomerDto {
  return {
    id: 'customer-1',
    name: 'Maria Silva',
    contact: '(11) 99999-0000',
    address: 'Rua das Flores, 123',
    notes: 'Prefere contato por WhatsApp',
    ...overrides,
  }
}

describe('CustomerForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exige o nome antes de submeter em modo criação', async () => {
    const user = userEvent.setup()
    render(<CustomerForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(screen.getByText('Informe o nome do cliente.')).toBeInTheDocument()
    expect(customersApi.createCustomer).not.toHaveBeenCalled()
  })

  it('cria um cliente novo enviando campos em branco como null', async () => {
    const created = makeCustomer({ id: 'customer-new', contact: null, address: null, notes: null })
    vi.mocked(customersApi.createCustomer).mockResolvedValue(created)
    const onSaved = vi.fn()
    const user = userEvent.setup()
    render(<CustomerForm onSaved={onSaved} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Maria Silva')
    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(customersApi.createCustomer).toHaveBeenCalledWith({ name: 'Maria Silva', contact: null, address: null, notes: null })
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(created))
  })

  it('pré-preenche os campos e chama updateCustomer quando um cliente é passado (modo edição)', async () => {
    const customer = makeCustomer()
    const updated = makeCustomer({ name: 'Maria Silva Souza' })
    vi.mocked(customersApi.updateCustomer).mockResolvedValue(updated)
    const onSaved = vi.fn()
    const user = userEvent.setup()
    render(<CustomerForm customer={customer} onSaved={onSaved} onCancel={vi.fn()} />)

    expect(screen.getByLabelText('Nome')).toHaveValue('Maria Silva')
    expect(screen.getByLabelText('Contato')).toHaveValue('(11) 99999-0000')

    await user.clear(screen.getByLabelText('Nome'))
    await user.type(screen.getByLabelText('Nome'), 'Maria Silva Souza')
    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(customersApi.updateCustomer).toHaveBeenCalledWith('customer-1', expect.objectContaining({ name: 'Maria Silva Souza' }))
    expect(customersApi.createCustomer).not.toHaveBeenCalled()
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(updated))
  })

  it('mostra a mensagem de erro da API quando o salvamento falha', async () => {
    vi.mocked(customersApi.createCustomer).mockRejectedValue(new ApiError(500, 'Falha ao salvar cliente'))
    const user = userEvent.setup()
    render(<CustomerForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Maria Silva')
    await user.click(screen.getByRole('button', { name: 'Salvar cliente' }))

    expect(await screen.findByText('Falha ao salvar cliente')).toBeInTheDocument()
  })
})
