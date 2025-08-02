package com.genifast.dms.controller;

import com.genifast.dms.dto.request.InviteUsersRequest;
import com.genifast.dms.dto.request.OrganizationCreateRequest;
import com.genifast.dms.dto.request.OrganizationUpdateRequest;
import com.genifast.dms.dto.request.UpdateOrganizationStatusRequest;
import com.genifast.dms.dto.request.UserActionRequest;
import com.genifast.dms.dto.response.CheckOrgResponse;
import com.genifast.dms.dto.response.InviteUsersResponse;
import com.genifast.dms.dto.response.OrganizationResponse;
import com.genifast.dms.service.OrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("hasAuthority('organization:create-request')")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationCreateRequest createDto) {

        OrganizationResponse newOrg = organizationService.createOrganization(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newOrg);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('organization:view-details')")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable("id") Long orgId) {
        return ResponseEntity.ok(organizationService.getOrganizationById(orgId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('organization:update')")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable("id") Long orgId,
            @Valid @RequestBody OrganizationUpdateRequest updateDto) {

        return ResponseEntity.ok(organizationService.updateOrganization(orgId, updateDto));
    }

    @GetMapping("/check-pending-request")
    public ResponseEntity<CheckOrgResponse> checkUserHasPendingRequest() {
        return ResponseEntity.ok(organizationService.checkUserHasPendingOrganizationRequest());
    }

    // Endpoint này thường dành cho role ADMIN
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('organization:update-status')")
    public ResponseEntity<OrganizationResponse> updateOrganizationStatus(
            @PathVariable("id") Long orgId,
            @Valid @RequestBody UpdateOrganizationStatusRequest statusDto) {

        OrganizationResponse updatedOrg = organizationService.updateOrganizationStatus(orgId, statusDto);
        return ResponseEntity.ok(updatedOrg);
    }

    @PostMapping("/{id}/invite-members")
    @PreAuthorize("hasAuthority('organization:invite-users')")
    public ResponseEntity<InviteUsersResponse> inviteUsers(
            @PathVariable("id") Long orgId,
            @Valid @RequestBody InviteUsersRequest inviteDto) {

        return ResponseEntity.ok(organizationService.inviteUsers(orgId, inviteDto));
    }

    @PostMapping("/{id}/remove-member")
    @PreAuthorize("hasAuthority('organization:remove-user')")
    public ResponseEntity<String> removeUserFromOrganization(
            @PathVariable("id") Long orgId,
            @Valid @RequestBody UserActionRequest removeUserDto) {

        organizationService.removeUserFromOrganization(orgId, removeUserDto);
        return ResponseEntity.ok("User is deleted successfully");
    }

    @PostMapping("/{id}/assign-manager")
    @PreAuthorize("hasAuthority('organization:assign-manager')")
    public ResponseEntity<String> assignManagerRole(
            @PathVariable("id") Long orgId,
            @Valid @RequestBody UserActionRequest assignDto) {

        organizationService.assignManagerRole(orgId, assignDto);
        return ResponseEntity.ok("Assigned successfully");
    }

    @PostMapping("/{id}/recall-manager")
    @PreAuthorize("hasAuthority('organization:recall-manager')")
    public ResponseEntity<String> recallManagerRole(
            @PathVariable("id") Long orgId,
            @Valid @RequestBody UserActionRequest recallDto) {

        organizationService.recallManagerRole(orgId, recallDto);
        return ResponseEntity.ok("Recalled successfully");
    }
}
