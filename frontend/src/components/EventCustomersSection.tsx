import type { CustomerDto } from '../api/customers'
import { newCustomerRow, type CustomerRow } from '../utils/eventCustomers'
import { Autocomplete } from './Autocomplete'
import { Button } from './Button'
import { FormField } from './FormField'

export interface EventCustomersSectionProps {
  rows: CustomerRow[]
  customers: CustomerDto[]
  onChange: (rows: CustomerRow[]) => void
  onCreateNew: (rowKey: string) => void
  disabled?: boolean
}

/**
 * Each person of the event (both of a couple, the parents of a birthday child) is a customer of their own
 * (ADR-0025); at most one of them is marked as the one who hired the job.
 */
export function EventCustomersSection({ rows, customers, onChange, onCreateNew, disabled }: EventCustomersSectionProps) {
  function updateRow(key: string, changes: Partial<Omit<CustomerRow, 'key'>>) {
    onChange(rows.map((row) => (row.key === key ? { ...row, ...changes } : row)))
  }

  function removeRow(key: string) {
    onChange(rows.filter((row) => row.key !== key))
  }

  // Only one contracting customer: marking a row unmarks the others.
  function markContracting(key: string) {
    onChange(rows.map((row) => ({ ...row, contracting: row.key === key })))
  }

  function addRow() {
    // the first customer added is, most of the time, the one who hired the job
    const hasContracting = rows.some((row) => row.contracting)
    onChange([...rows, newCustomerRow({ contracting: !hasContracting })])
  }

  // The backend key is event + customer, so a row only offers the customers no other row has picked.
  function optionsFor(row: CustomerRow) {
    const pickedElsewhere = new Set(rows.filter((other) => other.key !== row.key && other.customerId).map((other) => other.customerId))
    return customers
      .filter((customer) => !pickedElsewhere.has(customer.id))
      .map((customer) => ({ id: customer.id, label: customer.name }))
  }

  return (
    <div className="event-customers span-2">
      {rows.length === 0 && <p className="event-team-empty">Nenhum cliente adicionado.</p>}

      {rows.map((row, index) => {
        const position = index + 1
        return (
          <div key={row.key} className="event-customer-row">
            <FormField label={`Cliente ${position}`} htmlFor={`event-customer-${row.key}`}>
              <Autocomplete
                id={`event-customer-${row.key}`}
                options={optionsFor(row)}
                value={row.customerId}
                onChange={(customerId) => updateRow(row.key, { customerId })}
                placeholder="Buscar cliente..."
                onCreateNew={() => onCreateNew(row.key)}
              />
            </FormField>

            <label className="event-customer-contracting">
              <input
                type="radio"
                name="event-contracting-customer"
                checked={row.contracting}
                onChange={() => markContracting(row.key)}
                disabled={disabled}
                aria-label={`Cliente ${position} é o contratante`}
              />
              Contratante
            </label>

            <Button
              type="button"
              variant="ghost"
              onClick={() => removeRow(row.key)}
              disabled={disabled}
              aria-label={`Remover cliente ${position}`}
            >
              Remover
            </Button>
          </div>
        )
      })}

      <div className="event-team-actions">
        <Button type="button" variant="ghost" onClick={addRow} disabled={disabled}>
          + Adicionar cliente
        </Button>
      </div>
    </div>
  )
}
