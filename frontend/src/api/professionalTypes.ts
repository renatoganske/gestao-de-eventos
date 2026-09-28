import { apiFetch } from './client'

export interface ProfessionalTypeDto {
  id: string
  name: string
}

export interface CreateProfessionalTypeDto {
  name: string
}

export function fetchProfessionalTypes(): Promise<ProfessionalTypeDto[]> {
  return apiFetch<ProfessionalTypeDto[]>('/professional-types')
}

export function createProfessionalType(dto: CreateProfessionalTypeDto): Promise<ProfessionalTypeDto> {
  return apiFetch<ProfessionalTypeDto>('/professional-types', { method: 'POST', body: JSON.stringify(dto) })
}
