import type { EventVenueAssignmentDto, EventVenueSummaryDto } from '../api/events'

/** One line of the event's venues while it is being edited. `key` only identifies the row for React. */
export interface VenueRow {
  key: string
  venueId: string
  role: string
}

export function newVenueRow(overrides: Partial<Omit<VenueRow, 'key'>> = {}): VenueRow {
  return {
    key: `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`,
    venueId: '',
    role: '',
    ...overrides,
  }
}

export function venueRowsFromEvent(venues: EventVenueSummaryDto[] | undefined): VenueRow[] {
  return (venues ?? []).map((assignment) => newVenueRow({ venueId: assignment.venueId, role: assignment.role ?? '' }))
}

/**
 * Turns the edited rows into the payload. A row without a venue is dropped (an empty row the user left
 * behind); the caller validates beforehand that no such row still carries a role.
 */
export function venueRowsToAssignments(rows: VenueRow[]): EventVenueAssignmentDto[] {
  return rows.filter((row) => row.venueId).map((row) => ({ venueId: row.venueId, role: row.role.trim() || null }))
}

/** A role typed on a row that has no venue would be silently lost, so it is reported instead. */
export function hasRoleWithoutVenue(rows: VenueRow[]): boolean {
  return rows.some((row) => !row.venueId && row.role.trim() !== '')
}

/** "Igreja Matriz (Cerimônia), Quintal VIP" -- for lists, where each venue fits on one line. */
export function venueLabels(venues: EventVenueSummaryDto[] | undefined): string {
  return (venues ?? []).map((venue) => (venue.role ? `${venue.venueName} (${venue.role})` : venue.venueName)).join(', ')
}
