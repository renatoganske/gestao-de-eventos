import { render, screen } from '@testing-library/react'
import { TopBar } from './TopBar'

describe('TopBar', () => {
  it('renderiza o título como heading', () => {
    render(<TopBar title="Dashboard" />)

    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
  })

  it('renderiza a ação quando informada', () => {
    render(<TopBar title="Eventos" action={<button type="button">Novo evento</button>} />)

    expect(screen.getByRole('button', { name: 'Novo evento' })).toBeInTheDocument()
  })

  it('não renderiza a área de ação quando ela não é informada', () => {
    const { container } = render(<TopBar title="Dashboard" />)

    expect(container.querySelector('.topbar-action')).not.toBeInTheDocument()
  })
})
