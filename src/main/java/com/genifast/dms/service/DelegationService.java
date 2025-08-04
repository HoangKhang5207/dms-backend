package com.genifast.dms.service;

import com.genifast.dms.dto.request.DelegationRequest;
import com.genifast.dms.dto.response.DelegationResponse;
import java.util.List;

public interface DelegationService {
    DelegationResponse createDelegation(DelegationRequest delegationRequest);

    List<DelegationResponse> getDelegationsByDocument(Long documentId);

    void revokeDelegation(Long delegationId);
}