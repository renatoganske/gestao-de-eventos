import { clearStoredToken, getStoredToken, isTokenValid, storeToken } from './tokenStorage'

function makeToken(exp: number): string {
  const header = btoa(JSON.stringify({ alg: 'none' }))
  const payload = btoa(JSON.stringify({ sub: 'renato', exp }))
  return `${header}.${payload}.signature`
}

describe('tokenStorage', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('considera válido um token com exp no futuro', () => {
    const token = makeToken(Math.floor(Date.now() / 1000) + 3600)
    expect(isTokenValid(token)).toBe(true)
  })

  it('considera inválido um token expirado', () => {
    const token = makeToken(Math.floor(Date.now() / 1000) - 3600)
    expect(isTokenValid(token)).toBe(false)
  })

  it('considera inválido um token malformado', () => {
    expect(isTokenValid('nao-e-um-jwt')).toBe(false)
  })

  it('considera inválido null', () => {
    expect(isTokenValid(null)).toBe(false)
  })

  it('getStoredToken retorna o token guardado se ainda válido', () => {
    const token = makeToken(Math.floor(Date.now() / 1000) + 3600)
    storeToken(token)
    expect(getStoredToken()).toBe(token)
  })

  it('getStoredToken retorna null se o token guardado expirou', () => {
    const token = makeToken(Math.floor(Date.now() / 1000) - 3600)
    storeToken(token)
    expect(getStoredToken()).toBeNull()
  })

  it('clearStoredToken remove o token', () => {
    storeToken(makeToken(Math.floor(Date.now() / 1000) + 3600))
    clearStoredToken()
    expect(getStoredToken()).toBeNull()
  })
})
