package com.genifast.dms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.genifast.dms.service.OrganizationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/invitation")
@RequiredArgsConstructor
public class InvitationController {

    private final OrganizationService organizationService;

    @GetMapping("/accept")
    public ResponseEntity<String> acceptInvitation(
            @RequestParam Long orgId,
            @RequestParam Long deptId,
            @RequestParam String userEmail) {

        organizationService.acceptInvitation(orgId, deptId, userEmail);
        // Có thể redirect đến trang frontend
        return ResponseEntity.ok("Invitation accepted successfully! You can now log in.");
    }
}
