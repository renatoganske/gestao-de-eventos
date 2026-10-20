import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../api/client'
import * as eventsApi from '../api/events'
import type { EventDto } from '../api/events'
import * as hdsApi from '../api/hds'
import type { HdDto } from '../api/hds'
import { HdsPage } from './HdsPage'

vi.mock('../api/hds')
vi.mock('../api/events', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/events')>()),
  searchEvents: vi.fn(),
}))

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/hds']}>
      <Routes>
        <Route path="/hds" element={<HdsPage />} />
        <Route path="/eventos/:id" element={<p>Detalhe do evento</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

function makeEvent(overrides: Partial<EventDto> = {}): EventDto {
  return {
    id: 'ev-1',
    eventCode: 'RG10',
    type: { id: 't1', name: 'Casamento' },
    name: 'Casamento Ana e Beto',
    eventDate: '2025-03-15',
    daytimeWedding: null,
    outdoorWedding: null,
    guestCount: null,
    description: null,
    amount: null,
    sizeGb: null,
    deliveryStatus: 'PENDING',
    hdId: 'hd-1',
    venues: [],
    customers: [],
    eventProfessionals: [],
    ...overrides,
  }
}

function cardNames() {
  return screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent)
}

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
    eventCount: 0,
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

    renderPage()

    await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())
    expect(screen.getByText('1000 / 2000 GB')).toBeInTheDocument()
    expect(screen.getByText('50%')).toBeInTheDocument()
  })

  it('calcula a barra de uso pela capacidade real quando preenchida', async () => {
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([makeHd({ capacityGb: 2000, realCapacityGb: 1000, usedSpaceGb: 500 })])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    renderPage()

    await waitFor(() => expect(screen.getByText('500 / 1000 GB')).toBeInTheDocument())
    expect(screen.getByText('50%')).toBeInTheDocument()
  })

  it('destaca com borda e pill apenas os HDs retornados por near-capacity', async () => {
    const critical = makeHd({ id: 'hd-critical', name: 'HD Quase Cheio', usedSpaceGb: 1900 })
    const normal = makeHd({ id: 'hd-normal', name: 'HD Tranquilo', usedSpaceGb: 200 })
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([critical, normal])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([critical])

    renderPage()

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

    renderPage()

    await waitFor(() => expect(screen.getByText('Nenhum HD cadastrado.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(hdsApi.fetchHds).mockRejectedValue(new ApiError(500, 'Falha ao carregar HDs'))
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    renderPage()

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao carregar HDs'))
  })

  it('cria um novo HD pelo modal e mostra o card criado', async () => {
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    vi.mocked(hdsApi.createHd).mockResolvedValue(makeHd({ id: 'hd-new', name: 'HD Novo' }))
    const user = userEvent.setup()
    renderPage()

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
    renderPage()

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
    renderPage()

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
    renderPage()

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
    renderPage()

    await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

    const card = screen.getByText('HD Externo 4').closest('.hd-card') as HTMLElement
    await user.click(within(card).getByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir HD' })
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('HD vinculado a eventos')).toBeInTheDocument()
    expect(screen.getByText('HD Externo 4')).toBeInTheDocument()
  })

  it('mostra o espaço livre no card, com — sem capacidade e nunca negativo', async () => {
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([
      makeHd({ id: 'a', name: 'HD A', capacityGb: 2000, usedSpaceGb: 1500 }),
      makeHd({ id: 'b', name: 'HD B', capacityGb: null as unknown as number, usedSpaceGb: 10 }),
      makeHd({ id: 'c', name: 'HD C', capacityGb: 1000, usedSpaceGb: 1200 }),
    ])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    renderPage()

    await waitFor(() => expect(screen.getByText('HD A')).toBeInTheDocument())
    expect(within(screen.getByText('HD A').closest('.hd-card') as HTMLElement).getByText('Livre: 500 GB')).toBeInTheDocument()
    expect(within(screen.getByText('HD B').closest('.hd-card') as HTMLElement).getByText('Livre: —')).toBeInTheDocument()
    expect(within(screen.getByText('HD C').closest('.hd-card') as HTMLElement).getByText('Livre: 0 GB')).toBeInTheDocument()
  })

  describe('ordenação', () => {
    const hds = [
      makeHd({ id: 'a', name: 'Beta', capacityGb: 1000, usedSpaceGb: 900, status: 'FULL', physicalLocation: 'Estante B' }),
      makeHd({ id: 'b', name: 'Alfa', capacityGb: 1000, usedSpaceGb: 100, status: 'ACTIVE', physicalLocation: null }),
      makeHd({ id: 'c', name: 'Gama', capacityGb: 1000, usedSpaceGb: 500, status: 'ARCHIVED', physicalLocation: 'Estante A' }),
    ]

    beforeEach(() => {
      vi.mocked(hdsApi.fetchHds).mockResolvedValue(hds)
      vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    })

    it('abre ordenado por nome A→Z', async () => {
      renderPage()
      await waitFor(() => expect(screen.getByText('Alfa')).toBeInTheDocument())
      expect(cardNames()).toEqual(['Alfa', 'Beta', 'Gama'])
    })

    it('ordena por espaço livre e inverte a direção', async () => {
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('Alfa')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Ordenar por'), 'free')
      expect(cardNames()).toEqual(['Beta', 'Gama', 'Alfa'])

      await user.click(screen.getByRole('button', { name: 'Direção: crescente' }))
      expect(cardNames()).toEqual(['Alfa', 'Gama', 'Beta'])
    })

    it('ordena por % de uso, status e localização (nulos no fim)', async () => {
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('Alfa')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Ordenar por'), 'usage')
      expect(cardNames()).toEqual(['Alfa', 'Gama', 'Beta'])

      await user.selectOptions(screen.getByLabelText('Ordenar por'), 'status')
      expect(cardNames()).toEqual(['Gama', 'Alfa', 'Beta'])

      await user.selectOptions(screen.getByLabelText('Ordenar por'), 'location')
      expect(cardNames()).toEqual(['Gama', 'Beta', 'Alfa'])

      await user.click(screen.getByRole('button', { name: 'Direção: crescente' }))
      expect(cardNames()).toEqual(['Beta', 'Gama', 'Alfa'])
    })
  })

  describe('eventos do HD', () => {
    beforeEach(() => {
      vi.mocked(hdsApi.fetchHds).mockResolvedValue([makeHd()])
      vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
    })

    it('abre o modal com os eventos do HD ao clicar no card e navega ao clicar no evento', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent()])
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

      await user.click(screen.getByRole('button', { name: 'Ver eventos em HD Externo 4' }))

      const modal = await screen.findByRole('dialog', { name: 'Eventos em HD Externo 4' })
      expect(eventsApi.searchEvents).toHaveBeenCalledWith({ hdId: 'hd-1' })
      expect(await within(modal).findByText('Casamento Ana e Beto')).toBeInTheDocument()
      expect(within(modal).getByText('RG10')).toBeInTheDocument()
      expect(within(modal).getByText('2025-03-15')).toBeInTheDocument()
      expect(within(modal).getByText('Pendente')).toBeInTheDocument()

      await user.click(within(modal).getByText('Casamento Ana e Beto'))
      expect(await screen.findByText('Detalhe do evento')).toBeInTheDocument()
    })

    it('abre o modal pelo teclado com Enter e Espaço', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([])
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

      screen.getByRole('button', { name: 'Ver eventos em HD Externo 4' }).focus()
      await user.keyboard('{Enter}')
      expect(await screen.findByRole('dialog', { name: 'Eventos em HD Externo 4' })).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: 'Fechar' }))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

      screen.getByRole('button', { name: 'Ver eventos em HD Externo 4' }).focus()
      await user.keyboard(' ')
      expect(await screen.findByRole('dialog', { name: 'Eventos em HD Externo 4' })).toBeInTheDocument()
    })

    it('não abre o modal de eventos ao clicar em Editar ou Excluir', async () => {
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())
      const card = screen.getByText('HD Externo 4').closest('.hd-card') as HTMLElement

      await user.click(within(card).getByRole('button', { name: 'Editar' }))
      expect(await screen.findByRole('dialog', { name: 'Editar HD' })).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Cancelar' }))

      await user.click(within(card).getByRole('button', { name: 'Excluir' }))
      expect(await screen.findByRole('dialog', { name: 'Excluir HD' })).toBeInTheDocument()

      expect(eventsApi.searchEvents).not.toHaveBeenCalled()
      expect(screen.queryByRole('dialog', { name: 'Eventos em HD Externo 4' })).not.toBeInTheDocument()
    })

    it('mostra o estado vazio quando o HD não tem eventos', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([])
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

      await user.click(screen.getByRole('button', { name: 'Ver eventos em HD Externo 4' }))

      expect(await screen.findByText('Nenhum evento neste HD.')).toBeInTheDocument()
    })

    it('mostra o erro da API quando o carregamento dos eventos falha', async () => {
      vi.mocked(eventsApi.searchEvents).mockRejectedValue(new ApiError(500, 'Falha ao buscar eventos'))
      const user = userEvent.setup()
      renderPage()
      await waitFor(() => expect(screen.getByText('HD Externo 4')).toBeInTheDocument())

      await user.click(screen.getByRole('button', { name: 'Ver eventos em HD Externo 4' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao buscar eventos')
    })
  })
})

describe('HdsPage - contagem de eventos (GDE-58)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([
      makeHd({ id: 'hd-a', name: 'Alfa', eventCount: 7 }),
      makeHd({ id: 'hd-b', name: 'Beta', eventCount: 0 }),
      makeHd({ id: 'hd-c', name: 'Gama', eventCount: 25 }),
    ])
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])
  })

  it('mostra a contagem de eventos em cada card, inclusive zero', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('Alfa')).toBeInTheDocument())

    const counts = screen.getAllByText('Eventos', { selector: 'dt' }).map((dt) => dt.nextElementSibling?.textContent)
    expect(counts).toEqual(['7', '0', '25'])
  })

  it('ordena por Nº de eventos e inverte a direção', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('Alfa')).toBeInTheDocument())

    await user.selectOptions(screen.getByLabelText('Ordenar por'), 'eventCount')
    expect(cardNames()).toEqual(['Beta', 'Alfa', 'Gama'])

    await user.click(screen.getByRole('button', { name: 'Direção: crescente' }))
    expect(cardNames()).toEqual(['Gama', 'Alfa', 'Beta'])
  })
})
