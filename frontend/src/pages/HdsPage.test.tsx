import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as hdsApi from '../api/hds'
import type { HdDto } from '../api/hds'
import { HdsPage } from './HdsPage'

vi.mock('../api/hds')

function makeHd(overrides: Partial<HdDto> = {}): HdDto {
  return {
    id: 'hd-1',
    name: 'HD Externo 4',
    capacityGb: 2000,
    usedSpaceGb: 1000,
    physicalLocation: 'Estante A',
    serialNumber: 'SN-1',
    acquisitionDate: '2025-01-10',
    status: 'ACTIVE',
    ...overrides,
  }
}

describe('HdsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('mostra os HDs em cards com a barra de uso', async () => {
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([makeHd({})])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())
    expect(screen.getByText('1000 / 2000 GB')).toBeInTheDocument()
    expect(screen.getByText('50%')).toBeInTheDocument()
  })

  it('destaca com borda e pill apenas os HDs retornados por near-capacity', async () => {
    const critical = makeHd({ id: 'hd-critical', name: 'HD Quase Cheio', usedSpaceGb: 1900 })
    const normal = makeHd({ id: 'hd-normal', name: 'HD Tranquilo', usedSpaceGb: 200 })
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([critical, normal])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([critical])

    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('HD Quase Cheio')).toBeInTheDocument())

    const criticalCard = screen.getByText('HD Quase Cheio').closest('.hd-card') as HTMLElement
    const normalCard = screen.getByText('HD Tranquilo').closest('.hd-card') as HTMLElement

    expect(criticalCard).toHaveClass('hd-card-near-capacity')
    expect(within(criticalCard).getByText('Perto da capacidade')).toBeInTheDocument()

    expect(normalCard).not.toHaveClass('hd-card-near-capacity')
    expect(within(normalCard).queryByText('Perto da capacidade')).not.toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há HDs cadastrados', async () => {
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('Nenhum HD cadastrado.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(hdsApi.fetchHds).mockRejectedValue(new ApiError(500, 'Falha ao carregar HDs'))
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    render(<HdsPage />)

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao carregar HDs'))
  })

  it('cria um novo HD pelo modal e mostra o card criado', async () => {
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    vi.mocked(hdsApi.createHd).mockResolvedValue(makeHd({ id: 'hd-new', name: 'HD Novo' }))
    const user = userEvent.setup()
    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('Nenhum HD cadastrado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Novo HD' }))
    const modal = await screen.findByRole('dialog', { name: 'Novo HD' })
    await user.type(within(modal).getByLabelText('Nome'), 'HD Novo')
    await user.click(within(modal).getByRole('button', { name: 'Salvar HD' }))

    await waitFor(() => expect(screen.getByText('HD Novo')).toBeInTheDocument())
    expect(screen.queryByRole('dialog', { name: 'Novo HD' })).not.toBeInTheDocument()
  })

  it('destaca o HD recém-criado quando ele já nasce perto da capacidade, sem precisar recarregar a página', async () => {
    const created = makeHd({ id: 'hd-new', name: 'HD Quase Cheio', usedSpaceGb: 960, capacityGb: 1000 })
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValueOnce([]).mockResolvedValueOnce([created])
    vi.mocked(hdsApi.createHd).mockResolvedValue(created)
    const user = userEvent.setup()
    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('Nenhum HD cadastrado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Novo HD' }))
    const modal = await screen.findByRole('dialog', { name: 'Novo HD' })
    await user.type(within(modal).getByLabelText('Nome'), 'HD Quase Cheio')
    await user.click(within(modal).getByRole('button', { name: 'Salvar HD' }))

    await waitFor(() => expect(screen.getByText('HD Quase Cheio').closest('.hd-card')).toHaveClass('hd-card-near-capacity'))
    expect(screen.getByText('Perto da capacidade')).toBeInTheDocument()
  })

  it('edita um HD existente pelo modal', async () => {
    const hd = makeHd()
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([hd])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    vi.mocked(hdsApi.updateHd).mockResolvedValue({ ...hd, name: 'HD Renomeado' })
    const user = userEvent.setup()
    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

    const card = screen.getByText('HD Externo 4').closest('.hd-card') as HTMLElement
    await user.click(within(card).getByRole('button', { name: 'Editar' }))

    const modal = await screen.findByRole('dialog', { name: 'Editar HD' })
    expect(within(modal).getByLabelText('Nome')).toHaveValue('HD Externo 4')
    await user.clear(within(modal).getByLabelText('Nome'))
    await user.type(within(modal).getByLabelText('Nome'), 'HD Renomeado')
    await user.click(within(modal).getByRole('button', { name: 'Salvar HD' }))

    expect(hdsApi.updateHd).toHaveBeenCalledWith('hd-1', expect.objectContaining({ name: 'HD Renomeado' }))
    await waitFor(() => expect(screen.getByText('HD Renomeado')).toBeInTheDocument())
  })

  it('exclui um HD após confirmação, e não exclui quando cancelado', async () => {
    const hd = makeHd()
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([hd])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    vi.mocked(hdsApi.deleteHd).mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

    const card = screen.getByText('HD Externo 4').closest('.hd-card') as HTMLElement
    await user.click(within(card).getByRole('button', { name: 'Excluir' }))

    const dialog = await screen.findByRole('dialog', { name: 'Excluir HD' })
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }))
    expect(hdsApi.deleteHd).not.toHaveBeenCalled()
    expect(screen.getByText('HD Externo 4')).toBeInTheDocument()

    await user.click(within(card).getByRole('button', { name: 'Excluir' }))
    const dialogAgain = await screen.findByRole('dialog', { name: 'Excluir HD' })
    await user.click(within(dialogAgain).getByRole('button', { name: 'Excluir' }))

    expect(hdsApi.deleteHd).toHaveBeenCalledWith('hd-1')
    await waitFor(() => expect(screen.queryByText('HD Externo 4')).not.toBeInTheDocument())
  })

  it('mantém o modal de exclusão aberto com erro inline quando a exclusão falha', async () => {
    const hd = makeHd()
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([hd])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    vi.mocked(hdsApi.deleteHd).mockRejectedValue(new ApiError(409, 'HD vinculado a eventos'))
    const user = userEvent.setup()
    render(<HdsPage />)

    await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

    const card = screen.getByText('HD Externo 4').closest('.hd-card') as HTMLElement
    await user.click(within(card).getByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir HD' })
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('HD vinculado a eventos')).toBeInTheDocument()
    expect(screen.getByText('HD Externo 4')).toBeInTheDocument()
  })
})
