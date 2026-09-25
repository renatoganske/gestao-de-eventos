import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { AuthProvider } from './AuthContext'
import * as authApi from './authApi'
import { LoginPage } from './LoginPage'
import { ProtectedRoute } from './ProtectedRoute'
import { getStoredToken, storeToken } from './tokenStorage'

vi.mock('./authApi')

function makeToken(exp: number): string {
  const header = btoa(JSON.stringify({ alg: 'none' }))
  const payload = btoa(JSON.stringify({ sub: 'renato', exp }))
  return `${header}.${payload}.signature`
}

function EventsPageCallingProtectedApi() {
  useEffect(() => {
    apiFetch('/events').catch(() => {
      // 401 tratado pelo interceptor global (client.ts); a página só dispara a chamada
    })
  }, [])
  return <div>Tela de eventos</div>
}

describe('expiração de sessão (interceptor de 401)', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetAllMocks()
  })

  it('limpa o token e redireciona para /login preservando a rota de origem ao receber 401', async () => {
    storeToken(makeToken(Math.floor(Date.now() / 1000) + 3600))

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ status: 401, message: 'Authentication required' }), { status: 401 }),
    )

    render(
      <MemoryRouter initialEntries={['/eventos']}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/eventos" element={<EventsPageCallingProtectedApi />} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(screen.getByText('Tela de eventos')).toBeInTheDocument()

    await waitFor(() => expect(screen.getByLabelText('Usuário')).toBeInTheDocument())
    expect(getStoredToken()).toBeNull()

    const user = userEvent.setup()
    vi.mocked(authApi.login).mockResolvedValue(makeToken(Math.floor(Date.now() / 1000) + 3600))

    await user.type(screen.getByLabelText('Usuário'), 'renato')
    await user.type(screen.getByLabelText('Senha'), 'segredo')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(screen.getByText('Tela de eventos')).toBeInTheDocument())
  })
})
