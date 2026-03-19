import type { TaskPriority, TaskStatus } from '@/types'

const priorityConfig: Record<TaskPriority, { label: string; className: string }> = {
  LOW:      { label: 'Low',      className: 'bg-gray-100 text-gray-600' },
  MEDIUM:   { label: 'Medium',   className: 'bg-blue-100 text-blue-700' },
  HIGH:     { label: 'High',     className: 'bg-orange-100 text-orange-700' },
  CRITICAL: { label: 'Critical', className: 'bg-red-100 text-red-700' },
}

const statusConfig: Record<TaskStatus, { label: string; className: string }> = {
  TODO:        { label: 'To-Do',       className: 'bg-gray-100 text-gray-600' },
  IN_PROGRESS: { label: 'In Progress', className: 'bg-yellow-100 text-yellow-700' },
  IN_REVIEW:   { label: 'In Review',   className: 'bg-blue-100 text-blue-700' },
  DONE:        { label: 'Done',        className: 'bg-green-100 text-green-700' },
}

interface Props {
  type: 'priority' | 'status'
  value: TaskPriority | TaskStatus
}

export default function Badge({ type, value }: Props) {
  const config =
    type === 'priority'
      ? priorityConfig[value as TaskPriority]
      : statusConfig[value as TaskStatus]

  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${config.className}`}>
      {config.label}
    </span>
  )
}
