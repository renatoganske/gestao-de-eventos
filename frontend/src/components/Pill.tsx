import type { ReactNode } from 'react'

export type PillStatus = 'pending' | 'delivered' | 'archived' | 'critical'

export interface PillProps {
  status: PillStatus
  children: ReactNode
}

const STATUS_CLASS: Record<PillStatus, string> = {
  pending: 'pill-pending',
  delivered: 'pill-delivered',
  archived: 'pill-archived',
  critical: 'pill-critical',
}

export function Pill({ status, children }: PillProps) {
  return <span className={`pill ${STATUS_CLASS[status]}`}>{children}</span>
}
