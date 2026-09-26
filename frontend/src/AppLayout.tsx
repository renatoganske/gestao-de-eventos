import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'

export function AppLayout() {
  const { logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-header">
        <nav className="app-nav">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/eventos">Eventos</NavLink>
          <NavLink to="/hds">HDs</NavLink>
        </nav>
        <button type="button" onClick={logout}>
          Sair
        </button>
      </header>
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  )
}
