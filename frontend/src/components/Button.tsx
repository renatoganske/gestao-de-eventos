import type { ButtonHTMLAttributes } from 'react'

type ButtonVariant = 'default' | 'primary' | 'ghost'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
}

const VARIANT_CLASS: Record<ButtonVariant, string> = {
  default: '',
  primary: 'btn-primary',
  ghost: 'btn-ghost',
}

export function Button({ variant = 'default', className, ...props }: ButtonProps) {
  const classes = ['btn', VARIANT_CLASS[variant], className].filter(Boolean).join(' ')
  return <button className={classes} {...props} />
}
