import { configureStore } from '@reduxjs/toolkit'
import authReducer from '@/features/auth/authSlice'
import tasksReducer from '@/features/tasks/tasksSlice'
import orgReducer from '@/features/organizations/orgSlice'
import notificationReducer from '@/features/notifications/notificationSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    tasks: tasksReducer,
    org: orgReducer,
    notifications: notificationReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
