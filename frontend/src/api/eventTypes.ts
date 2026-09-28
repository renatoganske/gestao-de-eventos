import { apiFetch } from './client'

export interface EventTypeDto {
  id: string
  name: string
}

export function fetchEventTypes(): Promise<EventTypeDto[]> {
  return apiFetch<EventTypeDto[]>('/event-types')
}
