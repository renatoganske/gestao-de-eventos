import { apiFetch } from './client'
import type { PillStatus } from '../components/Pill'

export type DeliveryStatus = 'PENDING' | 'DELIVERED' | 'ARCHIVED'

export const DELIVERY_STATUS_LABEL: Record<DeliveryStatus, string> = {
  PENDING: 'Pendente',
  DELIVERED: 'Entregue',
  ARCHIVED: 'Arquivado',
}

export const DELIVERY_STATUS_TO_PILL: Record<DeliveryStatus, PillStatus> = {
  PENDING: 'pending',
  DELIVERED: 'delivered',
  ARCHIVED: 'archived',
}

export interface EventTypeDto {
  id: string
  name: string
}

export interface EventDto {
  id: string
  eventCode: string
  type: EventTypeDto
  name: string
  eventDate: string
  daytimeWedding: boolean | null
  outdoorWedding: boolean | null
  guestCount: number | null
  description: string | null
  amount: number | null
  sizeGb: number | null
  deliveryStatus: DeliveryStatus
  hdId: string | null
  eventVenueId: string | null
  customerId: string | null
}

export interface CreateEventDto {
  eventCode: string
  eventTypeId: string | null
  name: string
  eventDate: string | null
  daytimeWedding: boolean | null
  outdoorWedding: boolean | null
  guestCount: number | null
  description: string | null
  amount: number | null
  sizeGb: number | null
  deliveryStatus: DeliveryStatus
  hdId: string | null
  eventVenueId: string | null
  customerId: string | null
}

export interface SearchEventsParams {
  eventTypeId?: string
  venueId?: string
  professionalId?: string
  from?: string
  to?: string
  hdId?: string
  deliveryStatus?: DeliveryStatus
  customerName?: string
  eventCode?: string
  daytimeWedding?: boolean
  outdoorWedding?: boolean
}

function buildSearchQuery(params: SearchEventsParams): string {
  const query = new URLSearchParams()
  if (params.eventTypeId) {
    query.set('eventTypeId', params.eventTypeId)
  }
  if (params.venueId) {
    query.set('venueId', params.venueId)
  }
  if (params.professionalId) {
    query.set('professionalId', params.professionalId)
  }
  if (params.from) {
    query.set('from', params.from)
  }
  if (params.to) {
    query.set('to', params.to)
  }
  if (params.hdId) {
    query.set('hdId', params.hdId)
  }
  if (params.deliveryStatus) {
    query.set('deliveryStatus', params.deliveryStatus)
  }
  if (params.customerName) {
    query.set('customerName', params.customerName)
  }
  if (params.eventCode) {
    query.set('eventCode', params.eventCode)
  }
  // false is a real filter value ("only explicitly not daytime/outdoor"), so test for undefined, not falsiness.
  if (params.daytimeWedding !== undefined) {
    query.set('daytimeWedding', String(params.daytimeWedding))
  }
  if (params.outdoorWedding !== undefined) {
    query.set('outdoorWedding', String(params.outdoorWedding))
  }
  const queryString = query.toString()
  return queryString ? `?${queryString}` : ''
}

export function searchEvents(params: SearchEventsParams = {}): Promise<EventDto[]> {
  return apiFetch<EventDto[]>(`/events/search${buildSearchQuery(params)}`)
}

export function fetchEventById(id: string): Promise<EventDto> {
  return apiFetch<EventDto>(`/events/${id}`)
}

export function createEvent(dto: CreateEventDto): Promise<EventDto> {
  return apiFetch<EventDto>('/events', { method: 'POST', body: JSON.stringify(dto) })
}

export function updateEvent(id: string, dto: CreateEventDto): Promise<EventDto> {
  return apiFetch<EventDto>(`/events/${id}`, { method: 'PUT', body: JSON.stringify(dto) })
}
