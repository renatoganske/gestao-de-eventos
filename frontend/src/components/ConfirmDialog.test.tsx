import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfirmDialog } from './ConfirmDialog'

describe('ConfirmDialog', () => {
  it('mostra título e mensagem, e chama onConfirm/onCancel', async () => {
    const onConfirm = vi.fn()
    const onCancel = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfirmDialog title="Excluir HD" message="Tem certeza?" confirmLabel="Excluir" onConfirm={onConfirm} onCancel={onCancel} />,
    )

    expect(screen.getByRole('dialog', { name: 'Excluir HD' })).toBeInTheDocument()
    expect(screen.getByText('Tem certeza?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Excluir' }))
    expect(onConfirm).toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onCancel).toHaveBeenCalled()
  })

  it('desabilita os botões e mostra o rótulo de carregamento durante a confirmação', () => {
    render(
      <ConfirmDialog
        title="Excluir HD"
        message="Tem certeza?"
        isConfirming
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    expect(screen.getByRole('button', { name: 'Excluindo...' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  })

  it('mostra a mensagem de erro quando informada', () => {
    render(
      <ConfirmDialog title="Excluir HD" message="Tem certeza?" error="Falha ao excluir" onConfirm={vi.fn()} onCancel={vi.fn()} />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Falha ao excluir')
  })
})
