import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
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
    venues: [],
    customers: [],
    eventProfessionals: [],
    ...overrides,
  }
}

function mockReferenceData() {
  vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([{ id: 'type-1', name: 'Casamento', hasWeddingFields: false }])
  vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
    { id: 'venue-1', name: 'Buffet Jardim das Rosas', address: null, city: null, state: null, type: null, eventCount: 0 },
  ])
  vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([
    { id: 'prof-1', name: 'Renato', type: null, contact: null, specialtyTags: [], otherInfo: null, eventCount: 0 },
  ])
  vi.mocked(hdsApi.fetchHds).mockResolvedValue([
    { id: 'hd-1', name: 'HD Externo 4', capacityGb: 2000, usedSpaceGb: 1000, physicalLocation: null, serialNumber: null, acquisitionDate: null, status: 'ACTIVE', eventCount: 0 },
  ])
  vi.mocked(customersApi.fetchCustomer).mockResolvedValue({
    id: 'customer-1', name: 'Maria Silva', contact: null, address: null, notes: null, eventCount: 0,
  })
}

function renderEventsPage(initialPath = '/eventos') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/eventos" element={<EventsPage />} />
        <Route path="/eventos/novo" element={<div>Formulário novo</div>} />
        <Route path="/eventos/:id" element={<div>Detalhe do evento</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

function columnTexts(columnIndex: number): string[] {
  return screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[columnIndex].textContent ?? '')
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
      await screen.findByRole('option', { name: 'Mini Wedding' })

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-mini')
      expect(screen.getByLabelText('Casamento diurno')).toBeInTheDocument()
      expect(screen.getByLabelText('Casamento ao ar livre')).toBeInTheDocument()

      await user.selectOptions(screen.getByLabelText('Tipo'), 'type-legacy')
      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()
    })

    it('esconde os dois filtros enquanto o tipo não for casamento', async () => {
      renderEventsPage()

      await screen.findByRole('option', { name: 'BIRTHDAY' })

      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()

      await userEvent.setup().selectOptions(screen.getByLabelText('Tipo'), 'type-birthday')

      expect(screen.queryByLabelText('Casamento diurno')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Casamento ao ar livre')).not.toBeInTheDocument()
    })

    it('mostra os dois filtros, com Todos/Sim/Não, quando o tipo é casamento', async () => {
      const user = userEvent.setup()
      renderEventsPage()
      await screen.findByRole('option', { name: 'WEDDING' })

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
      await screen.findByRole('option', { name: 'WEDDING' })

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
      await screen.findByRole('option', { name: 'WEDDING' })

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
      await screen.findByRole('option', { name: 'WEDDING' })

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
      await screen.findByRole('option', { name: 'WEDDING' })

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

  it('mostra todos os clientes na coluna Clientes (contratante primeiro), e "—" quando o evento não tem cliente', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([
      makeEvent({
        id: 'evt-with-customer',
        eventCode: 'EVT-018',
        customers: [
          { customerId: 'customer-1', customerName: 'Maria Silva', contracting: true },
          { customerId: 'customer-2', customerName: 'Joao Silva', contracting: false },
        ],
      }),
      makeEvent({ id: 'evt-without-customer', eventCode: 'EVT-019', customers: [] }),
    ])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('Maria Silva, Joao Silva')).toBeInTheDocument())
    expect(screen.getByRole('columnheader', { name: 'Clientes' })).toBeInTheDocument()
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

describe('EventsPage - locais do evento (GDE-51)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockReferenceData()
  })

  it('lista todos os locais do evento, com o papel quando houver', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([
      makeEvent({
        venues: [
          { venueId: 'venue-2', venueName: 'Igreja Matriz', role: 'Cerimônia' },
          { venueId: 'venue-1', venueName: 'Buffet Jardim das Rosas', role: null },
        ],
      }),
    ])
    renderEventsPage()

    expect(await screen.findByText('Igreja Matriz (Cerimônia), Buffet Jardim das Rosas')).toBeInTheDocument()
  })

  it('mostra um traço quando o evento não tem local', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({ venues: [], customers: [] })])
    renderEventsPage()

    await screen.findByText('EVT-018')
    // clientes e locais vazios
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(2)
  })
})

