import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Sidebar, type SidebarItem } from './Sidebar'

const items: SidebarItem[] = [
  { to: '/', label: 'Dashboard', icon: <span data-testid="icon-dashboard" />, end: true },
  { to: '/eventos', label: 'Eventos', icon: <span data-testid="icon-eventos" /> },
  { to: '/hds', label: 'HDs', icon: <span data-testid="icon-hds" />, badge: 2 },
]

describe('Sidebar', () => {
  it('renderiza um link por item, com rótulo e badge', () => {
    render(
      <MemoryRouter initialEntries={['/eventos']}>
        <Sidebar items={items} />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: /Dashboard/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /HDs/ })).toHaveTextContent('2')
  })

  it('marca apenas o item da rota atual como ativo', () => {
    render(
      <MemoryRouter initialEntries={['/eventos']}>
        <Sidebar items={items} />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: /Eventos/ })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: /Dashboard/ })).not.toHaveAttribute('aria-current')
  })
})
