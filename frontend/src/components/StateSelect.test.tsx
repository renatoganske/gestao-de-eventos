import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BRAZILIAN_STATES } from '../constants/brazilianStates'
import { StateSelect } from './StateSelect'

describe('StateSelect', () => {
  it('lista as 27 UFs mais a opção vazia', () => {
    render(<StateSelect id="s" value="" onChange={vi.fn()} />)

    expect(BRAZILIAN_STATES).toHaveLength(27)
    expect(screen.getAllByRole('option')).toHaveLength(28)
    expect(screen.getByRole('option', { name: 'Selecione' })).toHaveValue('')
  })

  it('chama onChange com a sigla escolhida', async () => {
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<StateSelect id="s" value="" onChange={onChange} />)

    await user.selectOptions(screen.getByRole('combobox'), 'SP')

    expect(onChange).toHaveBeenCalledWith('SP')
  })

  it('mantém um valor legado fora da lista como opção selecionada', () => {
    render(<StateSelect id="s" value="Sao Paulo" onChange={vi.fn()} />)

    expect(screen.getAllByRole('option')).toHaveLength(29)
    expect(screen.getByRole('combobox')).toHaveValue('Sao Paulo')
  })

  it('não adiciona opção extra para uma UF válida', () => {
    render(<StateSelect id="s" value="RJ" onChange={vi.fn()} />)

    expect(screen.getAllByRole('option')).toHaveLength(28)
    expect(screen.getByRole('combobox')).toHaveValue('RJ')
  })
})
