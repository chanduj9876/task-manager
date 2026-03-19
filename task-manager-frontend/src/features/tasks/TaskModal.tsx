import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import Modal from '@/components/ui/Modal'
import Button from '@/components/ui/Button'
import { useAppSelector } from '@/app/hooks'
import { taskApi } from '@/api/taskApi'
import type { Task, TaskPriority, TaskStatus, Comment } from '@/types'

interface FormData {
  title: string
  description: string
  priority: TaskPriority
  status: TaskStatus
  assignedTo: string
  dueDate: string
}

interface Props {
  task: Task | null
  onClose: () => void
  onSave: (data: { title: string; description?: string; priority: TaskPriority; status?: TaskStatus; assignedTo?: string; dueDate?: string }) => Promise<void>
  onDelete: (taskId: string) => Promise<void>
  error?: string
}

export default function TaskModal({ task, onClose, onSave, onDelete, error }: Props) {
  const members = useAppSelector((s) => s.org.members.filter((m) => m.status === 'ACTIVE'))
  const [comments, setComments] = useState<Comment[]>([])
  const [commentText, setCommentText] = useState('')
  const [commentLoading, setCommentLoading] = useState(false)
  const commentsEndRef = useRef<HTMLDivElement>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    defaultValues: {
      title: task?.title ?? '',
      description: task?.description ?? '',
      priority: task?.priority ?? 'MEDIUM',
      status: task?.status ?? 'TODO',
      assignedTo: task?.assignedTo ?? '',
      dueDate: task?.dueDate ?? '',
    },
  })

  // Load comments when editing an existing task
  useEffect(() => {
    if (!task) return
    taskApi.getComments(task.id).then((res) => setComments(res.data.data ?? [])).catch(() => {})
  }, [task])

  // Scroll to latest comment when list grows
  useEffect(() => {
    commentsEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [comments.length])

  const onSubmit = async (data: FormData) => {
    await onSave({
      title: data.title,
      description: data.description || undefined,
      priority: data.priority,
      status: data.status || undefined,
      assignedTo: data.assignedTo || undefined,
      dueDate: data.dueDate || undefined,
    })
  }

  const handleAddComment = async () => {
    if (!task || !commentText.trim()) return
    setCommentLoading(true)
    try {
      const res = await taskApi.addComment(task.id, commentText.trim())
      setComments((prev) => [...prev, res.data.data])
      setCommentText('')
    } catch {
      // silently ignore — could add toast here later
    } finally {
      setCommentLoading(false)
    }
  }

  const handleDeleteComment = async (commentId: string) => {
    try {
      await taskApi.deleteComment(commentId)
      setComments((prev) => prev.filter((c) => c.id !== commentId))
    } catch {}
  }

  return (
    <Modal title={task ? 'Edit Task' : 'New Task'} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Title *</label>
          <input
            {...register('title', { required: 'Title is required' })}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {errors.title && <p className="mt-1 text-xs text-red-600">{errors.title.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            rows={3}
            {...register('description')}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Priority *</label>
          <select
            {...register('priority', { required: true })}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        {task && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select
              {...register('status', { required: true })}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="TODO">To Do</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="IN_REVIEW">In Review</option>
              <option value="DONE">Done</option>
            </select>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Assign to</label>
          <select
            {...register('assignedTo')}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">— Unassigned —</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.name} ({m.role})
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Due Date</label>
          <input
            type="date"
            {...register('dueDate')}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        {/* Comments section — only visible when editing an existing task */}
        {task && (
          <div className="pt-2 border-t border-gray-100">
            <p className="text-sm font-medium text-gray-700 mb-2">
              Comments {comments.length > 0 && <span className="text-gray-400 font-normal">({comments.length})</span>}
            </p>

            {/* Comment list */}
            <div className="space-y-2 mb-3 pr-1">
              {comments.length === 0 ? (
                <p className="text-xs text-gray-400 py-2">No comments yet. Be the first to comment.</p>
              ) : (
                comments.map((c) => (
                  <div key={c.id} className="bg-gray-50 rounded-lg px-3 py-2 group relative">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-xs font-medium text-gray-700">{c.userName}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-gray-400">
                          {new Date(c.createdAt).toLocaleString()}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleDeleteComment(c.id)}
                          className="text-gray-300 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100 text-xs leading-none"
                          title="Delete comment"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">{c.content}</p>
                  </div>
                ))
              )}
              <div ref={commentsEndRef} />
            </div>

            {/* Add comment input */}
            <div className="flex gap-2">
              <input
                type="text"
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAddComment() } }}
                placeholder="Add a comment… (Enter to send)"
                className="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              <Button
                type="button"
                size="sm"
                onClick={handleAddComment}
                disabled={commentLoading || !commentText.trim()}
              >
                {commentLoading ? '…' : 'Post'}
              </Button>
            </div>
          </div>
        )}

        {error && (
          <p className="text-sm text-red-600 -mb-2">{error}</p>
        )}

        <div className="flex justify-between pt-2">
          {task && (
            <Button
              type="button"
              variant="danger"
              size="sm"
              onClick={() => onDelete(task.id)}
              disabled={isSubmitting}
            >
              Delete
            </Button>
          )}
          <div className="flex gap-2 ml-auto">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : 'Save'}
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  )
}
