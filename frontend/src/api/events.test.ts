import { apiFetch } from './client'
import { searchEvents } from './events'

vi.mock('./client')

function lastRequestedPath(): string {
  const calls = vi.mocked(apiFetch).mock.calls
  return calls[calls.length - 1][0] as string
}

describe('searchEvents — montagem da query', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(apiFetch).mockResolvedValue([])
  })

  it('não envia query string quando não há filtro', async () => {
    await searchEvents()

    expect(lastRequestedPath()).toBe('/events/search')
  })

  it('envia daytimeWedding e outdoorWedding quando true', async () => {
    await searchEvents({ daytimeWedding: true, outdoorWedding: true })

    const query = new URLSearchParams(lastRequestedPath().split('?')[1])
    expect(query.get('daytimeWedding')).toBe('true')
    expect(query.get('outdoorWedding')).toBe('true')
  })

  it('envia false explícito, sem descartá-lo como se fosse "sem filtro"', async () => {
    await searchEvents({ daytimeWedding: false, outdoorWedding: false })

    const query = new URLSearchParams(lastRequestedPath().split('?')[1])
    expect(query.get('daytimeWedding')).toBe('false')
    expect(query.get('outdoorWedding')).toBe('false')
  })

  it('omite os parâmetros de casamento quando undefined', async () => {
    await searchEvents({ eventTypeId: 'type-1', daytimeWedding: undefined, outdoorWedding: undefined })

    const query = new URLSearchParams(lastRequestedPath().split('?')[1])
    expect(query.get('eventTypeId')).toBe('type-1')
    expect(query.has('daytimeWedding')).toBe(false)
    expect(query.has('outdoorWedding')).toBe(false)
  })

  it('combina os parâmetros de casamento com os demais filtros', async () => {
    await searchEvents({ eventTypeId: 'type-1', professionalId: 'prof-1', from: '2026-01-01', outdoorWedding: true })

    const query = new URLSearchParams(lastRequestedPath().split('?')[1])
    expect(Object.fromEntries(query)).toEqual({
      eventTypeId: 'type-1',
      professionalId: 'prof-1',
      from: '2026-01-01',
      outdoorWedding: 'true',
    })
  })
})
