package com.taskmanager.organization.service;

import com.taskmanager.common.exception.AppException;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.audit.annotation.Auditable;
import com.taskmanager.notification.dto.TaskEventDto;
import com.taskmanager.notification.kafka.NotificationProducer;
import com.taskmanager.organization.dto.*;
import com.taskmanager.organization.entity.InvitationStatus;
import com.taskmanager.organization.entity.Organization;
import com.taskmanager.organization.entity.UserOrganization;
import com.taskmanager.organization.repository.OrganizationRepository;
import com.taskmanager.organization.repository.UserOrganizationRepository;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.enums.Role;
import com.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService implements IOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    @Transactional
    @Auditable(entityType = "ORGANIZATION", action = "CREATE")
    @CacheEvict(value = "userOrgs", key = "#currentUser.id")
    public OrgResponse createOrganization(CreateOrgRequest request, UserDetailsImpl currentUser) {
        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Organization org = Organization.builder()
                .name(request.getName())
                .createdBy(creator)
                .build();
        organizationRepository.save(org);

        // Creator automatically becomes ADMIN of the org
        UserOrganization membership = UserOrganization.builder()
                .userId(creator.getId())
                .orgId(org.getId())
                .role(Role.ADMIN)
                .build();
        userOrganizationRepository.save(membership);

        return toOrgResponse(org, 1);
    }

        public OrgResponse getOrganization(UUID orgId, UserDetailsImpl currentUser) {
                assertMembership(currentUser.getId(), orgId, null);
                Organization org = organizationRepository.findById(orgId)
                                .orElseThrow(() -> new AppException("Organization not found", HttpStatus.NOT_FOUND));
                int count = userOrganizationRepository.findByOrgId(org.getId()).size();
                return toOrgResponse(org, count);
        }

        @Transactional
        @CacheEvict(value = "userOrgs", allEntries = true)
        public OrgResponse updateOrganization(UUID orgId, UpdateOrgRequest request, UserDetailsImpl currentUser) {
                assertMembership(currentUser.getId(), orgId, Role.ADMIN);

                Organization org = organizationRepository.findById(orgId)
                                .orElseThrow(() -> new AppException("Organization not found", HttpStatus.NOT_FOUND));
                org.setName(request.getName().trim());
                organizationRepository.save(org);

                int count = userOrganizationRepository.findByOrgId(org.getId()).size();
                return toOrgResponse(org, count);
        }

    @Transactional
    @Auditable(entityType = "ORGANIZATION", action = "MEMBER_INVITE")
    @Caching(evict = {
            @CacheEvict(value = "orgMembers", key = "#orgId"),
            @CacheEvict(value = "userOrgs", allEntries = true)
    })
    public OrgMemberResponse inviteUser(UUID orgId, InviteUserRequest request,
                                        UserDetailsImpl currentUser) {
        assertMembership(currentUser.getId(), orgId, Role.ADMIN);

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new AppException("Organization not found", HttpStatus.NOT_FOUND));

        User invitee = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("User with that email not found",
                        HttpStatus.NOT_FOUND));

        if (userOrganizationRepository.existsByUserIdAndOrgId(invitee.getId(), orgId)) {
            throw new AppException("User is already a member of this organization",
                    HttpStatus.CONFLICT);
        }

        InvitationStatus status = request.isForce() ? InvitationStatus.ACTIVE : InvitationStatus.PENDING;
        Role assignedRole = request.getRole() != null ? request.getRole() : Role.MEMBER;

        UserOrganization membership = UserOrganization.builder()
                .userId(invitee.getId())
                .orgId(orgId)
                .role(assignedRole)
                .status(status)
                .build();
        userOrganizationRepository.save(membership);

        // Notify invitee via Kafka only for PENDING invitations
        if (status == InvitationStatus.PENDING) {
            User inviter = userRepository.findById(currentUser.getId())
                    .orElse(null);
            String inviterName = inviter != null ? inviter.getName() : "Someone";
            notificationProducer.publishTaskEvent(TaskEventDto.builder()
                    .eventType("ORG_INVITATION_SENT")
                    .actorId(currentUser.getId())
                    .actorName(inviterName)
                    .assigneeId(invitee.getId())
                    .relatedOrgId(orgId)
                    .orgId(orgId)
                    .orgName(org.getName())
                    .build());
        }

        return OrgMemberResponse.builder()
                .userId(invitee.getId())
                .name(invitee.getName())
                .email(invitee.getEmail())
                .role(assignedRole)
                .status(status)
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    @Transactional
    @Auditable(entityType = "ORGANIZATION", action = "MEMBER_REMOVE")
    @Caching(evict = {
            @CacheEvict(value = "orgMembers", key = "#orgId"),
            @CacheEvict(value = "userOrgs", key = "#memberId")
    })
    public OrgMemberResponse removeMember(UUID orgId, UUID memberId, UserDetailsImpl currentUser) {
        assertMembership(currentUser.getId(), orgId, Role.ADMIN);

        if (currentUser.getId().equals(memberId)) {
            throw new AppException("You cannot remove yourself from the organization",
                    HttpStatus.BAD_REQUEST);
        }

        UserOrganization membership = userOrganizationRepository
                .findByUserIdAndOrgId(memberId, orgId)
                .orElseThrow(() -> new AppException("Member not found in this organization",
                        HttpStatus.NOT_FOUND));

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        userOrganizationRepository.delete(membership);

        return OrgMemberResponse.builder()
                .userId(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .role(membership.getRole())
                .status(membership.getStatus())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    @Transactional
    @Auditable(entityType = "ORGANIZATION", action = "INVITATION_ACCEPT")
    @Caching(evict = {
            @CacheEvict(value = "orgMembers", key = "#orgId"),
            @CacheEvict(value = "userOrgs", key = "#currentUser.id")
    })
    public OrgMemberResponse acceptInvitation(UUID orgId, UserDetailsImpl currentUser) {
        UserOrganization membership = userOrganizationRepository
                .findByUserIdAndOrgId(currentUser.getId(), orgId)
                .filter(m -> m.getStatus() == InvitationStatus.PENDING)
                .orElseThrow(() -> new AppException("No pending invitation found for this organization",
                        HttpStatus.NOT_FOUND));

        membership.setStatus(InvitationStatus.ACTIVE);
        userOrganizationRepository.save(membership);

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Notify the org admin (creator) that invitation was accepted
        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org != null) {
            notificationProducer.publishTaskEvent(TaskEventDto.builder()
                    .eventType("ORG_INVITATION_ACCEPTED")
                    .actorId(currentUser.getId())
                    .actorName(user.getName())
                    .assigneeId(org.getCreatedBy().getId())
                    .relatedOrgId(orgId)
                    .orgId(orgId)
                    .orgName(org.getName())
                    .build());
        }

        return OrgMemberResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(membership.getRole())
                .status(membership.getStatus())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    @Transactional
    @Auditable(entityType = "ORGANIZATION", action = "INVITATION_DECLINE")
    public void declineInvitation(UUID orgId, UserDetailsImpl currentUser) {
        UserOrganization membership = userOrganizationRepository
                .findByUserIdAndOrgId(currentUser.getId(), orgId)
                .filter(m -> m.getStatus() == InvitationStatus.PENDING)
                .orElseThrow(() -> new AppException("No pending invitation found for this organization",
                        HttpStatus.NOT_FOUND));
        userOrganizationRepository.delete(membership);
    }

    public List<OrgInvitationResponse> getPendingInvitations(UserDetailsImpl currentUser) {
        return userOrganizationRepository
                .findByUserIdAndStatus(currentUser.getId(), InvitationStatus.PENDING)
                .stream()
                .map(uo -> {
                    Organization org = organizationRepository.findById(uo.getOrgId())
                            .orElseThrow(() -> new AppException("Organization not found",
                                    HttpStatus.INTERNAL_SERVER_ERROR));
                    return OrgInvitationResponse.builder()
                            .orgId(org.getId())
                            .orgName(org.getName())
                            .invitedAt(uo.getJoinedAt())
                            .build();
                }).toList();
    }

    @Cacheable(value = "orgMembers", key = "#orgId")
    public List<OrgMemberResponse> getMembers(UUID orgId, UserDetailsImpl currentUser) {
        assertMembership(currentUser.getId(), orgId, null);

        return userOrganizationRepository.findByOrgId(orgId).stream()
                .map(uo -> {
                    User member = userRepository.findById(uo.getUserId())
                            .orElseThrow(() -> new AppException("User not found",
                                    HttpStatus.INTERNAL_SERVER_ERROR));
                    return OrgMemberResponse.builder()
                            .userId(member.getId())
                            .name(member.getName())
                            .email(member.getEmail())
                            .role(uo.getRole())
                            .status(uo.getStatus())
                            .joinedAt(uo.getJoinedAt())
                            .build();
                }).toList();
    }

    @Cacheable(value = "userOrgs", key = "#currentUser.id")
    public List<OrgResponse> getMyOrganizations(UserDetailsImpl currentUser) {
        return organizationRepository.findAllByUserId(currentUser.getId()).stream()
                .map(org -> {
                    int count = userOrganizationRepository.findByOrgId(org.getId()).size();
                    return toOrgResponse(org, count);
                }).toList();
    }

    public boolean isManagerOrAdminInOrg(UUID userId, UUID orgId) {
        return userOrganizationRepository.findByUserIdAndOrgId(userId, orgId)
                .filter(m -> m.getStatus() == InvitationStatus.ACTIVE)
                .map(m -> m.getRole() == Role.ADMIN || m.getRole() == Role.MANAGER)
                .orElse(false);
    }

    @Transactional
    @Auditable(entityType = "ORGANIZATION", action = "ROLE_CHANGE")
    @CacheEvict(value = "orgMembers", key = "#orgId")
    public OrgMemberResponse changeMemberRole(UUID orgId, UUID memberId, Role newRole,
                                               UserDetailsImpl currentUser) {
        assertMembership(currentUser.getId(), orgId, Role.ADMIN);

        if (currentUser.getId().equals(memberId)) {
            throw new AppException("You cannot change your own role", HttpStatus.BAD_REQUEST);
        }

        UserOrganization membership = userOrganizationRepository
                .findByUserIdAndOrgId(memberId, orgId)
                .orElseThrow(() -> new AppException("Member not found in this organization",
                        HttpStatus.NOT_FOUND));

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        membership.setRole(newRole);
        userOrganizationRepository.save(membership);

        return OrgMemberResponse.builder()
                .userId(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .role(newRole)
                .status(membership.getStatus())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    public void assertMembership(UUID userId, UUID orgId, Role requiredRole) {
        UserOrganization membership = userOrganizationRepository
                .findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new AppException(
                        "You are not a member of this organization", HttpStatus.FORBIDDEN));

        if (membership.getStatus() == InvitationStatus.PENDING) {
            throw new AppException(
                    "Your invitation is pending — please accept it first", HttpStatus.FORBIDDEN);
        }

        if (requiredRole != null && membership.getRole() != requiredRole) {
            throw new AppException(
                    "Insufficient permissions — " + requiredRole + " role required",
                    HttpStatus.FORBIDDEN);
        }
    }

    private OrgResponse toOrgResponse(Organization org, int memberCount) {
        return OrgResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .createdById(org.getCreatedBy().getId())
                .createdByName(org.getCreatedBy().getName())
                .createdAt(org.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }
}
