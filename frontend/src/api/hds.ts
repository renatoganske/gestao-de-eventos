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

export function fetchHdsNearCapacity(): Promise<HdDto[]> {
  return apiFetch<HdDto[]>('/hds/near-capacity')
}
