import { apiFetch } from './client'

export interface CustomerDto {
  id: string
  name: string
  contact: string | null
  address: string | null
  notes: string | null
}

export interface CreateCustomerDto {
  name: string
  contact: string | null
  address: string | null
  notes: string | null
}

export function fetchCustomers(): Promise<CustomerDto[]> {
  return apiFetch<CustomerDto[]>('/customers')
}

export function createCustomer(dto: CreateCustomerDto): Promise<CustomerDto> {
  return apiFetch<CustomerDto>('/customers', { method: 'POST', body: JSON.stringify(dto) })
}

export function updateCustomer(id: string, dto: CreateCustomerDto): Promise<CustomerDto> {
  return apiFetch<CustomerDto>(`/customers/${id}`, { method: 'PUT', body: JSON.stringify(dto) })
}

export function deleteCustomer(id: string): Promise<void> {
  return apiFetch<void>(`/customers/${id}`, { method: 'DELETE' })
}
