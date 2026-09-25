const TOKEN_KEY = 'gde_token'

interface TokenPayload {
  sub?: string
  exp?: number
}

function decodePayload(token: string): TokenPayload | null {
  const parts = token.split('.')
  if (parts.length !== 3) {
    return null
  }

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((char) => '%' + char.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    )
    return JSON.parse(json) as TokenPayload
  } catch {
    return null
  }
}

export function isTokenValid(token: string | null): token is string {
  if (!token) {
    return false
  }
  const payload = decodePayload(token)
  if (!payload?.exp) {
    return false
  }
  return payload.exp * 1000 > Date.now()
}

export function getStoredToken(): string | null {
  const token = sessionStorage.getItem(TOKEN_KEY)
  return isTokenValid(token) ? token : null
}

export function storeToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearStoredToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}
