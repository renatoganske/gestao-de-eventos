import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import * as customersApi from '../api/customers'
import * as eventsApi from '../api/events'
import type { EventDto } from '../api/events'
import * as eventTypesApi from '../api/eventTypes'
import * as eventVenuesApi from '../api/eventVenues'
import * as hdsApi from '../api/hds'
import { EventFormPage } from './EventFormPage'

vi.mock('../api/events')
vi.mock('../api/eventTypes')
vi.mock('../api/eventVenues')
vi.mock('../api/hds')
vi.mock('../api/customers')

const CREATED_EVENT: EventDto = {
  id: 'evt-1',
  eventCode: 'EVT-1',
  type: { id: 'type-other', name: 'PHOTO_SHOOT' },
  name: 'Evento anterior',
  eventDate: '2026-11-20',
  daytimeWedding: null,
  outdoorWedding: null,
  guestCount: null,
  description: null,
  amount: null,
  sizeGb: null,
  deliveryStatus: 'PENDING',
  hdId: null,
  eventVenueId: null,
  customerId: 'customer-1',
}

function renderForm() {
  return render(
    <MemoryRouter initialEntries={['/eventos/novo']}>
      <Routes>
        <Route path="/eventos" element={<div>Lista de eventos</div>} />
        <Route path="/eventos/novo" element={<EventFormPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

async function fillCustomer(user: ReturnType<typeof userEvent.setup>) {
  await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
  await user.type(screen.getByLabelText('Nome do evento'), 'Evento anterior')
  await user.click(screen.getByLabelText('Cliente'))
  await user.click(await screen.findByRole('option', { name: 'Maria Silva' }))
  expect(screen.getByLabelText('Cliente')).toHaveValue('Maria Silva')
}

async function expectFreshForm() {
  await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
  expect(screen.getByLabelText('Cliente')).toHaveValue('')
  expect(screen.getByLabelText('Nome do evento')).toHaveValue('')
}

describe('EventFormPage - rascunho de novo evento (GDE-41)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    sessionStorage.clear()
    vi.mocked(eventTypesApi.fetchEventTypes).mockResolvedValue([{ id: 'type-other', name: 'PHOTO_SHOOT', hasWeddingFields: false }])
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([])
    vi.mocked(hdsApi.fetchHds).mockResolvedValue([])
    vi.mocked(customersApi.fetchCustomers).mockResolvedValue([
      { id: 'customer-1', name: 'Maria Silva', contact: null, address: null, notes: null },
    ])
  })

  it('abre vazio (inclusive Cliente) depois de salvar e reabrir', async () => {
    vi.mocked(eventsApi.createEvent).mockResolvedValue(CREATED_EVENT)
    const user = userEvent.setup()
    const first = renderForm()
    await fillCustomer(user)
    await user.type(screen.getByLabelText('Código do evento'), 'EVT-1')
    await user.selectOptions(screen.getByLabelText('Tipo'), 'type-other')
    await user.type(screen.getByLabelText('Data do evento'), '2026-11-20')
    await user.click(screen.getByRole('button', { name: 'Salvar evento' }))
    await screen.findByText('Evento criado com sucesso.')
    first.unmount()

    renderForm()
    await expectFreshForm()
  })

  it('abre vazio depois de sair pelo link "Voltar para eventos"', async () => {
    const user = userEvent.setup()
    const first = renderForm()
    await fillCustomer(user)
    await user.click(screen.getByRole('link', { name: 'Voltar para eventos' }))
    await screen.findByText('Lista de eventos')
    first.unmount()

    renderForm()
    await expectFreshForm()
  })

  it('abre vazio depois de Cancelar', async () => {
    const user = userEvent.setup()
    const first = renderForm()
    await fillCustomer(user)
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    await screen.findByText('Lista de eventos')
    first.unmount()

    renderForm()
    await expectFreshForm()
  })

  it('recupera o rascunho (inclusive Cliente) ao remontar sem sair pela UI, como numa recarga', async () => {
    const user = userEvent.setup()
    const first = renderForm()
    await fillCustomer(user)
    first.unmount()

    renderForm()
    await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeInTheDocument())
    expect(screen.getByLabelText('Nome do evento')).toHaveValue('Evento anterior')
    expect(screen.getByLabelText('Cliente')).toHaveValue('Maria Silva')
  })
})