describe('EventsPage - aviso vindo de outra tela (GDE-50)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockReferenceData()
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([])
  })

  it('mostra o toast recebido pelo state da navegação e o limpa do histórico', async () => {
    render(
      <MemoryRouter
        initialEntries={[{ pathname: '/eventos', state: { toast: { kind: 'success', text: 'Evento excluído com sucesso.' } } }]}
      >
        <Routes>
          <Route path="/eventos" element={<EventsPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Evento excluído com sucesso.')).toBeInTheDocument()
  })

  it('sem state na navegação, não mostra toast nenhum', async () => {
    renderEventsPage()

    await screen.findByRole('link', { name: '+ Novo evento' })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('abre ordenada por data decrescente (mais recentes primeiro) e, em empate, pelo número do código (RG2 antes de RG10)', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([
      makeEvent({ id: 'e1', eventCode: 'RG10', eventDate: '2026-05-01' }),
      makeEvent({ id: 'e2', eventCode: 'RG3', eventDate: '2026-06-01' }),
      makeEvent({ id: 'e3', eventCode: 'RG2', eventDate: '2026-05-01' }),
    ])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('RG3')).toBeInTheDocument())
    expect(columnTexts(0)).toEqual(['RG3', 'RG2', 'RG10'])
    expect(screen.getByRole('columnheader', { name: /Data/ })).toHaveAttribute('aria-sort', 'descending')

    await userEvent.click(screen.getByRole('button', { name: /Data/ }))
    expect(columnTexts(0)).toEqual(['RG2', 'RG10', 'RG3'])
  })

  it('ordena pelo código numericamente e não refaz a busca ao ordenar', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([
      makeEvent({ id: 'e1', eventCode: 'RG10' }),
      makeEvent({ id: 'e2', eventCode: 'RG2' }),
    ])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('RG10')).toBeInTheDocument())
    const callsBefore = vi.mocked(eventsApi.searchEvents).mock.calls.length
    await userEvent.click(screen.getByRole('button', { name: /Código/ }))

    expect(columnTexts(0)).toEqual(['RG2', 'RG10'])
    expect(vi.mocked(eventsApi.searchEvents).mock.calls.length).toBe(callsBefore)
  })

  it('torna clientes, locais, tipo e status ordenáveis', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

    renderEventsPage()

    await waitFor(() => expect(screen.getByText('EVT-018')).toBeInTheDocument())
    for (const name of [/Clientes/, /Locais/, /Tipo/, /Status/]) {
      expect(screen.getByRole('button', { name })).toBeInTheDocument()
    }
  })

  describe('filtros vindos da URL (GDE-56)', () => {
    it('filtra por customerId da URL na primeira busca e mostra o cliente ativo', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

      renderEventsPage('/eventos?customerId=customer-1')

      expect(await screen.findByText('Cliente: Maria Silva')).toBeInTheDocument()
      expect(eventsApi.searchEvents).toHaveBeenCalledTimes(1)
      expect(eventsApi.searchEvents).toHaveBeenCalledWith(expect.objectContaining({ customerId: 'customer-1' }))
      expect(customersApi.fetchCustomer).toHaveBeenCalledWith('customer-1')
      expect(customersApi.fetchCustomers).not.toHaveBeenCalled()
    })

    it('mostra o nome do cliente no filtro mesmo quando ele não tem eventos', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([])

      renderEventsPage('/eventos?customerId=customer-1')

      expect(await screen.findByText('Cliente: Maria Silva')).toBeInTheDocument()
    })

    it('usa um texto claro quando o cliente do filtro não é encontrado', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([])
      vi.mocked(customersApi.fetchCustomer).mockRejectedValue(new ApiError(404, 'Cliente não encontrado'))

      renderEventsPage('/eventos?customerId=customer-x')

      expect(await screen.findByText('Cliente: não encontrado')).toBeInTheDocument()
    })

    it('filtra por professionalId, hdId e venueId da URL, preenchendo os campos do painel', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

      renderEventsPage('/eventos?professionalId=prof-1&hdId=hd-1&venueId=venue-1')

      expect(await screen.findByText('Profissional: Renato')).toBeInTheDocument()
      expect(eventsApi.searchEvents).toHaveBeenCalledWith(
        expect.objectContaining({ professionalId: 'prof-1', hdId: 'hd-1', venueId: 'venue-1' }),
      )
      await waitFor(() => expect(screen.getByLabelText('Profissional')).toHaveValue('prof-1'))
      expect(screen.getByLabelText('HD')).toHaveValue('hd-1')
      expect(screen.getByLabelText('Local')).toHaveValue('venue-1')
    })

    it('mostra o estado vazio quando a pessoa não tem eventos', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([])

      renderEventsPage('/eventos?customerId=customer-1')

      expect(await screen.findByText('Nenhum evento encontrado com esses filtros.')).toBeInTheDocument()
    })

    it('"Limpar filtros" remove o filtro vindo da URL e busca de novo sem ele', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])
      const user = userEvent.setup()
      renderEventsPage('/eventos?customerId=customer-1')
      await screen.findByText('Cliente: Maria Silva')

      await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))

      await waitFor(() => expect(screen.queryByText('Cliente: Maria Silva')).not.toBeInTheDocument())
      expect(vi.mocked(eventsApi.searchEvents).mock.lastCall?.[0]?.customerId).toBeUndefined()
    })

    it('sem parâmetros na URL não mostra filtro ativo nem filtra', async () => {
      vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

      renderEventsPage()

      await screen.findByText('EVT-018')
      expect(screen.queryByText(/^Cliente:/)).not.toBeInTheDocument()
      expect(vi.mocked(eventsApi.searchEvents).mock.calls[0][0]?.customerId).toBeUndefined()
      expect(customersApi.fetchCustomer).not.toHaveBeenCalled()
      expect(customersApi.fetchCustomers).not.toHaveBeenCalled()
    })
  })
})

