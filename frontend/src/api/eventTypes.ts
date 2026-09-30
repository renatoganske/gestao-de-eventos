import { apiFetch } from './client'

export interface EventTypeDto {
  id: string
  name: string
  /** Events of this type carry the wedding flags (daytime / outdoor). ADR-0024. */
  hasWeddingFields: boolean
}

export interface CreateEventTypeDto {
  name: string
  hasWeddingFields?: boolean
}

export function fetchEventTypes(): Promise<EventTypeDto[]> {
  return apiFetch<EventTypeDto[]>('/event-types')
}

export function createEventType(dto: CreateEventTypeDto): Promise<EventTypeDto> {
  return apiFetch<EventTypeDto>('/event-types', { method: 'POST', body: JSON.stringify(dto) })
}
