import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './AuthContext'
import { ProtectedRoute } from './ProtectedRoute'
import { storeToken } from './tokenStorage'

function makeToken(exp: number): string {
  const header = btoa(JSON.stringify({ alg: 'none' }))
  const payload = btoa(JSON.stringify({ sub: 'renato', exp }))
  return `${header}.${payload}.signature`
}

function renderProtected(initialPath = '/dashboard') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>Tela de login</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<div>Conteúdo protegido</div>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('redireciona para /login sem token, sem chegar a renderizar o conteúdo protegido', () => {
    renderProtected()

    expect(screen.getByText('Tela de login')).toBeInTheDocument()
    expect(screen.queryByText('Conteúdo protegido')).not.toBeInTheDocument()
  })

  it('redireciona para /login quando o token está expirado', () => {
    storeToken(makeToken(Math.floor(Date.now() / 1000) - 3600))

    renderProtected()

    expect(screen.getByText('Tela de login')).toBeInTheDocument()
    expect(screen.queryByText('Conteúdo protegido')).not.toBeInTheDocument()
  })

  it('renderiza o conteúdo protegido com um token válido', () => {
    storeToken(makeToken(Math.floor(Date.now() / 1000) + 3600))

    renderProtected()

    expect(screen.getByText('Conteúdo protegido')).toBeInTheDocument()
    expect(screen.queryByText('Tela de login')).not.toBeInTheDocument()
  })
})
