import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as eventVenuesApi from '../api/eventVenues'
import { EventVenueQuickCreateForm } from './EventVenueQuickCreateForm'

vi.mock('../api/eventVenues')

describe('EventVenueQuickCreateForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exige o nome antes de submeter', async () => {
    const user = userEvent.setup()
    render(<EventVenueQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(screen.getByText('Informe o nome do local.')).toBeInTheDocument()
    expect(eventVenuesApi.createEventVenue).not.toHaveBeenCalled()
  })

  it('envia campos em branco como null e chama onCreated com o local criado', async () => {
    const created = { id: 'venue-new', name: 'Salão Central', address: null, city: null, state: null, type: null }
    vi.mocked(eventVenuesApi.createEventVenue).mockResolvedValue(created)
    const onCreated = vi.fn()
    const user = userEvent.setup()
    render(<EventVenueQuickCreateForm onCreated={onCreated} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Salão Central')
    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(eventVenuesApi.createEventVenue).toHaveBeenCalledWith({
      name: 'Salão Central',
      address: null,
      city: null,
      state: null,
      type: null,
    })
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created))
  })

  it('mostra a mensagem de erro da API quando a criação falha', async () => {
    vi.mocked(eventVenuesApi.createEventVenue).mockRejectedValue(new ApiError(500, 'Falha ao criar local'))
    const user = userEvent.setup()
    render(<EventVenueQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Salão Central')
    await user.click(screen.getByRole('button', { name: 'Salvar local' }))

    expect(await screen.findByText('Falha ao criar local')).toBeInTheDocument()
  })
})
