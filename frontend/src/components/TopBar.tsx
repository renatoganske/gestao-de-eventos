import type { ReactNode } from 'react'

export interface TopBarProps {
  title: string
  action?: ReactNode
}

export function TopBar({ title, action }: TopBarProps) {
  return (
    <div className="topbar">
      <h1>{title}</h1>
      {action && <div className="topbar-action">{action}</div>}
    </div>
  )
}
