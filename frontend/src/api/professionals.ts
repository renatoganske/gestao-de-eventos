import { apiFetch } from './client'

export interface ProfessionalDto {
  id: string
  name: string
  type: string | null
  contact: string | null
  specialty: string | null
  otherInfo: string | null
}

export function fetchProfessionals(): Promise<ProfessionalDto[]> {
  return apiFetch<ProfessionalDto[]>('/professionals')
}
