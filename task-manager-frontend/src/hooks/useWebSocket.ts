import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { addNotification } from '@/features/notifications/notificationSlice'

export function useWebSocket() {
  const dispatch = useAppDispatch()
  const token = useAppSelector((s) => s.auth.token)
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!token) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (frame) => {
          try {
            const notification = JSON.parse(frame.body)
            dispatch(addNotification(notification))
          } catch {
            // malformed frame — ignore
          }
        })
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [token, dispatch])
}
