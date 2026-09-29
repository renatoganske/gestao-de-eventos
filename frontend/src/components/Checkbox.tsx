import type { InputHTMLAttributes } from 'react'

export interface CheckboxProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'onChange' | 'checked'> {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}

/** Checkbox nativo estilizado, com label inline clicável (o input fica dentro do label). */
export function Checkbox({ label, checked, onChange, disabled, className, ...rest }: CheckboxProps) {
  const classes = ['checkbox', disabled ? 'checkbox-disabled' : '', className].filter(Boolean).join(' ')
  return (
    <label className={classes}>
      <input
        {...rest}
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
      />
      <span>{label}</span>
    </label>
  )
}
