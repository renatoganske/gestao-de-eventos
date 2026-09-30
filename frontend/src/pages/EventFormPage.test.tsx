import { render, screen, waitFor, within } from '@testing-library/react'
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
    { id: 'type-wedding', name: 'WEDDING', hasWeddingFields: true },
    { id: 'type-other', name: 'PHOTO_SHOOT', hasWeddingFields: false },
  ])
  vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
    { id: 'venue-1', name: 'Buffet Jardim das Rosas', address: null, city: null, state: null, type: null },
  ])
  vi.mocked(hdsApi.fetchHds).mockResolvedValue([
    { id: 'hd-1', name: 'HD Externo 4', capacityGb: 2000, usedSpaceGb: 1000, physicalLocation: null, serialNumber: null, acquisitionDate: null, status: 'ACTIVE' },
  ])
  vi.mocked(customersApi.fetchCustomers).mockResolvedValue([
    { id: 'customer-1', name: 'Maria Silva', contact: null, address: null, notes: null },
  ])
  vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([
    { id: 'prof-1', name: 'Renato Fotógrafo', type: null, contact: null, specialtyTags: [], otherInfo: null },
    { id: 'prof-2', name: 'Ana Videomaker', type: null, contact: null, specialtyTags: [], otherInfo: null },
  ])
  vi.mocked(professionalTypesApi.fetchProfessionalTypes).mockResolvedValue([{ id: 'ptype-1', name: 'Fotógrafo' }])
}

