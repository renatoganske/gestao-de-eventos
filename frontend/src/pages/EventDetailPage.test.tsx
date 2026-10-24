import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../api/client'
import * as eventsApi from '../api/events'
import type { EventDto } from '../api/events'
import * as hdsApi from '../api/hds'
import { EventDetailPage } from './EventDetailPage'

vi.mock('../api/events', async (importOriginal) => ({
  // Labels and pill mapping stay real; only the requests are mocked.
  ...(await importOriginal<typeof import('../api/events')>()),
  fetchEventById: vi.fn(),
  deleteEvent: vi.fn(),
}))
vi.mock('../api/hds')

function makeEvent(overrides: Partial<EventDto> = {}): EventDto {
  return {
    id: 'evt-1',
    eventCode: 'EVT-018',
    type: { id: 'type-wedding', name: 'Casamento' },
    name: 'Casamento Maria & João',
    eventDate: '2026-10-15',
    daytimeWedding: true,
    outdoorWedding: false,
    guestCount: 120,
    description: 'Cerimônia ao ar livre',
    amount: 5000,
    sizeGb: 40,
    deliveryStatus: 'PENDING',
    hdId: 'hd-1',
    venues: [{ venueId: 'venue-1', venueName: 'Igreja Matriz', role: 'Cerimônia' }],
    customers: [
      { customerId: 'customer-1', customerName: 'Maria Silva', contracting: true },
      { customerId: 'customer-2', customerName: 'João Souza', contracting: false },
    ],
    eventProfessionals: [
      {
        eventId: 'evt-1',
        eventCode: 'EVT-018',
        eventName: 'Casamento Maria & João',
        professionalId: 'prof-1',
        professionalName: 'Ana Videomaker',
        roleInEvent: 'Vídeo',
      },
    ],
    ...overrides,
  }
}

function renderDetail(initialPath = '/eventos/evt-1') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/eventos" element={<div>Lista de eventos</div>} />
        <Route path="/eventos/:id" element={<EventDetailPage />} />
        <Route path="/eventos/:id/editar" element={<div>Formulário de edição</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('EventDetailPage (GDE-64)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    sessionStorage.clear()
    vi.mocked(hdsApi.fetchHd).mockResolvedValue({
      id: 'hd-1', name: 'HD Externo 4', capacityGb: 2000, usedSpaceGb: 1000, physicalLocation: null,
      serialNumber: null, acquisitionDate: null, status: 'ACTIVE', eventCount: 1,
    })
  })

  it('mostra os dados do evento sem nenhum campo editável', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Casamento Maria & João' })).toBeInTheDocument()
    expect(screen.getByText('EVT-018')).toBeInTheDocument()
    expect(screen.getByText('Casamento')).toBeInTheDocument()
    expect(screen.getByText('15/10/2026')).toBeInTheDocument()
    expect(screen.getByText('120')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*5\.000,00/)).toBeInTheDocument()
    expect(screen.getByText('40 GB')).toBeInTheDocument()
    expect(screen.getByText('Pendente')).toBeInTheDocument()
    expect(await screen.findByText('HD Externo 4')).toBeInTheDocument()
    expect(screen.getByText('Cerimônia ao ar livre')).toBeInTheDocument()
    expect(screen.getByText('Igreja Matriz')).toBeInTheDocument()
    expect(screen.getByText(/Cerimônia$/)).toBeInTheDocument()
    expect(screen.getByText('Ana Videomaker')).toBeInTheDocument()
    expect(screen.getByText(/Vídeo/)).toBeInTheDocument()

    const contracting = screen.getByText('Maria Silva').closest('li') as HTMLElement
    expect(within(contracting).getByText('Contratante')).toBeInTheDocument()
    const other = screen.getByText('João Souza').closest('li') as HTMLElement
    expect(within(other).queryByText('Contratante')).not.toBeInTheDocument()

    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
  })

  it('mostra as flags de casamento só quando o evento as usa', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    const { unmount } = renderDetail()
    expect(await screen.findByText('Casamento diurno?')).toBeInTheDocument()
    expect(screen.getByText('Casamento ao ar livre?')).toBeInTheDocument()
    unmount()

    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent({ daytimeWedding: null, outdoorWedding: null }))
    renderDetail()
    await screen.findByRole('heading', { name: 'Casamento Maria & João' })
    expect(screen.queryByText('Casamento diurno?')).not.toBeInTheDocument()
    expect(screen.queryByText('Casamento ao ar livre?')).not.toBeInTheDocument()
  })

  it('mostra estados vazios para clientes, locais e equipe e não busca HD quando não há', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(
      makeEvent({ hdId: null, customers: [], venues: [], eventProfessionals: [] }),
    )
    renderDetail()

    expect(await screen.findByText('Nenhum cliente vinculado.')).toBeInTheDocument()
    expect(screen.getByText('Nenhum local vinculado.')).toBeInTheDocument()
    expect(screen.getByText('Nenhum profissional vinculado.')).toBeInTheDocument()
    expect(hdsApi.fetchHd).not.toHaveBeenCalled()
  })

  it('Editar leva para o formulário e descarta um rascunho antigo desse evento', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    sessionStorage.setItem('gde_draft:event-edit:evt-1', JSON.stringify({ name: 'abandonado' }))
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('link', { name: 'Editar' }))

    expect(await screen.findByText('Formulário de edição')).toBeInTheDocument()
    expect(sessionStorage.getItem('gde_draft:event-edit:evt-1')).toBeNull()
  })

  it('Excluir pede confirmação e, ao confirmar, exclui e volta para a lista', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    vi.mocked(eventsApi.deleteEvent).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/vínculos com 2 clientes e 1 profissional/)).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await screen.findByText('Lista de eventos')).toBeInTheDocument()
    expect(eventsApi.deleteEvent).toHaveBeenCalledWith('evt-1')
  })

  it('cancelar a confirmação não exclui', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Excluir' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(eventsApi.deleteEvent).not.toHaveBeenCalled()
  })

  it('se a exclusão falhar, mostra o erro no diálogo e continua no detalhe', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    vi.mocked(eventsApi.deleteEvent).mockRejectedValue(new ApiError(409, 'Evento em uso.'))
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('Evento em uso.')).toBeInTheDocument()
    expect(screen.queryByText('Lista de eventos')).not.toBeInTheDocument()
  })

  it('evento inexistente mostra mensagem amigável, sem botões de ação', async () => {
    vi.mocked(eventsApi.fetchEventById).mockRejectedValue(new ApiError(404, 'Event not found with id: evt-x'))
    renderDetail('/eventos/evt-x')

    expect(await screen.findByRole('alert')).toHaveTextContent('Evento não encontrado. Ele pode ter sido excluído.')
    expect(screen.queryByRole('link', { name: 'Editar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
  })

  it('ainda mostra o evento se o nome do HD não puder ser carregado', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    vi.mocked(hdsApi.fetchHd).mockRejectedValue(new Error('offline'))
    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Casamento Maria & João' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
