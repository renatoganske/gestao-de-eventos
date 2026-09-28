import { render, screen } from '@testing-library/react'
import { Card } from './Card'

describe('Card', () => {
  it('renderiza o conteúdo sem cabeçalho quando nenhum título é informado', () => {
    render(<Card>Conteúdo</Card>)

    expect(screen.getByText('Conteúdo')).toBeInTheDocument()
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
  })

  it('renderiza título e ação quando informados', () => {
    render(
      <Card title="Próximos eventos" action={<a href="#eventos">Ver todos</a>}>
        Conteúdo
      </Card>,
    )

    expect(screen.getByRole('heading', { name: 'Próximos eventos' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver todos' })).toBeInTheDocument()
  })
})
