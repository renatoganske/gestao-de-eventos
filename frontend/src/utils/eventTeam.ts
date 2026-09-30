import type { EventProfessionalAssignmentDto, EventProfessionalSummaryDto } from '../api/events'

/** One line of the event team while it is being edited. `key` only identifies the row for React. */
export interface TeamRow {
  key: string
  professionalId: string
  roleInEvent: string
}

export function newTeamRow(overrides: Partial<Omit<TeamRow, 'key'>> = {}): TeamRow {
  return {
    key: `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`,
    professionalId: '',
    roleInEvent: '',
    ...overrides,
  }
}

export function teamRowsFromEvent(eventProfessionals: EventProfessionalSummaryDto[] | undefined): TeamRow[] {
  return (eventProfessionals ?? []).map((assignment) =>
    newTeamRow({ professionalId: assignment.professionalId, roleInEvent: assignment.roleInEvent ?? '' }),
  )
}

/**
 * Turns the edited rows into the payload. A row without a professional is dropped (an empty row the user
 * left behind); the caller validates beforehand that no such row still carries a role.
 */
export function teamRowsToAssignments(rows: TeamRow[]): EventProfessionalAssignmentDto[] {
  return rows
    .filter((row) => row.professionalId)
    .map((row) => ({ professionalId: row.professionalId, roleInEvent: row.roleInEvent.trim() || null }))
}

/** A role typed on a row that has no professional would be silently lost, so it is reported instead. */
export function hasRoleWithoutProfessional(rows: TeamRow[]): boolean {
  return rows.some((row) => !row.professionalId && row.roleInEvent.trim() !== '')
}