function makeEvent(overrides: Partial<EventDto> = {}): EventDto {
  return {
    id: 'evt-1',
    eventCode: 'EVT-018',
    type: { id: 'type-wedding', name: 'WEDDING' },
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
    eventVenueId: 'venue-1',
    customerId: 'customer-1',
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
        <Route path="/eventos/:id" element={<EventFormPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

// delay: null tira o setTimeout(0) que o userEvent insere entre cada tecla/ação. Com o padrão, o teste de
// criação (~50 teclas + 5 interações) levava ~3s isolado e estourava o timeout de 5s sob carga da suíte completa.
describe('EventFormPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    sessionStorage.clear()
    mockReferenceData()
  })

  it('mostra erros inline e não submete quando campos obrigatórios estão vazios', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar evento' })).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    expect(screen.getByText('Informe o código do evento.')).toBeInTheDocument()
    expect(screen.getByText('Selecione o tipo do evento.')).toBeInTheDocument()
    expect(screen.getByText('Informe o nome do evento.')).toBeInTheDocument()
    expect(screen.getByText('Informe a data do evento.')).toBeInTheDocument()
    expect(eventsApi.createEvent).not.toHaveBeenCalled()
  })

  it('só mostra os campos de casamento quando o tipo selecionado é Casamento', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
    expect(screen.queryByLabelText('Casamento diurno?')).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-wedding')
    expect(screen.getByLabelText('Casamento diurno?')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-other')
    expect(screen.queryByLabelText('Casamento diurno?')).not.toBeInTheDocument()
  })

  it('cria um evento novo com os dados preenchidos e mostra toast de sucesso', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

    await user.type(screen.getByLabelText('Código do evento'), 'EVT-099')
    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-other')
    await user.type(screen.getByLabelText('Nome do evento'), 'Ensaio Externo')
    await user.type(screen.getByLabelText('Data do evento'), '2026-11-20')

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.createEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          eventCode: 'EVT-099',
          eventTypeId: 'type-other',
          name: 'Ensaio Externo',
          eventDate: '2026-11-20',
          daytimeWedding: null,
          outdoorWedding: null,
        }),
      ),
    )
    expect(await screen.findByText('Evento criado com sucesso.')).toBeInTheDocument()
  })

  it('mostra toast de erro quando a submissão falha, sem limpar os campos', async () => {
    vi.mocked(eventsApi.createEvent).mockRejectedValue(new ApiError(500, 'Falha ao salvar evento'))
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

    await user.type(screen.getByLabelText('Código do evento'), 'EVT-099')
    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-other')
    await user.type(screen.getByLabelText('Nome do evento'), 'Ensaio Externo')
    await user.type(screen.getByLabelText('Data do evento'), '2026-11-20')
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    expect(await screen.findByText('Falha ao salvar evento')).toBeInTheDocument()
    expect(screen.getByLabelText('Código do evento')).toHaveValue('EVT-099')
  })

  it('permite cadastrar um cliente inline pelo botão "+ Novo" e seleciona o cliente criado', async () => {
    vi.mocked(customersApi.createCustomer).mockResolvedValue({
      id: 'customer-new',
      name: 'Cliente Novo',
      contact: null,
      address: null,
      notes: null,
    })
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Cliente')).toBeInTheDocument())

    const customerField = screen.getByLabelText('Cliente').closest('.form-field') as HTMLElement
    await user.click(within(customerField).getByRole('button', { name: '+ Novo' }))

    const modal = await screen.findByRole('dialog', { name: 'Novo cliente' })
    await user.type(within(modal).getByLabelText('Nome'), 'Cliente Novo')
    await user.click(within(modal).getByRole('button', { name: 'Salvar cliente' }))

    await waitFor(() =>
      expect(customersApi.createCustomer).toHaveBeenCalledWith({
        name: 'Cliente Novo',
        contact: null,
        address: null,
        notes: null,
      }),
    )
    expect(screen.queryByRole('dialog', { name: 'Novo cliente' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Cliente')).toHaveValue('Cliente Novo')
    expect(await screen.findByText('Cliente criado e selecionado.')).toBeInTheDocument()
  })

  it('permite cadastrar um tipo de evento inline pelo botão "+ Novo" e seleciona o tipo criado', async () => {
    vi.mocked(eventTypesApi.createEventType).mockResolvedValue({ id: 'type-new', name: 'FORMATURA', hasWeddingFields: false })
    const user = userEvent.setup()
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

    const typeField = screen.getByLabelText('Tipo').closest('.form-field') as HTMLElement
    await user.click(within(typeField).getByRole('button', { name: '+ Novo' }))

    const modal = await screen.findByRole('dialog', { name: 'Novo tipo de evento' })
    await user.type(within(modal).getByLabelText('Nome'), 'FORMATURA')
    await user.click(within(modal).getByRole('button', { name: 'Salvar tipo' }))

    await waitFor(() => expect(eventTypesApi.createEventType).toHaveBeenCalledWith({ name: 'FORMATURA', hasWeddingFields: false }))
    expect(screen.queryByRole('dialog', { name: 'Novo tipo de evento' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Tipo')).toHaveValue('type-new')
    expect(screen.getByRole('option', { name: 'FORMATURA' })).toBeInTheDocument()
    expect(await screen.findByText('Tipo criado e selecionado.')).toBeInTheDocument()
  })

  it('cria um tipo já marcado como "usa campos de casamento" e passa a mostrar as flags de casamento (GDE-48)', async () => {
    vi.mocked(eventTypesApi.createEventType).mockResolvedValue({ id: 'type-mini', name: 'Mini Wedding', hasWeddingFields: true })
    const user = userEvent.setup()
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
    expect(screen.queryByLabelText('Casamento diurno?')).not.toBeInTheDocument()

    const typeField = screen.getByLabelText('Tipo').closest('.form-field') as HTMLElement
    await user.click(within(typeField).getByRole('button', { name: '+ Novo' }))

    const modal = await screen.findByRole('dialog', { name: 'Novo tipo de evento' })
    await user.type(within(modal).getByLabelText('Nome'), 'Mini Wedding')
    await user.click(within(modal).getByLabelText('Usa campos de casamento (diurno e ao ar livre)'))
    await user.click(within(modal).getByRole('button', { name: 'Salvar tipo' }))

    await waitFor(() =>
      expect(eventTypesApi.createEventType).toHaveBeenCalledWith({ name: 'Mini Wedding', hasWeddingFields: true }),
    )
    expect(await screen.findByLabelText('Casamento diurno?')).toBeInTheDocument()
    expect(screen.getByLabelText('Casamento ao ar livre?')).toBeInTheDocument()
  })

  it('decide se mostra as flags de casamento pela marcação do tipo, e não pelo nome (GDE-48)', async () => {
    vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([
      { id: 'type-mini', name: 'Mini Wedding', hasWeddingFields: true },
      { id: 'type-legacy', name: 'WEDDING', hasWeddingFields: false },
    ])
    const user = userEvent.setup()
    renderForm('/eventos/novo')
    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())

    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-mini')
    expect(screen.getByLabelText('Casamento diurno?')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-legacy')
    expect(screen.queryByLabelText('Casamento diurno?')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Casamento ao ar livre?')).not.toBeInTheDocument()
  })

  it('exibe no modal o erro de nome vazio e o erro 409 ao criar tipo duplicado', async () => {
    vi.mocked(eventTypesApi.createEventType).mockRejectedValue(new ApiError(409, 'Tipo de evento já existe.'))
    const user = userEvent.setup()
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
    const typeField = screen.getByLabelText('Tipo').closest('.form-field') as HTMLElement
    await user.click(within(typeField).getByRole('button', { name: '+ Novo' }))

    const modal = await screen.findByRole('dialog', { name: 'Novo tipo de evento' })
    await user.click(within(modal).getByRole('button', { name: 'Salvar tipo' }))
    expect(within(modal).getByText('Informe o nome do tipo.')).toBeInTheDocument()
    expect(eventTypesApi.createEventType).not.toHaveBeenCalled()

    await user.type(within(modal).getByLabelText('Nome'), 'WEDDING')
    await user.click(within(modal).getByRole('button', { name: 'Salvar tipo' }))
    expect(await within(modal).findByText('Tipo de evento já existe.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tipo')).toHaveValue('')
  })

  it('carrega o evento existente e reaproveita o mesmo formulário para edição', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent())
    vi.mocked(eventsApi.updateEvent).mockResolvedValue(makeEvent({ name: 'Casamento Atualizado' }))
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/evt-1')

    await waitFor(() => expect(screen.getByLabelText('Nome do evento')).toHaveValue('Casamento Maria & João'))
    expect(screen.getByLabelText('Código do evento')).toHaveValue('EVT-018')
    expect(screen.getByLabelText('Cliente')).toHaveValue('Maria Silva')
    expect(screen.getByLabelText('Casamento diurno?')).toBeChecked()

    await user.clear(screen.getByLabelText('Nome do evento'))
    await user.type(screen.getByLabelText('Nome do evento'), 'Casamento Atualizado')
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.updateEvent).toHaveBeenCalledWith(
        'evt-1',
        expect.objectContaining({ name: 'Casamento Atualizado', eventCode: 'EVT-018' }),
      ),
    )
    expect(await screen.findByText('Evento atualizado com sucesso.')).toBeInTheDocument()
  })
})

describe('EventFormPage - equipe do evento (GDE-37)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    sessionStorage.clear()
    mockReferenceData()
  })

  const EXISTING_TEAM = [
    {
      eventId: 'evt-1',
      eventCode: 'EVT-018',
      eventName: 'Casamento Maria & João',
      professionalId: 'prof-1',
      professionalName: 'Renato Fotógrafo',
      roleInEvent: 'Fotógrafo principal',
    },
  ]

  async function fillBasicFields(user: ReturnType<typeof userEvent.setup>) {
    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
    await user.type(screen.getByLabelText('Código do evento'), 'EVT-099')
    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-other')
    await user.type(screen.getByLabelText('Nome do evento'), 'Ensaio Externo')
    await user.type(screen.getByLabelText('Data do evento'), '2026-11-20')
  }

  async function pickProfessional(user: ReturnType<typeof userEvent.setup>, rowLabel: string, name: string) {
    await user.click(screen.getByLabelText(rowLabel))
    await user.click(await screen.findByRole('option', { name }))
  }

  it('começa sem ninguém na equipe e permite adicionar e remover linhas', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await waitFor(() => expect(screen.getByText('Nenhum profissional adicionado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    expect(screen.getByLabelText('Profissional 1')).toBeInTheDocument()
    expect(screen.getByLabelText('Profissional 2')).toBeInTheDocument()
    expect(screen.queryByText('Nenhum profissional adicionado.')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Remover profissional 1' }))
    expect(screen.getByLabelText('Profissional 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Profissional 2')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Remover profissional 1' }))
    expect(screen.getByText('Nenhum profissional adicionado.')).toBeInTheDocument()
  })

  it('ao criar sem equipe, envia professionals como array vazio, nunca omitido', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() => expect(eventsApi.createEvent).toHaveBeenCalledWith(expect.objectContaining({ professionals: [] })))
  })

  it('envia a equipe montada, com papel quando informado e null quando não', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await pickProfessional(user, 'Profissional 1', 'Renato Fotógrafo')
    await user.type(screen.getByLabelText('Papel 1'), '  Fotógrafo principal  ')
    await pickProfessional(user, 'Profissional 2', 'Ana Videomaker')

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.createEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          professionals: [
            { professionalId: 'prof-1', roleInEvent: 'Fotógrafo principal' },
            { professionalId: 'prof-2', roleInEvent: null },
          ],
        }),
      ),
    )
  })

  it('descarta uma linha totalmente vazia em vez de enviar um profissional em branco', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() => expect(eventsApi.createEvent).toHaveBeenCalledWith(expect.objectContaining({ professionals: [] })))
  })

  it('não oferece em outra linha um profissional que já foi escolhido, evitando a duplicidade', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await waitFor(() => expect(screen.getByRole('button', { name: '+ Adicionar profissional' })).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await pickProfessional(user, 'Profissional 1', 'Renato Fotógrafo')

    await user.click(screen.getByLabelText('Profissional 2'))
    expect(await screen.findByRole('option', { name: 'Ana Videomaker' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Renato Fotógrafo' })).not.toBeInTheDocument()

    // a própria linha continua vendo a sua escolha
    await user.click(screen.getByLabelText('Profissional 1'))
    expect(await screen.findByRole('option', { name: 'Renato Fotógrafo' })).toBeInTheDocument()
  })

  it('mostra um erro e não submete quando uma linha tem papel mas nenhum profissional', async () => {
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await fillBasicFields(user)

    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    await user.type(screen.getByLabelText('Papel 1'), 'Assistente')
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    expect(
      screen.getByText('Selecione o profissional em cada linha que tem um papel, ou remova a linha.'),
    ).toBeInTheDocument()
    expect(eventsApi.createEvent).not.toHaveBeenCalled()
  })

  it('no modo edição, pré-preenche a equipe a partir do evento', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent({ eventProfessionals: EXISTING_TEAM }))
    renderForm('/eventos/evt-1')

    await waitFor(() => expect(screen.getByLabelText('Profissional 1')).toHaveValue('Renato Fotógrafo'))
    expect(screen.getByLabelText('Papel 1')).toHaveValue('Fotógrafo principal')
  })

  it('no modo edição, salvar sem mexer na equipe reenvia a equipe atual, sem omiti-la', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent({ eventProfessionals: EXISTING_TEAM }))
    vi.mocked(eventsApi.updateEvent).mockResolvedValue(makeEvent({ eventProfessionals: EXISTING_TEAM }))
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/evt-1')
    await waitFor(() => expect(screen.getByLabelText('Profissional 1')).toHaveValue('Renato Fotógrafo'))

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.updateEvent).toHaveBeenCalledWith(
        'evt-1',
        expect.objectContaining({ professionals: [{ professionalId: 'prof-1', roleInEvent: 'Fotógrafo principal' }] }),
      ),
    )
  })

  it('no modo edição, remover todas as linhas esvazia a equipe enviando [] (e não omitindo)', async () => {
    vi.mocked(eventsApi.fetchEventById).mockResolvedValue(makeEvent({ eventProfessionals: EXISTING_TEAM }))
    vi.mocked(eventsApi.updateEvent).mockResolvedValue(makeEvent({ eventProfessionals: [] }))
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/evt-1')
    await waitFor(() => expect(screen.getByLabelText('Profissional 1')).toHaveValue('Renato Fotógrafo'))

    await user.click(screen.getByRole('button', { name: 'Remover profissional 1' }))
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.updateEvent).toHaveBeenCalledWith('evt-1', expect.objectContaining({ professionals: [] })),
    )
  })

  it('abre normalmente um rascunho salvo antes de a equipe existir, sem quebrar nem perder o restante', async () => {
    sessionStorage.setItem(
      'gde_draft:event-create',
      JSON.stringify({
        eventCode: 'EVT-777',
        eventTypeId: 'type-other',
        name: 'Rascunho antigo',
        eventDate: '2026-12-01',
        daytimeWedding: false,
        outdoorWedding: false,
        guestCount: '',
        description: '',
        amount: '',
        sizeGb: '',
        deliveryStatus: 'PENDING',
        hdId: '',
        eventVenueId: '',
        customerId: '',
      }),
    )
    vi.mocked(eventsApi.createEvent).mockResolvedValue(makeEvent())
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')

    await waitFor(() => expect(screen.getByLabelText('Nome do evento')).toHaveValue('Rascunho antigo'))
    expect(screen.getByText('Nenhum profissional adicionado.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))

    await waitFor(() =>
      expect(eventsApi.createEvent).toHaveBeenCalledWith(
        expect.objectContaining({ eventCode: 'EVT-777', professionals: [] }),
      ),
    )
  })

  it('cadastra um profissional inline pelo "+ Novo" da linha e o seleciona nela', async () => {
    vi.mocked(professionalsApi.createProfessional).mockResolvedValue({
      id: 'prof-3',
      name: 'Bia Assistente',
      type: { id: 'ptype-1', name: 'Fotógrafo' },
      contact: null,
      specialtyTags: [],
      otherInfo: null,
    })
    const user = userEvent.setup({ delay: null })
    renderForm('/eventos/novo')
    await waitFor(() => expect(screen.getByRole('button', { name: '+ Adicionar profissional' })).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Adicionar profissional' }))
    const row = screen.getByLabelText('Profissional 1').closest('.event-team-row') as HTMLElement
    await user.click(within(row).getByRole('button', { name: '+ Novo' }))

    const modal = await screen.findByRole('dialog', { name: 'Novo profissional' })
    await user.type(within(modal).getByLabelText('Nome'), 'Bia Assistente')
    await user.selectOptions(await within(modal).findByLabelText('Tipo'), 'ptype-1')
    await user.click(within(modal).getByRole('button', { name: 'Salvar profissional' }))

    await waitFor(() =>
      expect(professionalsApi.createProfessional).toHaveBeenCalledWith({
        name: 'Bia Assistente',
        typeId: 'ptype-1',
        contact: null,
        specialtyTagIds: [],
        otherInfo: null,
      }),
    )
    expect(screen.queryByRole('dialog', { name: 'Novo profissional' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Profissional 1')).toHaveValue('Bia Assistente')
    expect(await screen.findByText('Profissional criado e selecionado.')).toBeInTheDocument()
  })
})
