import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import * as customersApi from '../api/customers'
import * as eventsApi from '../api/events'
import type { EventDto } from '../api/events'
import * as eventTypesApi from '../api/eventTypes'
import * as eventVenuesApi from '../api/eventVenues'
import * as hdsApi from '../api/hds'
import * as professionalsApi from '../api/professionals'
import * as professionalTypesApi from '../api/professionalTypes'
import { EventFormPage } from './EventFormPage'

vi.mock('../api/events')
vi.mock('../api/eventTypes')
vi.mock('../api/eventVenues')
vi.mock('../api/hds')
vi.mock('../api/professionals')
vi.mock('../api/customers')
vi.mock('../api/professionalTypes')

function mockReferenceData() {
  vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([
    { id: 'type-other', name: 'PHOTO_SHOOT', hasWeddingFields: false },
  ])
  vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
    { id: 'venue-1', name: 'Buffet Jardim das Rosas', address: null, city: null, state: null, type: null, eventCount: 0 },
    { id: 'venue-2', name: 'Igreja Matriz', address: null, city: null, state: null, type: null, eventCount: 0 },
  ])
  vi.mocked(hdsApi.fetchHds).mockResolvedValue([])
  vi.mocked(customersApi.fetchCustomers).mockResolvedValue([])
  vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([])
  vi.mocked(professionalTypesApi.fetchProfessionalTypes).mockResolvedValue([])
}

