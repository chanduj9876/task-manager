import { Link, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { logoutAsync } from '@/features/auth/authSlice'
import NotificationBell from '@/features/notifications/NotificationBell'

export default function Navbar() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((s) => s.auth.user)

  const handleLogout = () => {
    dispatch(logoutAsync())
    navigate('/login')
  }

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <Link to="/dashboard" className="text-xl font-bold text-primary-600">
        TaskManager
      </Link>
      <div className="flex items-center gap-4">
        <NotificationBell />
        <span className="text-sm text-gray-600">{user?.name}</span>
        <button
          onClick={handleLogout}
          className="text-sm text-gray-500 hover:text-red-600 transition-colors"
        >
          Logout
        </button>
      </div>
    </nav>
  )
}
