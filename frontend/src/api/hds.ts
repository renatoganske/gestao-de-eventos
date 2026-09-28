import { apiFetch } from './client'

export type HdStatus = 'ACTIVE' | 'FULL' | 'DEFECTIVE' | 'ARCHIVED'

export const HD_STATUS_LABEL: Record<HdStatus, string> = {
  ACTIVE: 'Ativo',
  FULL: 'Cheio',
  DEFECTIVE: 'Defeituoso',
  ARCHIVED: 'Arquivado',
}

export interface HdDto {
  id: string
  name: string
  capacityGb: number
  usedSpaceGb: number
  physicalLocation: string | null
  serialNumber: string | null
  acquisitionDate: string | null
  status: HdStatus
}

export interface CreateHdDto {
  name: string
  capacityGb: number | null
  usedSpaceGb: number | null
  physicalLocation: string | null
  serialNumber: string | null
  acquisitionDate: string | null
  status: HdStatus | null
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

export function updateHd(id: string, dto: CreateHdDto): Promise<HdDto> {
  return apiFetch<HdDto>(`/hds/${id}`, { method: 'PUT', body: JSON.stringify(dto) })
}

export function deleteHd(id: string): Promise<void> {
  return apiFetch<void>(`/hds/${id}`, { method: 'DELETE' })
}
