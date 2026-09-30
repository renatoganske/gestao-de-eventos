import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../api/client'
import * as customersApi from '../api/customers'
import * as eventsApi from '../api/events'
import type { EventDto } from '../api/events'
import * as eventTypesApi from '../api/eventTypes'
import * as eventVenuesApi from '../api/eventVenues'
import * as hdsApi from '../api/hds'
import * as professionalsApi from '../api/professionals'
import { EventsPage } from './EventsPage'

vi.mock('../api/events')
vi.mock('../api/eventTypes')
vi.mock('../api/eventVenues')
vi.mock('../api/professionals')
vi.mock('../api/hds')
vi.mock('../api/customers')

function makeEvent(overrides: Partial<EventDto>): EventDto {
  return {
    id: 'evt-1',
    eventCode: 'EVT-018',
    type: { id: 'type-1', name: 'Casamento' },
    name: 'Casamento Maria & João',
    eventDate: '2026-10-15',
    daytimeWedding: null,
    outdoorWedding: null,
    guestCount: null,
    description: null,
    amount: null,
    sizeGb: null,
    deliveryStatus: 'PENDING',
    hdId: null,
    eventVenueId: null,
    customerId: null,
    eventProfessionals: [],
    ...overrides,
  }
}

function mockReferenceData() {
  vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([{ id: 'type-1', name: 'Casamento', hasWeddingFields: false }])
  vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
    { id: 'venue-1', name: 'Buffet Jardim das Rosas', address: null, city: null, state: null, type: null },
  ])
  vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([
    { id: 'prof-1', name: 'Renato', type: null, contact: null, specialtyTags: [], otherInfo: null },
  ])
  vi.mocked(hdsApi.fetchHds).mockResolvedValue([
    { id: 'hd-1', name: 'HD Externo 4', capacityGb: 2000, usedSpaceGb: 1000, physicalLocation: null, serialNumber: null, acquisitionDate: null, status: 'ACTIVE' },
  ])
  vi.mocked(customersApi.fetchCustomers).mockResolvedValue([
    { id: 'customer-1', name: 'Maria Silva', contact: null, address: null, notes: null },
  ])
}

