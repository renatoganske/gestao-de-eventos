import { apiFetch } from './client'

export type DeliveryStatus = 'PENDING' | 'DELIVERED' | 'ARCHIVED'

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

export interface SearchEventsParams {
  from?: string
  to?: string
  deliveryStatus?: DeliveryStatus
}

function buildSearchQuery(params: SearchEventsParams): string {
  const query = new URLSearchParams()
  if (params.from) {
    query.set('from', params.from)
  }
  if (params.to) {
    query.set('to', params.to)
  }
  if (params.deliveryStatus) {
    query.set('deliveryStatus', params.deliveryStatus)
  }
  const queryString = query.toString()
  return queryString ? `?${queryString}` : ''
}

export function searchEvents(params: SearchEventsParams = {}): Promise<EventDto[]> {
  return apiFetch<EventDto[]>(`/events/search${buildSearchQuery(params)}`)
}
