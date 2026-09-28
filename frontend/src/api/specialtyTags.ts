import { apiFetch } from './client'

export interface SpecialtyTagDto {
  id: string
  name: string
}

export interface CreateSpecialtyTagDto {
  name: string
}

export function fetchSpecialtyTags(): Promise<SpecialtyTagDto[]> {
  return apiFetch<SpecialtyTagDto[]>('/specialty-tags')
}

export function createSpecialtyTag(dto: CreateSpecialtyTagDto): Promise<SpecialtyTagDto> {
  return apiFetch<SpecialtyTagDto>('/specialty-tags', { method: 'POST', body: JSON.stringify(dto) })
}
