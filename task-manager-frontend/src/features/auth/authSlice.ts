import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { authApi } from '@/api/authApi'
import type { AuthResponse, Role } from '@/types'

interface AuthState {
  token: string | null
  user: { id: string; name: string; email: string; role: Role } | null
  loading: boolean
  error: string | null
}

const TOKEN_KEY = 'tm_token'
const USER_KEY = 'tm_user'

const initialState: AuthState = {
  token: localStorage.getItem(TOKEN_KEY),
  user: JSON.parse(localStorage.getItem(USER_KEY) ?? 'null'),
  loading: false,
  error: null,
}

export const loginAsync = createAsyncThunk(
  'auth/login',
  async (credentials: { email: string; password: string }, { rejectWithValue }) => {
    try {
      const res = await authApi.login(credentials)
      return res.data.data
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Login failed'
      return rejectWithValue(msg)
    }
  },
)

export const signupAsync = createAsyncThunk(
  'auth/signup',
  async (
    payload: { name: string; email: string; password: string },
    { rejectWithValue },
  ) => {
    try {
      const res = await authApi.signup(payload)
      return res.data.data
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Signup failed'
      return rejectWithValue(msg)
    }
  },
)

function persistAuth(data: AuthResponse) {
  localStorage.setItem(TOKEN_KEY, data.token)
  const user = { id: data.userId, name: data.name, email: data.email, role: data.role }
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export const logoutAsync = createAsyncThunk('auth/logoutAsync', async (_, { dispatch }) => {
  await authApi.logout().catch(() => {})
  dispatch(authSlice.actions.logout())
})

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      state.token = null
      state.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginAsync.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(loginAsync.fulfilled, (state, action) => {
        state.loading = false
        state.token = action.payload.token
        state.user = persistAuth(action.payload)
      })
      .addCase(loginAsync.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(signupAsync.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(signupAsync.fulfilled, (state, action) => {
        state.loading = false
        state.token = action.payload.token
        state.user = persistAuth(action.payload)
      })
      .addCase(signupAsync.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
  },
})

export const { logout } = authSlice.actions
export default authSlice.reducer
