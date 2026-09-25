import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { clearStoredToken } from '../auth/tokenStorage'
import { useFormDraft } from './useFormDraft'

interface DraftShape {
  titulo: string
}

function ExampleForm({ onSubmit }: { onSubmit?: () => void }) {
  const [draft, setDraft, clearDraft] = useFormDraft<DraftShape>('exemplo-evento', { titulo: '' })

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        clearDraft()
        onSubmit?.()
      }}
    >
      <label htmlFor="titulo">Título</label>
      <input id="titulo" value={draft.titulo} onChange={(event) => setDraft({ titulo: event.target.value })} />
      <button type="submit">Salvar</button>
    </form>
  )
}

describe('useFormDraft', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('restaura o rascunho após remontar o componente (simulando expiração de sessão + novo login)', async () => {
    const user = userEvent.setup()
    const { unmount } = render(<ExampleForm />)

    await user.type(screen.getByLabelText('Título'), 'Casamento João e Maria')

    // sessão expira: token é limpo, mas o rascunho vive numa chave separada em sessionStorage
    clearStoredToken()
    unmount()

    render(<ExampleForm />)

    expect(screen.getByLabelText('Título')).toHaveValue('Casamento João e Maria')
  })

  it('limpa o rascunho após submissão bem-sucedida', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    const { unmount } = render(<ExampleForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Título'), 'Rascunho a descartar')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(onSubmit).toHaveBeenCalled()
    unmount()

    render(<ExampleForm />)
    expect(screen.getByLabelText('Título')).toHaveValue('')
  })

  it('não quebra quando sessionStorage não está disponível', () => {
    const original = window.sessionStorage.setItem
    window.sessionStorage.setItem = () => {
      throw new Error('quota excedida')
    }

    expect(() => render(<ExampleForm />)).not.toThrow()

    window.sessionStorage.setItem = original
  })
})
