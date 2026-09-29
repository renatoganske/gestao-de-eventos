import { describe, expect, it } from 'vitest'
import { effectiveCapacityGb, usagePercent } from './hdCapacity'

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
