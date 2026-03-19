import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom'
import ProtectedRoute from '@/components/layout/ProtectedRoute'
import LoginPage from '@/features/auth/LoginPage'
import SignupPage from '@/features/auth/SignupPage'
import DashboardPage from '@/features/dashboard/DashboardPage'
import TasksPage from '@/features/tasks/TasksPage'
import OrgManagementPage from '@/features/organizations/OrgManagementPage'
import Navbar from '@/components/layout/Navbar'
import Sidebar from '@/components/layout/Sidebar'
import NotificationsPage from '@/features/notifications/NotificationsPage'

const AuditPage = lazy(() => import('@/features/audit/AuditPage'))

function AppLayout() {
  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />
      <div className="flex flex-col flex-1 overflow-hidden">
        <Navbar />
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/signup', element: <SignupPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/tasks', element: <TasksPage /> },
          { path: '/organizations', element: <OrgManagementPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          {
            element: <ProtectedRoute allowedRoles={['ADMIN']} />,
            children: [
              {
                path: '/audit',
                element: (
                  <Suspense fallback={<div className="p-6 text-gray-400">Loading…</div>}>
                    <AuditPage />
                  </Suspense>
                ),
              },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
])

export default router
