import axiosClient from './axiosClient'
import type { ApiResponse, Organization, OrgMember, OrgInvitation, Role } from '@/types'

export const orgApi = {
  createOrg: (name: string) =>
    axiosClient.post<ApiResponse<Organization>>('/api/orgs', { name }),

  getMyOrgs: () =>
    axiosClient.get<ApiResponse<Organization[]>>('/api/orgs/mine'),

  getMembers: (orgId: string) =>
    axiosClient.get<ApiResponse<OrgMember[]>>(`/api/orgs/${orgId}/members`),

  inviteUser: (orgId: string, email: string, force: boolean, role?: Role) =>
    axiosClient.post<ApiResponse<OrgMember>>(`/api/orgs/${orgId}/invite`, { email, force, role: role ?? 'MEMBER' }),

  getPendingInvitations: () =>
    axiosClient.get<ApiResponse<OrgInvitation[]>>('/api/orgs/invitations/pending'),

  acceptInvitation: (orgId: string) =>
    axiosClient.post<ApiResponse<OrgMember>>(`/api/orgs/${orgId}/invitations/accept`),

  declineInvitation: (orgId: string) =>
    axiosClient.post<ApiResponse<void>>(`/api/orgs/${orgId}/invitations/decline`),

  removeMember: (orgId: string, memberId: string) =>
    axiosClient.delete<ApiResponse<void>>(`/api/orgs/${orgId}/members/${memberId}`),

  changeMemberRole: (orgId: string, memberId: string, role: Role) =>
    axiosClient.put<ApiResponse<OrgMember>>(`/api/orgs/${orgId}/members/${memberId}/role`, { role }),
}
