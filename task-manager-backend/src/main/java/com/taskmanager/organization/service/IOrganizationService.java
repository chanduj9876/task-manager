package com.taskmanager.organization.service;

import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.organization.dto.*;
import com.taskmanager.user.enums.Role;

import java.util.List;
import java.util.UUID;

public interface IOrganizationService {
    OrgResponse createOrganization(CreateOrgRequest request, UserDetailsImpl currentUser);

    OrgResponse getOrganization(UUID orgId, UserDetailsImpl currentUser);

    OrgResponse updateOrganization(UUID orgId, UpdateOrgRequest request, UserDetailsImpl currentUser);

    OrgMemberResponse inviteUser(UUID orgId, InviteUserRequest request, UserDetailsImpl currentUser);
    OrgMemberResponse removeMember(UUID orgId, UUID memberId, UserDetailsImpl currentUser);
    OrgMemberResponse acceptInvitation(UUID orgId, UserDetailsImpl currentUser);
    void declineInvitation(UUID orgId, UserDetailsImpl currentUser);
    List<OrgInvitationResponse> getPendingInvitations(UserDetailsImpl currentUser);
    List<OrgMemberResponse> getMembers(UUID orgId, UserDetailsImpl currentUser);
    List<OrgResponse> getMyOrganizations(UserDetailsImpl currentUser);
    OrgMemberResponse changeMemberRole(UUID orgId, UUID memberId, Role newRole, UserDetailsImpl currentUser);
    boolean isManagerOrAdminInOrg(UUID userId, UUID orgId);
    void assertMembership(UUID userId, UUID orgId, Role requiredRole);
}
