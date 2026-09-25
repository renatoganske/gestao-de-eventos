import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { LoginPage } from './auth/LoginPage'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { DashboardPage } from './pages/DashboardPage'
import { EventsPage } from './pages/EventsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/eventos" element={<EventsPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
