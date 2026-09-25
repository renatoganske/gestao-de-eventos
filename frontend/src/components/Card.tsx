import type { ReactNode } from 'react'

export interface CardProps {
  title?: string
  action?: ReactNode
  className?: string
  children: ReactNode
}

export function Card({ title, action, className, children }: CardProps) {
  const classes = ['panel', className].filter(Boolean).join(' ')
  return (
    <div className={classes}>
      {title && (
        <div className="panel-head">
          <h2>{title}</h2>
          {action}
        </div>
      )}
      {children}
    </div>
  )
}
