import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { LoginPage } from './auth/LoginPage'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { DashboardPage } from './pages/DashboardPage'
import { EventFormPage } from './pages/EventFormPage'
import { EventsPage } from './pages/EventsPage'
import { HdsPage } from './pages/HdsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/eventos" element={<EventsPage />} />
          <Route path="/eventos/novo" element={<EventFormPage />} />
          <Route path="/eventos/:id" element={<EventFormPage />} />
          <Route path="/hds" element={<HdsPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
