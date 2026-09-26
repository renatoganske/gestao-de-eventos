import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

describe('EventVenuesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('mostra a lista de locais numa tabela', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([makeVenue({})])

    render(<EventVenuesPage />)

    await waitFor(() => expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument())
    expect(screen.getByText('São Paulo')).toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há locais cadastrados', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([])

    render(<EventVenuesPage />)

    await waitFor(() => expect(screen.getByText('Nenhum local cadastrado.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockRejectedValue(new ApiError(500, 'Falha ao carregar locais'))

    render(<EventVenuesPage />)

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao carregar locais'))
  })

  it('cria um novo local pelo modal', async () => {
    vi.mocked(eventVenuesApi.fetchEventVenues).mockResolvedValue([])
    vi.mocked(eventVenuesApi.createEventVenue).mockResolvedValue(makeVenue({ id: 'venue-new', name: 'Local Novo' }))
    const user = userEvent.setup()
    render(<EventVenuesPage />)

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
    render(<EventVenuesPage />)

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
    render(<EventVenuesPage />)

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
    render(<EventVenuesPage />)

    await waitFor(() => expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument())

    const row = screen.getByText('Buffet Jardim das Rosas').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir local' })
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('Local vinculado a eventos')).toBeInTheDocument()
    expect(screen.getByText('Buffet Jardim das Rosas')).toBeInTheDocument()
  })
})
