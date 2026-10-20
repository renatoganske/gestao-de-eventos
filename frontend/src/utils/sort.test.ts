import { codeNumber, compareSortValues, sortRows } from './sort'

describe('compareSortValues', () => {
  it('ignora acento e caixa no texto', () => {
    expect(compareSortValues('ágata', 'Agata')).toBe(0)
    expect(compareSortValues('Álvaro', 'bruno')).toBeLessThan(0)
    expect(compareSortValues('zélia', 'Álvaro')).toBeGreaterThan(0)
  })

  it('compara números numericamente', () => {
    expect(compareSortValues(2, 10)).toBeLessThan(0)
    expect(compareSortValues(10, 2, 'desc')).toBeLessThan(0)
  })

  it('deixa nulos e indefinidos no fim em qualquer direção', () => {
    expect(compareSortValues(null, 'a', 'asc')).toBeGreaterThan(0)
    expect(compareSortValues(null, 'a', 'desc')).toBeGreaterThan(0)
    expect(compareSortValues('a', undefined, 'desc')).toBeLessThan(0)
    expect(compareSortValues(null, undefined)).toBe(0)
  })
})

describe('sortRows', () => {
  it('devolve uma cópia ordenada, sem alterar a lista original, e mantém empates estáveis', () => {
    const rows = [
      { id: 1, k: 'b' },
      { id: 2, k: 'a' },
      { id: 3, k: 'b' },
    ]

    const sorted = sortRows(rows, (row) => row.k, 'asc')

    expect(sorted.map((row) => row.id)).toEqual([2, 1, 3])
    expect(rows.map((row) => row.id)).toEqual([1, 2, 3])
  })
})

describe('codeNumber', () => {
  it('extrai o sufixo numérico para ordenar RG2 antes de RG10', () => {
    expect(codeNumber('RG2')).toBe(2)
    expect(codeNumber('RG10')).toBe(10)
    expect(codeNumber('EVT-018')).toBe(18)
  })

  it('devolve null sem dígitos ou sem código', () => {
    expect(codeNumber('SEM')).toBeNull()
    expect(codeNumber(null)).toBeNull()
  })
})
