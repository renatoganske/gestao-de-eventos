/** Valida a capacidade real (opcional): positiva e, se houver nominal, não maior que ela. */
export function validateRealCapacity(nominal: string, real: string): string | null {
  if (!real) {
    return null
  }
  const realValue = Number(real)
  if (!(realValue > 0)) {
    return 'A capacidade real deve ser maior que zero.'
  }
  if (nominal && realValue > Number(nominal)) {
    return 'A capacidade real não pode ser maior que a nominal.'
  }
  return null
}