describe('EventsPage - filtro de local vindo da URL (GDE-57)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockReferenceData()
  })

  it('filtra por venueId da URL e mostra o local como filtro ativo', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])

    renderEventsPage('/eventos?venueId=venue-1')

    expect(await screen.findByText('Local: Buffet Jardim das Rosas')).toBeInTheDocument()
    expect(eventsApi.searchEvents).toHaveBeenCalledTimes(1)
    expect(eventsApi.searchEvents).toHaveBeenCalledWith(expect.objectContaining({ venueId: 'venue-1' }))
  })

  it('mostra o estado vazio quando o local não tem eventos', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([])

    renderEventsPage('/eventos?venueId=venue-1')

    expect(await screen.findByText('Nenhum evento encontrado com esses filtros.')).toBeInTheDocument()
  })

  it('"Limpar filtros" remove o local ativo e busca de novo sem ele', async () => {
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([makeEvent({})])
    const user = userEvent.setup()
    renderEventsPage('/eventos?venueId=venue-1')
    await screen.findByText('Local: Buffet Jardim das Rosas')

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))

    await waitFor(() => expect(screen.queryByText(/^Local:/)).not.toBeInTheDocument())
    expect(vi.mocked(eventsApi.searchEvents).mock.lastCall?.[0]?.venueId).toBeUndefined()
  })
})

function CurrentSearch() {
  return <span data-testid="current-search">{useLocation().search}</span>
}

function BackButton() {
  const navigate = useNavigate()
  return <button onClick={() => navigate(-1)}>Voltar</button>
}

