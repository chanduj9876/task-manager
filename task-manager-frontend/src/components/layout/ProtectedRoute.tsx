import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '@/app/hooks'
import type { Role } from '@/types'

interface Props {
  allowedRoles?: Role[]
}

export default function ProtectedRoute({ allowedRoles }: Props) {
  const { token, user } = useAppSelector((s) => s.auth)

  if (!token || !user) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
