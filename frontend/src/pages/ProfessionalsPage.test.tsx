import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '../api/client'
import * as professionalsApi from '../api/professionals'
import type { ProfessionalDto } from '../api/professionals'
import * as professionalTypesApi from '../api/professionalTypes'
import type { ProfessionalTypeDto } from '../api/professionalTypes'
import * as specialtyTagsApi from '../api/specialtyTags'
import type { SpecialtyTagDto } from '../api/specialtyTags'
import { ProfessionalsPage } from './ProfessionalsPage'

vi.mock('../api/professionals')
vi.mock('../api/professionalTypes')
vi.mock('../api/specialtyTags')

const TYPE_PHOTOGRAPHER: ProfessionalTypeDto = { id: 'type-1', name: 'Fotógrafo' }
const TAG_WEDDING: SpecialtyTagDto = { id: 'tag-1', name: 'Casamento' }

function makeProfessional(overrides: Partial<ProfessionalDto> = {}): ProfessionalDto {
  return {
    id: 'prof-1',
    name: 'Renato Ganske',
    type: TYPE_PHOTOGRAPHER,
    contact: '(11) 98888-0000',
    specialtyTags: [TAG_WEDDING],
    otherInfo: null,
    ...overrides,
  }
}

function mockReferenceData() {
  vi.mocked(professionalTypesApi.fetchProfessionalTypes).mockResolvedValue([TYPE_PHOTOGRAPHER])
  vi.mocked(specialtyTagsApi.fetchSpecialtyTags).mockResolvedValue([TAG_WEDDING])
}

