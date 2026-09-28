import { Autocomplete, type AutocompleteOption } from './Autocomplete'

export interface TagMultiSelectProps {
  id: string
  options: AutocompleteOption[]
  selectedIds: string[]
  onChange: (ids: string[]) => void
  placeholder?: string
  onCreateNew?: () => void
}

export function TagMultiSelect({ id, options, selectedIds, onChange, placeholder, onCreateNew }: TagMultiSelectProps) {
  const availableOptions = options.filter((option) => !selectedIds.includes(option.id))
  const selectedOptions = selectedIds
    .map((selectedId) => options.find((option) => option.id === selectedId))
    .filter((option): option is AutocompleteOption => Boolean(option))

  function addTag(tagId: string) {
    if (tagId && !selectedIds.includes(tagId)) {
      onChange([...selectedIds, tagId])
    }
  }

  function removeTag(tagId: string) {
    onChange(selectedIds.filter((id) => id !== tagId))
  }

  return (
    <div className="tag-multi-select">
      <Autocomplete
        key={selectedIds.join(',')}
        id={id}
        options={availableOptions}
        value=""
        onChange={addTag}
        placeholder={placeholder}
        onCreateNew={onCreateNew}
      />
      {selectedOptions.length > 0 && (
        <ul className="tag-chip-list">
          {selectedOptions.map((option) => (
            <li key={option.id} className="tag-chip">
              {option.label}
              <button type="button" aria-label={`Remover ${option.label}`} onClick={() => removeTag(option.id)}>
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
