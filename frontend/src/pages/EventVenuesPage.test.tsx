import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { ApiError } from '../api/client'
import * as eventVenuesApi from '../api/eventVenues'
import type { EventVenueDto } from '../api/eventVenues'
import { EventVenuesPage } from './EventVenuesPage'

vi.mock('../api/eventVenues')

function makeVenue(overrides: Partial<EventVenueDto> = {}): EventVenueDto {
  return {
    id: 'venue-1',
    name: 'Buffet Jardim das Rosas',
    address: 'Av. Central, 500',
    city: 'São Paulo',
    state: 'SP',
    type: 'Buffet',
    ...overrides,
  }
}

function columnTexts(columnIndex: number): string[] {
  return screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[columnIndex].textContent ?? '')
}

function EventsDestination() {
  const location = useLocation()
  return <div>Eventos {location.pathname}{location.search}</div>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<EventVenuesPage />} />
        <Route path="/eventos" element={<EventsDestination />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('EventVenuesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('mostra a lista de locais numa tabela', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([makeVenue({})])

    renderPage()

    await waitFor(() => expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument())
    expect(screen.getByText('São Paulo')).toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há locais cadastrados', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([])

    renderPage()

    await waitFor(() => expect(screen.getByText('Nenhum local cadastrado.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockRejectedValue(new ApiError(500, 'Falha ao carregar locais'))

    renderPage()

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao carregar locais'))
  })

  it('cria um novo local pelo modal', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([])
    vi.mocked(eventVenuesApi.createEventVenue).mockResolvedValue(makeVenue({ id: 'venue-new', name: 'Local Novo' }))
    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(screen.getByText('Nenhum local cadastrado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Novo local' }))
    const modal = await screen.findByRole('dialog', { name: 'Novo local' })
    await user.type(within(modal).getByLabelText('Nome'), 'Local Novo')
    await user.click(within(modal).getByRole('button', { name: 'Salvar local' }))

    await waitFor(() => expect(screen.getByText('Local Novo')).toBeInTheDocument())
    expect(screen.queryByRole('dialog', { name: 'Novo local' })).not.toBeInTheDocument()
  })

  it('edita um local existente pelo modal', async () => {
    const venue = makeVenue()
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([venue])
    vi.mocked(eventVenuesApi.updateEventVenue).mockResolvedValue({ ...venue, name: 'Buffet Renomeado' })
    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument())

    const row = screen.getByText('Buffet Jardim das Rosas').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Editar' }))

    const modal = await screen.findByRole('dialog', { name: 'Editar local' })
    expect(within(modal).getByLabelText('Nome')).toHaveValue('Buffet Jardim das Rosas')
    await user.clear(within(modal).getByLabelText('Nome'))
    await user.type(within(modal).getByLabelText('Nome'), 'Buffet Renomeado')
    await user.click(within(modal).getByRole('button', { name: 'Salvar local' }))

    expect(eventVenuesApi.updateEventVenue).toHaveBeenCalledWith('venue-1', expect.objectContaining({ name: 'Buffet Renomeado' }))
    await waitFor(() => expect(screen.getByText('Buffet Renomeado')).toBeInTheDocument())
  })

  it('exclui um local após confirmação, e não exclui quando cancelado', async () => {
    const venue = makeVenue()
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([venue])
    vi.mocked(eventVenuesApi.deleteEventVenue).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument())

    const row = screen.getByText('Buffet Jardim das Rosas').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    const dialog = await screen.findByRole('dialog', { name: 'Excluir local' })
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }))
    expect(eventVenuesApi.deleteEventVenue).not.toHaveBeenCalled()
    expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialogAgain = await screen.findByRole('dialog', { name: 'Excluir local' })
    await user.click(within(dialogAgain).getByRole('button', { name: 'Excluir' }))

    expect(eventVenuesApi.deleteEventVenue).toHaveBeenCalledWith('venue-1')
    await waitFor(() => expect(screen.queryByText('Buffet Jardim das Rosas')).not.toBeInTheDocument())
  })

  it('mantém o modal de exclusão aberto com erro inline quando a exclusão falha', async () => {
    const venue = makeVenue()
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([venue])
    vi.mocked(eventVenuesApi.deleteEventVenue).mockRejectedValue(new ApiError(409, 'Local vinculado a eventos'))
    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument())

    const row = screen.getByText('Buffet Jardim das Rosas').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir local' })
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('Local vinculado a eventos')).toBeInTheDocument()
    expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument()
  })

  it('abre ordenada por nome A-Z e permite ordenar por cidade, estado e tipo', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
      makeVenue({ id: 'v1', name: 'Sítio Verde', city: 'Campinas', state: 'SP', type: 'Sítio' }),
      makeVenue({ id: 'v2', name: 'Buffet Rosa', city: 'Belo Horizonte', state: 'MG', type: 'Buffet' }),
      makeVenue({ id: 'v3', name: 'Igreja Matriz', city: null, state: null, type: null }),
    ])

    renderPage()

    await waitFor(() => expect(screen.getByText('Sítio Verde')).toBeInTheDocument())
    expect(columnTexts(0)).toEqual(['Buffet Rosa', 'Igreja Matriz', 'Sítio Verde'])

    await userEvent.click(screen.getByRole('button', { name: /Cidade/ }))
    expect(columnTexts(0)).toEqual(['Buffet Rosa', 'Sítio Verde', 'Igreja Matriz'])

    await userEvent.click(screen.getByRole('button', { name: /Estado/ }))
    expect(columnTexts(0)).toEqual(['Buffet Rosa', 'Sítio Verde', 'Igreja Matriz'])

    await userEvent.click(screen.getByRole('button', { name: /Tipo/ }))
    expect(columnTexts(0)).toEqual(['Buffet Rosa', 'Sítio Verde', 'Igreja Matriz'])
    expect(eventVenuesApi.fetchEventVenues).toHaveBeenCalledTimes(1)
  })

  it('navega para os eventos do local ao clicar na linha', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([makeVenue()])
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByText('Buffet Jardim das Rosas'))

    expect(await screen.findByText('Eventos /eventos?venueId=venue-1')).toBeInTheDocument()
  })

  it('abre a edição pelo botão Editar sem navegar para os eventos', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([makeVenue()])
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Buffet Jardim das Rosas')
    await user.click(screen.getByRole('button', { name: 'Editar' }))

    expect(await screen.findByRole('dialog', { name: /Editar/ })).toBeInTheDocument()
    expect(screen.queryByText(/^Eventos \//)).not.toBeInTheDocument()
  })

  it('abre a exclusão pelo botão Excluir sem navegar para os eventos', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([makeVenue()])
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Buffet Jardim das Rosas')
    await user.click(screen.getByRole('button', { name: 'Excluir' }))

    expect(await screen.findByRole('dialog', { name: /Excluir/ })).toBeInTheDocument()
    expect(screen.queryByText(/^Eventos \//)).not.toBeInTheDocument()
  })
})

