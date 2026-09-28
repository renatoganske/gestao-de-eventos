import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { setUnauthorizedHandler } from '../api/client'
import { login as loginRequest } from './authApi'
import { clearStoredToken, getStoredToken, storeToken } from './tokenStorage'

interface AuthContextValue {
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken())
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearStoredToken()
      setToken(null)
      navigate('/login', { state: { from: location.pathname + location.search }, replace: true })
    })
    return () => setUnauthorizedHandler(null)
  }, [navigate, location.pathname, location.search])

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: token !== null,
      login: async (username: string, password: string) => {
        const newToken = await loginRequest(username, password)
        storeToken(newToken)
        setToken(newToken)
      },
      logout: () => {
        clearStoredToken()
        setToken(null)
      },
    }),
    [token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
