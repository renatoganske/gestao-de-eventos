import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as professionalsApi from '../api/professionals'
import * as professionalTypesApi from '../api/professionalTypes'
import { ProfessionalQuickCreateForm } from './ProfessionalQuickCreateForm'

vi.mock('../api/professionals')
vi.mock('../api/professionalTypes')

const CREATED = {
  id: 'prof-9',
  name: 'Carla Assistente',
  type: null,
  contact: null,
  specialtyTags: [],
  otherInfo: null,
}

describe('ProfessionalQuickCreateForm', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(professionalTypesApi.fetchProfessionalTypes).mockResolvedValue([{ id: 'ptype-1', name: 'Fotógrafo' }])
  })

  it('exige o nome e não chama a API sem ele', async () => {
    const user = userEvent.setup()
    render(<ProfessionalQuickCreateForm onCreated={vi.fn()} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Salvar profissional' }))

    expect(screen.getByText('Informe o nome do profissional.')).toBeInTheDocument()
    expect(professionalsApi.createProfessional).not.toHaveBeenCalled()
  })

  it('cria só com o nome, sem tipo nem contato, e devolve o profissional criado', async () => {
    vi.mocked(professionalsApi.createProfessional).mockResolvedValue(CREATED)
    const onCreated = vi.fn()
    const user = userEvent.setup()
    render(<ProfessionalQuickCreateForm onCreated={onCreated} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), '  Carla Assistente  ')
    await user.click(screen.getByRole('button', { name: 'Salvar profissional' }))

    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(CREATED))
    expect(professionalsApi.createProfessional).toHaveBeenCalledWith({
      name: 'Carla Assistente',
      typeId: null,
      contact: null,
      specialtyTagIds: [],
      otherInfo: null,
    })
  })

  it('mostra a mensagem da API quando a criação falha e não fecha o formulário', async () => {
    vi.mocked(professionalsApi.createProfessional).mockRejectedValue(new ApiError(409, 'Já existe um registro com esse nome.'))
    const onCreated = vi.fn()
    const user = userEvent.setup()
    render(<ProfessionalQuickCreateForm onCreated={onCreated} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Carla')
    await user.click(screen.getByRole('button', { name: 'Salvar profissional' }))

    expect(await screen.findByText('Já existe um registro com esse nome.')).toBeInTheDocument()
    expect(onCreated).not.toHaveBeenCalled()
  })

  it('não impede o cadastro quando a lista de tipos falha ao carregar', async () => {
    vi.mocked(professionalTypesApi.fetchProfessionalTypes).mockRejectedValue(new Error('rede'))
    vi.mocked(professionalsApi.createProfessional).mockResolvedValue(CREATED)
    const onCreated = vi.fn()
    const user = userEvent.setup()
    render(<ProfessionalQuickCreateForm onCreated={onCreated} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nome'), 'Carla Assistente')
    await user.click(screen.getByRole('button', { name: 'Salvar profissional' }))

    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(CREATED))
  })

  it('chama onCancel ao cancelar', async () => {
    const onCancel = vi.fn()
    const user = userEvent.setup()
    render(<ProfessionalQuickCreateForm onCreated={vi.fn()} onCancel={onCancel} />)

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onCancel).toHaveBeenCalled()
  })
})
