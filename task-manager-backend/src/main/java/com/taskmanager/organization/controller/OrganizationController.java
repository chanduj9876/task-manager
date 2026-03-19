package com.taskmanager.organization.controller;

import com.taskmanager.common.response.ApiResponse;
import com.taskmanager.common.security.UserDetailsImpl;
import com.taskmanager.organization.dto.*;
import com.taskmanager.organization.service.IOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Organizations", description = "Manage organizations, members, and invitations")
@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final IOrganizationService organizationService;

    @Operation(summary = "Create a new organization")
    @PostMapping
    public ResponseEntity<ApiResponse<OrgResponse>> createOrg(
            @Valid @RequestBody CreateOrgRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        OrgResponse response = organizationService.createOrganization(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Organization created"));
    }

        @Operation(summary = "Get an organization by ID")
        @GetMapping("/{orgId}")
        public ResponseEntity<ApiResponse<OrgResponse>> getOrg(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
            organizationService.getOrganization(orgId, currentUser)));
        }

        @Operation(summary = "Update organization name (admin only)")
        @PutMapping("/{orgId}")
        public ResponseEntity<ApiResponse<OrgResponse>> updateOrg(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrgRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
            organizationService.updateOrganization(orgId, request, currentUser),
            "Organization updated"));
        }

    @Operation(summary = "Invite a user to the organization")
    @PostMapping("/{orgId}/invite")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> inviteUser(
            @PathVariable UUID orgId,
            @Valid @RequestBody InviteUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        OrgMemberResponse member = organizationService.inviteUser(orgId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(member, "User invited successfully"));
    }

    @Operation(summary = "List all active members of an organization")
    @GetMapping("/{orgId}/members")
    public ResponseEntity<ApiResponse<List<OrgMemberResponse>>> getMembers(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<OrgMemberResponse> members = organizationService.getMembers(orgId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @Operation(summary = "List organizations the current user belongs to")
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getMyOrgs(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<OrgResponse> orgs = organizationService.getMyOrganizations(currentUser);
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    @Operation(summary = "Get pending invitations for the current user")
    @GetMapping("/invitations/pending")
    public ResponseEntity<ApiResponse<List<OrgInvitationResponse>>> getPendingInvitations(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<OrgInvitationResponse> invitations = organizationService.getPendingInvitations(currentUser);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    @Operation(summary = "Remove a member from an organization (admin/manager only)")
    @DeleteMapping("/{orgId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID orgId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        organizationService.removeMember(orgId, memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed"));
    }

    @Operation(summary = "Accept a pending invitation")
    @PostMapping("/{orgId}/invitations/accept")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> acceptInvitation(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        OrgMemberResponse member = organizationService.acceptInvitation(orgId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(member, "Invitation accepted"));
    }

    @Operation(summary = "Change a member's role within an organization (admin only)")
    @PutMapping("/{orgId}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> changeMemberRole(
            @PathVariable UUID orgId,
            @PathVariable UUID memberId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        OrgMemberResponse member = organizationService.changeMemberRole(
                orgId, memberId, request.getRole(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(member, "Member role updated"));
    }

    @Operation(summary = "Decline a pending invitation")
    @PostMapping("/{orgId}/invitations/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        organizationService.declineInvitation(orgId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined"));
    }
}
