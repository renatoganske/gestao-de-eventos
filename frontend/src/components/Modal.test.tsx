import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Modal } from './Modal'

describe('Modal', () => {
  it('renderiza o título e o conteúdo', () => {
    render(
      <Modal title="Novo cliente" onClose={() => {}}>
        <p>Conteúdo do modal</p>
      </Modal>,
    )

    expect(screen.getByRole('dialog', { name: 'Novo cliente' })).toBeInTheDocument()
    expect(screen.getByText('Conteúdo do modal')).toBeInTheDocument()
  })

  it('chama onClose ao clicar no botão de fechar', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <Modal title="Novo cliente" onClose={onClose}>
        <p>Conteúdo</p>
      </Modal>,
    )

    await user.click(screen.getByRole('button', { name: 'Fechar' }))

    expect(onClose).toHaveBeenCalled()
  })

  it('chama onClose ao clicar fora do modal (overlay)', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <Modal title="Novo cliente" onClose={onClose}>
        <p>Conteúdo</p>
      </Modal>,
    )

    await user.click(screen.getByRole('dialog').parentElement as HTMLElement)

    expect(onClose).toHaveBeenCalled()
  })

  it('não chama onClose ao clicar dentro do modal', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <Modal title="Novo cliente" onClose={onClose}>
        <p>Conteúdo</p>
      </Modal>,
    )

    await user.click(screen.getByText('Conteúdo'))

    expect(onClose).not.toHaveBeenCalled()
  })
})
