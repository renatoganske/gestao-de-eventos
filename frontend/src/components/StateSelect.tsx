import { BRAZILIAN_STATES } from '../constants/brazilianStates'

export interface StateSelectProps {
  id: string
  value: string
  onChange: (value: string) => void
  disabled?: boolean
}

export function StateSelect({ id, value, onChange, disabled }: StateSelectProps) {
  const trimmed = value.trim()
  // Valor legado (fora das 27 UFs) continua exibido para não ser perdido ao editar.
  const isLegacy = trimmed !== '' && !BRAZILIAN_STATES.some((state) => state.code === trimmed)

  return (
    <select id={id} value={value} onChange={(event) => onChange(event.target.value)} disabled={disabled}>
      <option value="">Selecione</option>
      {isLegacy && <option value={value}>{value}</option>}
      {BRAZILIAN_STATES.map((state) => (
        <option key={state.code} value={state.code}>
          {state.code} - {state.name}
        </option>
      ))}
    </select>
  )
}
