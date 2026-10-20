import { describe, expect, it } from 'vitest'
import { effectiveCapacityGb, freeSpaceGb, usagePercent } from './hdCapacity'

describe('effectiveCapacityGb', () => {
  it('usa a capacidade real quando preenchida', () => {
    expect(effectiveCapacityGb({ capacityGb: 2000, realCapacityGb: 1863 })).toBe(1863)
  })

  it('usa a nominal quando a real é nula ou ausente', () => {
    expect(effectiveCapacityGb({ capacityGb: 2000, realCapacityGb: null })).toBe(2000)
    expect(effectiveCapacityGb({ capacityGb: 2000 })).toBe(2000)
  })

  it('retorna null sem nenhuma capacidade', () => {
    expect(effectiveCapacityGb({ capacityGb: null, realCapacityGb: null })).toBeNull()
  })
})

describe('usagePercent', () => {
  it('calcula sobre a capacidade real quando existe', () => {
    expect(usagePercent({ usedSpaceGb: 930, capacityGb: 2000, realCapacityGb: 1000 })).toBe(93)
  })

  it('calcula sobre a nominal sem capacidade real', () => {
    expect(usagePercent({ usedSpaceGb: 500, capacityGb: 2000, realCapacityGb: null })).toBe(25)
  })

  it('retorna 0 sem nenhuma capacidade', () => {
    expect(usagePercent({ usedSpaceGb: 500, capacityGb: null, realCapacityGb: null })).toBe(0)
  })

  it('limita a 100', () => {
    expect(usagePercent({ usedSpaceGb: 1500, capacityGb: 2000, realCapacityGb: 1000 })).toBe(100)
  })
})

describe('freeSpaceGb', () => {
  it('é a capacidade efetiva menos o usado', () => {
    expect(freeSpaceGb({ capacityGb: 2000, usedSpaceGb: 500 })).toBe(1500)
    expect(freeSpaceGb({ capacityGb: 2000, realCapacityGb: 1000, usedSpaceGb: 400 })).toBe(600)
  })

  it('nunca é negativo', () => {
    expect(freeSpaceGb({ capacityGb: 1000, usedSpaceGb: 1200 })).toBe(0)
  })

  it('retorna null sem capacidade', () => {
    expect(freeSpaceGb({ capacityGb: null, realCapacityGb: null, usedSpaceGb: 10 })).toBeNull()
  })
})
