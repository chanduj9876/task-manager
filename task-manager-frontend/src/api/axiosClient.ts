import axios from 'axios'
import type { Store } from '@reduxjs/toolkit'

// Store reference is injected after the store is created (avoids circular import)
let _store: Store | null = null

export function injectStore(store: Store) {
  _store = store
}

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

axiosClient.interceptors.request.use((config) => {
  const token = (_store?.getState() as { auth: { token: string | null } } | undefined)?.auth?.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && _store) {
      const { logout } = await import('@/features/auth/authSlice')
      _store.dispatch(logout())
    }
    return Promise.reject(error)
  },
)

export default axiosClient
