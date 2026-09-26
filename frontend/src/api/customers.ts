import { apiFetch } from './client'

export interface CustomerDto {
  id: string
  name: string
  contact: string | null
  address: string | null
  notes: string | null
}

export function fetchCustomers(): Promise<CustomerDto[]> {
  return apiFetch<CustomerDto[]>('/customers')
}
