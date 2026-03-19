import { NavLink } from 'react-router-dom'
import { useAppSelector } from '@/app/hooks'

const navItems = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/organizations', label: 'Organizations' },
  { to: '/notifications', label: 'Notifications' },
]

export default function Sidebar() {
  const user = useAppSelector((s) => s.auth.user)

  return (
    <aside className="w-56 min-h-full bg-gray-50 border-r border-gray-200 py-6 px-4 flex flex-col gap-1">
      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) =>
            `block px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
              isActive
                ? 'bg-primary-100 text-primary-700'
                : 'text-gray-600 hover:bg-gray-100'
            }`
          }
        >
          {item.label}
        </NavLink>
      ))}
      {user?.role === 'ADMIN' && (
        <NavLink
          to="/audit"
          className={({ isActive }) =>
            `block px-3 py-2 rounded-lg text-sm font-medium transition-colors mt-4 ${
              isActive
                ? 'bg-primary-100 text-primary-700'
                : 'text-gray-500 hover:bg-gray-100'
            }`
          }
        >
          Audit Logs
        </NavLink>
      )}
    </aside>
  )
}
