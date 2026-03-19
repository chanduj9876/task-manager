import axiosClient from './axiosClient'
import type { ApiResponse, Notification } from '@/types'

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const notificationApi = {
  getAll: () =>
    axiosClient.get<ApiResponse<PageResponse<Notification>>>('/api/notifications', {
      params: { page: 0, size: 100 }
    }),

  getUnreadCount: () =>
    axiosClient.get<ApiResponse<number>>('/api/notifications/unread-count'),

  markRead: (id: string) =>
    axiosClient.patch<ApiResponse<Notification>>(`/api/notifications/${id}/read`),

  markAllRead: () =>
    axiosClient.patch<ApiResponse<null>>('/api/notifications/read-all'),
}
