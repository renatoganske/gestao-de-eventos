import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as hdsApi from '../api/hds'
import type { HdDto } from '../api/hds'
import { HdForm } from './HdForm'

vi.mock('../api/hds')

function makeHd(overrides: Partial<HdDto> = {}): HdDto {
  return {
    id: 'hd-1',
    name: 'HD Externo 4',
    capacityGb: 2000,
    realCapacityGb: 1863,
    usedSpaceGb: 1000,
    physicalLocation: 'Estante A',
    serialNumber: 'SN-1',
    acquisitionDate: '2025-01-10',
    status: 'ACTIVE',
    ...overrides,
  }
}

describe('HdForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exige o nome antes de submeter em modo criação', async () => {
    const user = userEvent.setup()
    render(<HdForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(screen.getByText('Informe o nome do HD.')).toBeInTheDocument()
    expect(hdsApi.createHd).not.toHaveBeenCalled()
  })

  it('cria um HD novo com status ACTIVE por padrão', async () => {
    const created = makeHd({ id: 'hd-new' })
    vi.mocked(hdsApi.createHd).mockResolvedValue(created)
    const onSaved = vi.fn()
    const user = userEvent.setup()
    render(<HdForm onSaved={onSaved} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'HD Externo 4')
    await user.type(screen.getByLabelText('Capacidade nominal (GB)'), '2000')
    await user.type(screen.getByLabelText('Capacidade real (GB)'), '1863')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(hdsApi.createHd).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'HD Externo 4', capacityGb: 2000, realCapacityGb: 1863, status: 'ACTIVE' }),
    )
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(created))
  })

  it('pré-preenche os campos e chama updateHd quando um HD é passado (modo edição)', async () => {
    const hd = makeHd()
    const updated = makeHd({ name: 'HD Externo 4 Renomeado' })
    vi.mocked(hdsApi.updateHd).mockResolvedValue(updated)
    const onSaved = vi.fn()
    const user = userEvent.setup()
    render(<HdForm hd={hd} onSaved={onSaved} onCancel={vi.fn()} />)

    expect(screen.getByLabelText('Nome')).toHaveValue('HD Externo 4')
    expect(screen.getByLabelText('Capacidade nominal (GB)')).toHaveValue(2000)
    expect(screen.getByLabelText('Capacidade real (GB)')).toHaveValue(1863)
    expect(screen.getByLabelText('Status')).toHaveValue('ACTIVE')

    await user.clear(screen.getByLabelText('Nome'))
    await user.type(screen.getByLabelText('Nome'), 'HD Externo 4 Renomeado')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(hdsApi.updateHd).toHaveBeenCalledWith('hd-1', expect.objectContaining({ name: 'HD Externo 4 Renomeado' }))
    expect(hdsApi.createHd).not.toHaveBeenCalled()
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(updated))
  })

  it('bloqueia capacidade real maior que a nominal', async () => {
    const user = userEvent.setup()
    render(<HdForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'HD')
    await user.type(screen.getByLabelText('Capacidade nominal (GB)'), '1000')
    await user.type(screen.getByLabelText('Capacidade real (GB)'), '1200')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))
    expect(screen.getByText('A capacidade real não pode ser maior que a nominal.')).toBeInTheDocument()

    expect(hdsApi.createHd).not.toHaveBeenCalled()
  })

  it('envia realCapacityGb nulo quando o campo fica vazio', async () => {
    vi.mocked(hdsApi.createHd).mockResolvedValue(makeHd())
    const user = userEvent.setup()
    render(<HdForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'HD')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(hdsApi.createHd).toHaveBeenCalledWith(expect.objectContaining({ realCapacityGb: null }))
  })

  it('mostra a mensagem de erro da API quando o salvamento falha', async () => {
    vi.mocked(hdsApi.createHd).mockRejectedValue(new ApiError(500, 'Falha ao salvar HD'))
    const user = userEvent.setup()
    render(<HdForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'HD Externo 4')
    await user.click(screen.getByRole('button', { name: 'Salvar HD' }))

    expect(await screen.findByText('Falha ao salvar HD')).toBeInTheDocument()
  })
})
