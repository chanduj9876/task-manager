import { useAppSelector } from '@/app/hooks'

export function useAuth() {
  const { user, token } = useAppSelector((s) => s.auth)
  return {
    user,
    token,
    isAuthenticated: !!token && !!user,
    isAdmin: user?.role === 'ADMIN',
    isManager: user?.role === 'MANAGER' || user?.role === 'ADMIN',
  }
}
