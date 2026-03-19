import { useEffect, useState } from 'react'
import axiosClient from '@/api/axiosClient'

interface AuditLog {
  id: string
  entityType: string
  entityId: string
  action: string
  performedBy: string
  performedByName: string | null
  details: string | null
  timestamp: string
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

const ACTION_COLORS: Record<string, string> = {
  CREATE: 'bg-green-100 text-green-700',
  ASSIGN: 'bg-blue-100 text-blue-700',
  STATUS_CHANGE: 'bg-purple-100 text-purple-700',
  MEMBER_INVITE: 'bg-yellow-100 text-yellow-700',
  MEMBER_REMOVE: 'bg-red-100 text-red-700',
  INVITATION_ACCEPT: 'bg-emerald-100 text-emerald-700',
  INVITATION_DECLINE: 'bg-gray-100 text-gray-600',
  DELETE: 'bg-red-100 text-red-700',
  UPDATE: 'bg-blue-100 text-blue-700',
}

export default function AuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [entityType, setEntityType] = useState('TASK')
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)

  useEffect(() => {
    axiosClient
      .get<{ data: PageResponse<AuditLog> }>('/api/audit', { 
        params: { entityType, page, size: 20 } 
      })
      .then((r) => {
        setLogs(r.data.data?.content ?? [])
        setTotalElements(r.data.data?.totalElements ?? 0)
      })
      .catch(() => {})
  }, [entityType, page])

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Audit Logs</h1>
        <select
          value={entityType}
          onChange={(e) => setEntityType(e.target.value)}
          className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="TASK">Tasks</option>
          <option value="ORGANIZATION">Organizations</option>
          <option value="COMMENT">Comments</option>
        </select>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr className="text-left text-gray-500">
              <th className="px-4 py-3 font-medium">Action</th>
              <th className="px-4 py-3 font-medium">Details</th>
              <th className="px-4 py-3 font-medium">Performed By</th>
              <th className="px-4 py-3 font-medium">Entity ID</th>
              <th className="px-4 py-3 font-medium">Time</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {logs.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-400">No audit logs found</td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${
                      ACTION_COLORS[log.action] ?? 'bg-gray-100 text-gray-600'
                    }`}>
                      {log.action.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-700 max-w-xs truncate">
                    {log.details ?? <span className="text-gray-300">—</span>}
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-medium text-gray-800">
                      {log.performedByName ?? <span className="text-gray-400 font-mono text-xs">{log.performedBy}</span>}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-400 font-mono text-xs truncate max-w-[140px]" title={log.entityId}>
                    {log.entityId.slice(0, 8)}…
                  </td>
                  <td className="px-4 py-3 text-gray-400 whitespace-nowrap">
                    {new Date(log.timestamp).toLocaleString()}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      {totalElements > 0 && (
        <div className="flex items-center justify-between px-4">
          <p className="text-sm text-gray-600">
            Showing {logs.length} of {totalElements} logs
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 text-sm font-medium rounded-lg border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Previous
            </button>
            <span className="px-3 py-1.5 text-sm text-gray-600">
              Page {page + 1}
            </span>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={logs.length < 20}
              className="px-3 py-1.5 text-sm font-medium rounded-lg border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

