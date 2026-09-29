import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import { Checkbox } from './Checkbox'

describe('Checkbox', () => {
  it('associa a label ao input e reflete o estado checked', () => {
    render(<Checkbox label="Ao ar livre?" checked onChange={() => {}} />)

    expect(screen.getByLabelText('Ao ar livre?')).toBeChecked()
  })

  it('chama onChange com o novo valor ao clicar na label', async () => {
    const onChange = vi.fn()
    render(<Checkbox label="Diurno?" checked={false} onChange={onChange} />)

    await userEvent.click(screen.getByText('Diurno?'))

    expect(onChange).toHaveBeenCalledWith(true)
  })

  it('alterna via teclado (Tab + Espaço)', async () => {
    const onChange = vi.fn()
    render(<Checkbox label="Diurno?" checked={false} onChange={onChange} />)

    await userEvent.tab()
    expect(screen.getByLabelText('Diurno?')).toHaveFocus()
    await userEvent.keyboard(' ')

    expect(onChange).toHaveBeenCalledWith(true)
  })

  it('não dispara onChange quando desabilitado', async () => {
    const onChange = vi.fn()
    render(<Checkbox label="Diurno?" checked={false} onChange={onChange} disabled />)

    await userEvent.click(screen.getByText('Diurno?'))

    expect(screen.getByLabelText('Diurno?')).toBeDisabled()
    expect(onChange).not.toHaveBeenCalled()
  })
})
