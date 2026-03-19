import axiosClient from './axiosClient'
import type { ApiResponse, Task, TaskPriority, TaskStatus, Comment } from '@/types'

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const taskApi = {
  getTasks: (orgId: string, status?: TaskStatus, assignedTo?: string) =>
    axiosClient.get<ApiResponse<PageResponse<Task>>>('/api/tasks', {
      params: { orgId, status, assignedTo, page: 0, size: 1000 },
    }),

  getTask: (taskId: string) =>
    axiosClient.get<ApiResponse<Task>>(`/api/tasks/${taskId}`),

  createTask: (
    orgId: string,
    data: { title: string; description?: string; priority: TaskPriority; assignedTo?: string; dueDate?: string },
  ) => axiosClient.post<ApiResponse<Task>>('/api/tasks', data, { params: { orgId } }),

  updateTask: (
    taskId: string,
    data: { title: string; description?: string; priority: TaskPriority; dueDate?: string },
  ) => axiosClient.put<ApiResponse<Task>>(`/api/tasks/${taskId}`, data),

  assignTask: (taskId: string, assignedTo: string | null) =>
    axiosClient.patch<ApiResponse<Task>>(`/api/tasks/${taskId}/assign`, { assignedTo }),

  changeStatus: (taskId: string, status: TaskStatus) =>
    axiosClient.patch<ApiResponse<Task>>(`/api/tasks/${taskId}/status`, { status }),

  deleteTask: (taskId: string) =>
    axiosClient.delete<ApiResponse<null>>(`/api/tasks/${taskId}`),

  getComments: (taskId: string) =>
    axiosClient.get<ApiResponse<Comment[]>>(`/api/tasks/${taskId}/comments`),

  addComment: (taskId: string, content: string) =>
    axiosClient.post<ApiResponse<Comment>>(`/api/tasks/${taskId}/comments`, { content }),

  deleteComment: (commentId: string) =>
    axiosClient.delete<ApiResponse<null>>(`/api/comments/${commentId}`),
}
