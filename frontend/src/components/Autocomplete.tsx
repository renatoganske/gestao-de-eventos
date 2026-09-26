import { useState } from 'react'
import { Button } from './Button'

export interface AutocompleteOption {
  id: string
  label: string
}

export interface AutocompleteProps {
  id: string
  options: AutocompleteOption[]
  value: string
  onChange: (id: string) => void
  placeholder?: string
  onCreateNew?: () => void
  createNewLabel?: string
}

function resolveLabel(options: AutocompleteOption[], value: string): string {
  return options.find((option) => option.id === value)?.label ?? ''
}

export function Autocomplete({ id, options, value, onChange, placeholder, onCreateNew, createNewLabel }: AutocompleteProps) {
  const [query, setQuery] = useState(() => resolveLabel(options, value))
  const [prevValue, setPrevValue] = useState(value)
  const [isOpen, setIsOpen] = useState(false)

  if (value !== prevValue) {
    setPrevValue(value)
    setQuery(resolveLabel(options, value))
  }

  const filtered = query
    ? options.filter((option) => option.label.toLowerCase().includes(query.toLowerCase())).slice(0, 8)
    : options.slice(0, 8)

  function handleQueryChange(text: string) {
    setQuery(text)
    setIsOpen(true)
    if (!text) {
      onChange('')
    }
  }

  function selectOption(option: AutocompleteOption) {
    onChange(option.id)
    setQuery(option.label)
    setIsOpen(false)
  }

  return (
    <div className="autocomplete">
      <div className="autocomplete-row">
        <input
          id={id}
          type="text"
          role="combobox"
          aria-expanded={isOpen}
          autoComplete="off"
          value={query}
          placeholder={placeholder}
          onFocus={() => setIsOpen(true)}
          onBlur={() => setIsOpen(false)}
          onChange={(event) => handleQueryChange(event.target.value)}
        />
        {onCreateNew && (
          <Button type="button" variant="ghost" onClick={onCreateNew}>
            + Novo
          </Button>
        )}
      </div>
      {isOpen && filtered.length > 0 && (
        <ul className="autocomplete-list" role="listbox">
          {filtered.map((option) => (
            <li key={option.id}>
              <button
                type="button"
                role="option"
                aria-selected={option.id === value}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => selectOption(option)}
              >
                {option.label}
              </button>
            </li>
          ))}
        </ul>
      )}
      {isOpen && query && filtered.length === 0 && (
        <ul className="autocomplete-list">
          <li className="autocomplete-empty">{createNewLabel ?? 'Nenhum resultado encontrado.'}</li>
        </ul>
      )}
    </div>
  )
}
