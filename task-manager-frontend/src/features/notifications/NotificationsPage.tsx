import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import {
  fetchNotifications,
  markReadAsync,
  markAllReadAsync,
} from '@/features/notifications/notificationSlice'

export default function NotificationsPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const { items, unreadCount } = useAppSelector((s) => s.notifications)

  useEffect(() => {
    dispatch(fetchNotifications())
  }, [dispatch])

  return (
    <div className="p-6 space-y-4 max-w-2xl">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">
          Notifications
          {unreadCount > 0 && (
            <span className="ml-2 text-sm font-medium bg-red-100 text-red-600 px-2 py-0.5 rounded-full">
              {unreadCount} unread
            </span>
          )}
        </h1>
        {unreadCount > 0 && (
          <button
            onClick={() => dispatch(markAllReadAsync())}
            className="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            Mark all as read
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 divide-y divide-gray-100 overflow-hidden">
        {items.length === 0 ? (
          <div className="px-6 py-12 text-center text-gray-400">
            <p className="text-sm">No notifications yet.</p>
            <p className="text-xs mt-1">You'll be notified when tasks are assigned or updated.</p>
          </div>
        ) : (
          items.map((n) => (
            <div
              key={n.id}
              onClick={() => {
                if (!n.read) dispatch(markReadAsync(n.id))
                if (n.relatedOrgId) navigate('/organizations')
                else if (n.relatedTaskId) navigate('/tasks')
              }}
              className={`flex items-start gap-3 px-5 py-4 cursor-pointer hover:bg-gray-50 transition-colors ${
                !n.read ? 'bg-blue-50' : ''
              }`}
            >
              <div className={`mt-1 h-2 w-2 rounded-full flex-shrink-0 ${!n.read ? 'bg-blue-500' : 'bg-transparent'}`} />
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-800">{n.message}</p>
                <p className="text-xs text-gray-400 mt-0.5">
                  {new Date(n.createdAt).toLocaleString()}
                </p>
              </div>
              {!n.read && (
                <span className="text-xs text-blue-500 font-medium flex-shrink-0">New</span>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  )
}
