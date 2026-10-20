export type SortDirection = 'asc' | 'desc'
export type SortValue = string | number | null | undefined

/**
 * Compares two column values. Text ignores accent and case (pt-BR), numbers compare numerically, and
 * null/undefined always go last regardless of `direction` -- so they never bury the real values.
 */
export function compareSortValues(a: SortValue, b: SortValue, direction: SortDirection = 'asc'): number {
  const aMissing = a === null || a === undefined
  const bMissing = b === null || b === undefined
  if (aMissing || bMissing) {
    if (aMissing && bMissing) return 0
    return aMissing ? 1 : -1
  }

  const result =
    typeof a === 'number' && typeof b === 'number'
      ? a - b
      : String(a).localeCompare(String(b), 'pt-BR', { sensitivity: 'base' })
  return direction === 'asc' ? result : -result
}

/** Returns a sorted copy; ties keep their original relative order (Array.prototype.sort is stable). */
export function sortRows<T>(rows: T[], getValue: (row: T) => SortValue, direction: SortDirection): T[] {
  return [...rows].sort((a, b) => compareSortValues(getValue(a), getValue(b), direction))
}

/** Numeric suffix of an event code ("RG10" -> 10) so RG2 sorts before RG10; null when there are no digits. */
export function codeNumber(code: string | null | undefined): number | null {
  const match = code?.match(/(\d+)\s*$/)
  return match ? Number(match[1]) : null
}
