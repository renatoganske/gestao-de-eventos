import { apiFetch } from './client'

export interface EventTypeDto {
  id: string
  name: string
}

export interface CreateEventTypeDto {
  name: string
}

export function fetchEventTypes(): Promise<EventTypeDto[]> {
  return apiFetch<EventTypeDto[]>('/event-types')
}

export function createEventType(dto: CreateEventTypeDto): Promise<EventTypeDto> {
  return apiFetch<EventTypeDto>('/event-types', { method: 'POST', body: JSON.stringify(dto) })
}
