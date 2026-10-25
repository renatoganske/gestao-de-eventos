import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchHdsNearCapacity, type HdDto } from '../api/hds'
import { ApiError } from '../api/client'
import { fetchDashboardSummary } from '../api/dashboard'
import { DELIVERY_STATUS_LABEL, DELIVERY_STATUS_TO_PILL, fetchUpcomingEvents, type EventListItemDto } from '../api/events'
import { Card } from '../components/Card'
import { Pill } from '../components/Pill'
import { Table } from '../components/Table'
import { TopBar } from '../components/TopBar'
import { effectiveCapacityGb, usagePercent } from '../utils/hdCapacity'
import './DashboardPage.css'

interface DashboardData {
  periodEventCount: number
  pendingCount: number
  upcomingEvents: EventListItemDto[]
  hdsNearCapacity: HdDto[]
}

const GENERIC_ERROR_MESSAGE = 'Não foi possível carregar o dashboard. Tente novamente em instantes.'

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setIsLoading(true)
      setError(null)
      try {
        // The counts and the next five come ready from the backend (ADR-0029), instead of three full
        // event searches counted and cut here. The HD list stays: the panel below shows each of them.
        const [summary, upcomingEvents, hdsNearCapacity] = await Promise.all([
          fetchDashboardSummary(),
          fetchUpcomingEvents(5),
          fetchHdsNearCapacity(),
        ])
        if (cancelled) {
          return
        }
        setData({
          periodEventCount: summary.eventsThisMonth,
          pendingCount: summary.pendingDeliveries,
          upcomingEvents,
          hdsNearCapacity,
        })
      } catch (err) {
        if (cancelled) {
          return
        }
        setError(err instanceof ApiError ? err.message : GENERIC_ERROR_MESSAGE)
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section>
      <TopBar title="Dashboard" />
      <div className="dashboard-content">
        {isLoading && <p>Carregando...</p>}

        {!isLoading && error && (
          <p role="alert" className="dashboard-error">
            {error}
          </p>
        )}

        {!isLoading && !error && data && (
          <>
            <div className="stat-row">
              <div className="stat-tile">
                <span className="label">Eventos no mês</span>
                <span className="value mono">{data.periodEventCount}</span>
              </div>
              <div className="stat-tile">
                <span className="label">Entregas pendentes</span>
                <span className="value mono">{data.pendingCount}</span>
              </div>
              <div className={data.hdsNearCapacity.length > 0 ? 'stat-tile alert' : 'stat-tile'}>
                <span className="label">HDs perto da capacidade</span>
                <span className="value mono">{data.hdsNearCapacity.length}</span>
              </div>
            </div>

            <Card title="Próximos eventos" action={<Link className="card-action" to="/eventos">Ver todos →</Link>}>
              {data.upcomingEvents.length === 0 ? (
                <p className="dashboard-empty">Nenhum evento agendado.</p>
              ) : (
                <Table
                  columns={[
                    { key: 'code', header: 'Código', render: (row) => <span className="mono">{row.eventCode}</span> },
                    { key: 'name', header: 'Evento', render: (row) => row.name },
                    { key: 'date', header: 'Data', render: (row) => <span className="mono">{row.eventDate}</span> },
                    {
                      key: 'status',
                      header: 'Status',
                      render: (row) => (
                        <Pill status={DELIVERY_STATUS_TO_PILL[row.deliveryStatus]}>
                          {DELIVERY_STATUS_LABEL[row.deliveryStatus]}
                        </Pill>
                      ),
                    },
                  ]}
                  rows={data.upcomingEvents}
                  rowKey={(row) => row.id}
                />
              )}
            </Card>

            <Card title="Armazenamento">
              {data.hdsNearCapacity.length === 0 ? (
                <p className="dashboard-empty">Nenhum HD perto da capacidade.</p>
              ) : (
                <div className="storage-panel">
                  {data.hdsNearCapacity.map((hd) => {
                    const percent = usagePercent(hd)
                    return (
                      <div key={hd.id} className="storage-row">
                        <div className="storage-row-head">
                          <span>
                            {hd.name} <span className="mono storage-serial">{hd.serialNumber}</span>
                          </span>
                          <span className="mono">
                            {hd.usedSpaceGb} / {effectiveCapacityGb(hd) ?? '—'} GB
                          </span>
                        </div>
                        <div className="meter">
                          <div
                            className={percent >= 95 ? 'meter-fill crit' : 'meter-fill warn'}
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </Card>
          </>
        )}
      </div>
    </section>
  )
}
