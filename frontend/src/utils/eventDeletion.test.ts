import { deleteConfirmationMessage, formatIsoDate } from './eventDeletion'

describe('eventDeletion', () => {
  it('formata a data ISO como dd/mm/aaaa e devolve o resto como veio', () => {
    expect(formatIsoDate('2026-10-15')).toBe('15/10/2026')
    expect(formatIsoDate('ontem')).toBe('ontem')
  })

  it('sem vínculos, só pede a confirmação', () => {
    expect(
      deleteConfirmationMessage({ name: 'Ensaio', eventDate: '2026-01-02', customerCount: 0, professionalCount: 0 }),
    ).toBe('Excluir o evento "Ensaio" (02/01/2026)? Esta ação não pode ser desfeita.')
  })

  it('cita os vínculos que somem, no singular e no plural', () => {
    const message = deleteConfirmationMessage({
      name: 'Casamento',
      eventDate: '2026-10-15',
      customerCount: 1,
      professionalCount: 3,
    })
    expect(message).toContain('Os vínculos com 1 cliente e 3 profissionais serão removidos')
  })
})
