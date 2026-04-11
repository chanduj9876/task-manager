import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import {
  fetchMyOrgs,
  fetchMembers,
  createOrgAsync,
  inviteUserAsync,
  selectOrg,
  fetchPendingInvitations,
  acceptInvitationAsync,
  declineInvitationAsync,
  removeMemberAsync,
  changeMemberRoleAsync,
} from '@/features/organizations/orgSlice'
import Button from '@/components/ui/Button'
import type { Role } from '@/types'

interface CreateOrgForm {
  name: string
}

interface InviteForm {
  email: string
  role: Role
}

export default function OrgManagementPage() {
  const dispatch = useAppDispatch()
  const { orgs, members, selectedOrgId, pendingInvitations } = useAppSelector((s) => s.org)
  const currentUserId = useAppSelector((s) => s.auth.user?.id)
  const [showCreateOrg, setShowCreateOrg] = useState(false)
  const [showInvite, setShowInvite] = useState(false)
  const [forceAdd, setForceAdd] = useState(false)
  const [inviteMsg, setInviteMsg] = useState<string | null>(null)
  const [inviteError, setInviteError] = useState<string | null>(null)
  const [createOrgError, setCreateOrgError] = useState<string | null>(null)
  const [removingId, setRemovingId] = useState<string | null>(null)
  const [changingRoleId, setChangingRoleId] = useState<string | null>(null)

  const createForm = useForm<CreateOrgForm>()
  const inviteForm = useForm<InviteForm>()

  useEffect(() => {
    dispatch(fetchMyOrgs())
    dispatch(fetchPendingInvitations())
  }, [dispatch])

  useEffect(() => {
    if (selectedOrgId) dispatch(fetchMembers(selectedOrgId))
  }, [selectedOrgId, dispatch])

  const onCreateOrg = createForm.handleSubmit(async (data) => {
    setCreateOrgError(null)
    const result = await dispatch(createOrgAsync(data.name))
    if (createOrgAsync.fulfilled.match(result)) {
      createForm.reset()
      setShowCreateOrg(false)
    } else {
      const msg = (result.payload as string) ?? (result.error?.message ?? 'Failed to create organization.')
      setCreateOrgError(msg)
    }
  })

  const onInvite = inviteForm.handleSubmit(async (data) => {
    if (!selectedOrgId) return
    setInviteMsg(null)
    setInviteError(null)
    const result = await dispatch(inviteUserAsync({ orgId: selectedOrgId, email: data.email, force: forceAdd, role: data.role }))
    if (inviteUserAsync.fulfilled.match(result)) {
      const status = result.payload.status
      setInviteMsg(
        status === 'ACTIVE'
          ? `${data.email} has been added to the organization.`
          : `Invitation sent to ${data.email}. They need to accept before joining.`
      )
      inviteForm.reset()
    } else {
      const msg = (result.payload as string) ?? (result.error?.message ?? 'Failed to invite user.')
      setInviteError(msg)
    }
  })

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Organizations</h1>
        <Button size="sm" onClick={() => setShowCreateOrg((v) => !v)}>
          + New Organization
        </Button>
      </div>

      {/* Pending invitations banner */}
      {pendingInvitations.length > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 space-y-2">
          <p className="text-sm font-semibold text-yellow-800">Pending Invitations</p>
          {pendingInvitations.map((inv) => (
            <div key={inv.orgId} className="flex items-center justify-between bg-white rounded-lg px-4 py-2 border border-yellow-100">
              <div>
                <p className="text-sm font-medium text-gray-800">{inv.orgName}</p>
                <p className="text-xs text-gray-400">Invited {new Date(inv.invitedAt).toLocaleDateString()}</p>
              </div>
              <div className="flex gap-2">
                <Button
                  size="sm"
                  onClick={async () => {
                    await dispatch(acceptInvitationAsync(inv.orgId))
                    dispatch(fetchMyOrgs())
                  }}
                >
                  Accept
                </Button>
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => dispatch(declineInvitationAsync(inv.orgId))}
                >
                  Decline
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreateOrg && (
        <form onSubmit={onCreateOrg} className="bg-white border border-gray-200 rounded-xl p-4 space-y-3 max-w-md">
          <h2 className="font-semibold text-gray-700">Create Organization</h2>
          <input
            {...createForm.register('name', { required: true })}
            placeholder="Organization name"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {createOrgError && (
            <p className="text-xs text-red-600">{createOrgError}</p>
          )}
          <div className="flex gap-2">
            <Button type="submit" size="sm">Create</Button>
            <Button type="button" variant="secondary" size="sm" onClick={() => { setShowCreateOrg(false); setCreateOrgError(null) }}>Cancel</Button>
          </div>
        </form>
      )}

      {/* Org list */}
      <div className="grid grid-cols-3 gap-4">
        {orgs.map((org) => (
          <div
            key={org.id}
            className={`bg-white rounded-xl border p-4 cursor-pointer transition-colors ${
              org.id === selectedOrgId ? 'border-primary-500 ring-1 ring-primary-400' : 'border-gray-200 hover:border-gray-300'
            }`}
            onClick={() => { dispatch(selectOrg(org.id)); dispatch(fetchMembers(org.id)) }}
          >
            <h3 className="font-semibold text-gray-800 truncate">{org.name}</h3>
            <p className="text-xs text-gray-400 mt-1">{new Date(org.createdAt).toLocaleDateString()}</p>
          </div>
        ))}
      </div>

      {/* Members section */}
      {selectedOrgId && (
        <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-700">Members</h2>
            <Button size="sm" variant="secondary" onClick={() => { setShowInvite((v) => !v); setInviteMsg(null) }}>
              Invite Member
            </Button>
          </div>

          {showInvite && (
            <div className="space-y-3 max-w-sm">
              <form onSubmit={onInvite} className="flex gap-2">
                <input
                  {...inviteForm.register('email', { required: true })}
                  type="email"
                  placeholder="user@example.com"
                  className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <select
                  {...inviteForm.register('role')}
                  defaultValue="MEMBER"
                  className="border border-gray-300 rounded-lg px-2 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="MEMBER">Member</option>
                  <option value="MANAGER">Manager</option>
                  <option value="ADMIN">Admin</option>
                </select>
                <Button type="submit" size="sm">Send</Button>
              </form>
              {/* Force-add toggle */}
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <div
                  onClick={() => setForceAdd((v) => !v)}
                  className={`relative w-9 h-5 rounded-full transition-colors ${
                    forceAdd ? 'bg-primary-600' : 'bg-gray-300'
                  }`}
                >
                  <span
                    className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                      forceAdd ? 'translate-x-4' : 'translate-x-0'
                    }`}
                  />
                </div>
                <span className="text-sm text-gray-600">
                  {forceAdd
                    ? <><strong>Force add</strong> — member joins immediately</>  
                    : <><strong>Send invitation</strong> — member must accept first</>}
                </span>
              </label>
              {inviteMsg && (
                <p className="text-xs text-green-600">{inviteMsg}</p>
              )}
              {inviteError && (
                <p className="text-xs text-red-600">{inviteError}</p>
              )}
            </div>
          )}

          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-400 border-b border-gray-100">
                <th className="pb-2 font-medium">Name</th>
                <th className="pb-2 font-medium">Email</th>
                <th className="pb-2 font-medium">Role</th>
                <th className="pb-2 font-medium">Status</th>
                <th className="pb-2 font-medium">Joined</th>
                <th className="pb-2 font-medium"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {members.map((m) => (
                <tr key={m.userId} className="hover:bg-gray-50">
                  <td className="py-2 text-gray-800">{m.name}</td>
                  <td className="py-2 text-gray-500">{m.email}</td>
                  <td className="py-2">
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                      {m.role}
                    </span>
                  </td>
                  <td className="py-2">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                      m.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                    }`}>
                      {m.status === 'ACTIVE' ? 'Active' : 'Pending'}
                    </span>
                  </td>
                  <td className="py-2 text-gray-400">{new Date(m.joinedAt).toLocaleDateString()}</td>
                  <td className="py-2">
                    {m.userId !== currentUserId && (
                      <div className="flex items-center gap-2">
                        <select
                          value={m.role}
                          disabled={changingRoleId === m.userId}
                          onChange={async (e) => {
                            if (!selectedOrgId) return
                            setChangingRoleId(m.userId)
                            await dispatch(changeMemberRoleAsync({ orgId: selectedOrgId, memberId: m.userId, role: e.target.value as Role }))
                            setChangingRoleId(null)
                          }}
                          className="text-xs border border-gray-200 rounded px-1 py-0.5 focus:outline-none focus:ring-1 focus:ring-primary-500"
                        >
                          <option value="MEMBER">Member</option>
                          <option value="MANAGER">Manager</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                        <button
                          onClick={async () => {
                            if (!selectedOrgId) return
                            setRemovingId(m.userId)
                            await dispatch(removeMemberAsync({ orgId: selectedOrgId, memberId: m.userId }))
                            setRemovingId(null)
                          }}
                          disabled={removingId === m.userId}
                          className="text-xs text-red-500 hover:text-red-700 font-medium disabled:opacity-50"
                        >
                          {removingId === m.userId ? 'Removing…' : 'Remove'}
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
