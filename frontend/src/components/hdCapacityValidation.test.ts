import { validateRealCapacity } from './hdCapacityValidation'

describe('validateRealCapacity', () => {
  it('aceita vazio (campo opcional)', () => {
    expect(validateRealCapacity('1000', '')).toBeNull()
  })

  it('aceita valor positivo menor ou igual à nominal', () => {
    expect(validateRealCapacity('1000', '931')).toBeNull()
    expect(validateRealCapacity('1000', '1000')).toBeNull()
  })

  it('aceita valor positivo quando não há nominal', () => {
    expect(validateRealCapacity('', '931')).toBeNull()
  })

  it('rejeita zero e negativos', () => {
    expect(validateRealCapacity('1000', '0')).toBe('A capacidade real deve ser maior que zero.')
    expect(validateRealCapacity('1000', '-1')).toBe('A capacidade real deve ser maior que zero.')
  })

  it('rejeita valor maior que a nominal', () => {
    expect(validateRealCapacity('1000', '1001')).toBe('A capacidade real não pode ser maior que a nominal.')
  })
})
