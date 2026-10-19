import type { EventCustomerAssignmentDto, EventCustomerSummaryDto } from '../api/events'

/** One customer line of the event while it is being edited. `key` only identifies the row for React. */
export interface CustomerRow {
  key: string
  customerId: string
  contracting: boolean
}

export function newCustomerRow(overrides: Partial<Omit<CustomerRow, 'key'>> = {}): CustomerRow {
  return {
    key: `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`,
    customerId: '',
    contracting: false,
    ...overrides,
  }
}

export function customerRowsFromEvent(customers: EventCustomerSummaryDto[] | undefined): CustomerRow[] {
  return (customers ?? []).map((customer) =>
    newCustomerRow({ customerId: customer.customerId, contracting: customer.contracting }),
  )
}

/** Rows without a customer are dropped (an empty line the user left behind). */
export function customerRowsToAssignments(rows: CustomerRow[]): EventCustomerAssignmentDto[] {
  return rows
    .filter((row) => row.customerId)
    .map((row) => ({ customerId: row.customerId, contracting: row.contracting }))
}

/** Names for lists and labels, contracting customer first (the backend already sends them in that order). */
export function customerNames(customers: EventCustomerSummaryDto[] | undefined): string {
  return (customers ?? []).map((customer) => customer.customerName).join(', ')
}
