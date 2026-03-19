export type Role = 'ADMIN' | 'MANAGER' | 'MEMBER'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface User {
  id: string
  name: string
  email: string
  role: Role
  createdAt: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  userId: string
  name: string
  email: string
  role: Role
}

export interface Organization {
  id: string
  name: string
  createdById: string
  createdByName: string
  createdAt: string
  memberCount: number
}

export interface OrgMember {
  userId: string
  name: string
  email: string
  role: Role
  status: 'ACTIVE' | 'PENDING'
  joinedAt: string
}

export interface OrgInvitation {
  orgId: string
  orgName: string
  invitedAt: string
}

export interface Task {
  id: string
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  orgId: string
  assignedTo: string | null
  assignedToName: string | null
  createdBy: string
  createdByName: string | null
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: string
  taskId: string
  userId: string
  userName: string
  content: string
  createdAt: string
}

export interface Notification {
  id: string
  userId: string
  message: string
  eventType: string
  relatedTaskId: string | null
  relatedOrgId: string | null
  read: boolean
  createdAt: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string | null
  data: T
  timestamp: string
}
