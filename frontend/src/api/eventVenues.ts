import { apiFetch } from './client'

export interface EventVenueDto {
  id: string
  name: string
  address: string | null
  city: string | null
  state: string | null
  type: string | null
}

export interface CreateEventVenueDto {
  name: string
  address: string | null
  city: string | null
  state: string | null
  type: string | null
}

export function fetchEventVenues(): Promise<EventVenueDto[]> {
  return apiFetch<EventVenueDto[]>('/event-venues')
}

export function createEventVenue(dto: CreateEventVenueDto): Promise<EventVenueDto> {
  return apiFetch<EventVenueDto>('/event-venues', { method: 'POST', body: JSON.stringify(dto) })
}

export function updateEventVenue(id: string, dto: CreateEventVenueDto): Promise<EventVenueDto> {
  return apiFetch<EventVenueDto>(`/event-venues/${id}`, { method: 'PUT', body: JSON.stringify(dto) })
}

export function deleteEventVenue(id: string): Promise<void> {
  return apiFetch<void>(`/event-venues/${id}`, { method: 'DELETE' })
}
