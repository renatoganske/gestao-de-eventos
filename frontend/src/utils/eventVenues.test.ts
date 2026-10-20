import { hasRoleWithoutVenue, newVenueRow, venueLabels, venueRowsFromEvent, venueRowsToAssignments } from './eventVenues'

describe('eventVenues', () => {
  it('monta as linhas a partir do evento, com papel vazio quando não há', () => {
    const rows = venueRowsFromEvent([
      { venueId: 'v1', venueName: 'Igreja', role: 'Cerimônia' },
      { venueId: 'v2', venueName: 'Clube', role: null },
    ])

    expect(rows.map(({ venueId, role }) => ({ venueId, role }))).toEqual([
      { venueId: 'v1', role: 'Cerimônia' },
      { venueId: 'v2', role: '' },
    ])
  })

  it('descarta linhas sem local e manda papel em branco como null', () => {
    const assignments = venueRowsToAssignments([
      newVenueRow({ venueId: 'v1', role: ' Recepção ' }),
      newVenueRow({ venueId: 'v2', role: '   ' }),
      newVenueRow(),
    ])

    expect(assignments).toEqual([
      { venueId: 'v1', role: 'Recepção' },
      { venueId: 'v2', role: null },
    ])
  })

  it('aponta linha com papel e sem local', () => {
    expect(hasRoleWithoutVenue([newVenueRow({ role: 'Cerimônia' })])).toBe(true)
    expect(hasRoleWithoutVenue([newVenueRow(), newVenueRow({ venueId: 'v1', role: 'Cerimônia' })])).toBe(false)
  })

  it('formata os locais para a listagem, com o papel entre parênteses quando houver', () => {
    expect(
      venueLabels([
        { venueId: 'v1', venueName: 'Igreja Matriz', role: 'Cerimônia' },
        { venueId: 'v2', venueName: 'Quintal VIP', role: null },
      ]),
    ).toBe('Igreja Matriz (Cerimônia), Quintal VIP')
    expect(venueLabels([])).toBe('')
    expect(venueLabels(undefined)).toBe('')
  })
})