function makeEvent(overrides: Partial<EventDto> = {}): EventDto {
  return {
    id: 'evt-1',
    eventCode: 'EVT-018',
    type: { id: 'type-other', name: 'PHOTO_SHOOT' },
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

function renderForm(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/eventos" element={<div>Lista de eventos</div>} />
        <Route path="/eventos/novo" element={<EventFormPage />} />
        <Route path="/eventos/:id/editar" element={<EventFormPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

async function fillBasicFields(user: ReturnType<typeof userEvent.setup>) {
  await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
  await user.type(screen.getByLabelText('Código do evento'), 'EVT-099')
  await user.selectOptions(screen.getByLabelText('Tipo'), 'type-other')
  await user.type(screen.getByLabelText('Nome do evento'), 'Casamento Maria e Joao')
  await user.type(screen.getByLabelText('Data do evento'), '2026-11-20')
}

async function pickVenue(user: ReturnType<typeof userEvent.setup>, rowLabel: string, name: string) {
  await user.click(screen.getByLabelText(rowLabel))
  await user.click(await screen.findByRole('option', { name }))
}

describe('EventFormPage - vários locais por evento (GDE-51)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    sessionStorage.clear()
    mockReferenceData()
  })

  it('começa sem locais e permite adicionar e remover linhas', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByText('Nenhum local adicionado.')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    expect(screen.getByLabelText('Local 1')).toBeInTheDocument()
    expect(screen.getByLabelText('Local 2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Remover local 2' }))
    expect(screen.queryByLabelText('Local 2')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Remover local 1' }))
    expect(screen.getByText('Nenhum local adicionado.')).toBeInTheDocument()
  })

  it('ao criar sem locais, envia venues como array vazio, nunca omitido', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() => expect(eventsApi.createEvent).toHaveBeenCalledWith(expect.objectContaining({ venues: [] })))
  })

  it('envia cada local com o papel informado e null quando o papel fica em branco', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await pickVenue(user, 'Local 1', 'Igreja Matriz')
    await user.type(screen.getByLabelText('Papel do local 1'), '  Cerimônia ')
    await pickVenue(user, 'Local 2', 'Buffet Jardim das Rosas')
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.createEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          venues: [
            { venueId: 'venue-2', role: 'Cerimônia' },
            { venueId: 'venue-1', role: null },
          ],
        }),
      ),
    )
  })

  it('descarta uma linha totalmente vazia em vez de enviar um local em branco', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() => expect(eventsApi.createEvent).toHaveBeenCalledWith(expect.objectContaining({ venues: [] })))
  })

  it('mostra um erro e não submete quando uma linha tem papel mas nenhum local', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await user.type(screen.getByLabelText('Papel do local 1'), 'Cerimônia')
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    expect(await screen.findByText('Selecione o local em cada linha que tem um papel, ou remova a linha.')).toBeInTheDocument()
    expect(eventsApi.createEvent).not.toHaveBeenCalled()
  })

  it('não oferece em outra linha um local que já foi escolhido', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await waitFor(() => expect(screen.getByRole('button', { name: '+ Adicionar local' })).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    await pickVenue(user, 'Local 1', 'Igreja Matriz')

    await user.click(screen.getByLabelText('Local 2'))
    expect(await screen.findByRole('option', { name: 'Buffet Jardim das Rosas' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Igreja Matriz' })).not.toBeInTheDocument()
  })

  it('no modo edição, pré-preenche os locais com o papel e remover todos envia [] (e não omite)', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(
      makeEvent({
        venues: [
          { venueId: 'venue-2', venueName: 'Igreja Matriz', role: 'Cerimônia' },
          { venueId: 'venue-1', venueName: 'Buffet Jardim das Rosas', role: null },
        ],
      }),
    )
    vi.mocked(eventsApi.updateEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/evt-1/editar')

    await waitFor(() => expect(screen.getByLabelText('Local 1')).toHaveValue('Igreja Matriz'))
    expect(screen.getByLabelText('Papel do local 1')).toHaveValue('Cerimônia')
    expect(screen.getByLabelText('Local 2')).toHaveValue('Buffet Jardim das Rosas')
    expect(screen.getByLabelText('Papel do local 2')).toHaveValue('')

    await user.click(screen.getByRole('button', { name: 'Remover local 2' }))
    await user.click(screen.getByRole('button', { name: 'Remover local 1' }))
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.updateEvent).toHaveBeenCalledWith('evt-1', expect.objectContaining({ venues: [] })),
    )
  })

  it('no modo edição, salvar sem mexer nos locais reenvia os locais atuais, sem omiti-los', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(
      makeEvent({ venues: [{ venueId: 'venue-2', venueName: 'Igreja Matriz', role: 'Cerimônia' }] }),
    )
    vi.mocked(eventsApi.updateEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/evt-1/editar')

    await waitFor(() => expect(screen.getByLabelText('Local 1')).toHaveValue('Igreja Matriz'))
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.updateEvent).toHaveBeenCalledWith(
        'evt-1',
        expect.objectContaining({ venues: [{ venueId: 'venue-2', role: 'Cerimônia' }] }),
      ),
    )
  })

  it('cadastra um local inline pelo "+ Novo" da linha e o seleciona nela', async () => {
    vi.mocked(eventVenuesApi.createEventVenue).mockResolvedValue({
      id: 'venue-3',
      name: 'Quintal VIP',
      address: null,
      city: null,
      state: null,
      type: null,
      eventCount: 0,
    })
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await waitFor(() => expect(screen.getByRole('button', { name: '+ Adicionar local' })).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Adicionar local' }))
    const row = screen.getByLabelText('Local 1').closest('.event-venue-row') as HTMLElement
    await user.click(within(row).getByRole('button', { name: '+ Novo' }))

    const modal = await screen.findByRole('dialog', { name: 'Novo local' })
    await user.type(within(modal).getByLabelText('Nome'), 'Quintal VIP')
    await user.click(within(modal).getByRole('button', { name: 'Salvar local' }))

    await waitFor(() => expect(eventVenuesApi.createEventVenue).toHaveBeenCalled())
    expect(screen.queryByRole('dialog', { name: 'Novo local' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Local 1')).toHaveValue('Quintal VIP')
    expect(await screen.findByText('Local criado e selecionado.')).toBeInTheDocument()
  })

  it('abre normalmente um rascunho salvo quando o evento tinha um único local, sem quebrar', async () => {
    sessionStorage.setItem(
      'gde_draft:event-create',
      JSON.stringify({
        eventCode: 'EVT-778',
        eventTypeId: 'type-other',
        name: 'Rascunho de local único',
        eventDate: '2026-12-01',
        daytimeWedding: false,
        outdoorWedding: false,
        guestCount: '',
        description: '',
        amount: '',
        sizeGb: '',
        deliveryStatus: 'PENDING',
        hdId: '',
        eventVenueId: 'venue-1',
        customers: [],
        professionals: [],
      }),
    )
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Nome do evento')).toHaveValue('Rascunho de local único'))
    expect(screen.getByText('Nenhum local adicionado.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.createEvent).toHaveBeenCalledWith(expect.objectContaining({ eventCode: 'EVT-778', venues: [] })),
    )
  })
})
