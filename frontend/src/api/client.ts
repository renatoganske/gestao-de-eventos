import { clearStoredToken, getStoredToken } from '../auth/tokenStorage'

const API_BASE_URL = '/api'
const LOGIN_PATH = '/auth/login'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getStoredToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init?.headers as Record<string, string> | undefined),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, `Falha de conexão ao chamar ${path}`)
  }

  if (!response.ok) {
    if (response.status === 401 && path !== LOGIN_PATH) {
      clearStoredToken()
      unauthorizedHandler?.()
    }

    const message = await response
      .json()
      .then((body: { message?: string }) => body.message)
      .catch(() => undefined)
    throw new ApiError(response.status, message ?? `Falha ao chamar ${path}: ${response.status}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
