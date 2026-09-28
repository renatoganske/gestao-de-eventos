import { apiFetch } from './client'
import type { ProfessionalTypeDto } from './professionalTypes'
import type { SpecialtyTagDto } from './specialtyTags'

export interface ProfessionalDto {
  id: string
  name: string
  type: ProfessionalTypeDto | null
  contact: string | null
  specialtyTags: SpecialtyTagDto[]
  otherInfo: string | null
}

export interface CreateProfessionalDto {
  name: string
  typeId: string | null
  contact: string | null
  specialtyTagIds: string[]
  otherInfo: string | null
}

export function fetchProfessionals(): Promise<ProfessionalDto[]> {
  return apiFetch<ProfessionalDto[]>('/professionals')
}

export function createProfessional(dto: CreateProfessionalDto): Promise<ProfessionalDto> {
  return apiFetch<ProfessionalDto>('/professionals', { method: 'POST', body: JSON.stringify(dto) })
}

export function updateProfessional(id: string, dto: CreateProfessionalDto): Promise<ProfessionalDto> {
  return apiFetch<ProfessionalDto>(`/professionals/${id}`, { method: 'PUT', body: JSON.stringify(dto) })
}

export function deleteProfessional(id: string): Promise<void> {
  return apiFetch<void>(`/professionals/${id}`, { method: 'DELETE' })
}
