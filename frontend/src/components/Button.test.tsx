import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Button } from './Button'

describe('Button', () => {
  it('renderiza o texto e responde a clique', async () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Salvar</Button>)

    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(onClick).toHaveBeenCalledOnce()
  })

  it('aplica a classe do variant primary', () => {
    render(<Button variant="primary">Novo evento</Button>)

    expect(screen.getByRole('button', { name: 'Novo evento' })).toHaveClass('btn', 'btn-primary')
  })

  it('aplica a classe do variant ghost', () => {
    render(<Button variant="ghost">Cancelar</Button>)

    expect(screen.getByRole('button', { name: 'Cancelar' })).toHaveClass('btn', 'btn-ghost')
  })

  it('não aplica classe de variant extra no default', () => {
    render(<Button>Padrão</Button>)

    expect(screen.getByRole('button', { name: 'Padrão' }).className.trim()).toBe('btn')
  })

  it('respeita o atributo disabled', () => {
    render(<Button disabled>Entrando...</Button>)

    expect(screen.getByRole('button', { name: 'Entrando...' })).toBeDisabled()
  })
})
