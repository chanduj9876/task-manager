import { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import {
  fetchTasks,
  createTaskAsync,
  updateTaskAsync,
  assignTaskAsync,
  changeStatusAsync,
  deleteTaskAsync,
} from '@/features/tasks/tasksSlice'
import { fetchMembers, selectOrg } from '@/features/organizations/orgSlice'
import KanbanBoard from './KanbanBoard'
import TaskModal from './TaskModal'
import Button from '@/components/ui/Button'
import type { Task } from '@/types'

export default function TasksPage() {
  const dispatch = useAppDispatch()
  const orgId = useAppSelector((s) => s.org.selectedOrgId)
  const orgs = useAppSelector((s) => s.org.orgs)
  const loading = useAppSelector((s) => s.tasks.loading)
  const members = useAppSelector((s) => s.org.members.filter((m) => m.status === 'ACTIVE'))
  const currentUser = useAppSelector((s) => s.auth.user)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [filterAssigneeId, setFilterAssigneeId] = useState('')

  useEffect(() => {
    if (orgId) {
      setFilterAssigneeId('')        // reset any active member filter for the new org
      dispatch(fetchTasks({ orgId }))
      dispatch(fetchMembers(orgId))
    }
  }, [orgId, dispatch])

  const handleTaskClick = (task: Task) => {
    setSaveError(null)
    setEditingTask(task)
    setModalOpen(true)
  }

  const handleCreate = () => {
    setSaveError(null)
    setEditingTask(null)
    setModalOpen(true)
  }

  const handleSave = async (data: {
    title: string
    description?: string
    priority: import('@/types').TaskPriority
    status?: import('@/types').TaskStatus
    assignedTo?: string
    dueDate?: string
  }) => {
    if (!orgId) return
    setSaveError(null)
    try {
      if (editingTask) {
        await dispatch(updateTaskAsync({
          taskId: editingTask.id,
          title: data.title,
          description: data.description,
          priority: data.priority,
          dueDate: data.dueDate,
        })).unwrap()
        // Handle assignment change separately via the dedicated assign endpoint
        const currentAssignee = editingTask.assignedTo ?? ''
        const newAssignee = data.assignedTo ?? ''
        if (newAssignee !== currentAssignee) {
          await dispatch(assignTaskAsync({ taskId: editingTask.id, assignedTo: newAssignee || null })).unwrap()
        }
        // Handle status change separately via the dedicated status endpoint
        if (data.status && data.status !== editingTask.status) {
          await dispatch(changeStatusAsync({ taskId: editingTask.id, status: data.status })).unwrap()
        }
      } else {
        await dispatch(createTaskAsync({ orgId, ...data })).unwrap()
      }
      setModalOpen(false)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Something went wrong. Please try again.'
      setSaveError(msg)
    }
  }

  const handleDelete = async (taskId: string) => {
    setSaveError(null)
    try {
      await dispatch(deleteTaskAsync(taskId)).unwrap()
      setModalOpen(false)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete task.'
      setSaveError(msg)
    }
  }

  if (!orgId) {
    return (
      <div className="p-6 text-center text-gray-500">
        Select or create an organization first.
      </div>
    )
  }

  return (
    <div className="p-6 flex flex-col gap-4 h-full">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-bold text-gray-800">Tasks</h1>
          {/* Org selector */}
          <select
            value={orgId ?? ''}
            onChange={(e) => {
              dispatch(selectOrg(e.target.value))
              dispatch(fetchMembers(e.target.value))
            }}
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white font-medium text-gray-700"
          >
            {orgs.map((o) => (
              <option key={o.id} value={o.id}>{o.name}</option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2 flex-1 justify-end flex-wrap">
          {/* My Tasks toggle */}
          <button
            onClick={() =>
              setFilterAssigneeId((prev) =>
                prev === (currentUser?.id ?? '') ? '' : (currentUser?.id ?? '')
              )
            }
            className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
              filterAssigneeId === (currentUser?.id ?? '') && currentUser
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
            }`}
          >
            My Tasks
          </button>

          {/* Filter by member */}
          <select
            value={filterAssigneeId === (currentUser?.id ?? '') && currentUser ? '' : filterAssigneeId}
            onChange={(e) => setFilterAssigneeId(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="">All members</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.name}
              </option>
            ))}
          </select>

          {(filterAssigneeId) && (
            <button
              onClick={() => setFilterAssigneeId('')}
              className="text-xs text-gray-400 hover:text-gray-600 underline"
            >
              Clear filter
            </button>
          )}

          <Button onClick={handleCreate}>+ New Task</Button>
        </div>
      </div>

      {loading ? (
        <div className="text-center text-gray-400 py-20">Loading tasks…</div>
      ) : (
        <div className="flex-1">
          <KanbanBoard onTaskClick={handleTaskClick} filterAssigneeId={filterAssigneeId || undefined} />
        </div>
      )}

      {modalOpen && (
        <TaskModal
          task={editingTask}
          onClose={() => { setSaveError(null); setModalOpen(false) }}
          onSave={handleSave}
          onDelete={handleDelete}
          error={saveError ?? undefined}
        />
      )}
    </div>
  )
}
