import { ApiError, apiFetch } from './client'

function mockConflict() {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(JSON.stringify({ status: 409, message: 'Operation conflicts with existing data' }), { status: 409 }),
  )
}

describe('apiFetch — tratamento de 409', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('traduz conflito em criação/edição como registro duplicado', async () => {
    mockConflict()

    const error = await apiFetch('/professional-types', { method: 'POST', body: '{}' }).catch((err) => err)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.message).toBe('Já existe um registro com esse nome.')
  })

  it('traduz conflito em exclusão como registro em uso', async () => {
    mockConflict()

    const error = await apiFetch('/customers/1', { method: 'DELETE' }).catch((err) => err)

    expect(error.status).toBe(409)
    expect(error.message).toBe('Este registro está vinculado a outros dados e não pode ser excluído.')
  })
})
