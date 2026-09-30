import type { ProfessionalDto } from '../api/professionals'
import { newTeamRow, type TeamRow } from '../utils/eventTeam'
import { Autocomplete } from './Autocomplete'
import { Button } from './Button'
import { FormField } from './FormField'

export interface EventTeamSectionProps {
  rows: TeamRow[]
  professionals: ProfessionalDto[]
  onChange: (rows: TeamRow[]) => void
  onCreateNew: (rowKey: string) => void
  disabled?: boolean
  error?: string
}

export function EventTeamSection({ rows, professionals, onChange, onCreateNew, disabled, error }: EventTeamSectionProps) {
  function updateRow(key: string, changes: Partial<Omit<TeamRow, 'key'>>) {
    onChange(rows.map((row) => (row.key === key ? { ...row, ...changes } : row)))
  }

  function removeRow(key: string) {
    onChange(rows.filter((row) => row.key !== key))
  }

  // A professional can hold one role per event (the backend key is event + professional), so a row only
  // offers the professionals that no other row has already picked.
  function optionsFor(row: TeamRow) {
    const pickedElsewhere = new Set(rows.filter((other) => other.key !== row.key && other.professionalId).map((other) => other.professionalId))
    return professionals
      .filter((professional) => !pickedElsewhere.has(professional.id))
      .map((professional) => ({ id: professional.id, label: professional.name }))
  }

  return (
    <div className="event-team">
      {rows.length === 0 && <p className="event-team-empty">Nenhum profissional adicionado.</p>}

      {rows.map((row, index) => {
        const position = index + 1
        return (
          <div key={row.key} className="event-team-row">
            <FormField label={`Profissional ${position}`} htmlFor={`event-professional-${row.key}`}>
              <Autocomplete
                id={`event-professional-${row.key}`}
                options={optionsFor(row)}
                value={row.professionalId}
                onChange={(professionalId) => updateRow(row.key, { professionalId })}
                placeholder="Buscar profissional..."
                onCreateNew={() => onCreateNew(row.key)}
              />
            </FormField>

            <FormField label={`Papel ${position}`} htmlFor={`event-professional-role-${row.key}`}>
              <input
                id={`event-professional-role-${row.key}`}
                value={row.roleInEvent}
                onChange={(event) => updateRow(row.key, { roleInEvent: event.target.value })}
                placeholder="Ex.: Fotógrafo principal"
                disabled={disabled}
              />
            </FormField>

            <Button
              type="button"
              variant="ghost"
              onClick={() => removeRow(row.key)}
              disabled={disabled}
              aria-label={`Remover profissional ${position}`}
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

      <div className="event-team-actions">
        <Button type="button" variant="ghost" onClick={() => onChange([...rows, newTeamRow()])} disabled={disabled}>
          + Adicionar profissional
        </Button>
      </div>
    </div>
  )
}
