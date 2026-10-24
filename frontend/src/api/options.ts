import { apiFetch } from './client'

/** Id and name of a record, for selects and filters that need nothing else (ADR-0029). */
export interface OptionDto {
  id: string
  name: string
}

export function fetchCustomerOptions(): Promise<OptionDto[]> {
  return apiFetch<OptionDto[]>('/customers/options')
}

export function fetchProfessionalOptions(): Promise<OptionDto[]> {
  return apiFetch<OptionDto[]>('/professionals/options')
}

export function fetchEventVenueOptions(): Promise<OptionDto[]> {
  return apiFetch<OptionDto[]>('/event-venues/options')
}

export function fetchHdOptions(): Promise<OptionDto[]> {
  return apiFetch<OptionDto[]>('/hds/options')
}
