import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApiError } from '../api/client'
import { AuthProvider } from './AuthContext'
import * as authApi from './authApi'
import { LoginPage } from './LoginPage'

vi.mock('./authApi')

function renderLoginPage() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Página inicial</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetAllMocks()
  })

  it('faz login com sucesso e navega para a rota de origem', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockResolvedValue('token-valido')

    renderLoginPage()

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'segredo')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(screen.getByText('Página inicial')).toBeInTheDocument())
    expect(authApi.login).toHaveBeenCalledWith('renato', 'segredo')
  })

  it('mostra erro inline para credenciais inválidas sem navegar', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'Invalid credentials'))

    renderLoginPage()

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'errada')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuário ou senha inválidos.')
    expect(screen.queryByText('Página inicial')).not.toBeInTheDocument()
  })

  it('mostra erro inline de falha de conexão quando a chamada não completa', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(0, 'Falha de conexão ao chamar /auth/login'))

    renderLoginPage()

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'segredo')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Falha de conexão. Verifique sua internet e tente novamente.')
  })

  it('mostra erro de falha de conexão (não o status HTTP cru) quando o backend está fora do ar', async () => {
    // Regressão: em dev sem o backend rodando, o proxy do Vite responde 404 em vez de
    // derrubar o fetch -- a tela não deve vazar "Falha ao chamar /auth/login: 404".
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(404, 'Falha ao chamar /auth/login: 404'))

    renderLoginPage()

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'segredo')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Falha de conexão. Verifique sua internet e tente novamente.')
  })

  it('mostra a mensagem do backend quando o login é bloqueado por excesso de tentativas (429)', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(429, 'Too many failed login attempts. Try again in a few minutes.'))

    renderLoginPage()

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'segredo')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Too many failed login attempts. Try again in a few minutes.')
  })

  it('desabilita o botão e mostra "Entrando..." durante o request', async () => {
    const user = userEvent.setup()
    let resolveLogin!: (token: string) => void
    vi.mocked(authApi.login).mockReturnValue(
      new Promise((resolve) => {
        resolveLogin = resolve
      }),
    )

    renderLoginPage()

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'segredo')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    const button = screen.getByRole('button', { name: 'Entrando...' })
    expect(button).toBeDisabled()

    resolveLogin('token-valido')
    await waitFor(() => expect(screen.getByText('Página inicial')).toBeInTheDocument())
  })
})
