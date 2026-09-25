import type { ReactNode } from 'react'

export interface FormFieldProps {
  label: string
  htmlFor: string
  hint?: string
  error?: string
  className?: string
  children: ReactNode
}

export function FormField({ label, htmlFor, hint, error, className, children }: FormFieldProps) {
  const classes = ['form-field', className].filter(Boolean).join(' ')
  return (
    <div className={classes}>
      <label htmlFor={htmlFor}>{label}</label>
      {children}
      {error ? (
        <span className="hint form-field-error" role="alert">
          {error}
        </span>
      ) : (
        hint && <span className="hint">{hint}</span>
      )}
    </div>
  )
}
