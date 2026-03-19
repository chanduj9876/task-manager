import { useEffect } from 'react'
import { RouterProvider } from 'react-router-dom'
import router from '@/router'
import { useWebSocket } from '@/hooks/useWebSocket'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { fetchMyOrgs } from '@/features/organizations/orgSlice'
import { fetchNotifications } from '@/features/notifications/notificationSlice'

function AppInner() {
  const dispatch = useAppDispatch()
  const token = useAppSelector((s) => s.auth.token)

  // Connect WebSocket when authenticated
  useWebSocket()

  // Bootstrap data on login
  useEffect(() => {
    if (token) {
      dispatch(fetchMyOrgs())
      dispatch(fetchNotifications())
    }
  }, [token, dispatch])

  return <RouterProvider router={router} />
}

export default function App() {
  return <AppInner />
}
