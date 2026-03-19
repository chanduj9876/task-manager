import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd'
import type { DropResult } from '@hello-pangea/dnd'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { selectTasksForCurrentOrg, changeStatusAsync } from '@/features/tasks/tasksSlice'
import TaskCard from './TaskCard'
import type { Task, TaskStatus } from '@/types'

const COLUMNS: { id: TaskStatus; label: string }[] = [
  { id: 'TODO', label: 'To-Do' },
  { id: 'IN_PROGRESS', label: 'In Progress' },
  { id: 'IN_REVIEW', label: 'In Review' },
  { id: 'DONE', label: 'Done' },
]

interface Props {
  onTaskClick: (task: Task) => void
  filterAssigneeId?: string
}

export default function KanbanBoard({ onTaskClick, filterAssigneeId }: Props) {
  const dispatch = useAppDispatch()
  const tasks = useAppSelector(selectTasksForCurrentOrg)

  const tasksByStatus = (status: TaskStatus) =>
    tasks.filter((t) => t.status === status && (!filterAssigneeId || t.assignedTo === filterAssigneeId))

  const onDragEnd = (result: DropResult) => {
    if (!result.destination) return
    const newStatus = result.destination.droppableId as TaskStatus
    const taskId = result.draggableId
    const task = tasks.find((t) => t.id === taskId)
    if (task && task.status !== newStatus) {
      dispatch(changeStatusAsync({ taskId, status: newStatus }))
    }
  }

  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <div className="grid grid-cols-4 gap-4 h-full">
        {COLUMNS.map((col) => (
          <div key={col.id} className="flex flex-col">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-gray-600">{col.label}</h3>
              <span className="bg-gray-100 text-gray-500 text-xs px-2 py-0.5 rounded-full">
                {tasksByStatus(col.id).length}
              </span>
            </div>
            <Droppable droppableId={col.id}>
              {(provided, snapshot) => (
                <div
                  ref={provided.innerRef}
                  {...provided.droppableProps}
                  className={`flex-1 rounded-xl p-2 flex flex-col gap-2 min-h-[200px] transition-colors ${
                    snapshot.isDraggingOver ? 'bg-primary-50 border-2 border-primary-200' : 'bg-gray-50'
                  }`}
                >
                  {tasksByStatus(col.id).map((task, index) => (
                    <Draggable key={task.id} draggableId={task.id} index={index}>
                      {(drag) => (
                        <div
                          ref={drag.innerRef}
                          {...drag.draggableProps}
                          {...drag.dragHandleProps}
                        >
                          <TaskCard task={task} onClick={onTaskClick} />
                        </div>
                      )}
                    </Draggable>
                  ))}
                  {provided.placeholder}
                </div>
              )}
            </Droppable>
          </div>
        ))}
      </div>
    </DragDropContext>
  )
}
