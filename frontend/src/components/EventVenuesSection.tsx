import type { OptionDto } from '../api/options'
import { newVenueRow, type VenueRow } from '../utils/eventVenues'
import { Autocomplete } from './Autocomplete'
import { Button } from './Button'
import { FormField } from './FormField'

export interface EventVenuesSectionProps {
  rows: VenueRow[]
  venues: OptionDto[]
  onChange: (rows: VenueRow[]) => void
  onCreateNew: (rowKey: string) => void
  disabled?: boolean
  error?: string
}

export function EventVenuesSection({ rows, venues, onChange, onCreateNew, disabled, error }: EventVenuesSectionProps) {
  function updateRow(key: string, changes: Partial<Omit<VenueRow, 'key'>>) {
    onChange(rows.map((row) => (row.key === key ? { ...row, ...changes } : row)))
  }

  function removeRow(key: string) {
    onChange(rows.filter((row) => row.key !== key))
  }

  // A venue appears once per event (the backend key is event + venue), so a row only offers the venues
  // that no other row has already picked.
  function optionsFor(row: VenueRow) {
    const pickedElsewhere = new Set(rows.filter((other) => other.key !== row.key && other.venueId).map((other) => other.venueId))
    return venues.filter((venue) => !pickedElsewhere.has(venue.id)).map((venue) => ({ id: venue.id, label: venue.name }))
  }

  return (
    <div className="event-venues">
      {rows.length === 0 && <p className="event-venue-empty">Nenhum local adicionado.</p>}

      {rows.map((row, index) => {
        const position = index + 1
        return (
          <div key={row.key} className="event-venue-row">
            <FormField label={`Local ${position}`} htmlFor={`event-venue-${row.key}`}>
              <Autocomplete
                id={`event-venue-${row.key}`}
                options={optionsFor(row)}
                value={row.venueId}
                onChange={(venueId) => updateRow(row.key, { venueId })}
                placeholder="Buscar local..."
                onCreateNew={() => onCreateNew(row.key)}
              />
            </FormField>

            <FormField label={`Papel do local ${position}`} htmlFor={`event-venue-role-${row.key}`}>
              <input
                id={`event-venue-role-${row.key}`}
                value={row.role}
                onChange={(event) => updateRow(row.key, { role: event.target.value })}
                placeholder="Ex.: Cerimônia, Recepção, Making-of"
                disabled={disabled}
              />
            </FormField>

            <Button
              type="button"
              variant="ghost"
              onClick={() => removeRow(row.key)}
              disabled={disabled}
              aria-label={`Remover local ${position}`}
            >
              Remover
            </Button>
          </div>
        )
      })}

      {error && (
        <p role="alert" className="events-error">
          {error}
        </p>
      )}

      <div className="event-venue-actions">
        <Button type="button" variant="ghost" onClick={() => onChange([...rows, newVenueRow()])} disabled={disabled}>
          + Adicionar local
        </Button>
      </div>
    </div>
  )
}
