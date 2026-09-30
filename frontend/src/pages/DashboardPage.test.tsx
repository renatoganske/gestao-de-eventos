import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ApiError } from '../api/client'
import * as eventsApi from '../api/events'
import type { EventDto } from '../api/events'
import * as hdsApi from '../api/hds'
import type { HdDto } from '../api/hds'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/events')
vi.mock('../api/hds')

function makeEvent(overrides: Partial<EventDto>): EventDto {
  return {
    id: 'evt-1',
    eventCode: 'EVT-018',
    type: { id: 'type-1', name: 'Casamento' },
    name: 'Casamento Maria & João',
    eventDate: '2026-10-15',
    daytimeWedding: true,
    outdoorWedding: false,
    guestCount: 150,
    description: null,
    amount: 8000,
    sizeGb: 45,
    deliveryStatus: 'PENDING',
    hdId: null,
    eventVenueId: null,
    customerId: null,
    eventProfessionals: [],
    ...overrides,
  }
}

function makeHd(overrides: Partial<HdDto>): HdDto {
  return {
    id: 'hd-1',
    name: 'HD Externo 4',
    capacityGb: 2000,
    usedSpaceGb: 1840,
    physicalLocation: 'Estante A',
    serialNumber: 'SN-88213',
    acquisitionDate: '2025-01-10',
    status: 'ACTIVE',
    ...overrides,
  }
}

function mockSearchEvents(byParams: { period: EventDto[]; pending: EventDto[]; upcoming: EventDto[] }) {
  vi.mocked(eventsApi.searchEvents).mockImplementation(async (params = {}) => {
    if (params.deliveryStatus === 'PENDING') {
      return byParams.pending
    }
    if (params.to) {
      return byParams.period
    }
    return byParams.upcoming
  })
}

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('mostra o indicador de carregamento antes das respostas chegarem', () => {
    mockSearchEvents({ period: [], pending: [], upcoming: [] })
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockReturnValue(new Promise(() => {}))

    renderDashboard()

    expect(screen.getByText('Carregando...')).toBeInTheDocument()
  })

  it('mostra os stat tiles, a lista de próximos eventos e o painel de armazenamento', async () => {
    const upcoming = [
      makeEvent({ id: 'evt-1', eventCode: 'EVT-018', eventDate: '2026-10-15', deliveryStatus: 'PENDING' }),
      makeEvent({ id: 'evt-2', eventCode: 'EVT-020', eventDate: '2026-10-10', name: 'Ensaio externo', deliveryStatus: 'DELIVERED' }),
    ]
    mockSearchEvents({ period: upcoming, pending: [upcoming[0]], upcoming })
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([makeHd({})])

    const { container } = renderDashboard()

    await waitFor(() => expect(screen.getByText('EVT-020')).toBeInTheDocument())

    const statValues = Array.from(container.querySelectorAll('.stat-tile .value')).map((el) => el.textContent)
    expect(statValues).toEqual(['2', '1', '1'])
    expect(screen.getByText('EVT-020')).toBeInTheDocument()
    expect(screen.getByText('EVT-018')).toBeInTheDocument()
    expect(screen.getByText('HD Externo 4')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver todos →' })).toHaveAttribute('href', '/eventos')
  })

  it('calcula a barra de armazenamento pela capacidade real quando preenchida', async () => {
    mockSearchEvents({ period: [], pending: [], upcoming: [] })
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([
      makeHd({ capacityGb: 2000, realCapacityGb: 1000, usedSpaceGb: 960 }),
    ])

    const { container } = renderDashboard()

    await waitFor(() => expect(screen.getByText('960 / 1000 GB')).toBeInTheDocument())
    const fill = container.querySelector('.meter-fill') as HTMLElement
    expect(fill.style.width).toBe('96%')
    expect(fill).toHaveClass('crit')
  })

  it('ordena os próximos eventos por data', async () => {
    const upcoming = [
      makeEvent({ id: 'evt-later', eventCode: 'EVT-020', eventDate: '2026-10-20' }),
      makeEvent({ id: 'evt-earlier', eventCode: 'EVT-018', eventDate: '2026-10-05' }),
    ]
    mockSearchEvents({ period: upcoming, pending: [], upcoming })
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    renderDashboard()

    const codes = await waitFor(() => screen.getAllByText(/EVT-0\d\d/))
    expect(codes.map((el) => el.textContent)).toEqual(['EVT-018', 'EVT-020'])
  })

  it('mostra estados vazios quando não há eventos nem HDs perto da capacidade', async () => {
    mockSearchEvents({ period: [], pending: [], upcoming: [] })
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    renderDashboard()

    await waitFor(() => expect(screen.getByText('Nenhum evento agendado.')).toBeInTheDocument())
    expect(screen.getByText('Nenhum HD perto da capacidade.')).toBeInTheDocument()
  })

  it('mostra mensagem de erro quando alguma chamada falha', async () => {
    vi.mocked(eventsApi.searchEvents).mockRejectedValue(new ApiError(500, 'Falha ao chamar /events/search: 500'))
    vi.mocked(hdsApi.fetchHdsNearCapacity).mockResolvedValue([])

    renderDashboard()

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao chamar /events/search: 500'))
  })
})
