import { apiFetch } from './client'

export interface EventVenueDto {
  id: string
  name: string
  address: string | null
  city: string | null
  state: string | null
  type: string | null
}

export function fetchEventVenues(): Promise<EventVenueDto[]> {
  return apiFetch<EventVenueDto[]>('/event-venues')
}
