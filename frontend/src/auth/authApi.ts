import { apiFetch } from '../api/client'

interface LoginResponseDto {
  token: string
}

export async function login(username: string, password: string): Promise<string> {
  const response = await apiFetch<LoginResponseDto>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  return response.token
}
