import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { notificationApi } from '@/api/notificationApi'
import type { Notification } from '@/types'

interface NotificationState {
  items: Notification[]
  unreadCount: number
}

const initialState: NotificationState = {
  items: [],
  unreadCount: 0,
}

export const fetchNotifications = createAsyncThunk('notifications/fetchAll', async () => {
  const res = await notificationApi.getAll()
  return res.data.data.content
})

export const markReadAsync = createAsyncThunk('notifications/markRead', async (id: string) => {
  const res = await notificationApi.markRead(id)
  return res.data.data
})

export const markAllReadAsync = createAsyncThunk('notifications/markAllRead', async () => {
  await notificationApi.markAllRead()
})

const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    addNotification(state, action) {
      state.items.unshift(action.payload as Notification)
      state.unreadCount += 1
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.items = action.payload
        state.unreadCount = action.payload.filter((n) => !n.read).length
      })
      .addCase(markReadAsync.fulfilled, (state, action) => {
        const idx = state.items.findIndex((n) => n.id === action.payload.id)
        if (idx !== -1) {
          state.items[idx] = action.payload
          state.unreadCount = state.items.filter((n) => !n.read).length
        }
      })
      .addCase(markAllReadAsync.fulfilled, (state) => {
        state.items = state.items.map((n) => ({ ...n, read: true }))
        state.unreadCount = 0
      })
  },
})

export const { addNotification } = notificationSlice.actions
export default notificationSlice.reducer
