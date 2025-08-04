package com.genifast.dms.controller;

import com.genifast.dms.dto.request.DelegationRequest;
import com.genifast.dms.dto.response.DelegationResponse;
import com.genifast.dms.service.DelegationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delegations")
@RequiredArgsConstructor
public class DelegationController {

    private final DelegationService delegationService;

    @PostMapping
    public ResponseEntity<DelegationResponse> createDelegation(@Valid @RequestBody DelegationRequest request) {
        DelegationResponse response = delegationService.createDelegation(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/document/{docId}")
    public ResponseEntity<List<DelegationResponse>> getDelegationsByDocument(@PathVariable Long docId) {
        List<DelegationResponse> delegations = delegationService.getDelegationsByDocument(docId);
        return ResponseEntity.ok(delegations);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeDelegation(@PathVariable Long id) {
        delegationService.revokeDelegation(id);
        return ResponseEntity.noContent().build();
    }
}