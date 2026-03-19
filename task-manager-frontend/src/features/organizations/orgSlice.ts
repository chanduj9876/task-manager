import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { orgApi } from '@/api/orgApi'
import type { Organization, OrgMember, OrgInvitation, Role } from '@/types'

interface OrgState {
  orgs: Organization[]
  members: OrgMember[]
  pendingInvitations: OrgInvitation[]
  selectedOrgId: string | null
  loading: boolean
}

const initialState: OrgState = {
  orgs: [],
  members: [],
  pendingInvitations: [],
  selectedOrgId: null,
  loading: false,
}

export const fetchMyOrgs = createAsyncThunk('org/fetchMine', async () => {
  const res = await orgApi.getMyOrgs()
  return res.data.data
})

export const fetchMembers = createAsyncThunk('org/fetchMembers', async (orgId: string) => {
  const res = await orgApi.getMembers(orgId)
  return res.data.data
})

export const createOrgAsync = createAsyncThunk('org/create', async (name: string) => {
  const res = await orgApi.createOrg(name)
  return res.data.data
})

export const inviteUserAsync = createAsyncThunk(
  'org/invite',
  async (payload: { orgId: string; email: string; force: boolean; role?: Role }) => {
    const res = await orgApi.inviteUser(payload.orgId, payload.email, payload.force, payload.role)
    return res.data.data
  },
)

export const fetchPendingInvitations = createAsyncThunk('org/fetchPending', async () => {
  const res = await orgApi.getPendingInvitations()
  return res.data.data
})

export const acceptInvitationAsync = createAsyncThunk(
  'org/acceptInvitation',
  async (orgId: string) => {
    const res = await orgApi.acceptInvitation(orgId)
    return { orgId, member: res.data.data }
  },
)

export const declineInvitationAsync = createAsyncThunk(
  'org/declineInvitation',
  async (orgId: string) => {
    await orgApi.declineInvitation(orgId)
    return orgId
  },
)

export const removeMemberAsync = createAsyncThunk(
  'org/removeMember',
  async (payload: { orgId: string; memberId: string }) => {
    await orgApi.removeMember(payload.orgId, payload.memberId)
    return payload.memberId
  },
)

export const changeMemberRoleAsync = createAsyncThunk(
  'org/changeMemberRole',
  async (payload: { orgId: string; memberId: string; role: Role }) => {
    const res = await orgApi.changeMemberRole(payload.orgId, payload.memberId, payload.role)
    return res.data.data
  },
)

const orgSlice = createSlice({
  name: 'org',
  initialState,
  reducers: {
    selectOrg(state, action) {
      state.selectedOrgId = action.payload
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMyOrgs.pending, (state) => { state.loading = true })
      .addCase(fetchMyOrgs.fulfilled, (state, action) => {
        state.loading = false
        state.orgs = action.payload
        if (!state.selectedOrgId && action.payload.length > 0) {
          state.selectedOrgId = action.payload[0].id
        }
      })
      .addCase(fetchMyOrgs.rejected, (state) => { state.loading = false })
      .addCase(fetchMembers.fulfilled, (state, action) => {
        state.members = action.payload
      })
      .addCase(createOrgAsync.fulfilled, (state, action) => {
        state.orgs.push(action.payload)
        state.selectedOrgId = action.payload.id
      })
      .addCase(inviteUserAsync.fulfilled, (state, action) => {
        // Only add to local members list if force-added (ACTIVE), not pending
        if (action.payload.status === 'ACTIVE') {
          state.members.push(action.payload)
        }
      })
      .addCase(fetchPendingInvitations.fulfilled, (state, action) => {
        state.pendingInvitations = action.payload
      })
      .addCase(acceptInvitationAsync.fulfilled, (state, action) => {
        state.pendingInvitations = state.pendingInvitations.filter(
          (inv) => inv.orgId !== action.payload.orgId
        )
      })
      .addCase(declineInvitationAsync.fulfilled, (state, action) => {
        state.pendingInvitations = state.pendingInvitations.filter(
          (inv) => inv.orgId !== action.payload
        )
      })
      .addCase(removeMemberAsync.fulfilled, (state, action) => {
        state.members = state.members.filter((m) => m.userId !== action.payload)
      })
      .addCase(changeMemberRoleAsync.fulfilled, (state, action) => {
        const updated = action.payload
        state.members = state.members.map((m) =>
          m.userId === updated.userId ? { ...m, role: updated.role } : m
        )
      })
  },
})

export const { selectOrg } = orgSlice.actions
export default orgSlice.reducer
