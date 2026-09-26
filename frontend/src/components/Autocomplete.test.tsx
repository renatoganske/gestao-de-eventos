import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Autocomplete } from './Autocomplete'

const OPTIONS = [
  { id: 'c1', label: 'Maria Silva' },
  { id: 'c2', label: 'João Souza' },
]

describe('Autocomplete', () => {
  it('mostra o rótulo da opção selecionada quando value é informado', () => {
    render(<Autocomplete id="customer" options={OPTIONS} value="c1" onChange={() => {}} />)

    expect(screen.getByRole('combobox')).toHaveValue('Maria Silva')
  })

  it('filtra as opções conforme o usuário digita e seleciona uma delas', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Autocomplete id="customer" options={OPTIONS} value="" onChange={onChange} />)

    await user.type(screen.getByRole('combobox'), 'jo')

    expect(screen.getByRole('option', { name: 'João Souza' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Maria Silva' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('option', { name: 'João Souza' }))

    expect(onChange).toHaveBeenCalledWith('c2')
    expect(screen.getByRole('combobox')).toHaveValue('João Souza')
  })

  it('limpa a seleção quando o campo de busca é esvaziado', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Autocomplete id="customer" options={OPTIONS} value="c1" onChange={onChange} />)

    await user.clear(screen.getByRole('combobox'))

    expect(onChange).toHaveBeenCalledWith('')
  })

  it('mostra o botão "+ Novo" e aciona onCreateNew ao clicar', async () => {
    const user = userEvent.setup()
    const onCreateNew = vi.fn()
    render(<Autocomplete id="customer" options={OPTIONS} value="" onChange={() => {}} onCreateNew={onCreateNew} />)

    await user.click(screen.getByRole('button', { name: '+ Novo' }))

    expect(onCreateNew).toHaveBeenCalled()
  })
})
