import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as hdsApi from '../api/hds'
import type { HdDto } from '../api/hds'
import { HdQuickCreateForm } from './HdQuickCreateForm'

vi.mock('../api/hds')

describe('HdQuickCreateForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exige o nome antes de submeter', async () => {
    const user = userEvent.setup()
    render(<HdQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(screen.getByText('Informe o nome do HD.')).toBeInTheDocument()
    expect(hdsApi.createHd).not.toHaveBeenCalled()
  })

  it('converte a capacidade para número, zera o espaço usado e assume status ACTIVE por padrão', async () => {
    const created: HdDto = {
      id: 'hd-new',
      name: 'HD Externo 2TB',
      capacityGb: 2000,
      usedSpaceGb: 0,
      physicalLocation: null,
      serialNumber: null,
      acquisitionDate: null,
      status: 'ACTIVE',
    }
    vi.mocked(hdsApi.createHd).mockResolvedValue(created)
    const onCreated = vi.fn()
    const user = userEvent.setup()
    render(<HdQuickCreateForm onCreated={onCreated} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'HD Externo 2TB')
    await user.type(screen.getByLabelText('Capacidade (GB)'), '2000')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(hdsApi.createHd).toHaveBeenCalledWith({
      name: 'HD Externo 2TB',
      capacityGb: 2000,
      usedSpaceGb: null,
      physicalLocation: null,
      serialNumber: null,
      acquisitionDate: null,
      status: 'ACTIVE',
    })
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created))
  })

  it('mostra a mensagem de erro da API quando a criação falha', async () => {
    vi.mocked(hdsApi.createHd).mockRejectedValue(new ApiError(500, 'Falha ao criar HD'))
    const user = userEvent.setup()
    render(<HdQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'HD Externo 2TB')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(await screen.findByText('Falha ao criar HD')).toBeInTheDocument()
  })
})