describe('ProfessionalsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockReferenceData()
  })

  it('mostra a lista de profissionais numa tabela, com tipo e especialidades', async () => {
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([makeProfessional()])

    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Renato Ganske')).toBeInTheDocument())
    expect(screen.getByText('Fotógrafo')).toBeInTheDocument()
    expect(screen.getByText('Casamento')).toBeInTheDocument()
    expect(screen.getByText('(11) 98888-0000')).toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há profissionais cadastrados', async () => {
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([])

    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Nenhum profissional cadastrado.')).toBeInTheDocument())
  })

  it('mostra mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(professionalsApi.fetchProfessionals).mockRejectedValue(new ApiError(500, 'Falha ao carregar profissionais'))

    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Falha ao carregar profissionais'))
  })

  it('cria um novo profissional pelo modal, selecionando tipo e especialidade existentes', async () => {
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([])
    vi.mocked(professionalsApi.createProfessional).mockResolvedValue(
      makeProfessional({ id: 'prof-new', name: 'Profissional Novo' }),
    )
    const user = userEvent.setup()
    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Nenhum profissional cadastrado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Novo profissional' }))
    const modal = await screen.findByRole('dialog', { name: 'Novo profissional' })
    await user.type(within(modal).getByLabelText('Nome'), 'Profissional Novo')

    await user.type(within(modal).getByRole('combobox', { name: 'Tipo' }), 'Fot')
    await user.click(within(modal).getByRole('option', { name: 'Fotógrafo' }))

    await user.type(within(modal).getByRole('combobox', { name: 'Especialidades' }), 'Casa')
    await user.click(within(modal).getByRole('option', { name: 'Casamento' }))
    expect(within(modal).getByText('Casamento')).toBeInTheDocument()

    await user.click(within(modal).getByRole('button', { name: 'Salvar profissional' }))

    expect(professionalsApi.createProfessional).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Profissional Novo', typeId: 'type-1', specialtyTagIds: ['tag-1'] }),
    )
    await waitFor(() => expect(screen.getByText('Profissional Novo')).toBeInTheDocument())
    expect(screen.queryByRole('dialog', { name: 'Novo profissional' })).not.toBeInTheDocument()
  })

  it('cria um novo tipo de profissional inline via quick-create e o seleciona', async () => {
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([])
    vi.mocked(professionalTypesApi.createProfessionalType).mockResolvedValue({ id: 'type-2', name: 'Videomaker' })
    const user = userEvent.setup()
    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Nenhum profissional cadastrado.')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '+ Novo profissional' }))
    const modal = await screen.findByRole('dialog', { name: 'Novo profissional' })
    const typeField = within(modal).getByText('Tipo').closest('.form-field') as HTMLElement
    await user.click(within(typeField).getByRole('button', { name: '+ Novo' }))

    const quickCreateModal = await screen.findByRole('dialog', { name: 'Novo tipo de profissional' })
    await user.type(within(quickCreateModal).getByLabelText('Nome'), 'Videomaker')
    await user.click(within(quickCreateModal).getByRole('button', { name: 'Salvar tipo' }))

    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Novo tipo de profissional' })).not.toBeInTheDocument())
    expect(within(modal).getByRole('combobox', { name: 'Tipo' })).toHaveValue('Videomaker')
  })

  it('edita um profissional existente pelo modal, pré-preenchendo tipo e especialidades', async () => {
    const professional = makeProfessional()
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([professional])
    vi.mocked(professionalsApi.updateProfessional).mockResolvedValue({ ...professional, name: 'Renato Renomeado' })
    const user = userEvent.setup()
    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Renato Ganske')).toBeInTheDocument())

    const row = screen.getByText('Renato Ganske').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Editar' }))

    const modal = await screen.findByRole('dialog', { name: 'Editar profissional' })
    expect(within(modal).getByLabelText('Nome')).toHaveValue('Renato Ganske')
    expect(within(modal).getByRole('combobox', { name: 'Tipo' })).toHaveValue('Fotógrafo')
    expect(within(modal).getByText('Casamento')).toBeInTheDocument()

    await user.clear(within(modal).getByLabelText('Nome'))
    await user.type(within(modal).getByLabelText('Nome'), 'Renato Renomeado')
    await user.click(within(modal).getByRole('button', { name: 'Salvar profissional' }))

    expect(professionalsApi.updateProfessional).toHaveBeenCalledWith('prof-1', expect.objectContaining({ name: 'Renato Renomeado' }))
    await waitFor(() => expect(screen.getByText('Renato Renomeado')).toBeInTheDocument())
  })

  it('exclui um profissional após confirmação, e não exclui quando cancelado', async () => {
    const professional = makeProfessional()
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([professional])
    vi.mocked(professionalsApi.deleteProfessional).mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Renato Ganske')).toBeInTheDocument())

    const row = screen.getByText('Renato Ganske').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    const dialog = await screen.findByRole('dialog', { name: 'Excluir profissional' })
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }))
    expect(professionalsApi.deleteProfessional).not.toHaveBeenCalled()
    expect(screen.getByText('Renato Ganske')).toBeInTheDocument()

    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialogAgain = await screen.findByRole('dialog', { name: 'Excluir profissional' })
    await user.click(within(dialogAgain).getByRole('button', { name: 'Excluir' }))

    expect(professionalsApi.deleteProfessional).toHaveBeenCalledWith('prof-1')
    await waitFor(() => expect(screen.queryByText('Renato Ganske')).not.toBeInTheDocument())
  })

  it('mantém o modal de exclusão aberto com erro inline quando a exclusão falha', async () => {
    const professional = makeProfessional()
    vi.mocked(professionalsApi.fetchProfessionals).mockResolvedValue([professional])
    vi.mocked(professionalsApi.deleteProfessional).mockRejectedValue(new ApiError(409, 'Profissional vinculado a eventos'))
    const user = userEvent.setup()
    render(<ProfessionalsPage />)

    await waitFor(() => expect(screen.getByText('Renato Ganske')).toBeInTheDocument())

    const row = screen.getByText('Renato Ganske').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))
    const dialog = await screen.findByRole('dialog', { name: 'Excluir profissional' })
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    expect(await within(dialog).findByText('Profissional vinculado a eventos')).toBeInTheDocument()
    expect(screen.getByText('Renato Ganske')).toBeInTheDocument()
  })
})
