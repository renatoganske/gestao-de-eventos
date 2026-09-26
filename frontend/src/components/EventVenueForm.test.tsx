import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as eventVenuesApi from '../api/eventVenues'
import type { EventVenueDto } from '../api/eventVenues'
import { EventVenueForm } from './EventVenueForm'

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

describe('EventVenueForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exige o nome antes de submeter em modo criação', async () => {
    const user = userEvent.setup()
    render(<EventVenueForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(screen.getByText('Informe o nome do local.')).toBeInTheDocument()
    expect(eventVenuesApi.createEventVenue).not.toHaveBeenCalled()
  })

  it('cria um local novo enviando campos em branco como null', async () => {
    const created = makeVenue({ id: 'venue-new', address: null, city: null, state: null, type: null })
    vi.mocked(eventVenuesApi.createEventVenue).mockResolvedValue(created)
    const onSaved = vi.fn()
    const user = userEvent.setup()
    render(<EventVenueForm onSaved={onSaved} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Buffet Jardim das Rosas')
    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(eventVenuesApi.createEventVenue).toHaveBeenCalledWith({
      name: 'Buffet Jardim das Rosas',
      address: null,
      city: null,
      state: null,
      type: null,
    })
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(created))
  })

  it('pré-preenche os campos e chama updateEventVenue quando um local é passado (modo edição)', async () => {
    const venue = makeVenue()
    const updated = makeVenue({ name: 'Buffet Jardim das Rosas Novo' })
    vi.mocked(eventVenuesApi.updateEventVenue).mockResolvedValue(updated)
    const onSaved = vi.fn()
    const user = userEvent.setup()
    render(<EventVenueForm venue={venue} onSaved={onSaved} onCancel={vi.fn()} />)

    expect(screen.getByLabelText('Nome')).toHaveValue('Buffet Jardim das Rosas')
    expect(screen.getByLabelText('Cidade')).toHaveValue('São Paulo')

    await user.clear(screen.getByLabelText('Nome'))
    await user.type(screen.getByLabelText('Nome'), 'Buffet Jardim das Rosas Novo')
    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(eventVenuesApi.updateEventVenue).toHaveBeenCalledWith('venue-1', expect.objectContaining({ name: 'Buffet Jardim das Rosas Novo' }))
    expect(eventVenuesApi.createEventVenue).not.toHaveBeenCalled()
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(updated))
  })

  it('mostra a mensagem de erro da API quando o salvamento falha', async () => {
    vi.mocked(eventVenuesApi.createEventVenue).mockRejectedValue(new ApiError(500, 'Falha ao salvar local'))
    const user = userEvent.setup()
    render(<EventVenueForm onSaved={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Buffet Jardim das Rosas')
    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(await screen.findByText('Falha ao salvar local')).toBeInTheDocument()
  })
})
