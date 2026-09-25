import { NavLink, Route, Routes } from 'react-router-dom'
import { DashboardPage } from './pages/DashboardPage'
import { EventsPage } from './pages/EventsPage'

export default function App() {
  return (
    <div className="app-shell">
      <nav className="app-nav">
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        <NavLink to="/eventos">Eventos</NavLink>
      </nav>
      <main className="app-content">
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/eventos" element={<EventsPage />} />
        </Routes>
      </main>
    </div>
  )
}
