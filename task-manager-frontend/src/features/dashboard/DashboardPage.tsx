import { useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { selectTasksForCurrentOrg } from '@/features/tasks/tasksSlice'
import { fetchTasks } from '@/features/tasks/tasksSlice'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts'

const STATUS_COLORS = {
  TODO: '#6b7280',
  IN_PROGRESS: '#f59e0b',
  IN_REVIEW: '#3b82f6',
  DONE: '#10b981',
}

const PRIORITY_COLORS = {
  LOW: '#6b7280',
  MEDIUM: '#3b82f6',
  HIGH: '#f97316',
  CRITICAL: '#ef4444',
}

export default function DashboardPage() {
  const dispatch = useAppDispatch()
  const orgId = useAppSelector((s) => s.org.selectedOrgId)
  const tasks = useAppSelector(selectTasksForCurrentOrg)
  const user = useAppSelector((s) => s.auth.user)

  useEffect(() => {
    if (orgId) dispatch(fetchTasks({ orgId }))
  }, [orgId, dispatch])

  const byStatus = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'].map((s) => ({
    name: s.replace('_', ' '),
    count: tasks.filter((t) => t.status === s).length,
    fill: STATUS_COLORS[s as keyof typeof STATUS_COLORS],
  }))

  const byPriority = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((p) => ({
    name: p,
    value: tasks.filter((t) => t.priority === p).length,
    fill: PRIORITY_COLORS[p as keyof typeof PRIORITY_COLORS],
  }))

  const myTasks = user ? tasks.filter((t) => t.assignedTo === user.id) : []

  const stats = [
    { label: 'Total Tasks', value: tasks.length, color: 'text-gray-700' },
    { label: 'In Progress', value: tasks.filter((t) => t.status === 'IN_PROGRESS').length, color: 'text-yellow-600' },
    { label: 'Done', value: tasks.filter((t) => t.status === 'DONE').length, color: 'text-green-600' },
    { label: 'Assigned to Me', value: myTasks.length, color: 'text-blue-600' },
  ]

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-xl font-bold text-gray-800">Dashboard</h1>

      {!orgId && (
        <div className="rounded-xl border border-gray-200 bg-white p-8 text-center text-gray-500">
          Select or create an organization to see your dashboard.
        </div>
      )}

      {orgId && (
        <>
          {/* Stat cards */}
          <div className="grid grid-cols-4 gap-4">
            {stats.map((s) => (
              <div key={s.label} className="bg-white rounded-xl border border-gray-200 p-5">
                <p className="text-sm text-gray-500">{s.label}</p>
                <p className={`text-3xl font-bold mt-1 ${s.color}`}>{s.value}</p>
              </div>
            ))}
          </div>

          {/* Charts */}
          <div className="grid grid-cols-2 gap-6">
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h2 className="text-sm font-semibold text-gray-600 mb-4">Tasks by Status</h2>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={byStatus}>
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                    {byStatus.map((entry, i) => (
                      <Cell key={i} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h2 className="text-sm font-semibold text-gray-600 mb-4">Tasks by Priority</h2>
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie data={byPriority} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={75}>
                    {byPriority.map((entry, i) => (
                      <Cell key={i} fill={entry.fill} />
                    ))}
                  </Pie>
                  <Legend iconSize={10} />
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
