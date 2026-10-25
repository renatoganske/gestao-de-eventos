import { apiFetch } from './client'

/** The dashboard's numbers, counted by the backend (ADR-0029). */
export interface DashboardSummaryDto {
  eventsThisMonth: number
  pendingDeliveries: number
  hdsNearCapacity: number
}

export function fetchDashboardSummary(): Promise<DashboardSummaryDto> {
  return apiFetch<DashboardSummaryDto>('/dashboard/summary')
}