describe('EventVenuesPage - coluna Eventos (GDE-58)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('mostra a contagem de eventos de cada local, inclusive zero', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
      makeVenue({ id: 'v1', name: 'Buffet Rosa', eventCount: 4 }),
      makeVenue({ id: 'v2', name: 'Igreja Matriz', eventCount: 0 }),
    ])

    renderPage()

    await waitFor(() => expect(screen.getByText('Buffet Rosa')).toBeInTheDocument())
    expect(screen.getByRole('columnheader', { name: /Eventos/ })).toBeInTheDocument()
    expect(columnTexts(4)).toEqual(['4', '0'])
  })

  it('ordena pela coluna Eventos e inverte ao clicar de novo', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([
      makeVenue({ id: 'v1', name: 'Buffet Rosa', eventCount: 4 }),
      makeVenue({ id: 'v2', name: 'Igreja Matriz', eventCount: 0 }),
      makeVenue({ id: 'v3', name: 'Sítio Verde', eventCount: 12 }),
    ])
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByText('Buffet Rosa')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: /Eventos/ }))
    expect(columnTexts(0)).toEqual(['Igreja Matriz', 'Buffet Rosa', 'Sítio Verde'])

    await user.click(screen.getByRole('button', { name: /Eventos/ }))
    expect(columnTexts(0)).toEqual(['Sítio Verde', 'Buffet Rosa', 'Igreja Matriz'])
  })
})
