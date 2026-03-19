import axiosClient from './axiosClient'
import type { ApiResponse, AuthResponse } from '@/types'

export const authApi = {
  signup: (data: { name: string; email: string; password: string }) =>
    axiosClient.post<ApiResponse<AuthResponse>>('/api/auth/signup', data),

  login: (data: { email: string; password: string }) =>
    axiosClient.post<ApiResponse<AuthResponse>>('/api/auth/login', data),

  logout: () =>
    axiosClient.post<ApiResponse<null>>('/api/auth/logout'),
}