function renderWithUrl(initialPath = '/eventos') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <CurrentSearch />
      <BackButton />
      <Routes>
        <Route path="/eventos" element={<EventsPage />} />
        <Route path="/eventos/:id" element={<div>Detalhe do evento</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

function currentSearch() {
  return screen.getByTestId('current-search').textContent
}

describe('EventsPage - filtros e ordenação na URL (GDE-61)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockReferenceData()
    vi.mocked(eventsApi.searchEvents).mockResolvedValue([
      makeEvent({ id: 'e1', eventCode: 'RG10', eventDate: '2026-05-01' }),
      makeEvent({ id: 'e2', eventCode: 'RG2', eventDate: '2026-06-01' }),
    ])
  })

  it('lê filtros, busca livre e ordenação da URL na montagem', async () => {
    renderWithUrl('/eventos?q=Maria&deliveryStatus=DELIVERED&from=2026-01-01&to=2026-12-31&sort=code:desc')

    await screen.findByText('RG10')
    expect(screen.getByLabelText('Buscar por nome do cliente ou código do evento')).toHaveValue('Maria')
    expect(screen.getByLabelText('Status de entrega')).toHaveValue('DELIVERED')
    expect(screen.getByLabelText('Data inicial')).toHaveValue('2026-01-01')
    expect(vi.mocked(eventsApi.searchEvents)).toHaveBeenCalledWith(
      expect.objectContaining({ customerName: 'Maria', deliveryStatus: 'DELIVERED', from: '2026-01-01', to: '2026-12-31' }),
    )
    expect(columnTexts(0)).toEqual(['RG10', 'RG2'])
    expect(screen.getByRole('columnheader', { name: /Código/ })).toHaveAttribute('aria-sort', 'descending')
  })

  it('aplicar filtros grava a URL, e só então, não a cada tecla', async () => {
    const user = userEvent.setup()
    renderWithUrl()
    await screen.findByText('RG10')

    await user.type(screen.getByLabelText('Buscar por nome do cliente ou código do evento'), 'Maria')
    await screen.findByRole('option', { name: 'Pendente' })
    await user.selectOptions(screen.getByLabelText('Status de entrega'), 'PENDING')
    expect(currentSearch()).toBe('')

    await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    await waitFor(() => expect(currentSearch()).toBe('?q=Maria&deliveryStatus=PENDING'))
  })

  it('ordenar grava sort na URL sem refazer a busca, mantendo os filtros aplicados', async () => {
    renderWithUrl('/eventos?deliveryStatus=PENDING')
    await screen.findByText('RG10')
    const callsBefore = vi.mocked(eventsApi.searchEvents).mock.calls.length

    await userEvent.click(screen.getByRole('button', { name: /Código/ }))

    expect(currentSearch()).toBe('?deliveryStatus=PENDING&sort=code%3Aasc')
    expect(columnTexts(0)).toEqual(['RG2', 'RG10'])
    expect(vi.mocked(eventsApi.searchEvents).mock.calls.length).toBe(callsBefore)
  })

  it('a ordenação padrão não suja a URL', async () => {
    renderWithUrl('/eventos?sort=code:asc')
    await screen.findByText('RG10')

    // Data asc (clicking a new column) -> desc (the default, so the URL goes back to empty).
    await userEvent.click(screen.getByRole('button', { name: /Data/ }))
    await userEvent.click(screen.getByRole('button', { name: /Data/ }))

    expect(currentSearch()).toBe('')
  })

  it('ordenar a data de forma crescente grava sort=date:asc na URL', async () => {
    renderWithUrl('/eventos')
    await screen.findByText('RG10')

    await userEvent.click(screen.getByRole('button', { name: /Data/ }))

    expect(currentSearch()).toBe('?sort=date%3Aasc')
    expect(columnTexts(0)).toEqual(['RG10', 'RG2'])
  })

  it('abre em ordem crescente de data quando a URL traz sort=date:asc', async () => {
    renderWithUrl('/eventos?sort=date:asc')
    await screen.findByText('RG10')

    expect(screen.getByRole('columnheader', { name: /Data/ })).toHaveAttribute('aria-sort', 'ascending')
    expect(columnTexts(0)).toEqual(['RG10', 'RG2'])
  })

  it('abrir um evento e voltar mantém filtros, busca e ordenação', async () => {
    const user = userEvent.setup()
    renderWithUrl('/eventos?q=Maria&deliveryStatus=PENDING&sort=code:desc')
    await screen.findByText('RG10')

    await user.click(screen.getByText('RG2'))
    await screen.findByText('Detalhe do evento')
    await user.click(screen.getByRole('button', { name: 'Voltar' }))

    await screen.findByText('RG10')
    expect(currentSearch()).toBe('?q=Maria&deliveryStatus=PENDING&sort=code:desc')
    expect(screen.getByLabelText('Buscar por nome do cliente ou código do evento')).toHaveValue('Maria')
    expect(screen.getByLabelText('Status de entrega')).toHaveValue('PENDING')
    expect(columnTexts(0)).toEqual(['RG10', 'RG2'])
  })

  it('voltar do navegador restaura a visão anterior e refaz a busca', async () => {
    const user = userEvent.setup()
    renderWithUrl('/eventos')
    await screen.findByText('RG10')
    await screen.findByRole('option', { name: 'Pendente' })

    await user.selectOptions(screen.getByLabelText('Status de entrega'), 'PENDING')
    await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    await waitFor(() => expect(currentSearch()).toBe('?deliveryStatus=PENDING'))

    await user.click(screen.getByRole('button', { name: 'Voltar' }))

    await waitFor(() => expect(currentSearch()).toBe(''))
    await waitFor(() => expect(screen.getByLabelText('Status de entrega')).toHaveValue(''))
    expect(vi.mocked(eventsApi.searchEvents).mock.lastCall?.[0]?.deliveryStatus).toBeUndefined()
  })

  it('ignora parâmetros inválidos sem quebrar a tela', async () => {
    renderWithUrl(
      '/eventos?deliveryStatus=XYZ&from=ontem&to=2026-13&daytimeWedding=talvez&customerId=<script>&venueId=a%20b&sort=banana:up',
    )

    await screen.findByText('RG10')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    const params = vi.mocked(eventsApi.searchEvents).mock.calls[0][0]
    expect(params).toEqual(
      expect.objectContaining({
        deliveryStatus: undefined,
        from: undefined,
        daytimeWedding: undefined,
        customerId: undefined,
        venueId: undefined,
      }),
    )
    // Default sort: date descending (most recent first).
    expect(columnTexts(0)).toEqual(['RG2', 'RG10'])
  })

  it('"Limpar filtros" zera os campos e a URL, inclusive a ordenação', async () => {
    const user = userEvent.setup()
    renderWithUrl('/eventos?q=Maria&customerId=customer-1&hdId=hd-1&sort=code:desc')
    await screen.findByText('RG10')

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))

    await waitFor(() => expect(currentSearch()).toBe(''))
    expect(screen.getByLabelText('Buscar por nome do cliente ou código do evento')).toHaveValue('')
    expect(vi.mocked(eventsApi.searchEvents).mock.lastCall?.[0]?.customerId).toBeUndefined()
  })

  it('os parâmetros do GDE-56 continuam filtrando a primeira busca', async () => {
    renderWithUrl('/eventos?customerId=customer-1&professionalId=prof-1&hdId=hd-1&venueId=venue-1')

    await screen.findByText('RG10')
    expect(vi.mocked(eventsApi.searchEvents)).toHaveBeenCalledWith(
      expect.objectContaining({ customerId: 'customer-1', professionalId: 'prof-1', hdId: 'hd-1', venueId: 'venue-1' }),
    )
  })
})
