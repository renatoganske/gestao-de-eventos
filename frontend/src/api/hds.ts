import { apiFetch } from './client'

export interface HdDto {
  id: string
  name: string
  capacityGb: number
  usedSpaceGb: number
  physicalLocation: string | null
  serialNumber: string | null
  acquisitionDate: string | null
  status: string
}

export interface CreateHdDto {
  name: string
  capacityGb: number | null
  usedSpaceGb: number | null
  physicalLocation: string | null
  serialNumber: string | null
  acquisitionDate: string | null
  status: string | null
}

export function fetchHdsNearCapacity(): Promise<HdDto[]> {
  return apiFetch<HdDto[]>('/hds/near-capacity')
}

export function fetchHds(): Promise<HdDto[]> {
  return apiFetch<HdDto[]>('/hds')
}

export function createHd(dto: CreateHdDto): Promise<HdDto> {
  return apiFetch<HdDto>('/hds', { method: 'POST', body: JSON.stringify(dto) })
}
