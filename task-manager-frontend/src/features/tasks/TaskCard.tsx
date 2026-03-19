import type { Task } from '@/types'
import Badge from '@/components/ui/Badge'

interface Props {
  task: Task
  onClick: (task: Task) => void
}

export default function TaskCard({ task, onClick }: Props) {
  return (
    <div
      onClick={() => onClick(task)}
      className="bg-white rounded-lg border border-gray-200 p-3 shadow-sm hover:shadow-md cursor-pointer transition-shadow"
    >
      <p className="text-sm font-medium text-gray-800 mb-2 line-clamp-2">{task.title}</p>
      <div className="flex items-center justify-between">
        <Badge type="priority" value={task.priority} />
        {task.assignedToName && (
          <span className="text-xs text-gray-500 truncate max-w-[100px]">
            {task.assignedToName}
          </span>
        )}
      </div>
    </div>
  )
}
