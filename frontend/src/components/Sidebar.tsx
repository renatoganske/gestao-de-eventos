import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'

export interface SidebarItem {
  to: string
  label: string
  icon: ReactNode
  badge?: string | number
  end?: boolean
}

export interface SidebarProps {
  items: SidebarItem[]
}

export function Sidebar({ items }: SidebarProps) {
  return (
    <aside className="sidebar">
      <nav className="sidebar-nav" aria-label="Navegação principal">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => ['nav-item', isActive ? 'nav-item-active' : ''].filter(Boolean).join(' ')}
          >
            {item.icon}
            <span>{item.label}</span>
            {item.badge != null && <span className="nav-badge">{item.badge}</span>}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
