
interface HdCapacityFields {
  capacityGb?: number | null
  realCapacityGb?: number | null
  usedSpaceGb: number
}

/**
 * Capacidade efetiva = capacidade real quando preenchida, senão a nominal.
 * Espelha o HdCapacityPolicy do backend. Retorna null quando nenhuma das duas existe.
 */
export function effectiveCapacityGb(hd: Omit<HdCapacityFields, 'usedSpaceGb'>): number | null {
  return hd.realCapacityGb ?? hd.capacityGb ?? null
}

/** Percentual de uso (0-100, arredondado) sobre a capacidade efetiva; 0 se não há capacidade. */
export function usagePercent(hd: HdCapacityFields): number {
  const capacity = effectiveCapacityGb(hd)
  if (!capacity) {
    return 0
  }
  return Math.min(100, Math.round((hd.usedSpaceGb / capacity) * 100))
}

/** Espaço livre em GB (capacidade efetiva - usado, nunca negativo); null quando não há capacidade. */
export function freeSpaceGb(hd: HdCapacityFields): number | null {
  const capacity = effectiveCapacityGb(hd)
  if (capacity === null) {
    return null
  }
  return Math.max(0, capacity - hd.usedSpaceGb)
}