function renderEventsPage() {
  return render(
    <MemoryRouter initialEntries={['/eventos']}>
      <Routes>
        <Route path="/eventos" element={<EventsPage />} />
        <Route path="/eventos/novo" element={<div>Formulário novo</div>} />
        <Route path="/eventos/:id" element={<div>Detalhe do evento</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('EventsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockReferenceData()
  })

  it('descarta o rascunho de novo evento ao clicar em "+ Novo evento" (GDE-41)', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([])
    sessionStorage.setItem('gde_draft:event-create', JSON.stringify({ customerId: 'customer-1' }))
    sessionStorage.setItem('gde_draft:event-edit:evt-1', JSON.stringify({ name: 'outro' }))
    const user = userEvent.setup()
    renderEventsPage()

    await user.click(await screen.findByRole('link', { name: '+ Novo evento' }))

    expect(await screen.findByText('Formulário novo')).toBeInTheDocument()
    expect(sessionStorage.getItem('gde_draft:event-create')).toBeNull()
    expect(sessionStorage.getItem('gde_draft:event-edit:evt-1')).not.toBeNull()
    sessionStorage.clear()
  })

  it('descarta o rascunho de edicao do evento ao abrir a edicao pela lista, sem tocar nos de outros eventos (GDE-45)', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])
    sessionStorage.setItem('gde_draft:event-edit:evt-1', JSON.stringify({ name: 'abandonado' }))
    sessionStorage.setItem('gde_draft:event-edit:evt-2', JSON.stringify({ name: 'outro' }))
    const user = userEvent.setup()
    renderEventsPage()

    await user.click(await screen.findByText('EVT-018'))

    expect(await screen.findByText('Detalhe do evento')).toBeInTheDocument()
    expect(sessionStorage.getItem('gde_draft:event-edit:evt-1')).toBeNull()
    expect(sessionStorage.getItem('gde_draft:event-edit:evt-2')).not.toBeNull()
    sessionStorage.clear()
  })

  it('busca todos os eventos ao carregar, sem filtro ativo', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('EVT-018')).toBeInTheDocument())

    expect(eventsApi.searchEvents).toHaveBeenCalledWith({
      eventTypeId: undefined,
      venueId: undefined,
      professionalId: undefined,
      from: undefined,
      to: undefined,
      hdId: undefined,
      deliveryStatus: undefined,
    })
  })

  it('mostra o estado vazio quando não há eventos', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('Nenhum evento encontrado com esses filtros.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando a busca falha', async () => {
    vi.mocked(eventsApi.searchEvents).mockRejectedValue(new ApiError(500, 'Falha ao chamar /events/search: 500'))

    renderEventsPage()

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao chamar /events/search: 500'))
  })

  it('combina o texto livre com customerName e eventCode, sem duplicar resultados', async () => {
    const shared = makeEvent({ id: 'evt-shared', eventCode: 'EVT-018', name: 'Casamento Maria & João' })
    const onlyByCode = makeEvent({ id: 'evt-code', eventCode: 'EVT-020', name: 'Ensaio' })

    vi.mocked(eventsApi.searchEvents).mockImplementation(async (params = {}) => {
      if (params.customerName === 'maria') {
        return [shared]
      }
      if (params.eventCode === 'maria') {
        return [shared, onlyByCode]
      }
      return []
    })

    const user = userEvent.setup()
    renderEventsPage()

    await waitFor(() => expect(screen.getByRole('button', { name: 'Aplicar filtros' })).toBeInTheDocument())

    await user.type(screen.getByLabelText('Buscar por nome do cliente ou código do evento'), 'maria')
    await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    await waitFor(() => expect(screen.getAllByText(/EVT-0\d\d/)).toHaveLength(2))
    expect(eventsApi.searchEvents).toHaveBeenCalledWith(expect.objectContaining({ customerName: 'maria' }))
    expect(eventsApi.searchEvents).toHaveBeenCalledWith(expect.objectContaining({ eventCode: 'maria' }))
  })

  it('aplica o filtro estruturado de status de entrega junto da busca', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

    const user = userEvent.setup()
    renderEventsPage()

    await waitFor(() => expect(screen.getByLabelText('Status de entrega')).toBeInTheDocument())

    await user.selectOptions(screen.getByLabelText('Status de entrega'), 'DELIVERED')
    await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    await waitFor(() =>
      expect(eventsApi.searchEvents).toHaveBeenLastCalledWith(expect.objectContaining({ deliveryStatus: 'DELIVERED' })),
    )
  })

  it('limpar filtros reseta o texto livre, os filtros estruturados e busca tudo de novo', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

    const user = userEvent.setup()
    renderEventsPage()

    await waitFor(() => expect(screen.getByLabelText('Status de entrega')).toBeInTheDocument())

    await user.type(screen.getByLabelText('Buscar por nome do cliente ou código do evento'), 'maria')
    await user.selectOptions(screen.getByLabelText('Status de entrega'), 'DELIVERED')
    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))

    expect(screen.getByLabelText('Buscar por nome do cliente ou código do evento')).toHaveValue('')
    expect(screen.getByLabelText('Status de entrega')).toHaveValue('')
    await waitFor(() =>
      expect(eventsApi.searchEvents).toHaveBeenLastCalledWith({
        eventTypeId: undefined,
        venueId: undefined,
        professionalId: undefined,
        from: undefined,
        to: undefined,
        hdId: undefined,
        deliveryStatus: undefined,
      }),
    )
  })

  describe('filtros de casamento diurno e ao ar livre (GDE-47)', () => {
    beforeEach(() => {
      vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([
        { id: 'type-wedding', name: 'WEDDING', hasWeddingFields: true },
        { id: 'type-birthday', name: 'BIRTHDAY', hasWeddingFields: false },
      ])
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])
    })

    it('mostra os filtros para qualquer tipo marcado com campos de casamento, mesmo com outro nome, e não pelo nome (GDE-48)', async () => {
      vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([
        { id: 'type-mini', name: 'Mini Wedding', hasWeddingFields: true },
        { id: 'type-legacy', name: 'WEDDING', hasWeddingFields: false },
      ])
      const user = userEvent.setup()
      renderEventsPage()
      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-mini')
      expect(screen.getByLabelText('Casamento diurno')).toBeInTheDocument()
      expect(screen.getByLabelText('Casamento ao ar livre')).toBeInTheDocument()

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-legacy')
      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()
    })

    it('esconde os dois filtros enquanto o tipo não for casamento', async () => {
      renderEventsPage()

      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()

      await userEvent.setup().selectOptions(screen.getByLabelText('Tipo'), 'type-birthday')

      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()
    })

    it('mostra os dois filtros, com Todos/Sim/Não, quando o tipo é casamento', async () => {
      const user = userEvent.setup()
      renderEventsPage()
      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')

      for (const label of ['Casamento diurno', 'Casamento ao ar livre']) {
        const select = screen.getByLabelText(label)
        expect(select).toHaveValue('')
        expect(Array.from(select.querySelectorAll('option')).map((option) => option.textContent)).toEqual([
          'Todos',
          'Sim',
          'Não',
        ])
      }
    })

    it('envia true e false à busca, sem descartar o Não', async () => {
      const user = userEvent.setup()
      renderEventsPage()
      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')
      await user.selectOptions(screen.getByLabelText('Casamento diurno'), 'true')
      await user.selectOptions(screen.getByLabelText('Casamento ao ar livre'), 'false')
      await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

      await waitFor(() =>
        expect(eventsApi.searchEvents).toHaveBeenLastCalledWith(
          expect.objectContaining({ eventTypeId: 'type-wedding', daytimeWedding: true, outdoorWedding: false }),
        ),
      )
    })

    it('não envia os parâmetros de casamento enquanto ficarem em Todos', async () => {
      const user = userEvent.setup()
      renderEventsPage()
      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')
      await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

      await waitFor(() =>
        expect(eventsApi.searchEvents).toHaveBeenLastCalledWith(
          expect.objectContaining({ eventTypeId: 'type-wedding', daytimeWedding: undefined, outdoorWedding: undefined }),
        ),
      )
    })

    it('limpa os dois filtros e os esconde ao trocar o tipo para um que não é casamento', async () => {
      const user = userEvent.setup()
      renderEventsPage()
      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')
      await user.selectOptions(screen.getByLabelText('Casamento ao ar livre'), 'true')
      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-birthday')

      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
      await waitFor(() =>
        expect(eventsApi.searchEvents).toHaveBeenLastCalledWith(
          expect.objectContaining({ eventTypeId: 'type-birthday', outdoorWedding: undefined }),
        ),
      )

      // voltando para casamento, o filtro reaparece zerado (não "lembra" o Sim anterior)
      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')
      expect(screen.getByLabelText('Casamento ao ar livre')).toHaveValue('')
    })

    it('"Limpar filtros" zera e esconde os filtros de casamento', async () => {
      const user = userEvent.setup()
      renderEventsPage()
      await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')
      await user.selectOptions(screen.getByLabelText('Casamento diurno'), 'true')
      await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))

      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      await waitFor(() =>
        expect(eventsApi.searchEvents).toHaveBeenLastCalledWith(
          expect.objectContaining({ eventTypeId: undefined, daytimeWedding: undefined, outdoorWedding: undefined }),
        ),
      )
    })
  })

  it('mostra o nome do cliente na coluna Cliente, e "—" quando o evento não tem cliente vinculado', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([
      makeEvent({ id: 'evt-with-customer', eventCode: 'EVT-018', customerId: 'customer-1' }),
      makeEvent({ id: 'evt-without-customer', eventCode: 'EVT-019', customerId: null }),
    ])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('Maria Silva')).toBeInTheDocument())
    expect(screen.getByRole('columnheader', { name: 'Cliente' })).toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Evento' })).not.toBeInTheDocument()

    const rows = screen.getAllByRole('row')
    expect(rows[2]).toHaveTextContent('—')
  })

  it('navega para o detalhe do evento ao clicar numa linha', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({ id: 'evt-42', eventCode: 'EVT-042' })])

    const user = userEvent.setup()
    renderEventsPage()

    await waitFor(() => expect(screen.getByText('EVT-042')).toBeInTheDocument())
    await user.click(screen.getByText('EVT-042'))

    expect(screen.getByText('Detalhe do evento')).toBeInTheDocument()
  })
})
