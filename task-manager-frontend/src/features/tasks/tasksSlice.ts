import { createAsyncThunk, createEntityAdapter, createSlice } from '@reduxjs/toolkit'
import { taskApi } from '@/api/taskApi'
import type { Task, TaskPriority, TaskStatus } from '@/types'
import type { RootState } from '@/app/store'

const tasksAdapter = createEntityAdapter<Task>()

export const fetchTasks = createAsyncThunk(
  'tasks/fetchAll',
  async (params: { orgId: string; status?: TaskStatus; assignedTo?: string }) => {
    const res = await taskApi.getTasks(params.orgId, params.status, params.assignedTo)
    return res.data.data.content
  },
)

export const createTaskAsync = createAsyncThunk(
  'tasks/create',
  async (payload: {
    orgId: string
    title: string
    description?: string
    priority: TaskPriority
    assignedTo?: string
    dueDate?: string
  }) => {
    const { orgId, ...data } = payload
    const res = await taskApi.createTask(orgId, data)
    return res.data.data
  },
)

export const updateTaskAsync = createAsyncThunk(
  'tasks/update',
  async (payload: {
    taskId: string
    title: string
    description?: string
    priority: TaskPriority
    dueDate?: string
  }) => {
    const { taskId, ...data } = payload
    const res = await taskApi.updateTask(taskId, data)
    return res.data.data
  },
)

export const changeStatusAsync = createAsyncThunk(
  'tasks/changeStatus',
  async (payload: { taskId: string; status: TaskStatus }) => {
    const res = await taskApi.changeStatus(payload.taskId, payload.status)
    return res.data.data
  },
)

export const assignTaskAsync = createAsyncThunk(
  'tasks/assign',
  async (payload: { taskId: string; assignedTo: string | null }) => {
    const res = await taskApi.assignTask(payload.taskId, payload.assignedTo)
    return res.data.data
  },
)

export const deleteTaskAsync = createAsyncThunk(
  'tasks/delete',
  async (taskId: string) => {
    await taskApi.deleteTask(taskId)
    return taskId
  },
)

const tasksSlice = createSlice({
  name: 'tasks',
  initialState: tasksAdapter.getInitialState({ loading: false, error: null as string | null }),
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchTasks.pending, (state) => { state.loading = true })
      .addCase(fetchTasks.fulfilled, (state, action) => {
        state.loading = false
        tasksAdapter.setAll(state, action.payload)
      })
      .addCase(fetchTasks.rejected, (state) => { state.loading = false })
      .addCase(createTaskAsync.fulfilled, tasksAdapter.addOne)
      .addCase(updateTaskAsync.fulfilled, tasksAdapter.upsertOne)
      .addCase(changeStatusAsync.fulfilled, tasksAdapter.upsertOne)
      .addCase(assignTaskAsync.fulfilled, tasksAdapter.upsertOne)
      .addCase(deleteTaskAsync.fulfilled, (state, action) => {
        tasksAdapter.removeOne(state, action.payload)
      })
      // Clear tasks immediately when the selected org changes so stale data
      // from the previous org never bleeds into the new org's board.
      .addMatcher(
        (action) => action.type === 'org/selectOrg',
        (state) => {
          tasksAdapter.removeAll(state)
          state.loading = true
        },
      )
  },
})

export const {
  selectAll: selectAllTasks,
  selectById: selectTaskById,
} = tasksAdapter.getSelectors((state: RootState) => state.tasks)

// Scoped selector — only returns tasks belonging to the currently selected org.
// Guards against stale cross-org data accumulating in the adapter.
export const selectTasksForCurrentOrg = (state: RootState) => {
  const orgId = state.org.selectedOrgId
  if (!orgId) return []
  return selectAllTasks(state).filter((t) => t.orgId === orgId)
}

export default tasksSlice.reducer
