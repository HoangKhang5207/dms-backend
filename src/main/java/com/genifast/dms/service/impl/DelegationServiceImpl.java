package com.genifast.dms.service.impl;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.config.CustomPermissionEvaluator;
import com.genifast.dms.dto.request.DelegationRequest;
import com.genifast.dms.dto.response.DelegationResponse;
import com.genifast.dms.entity.Delegation;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.DelegationMapper;
import com.genifast.dms.repository.DelegationRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.DelegationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DelegationServiceImpl implements DelegationService {

    private final DelegationRepository delegationRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DelegationMapper delegationMapper;
    private final CustomPermissionEvaluator permissionEvaluator;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('delegate_process')")
    public DelegationResponse createDelegation(DelegationRequest req) {
        User delegator = findUserByEmail(JwtUtils.getCurrentUserLogin()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage())));

        User delegatee = findUserById(req.getDelegateeId());
        Document document = findDocById(req.getDocumentId());

        // --- VALIDATION LOGIC ---
        if (delegator.getId().equals(delegatee.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Không thể tự ủy quyền cho chính mình.");
        }

        // Logic ABAC: Kiểm tra xem người ủy quyền có thực sự sở hữu quyền mà họ đang cố
        // gắng ủy quyền hay không.
        // Phần này sẽ được xử lý bởi Custom Permission Evaluator ở bước sau.
        // Tạm thời, chúng ta giả định người có quyền 'delegate_process' có thể ủy
        // quyền.
        // THỰC HIỆN KIỂM TRA LOGIC ABAC ---
        // Lấy thông tin xác thực của người dùng hiện tại (delegator)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Sử dụng PermissionEvaluator để kiểm tra xem delegator có quyền
        // `req.getPermission()` trên tài liệu `req.getDocumentId()` hay không.
        boolean delegatorHasPermission = permissionEvaluator.hasPermission(
                authentication,
                req.getDocumentId(),
                req.getPermission());

        if (!delegatorHasPermission) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION,
                    "Không thể ủy quyền: bạn không sở hữu quyền '" + req.getPermission() + "' trên tài liệu này.");
        }

        Delegation delegation = Delegation.builder()
                .delegator(delegator)
                .delegatee(delegatee)
                .document(document)
                .permission(req.getPermission())
                .expiryAt(req.getExpiryAt())
                .build();

        Delegation savedDelegation = delegationRepository.save(delegation);
        return delegationMapper.toDelegationResponse(savedDelegation);
    }

    @Override
    @PreAuthorize("hasAuthority('documents:read')") // Cần quyền đọc tài liệu để xem các ủy quyền
    public List<DelegationResponse> getDelegationsByDocument(Long documentId) {
        return delegationRepository.findByDocumentId(documentId).stream()
                .map(delegationMapper::toDelegationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeDelegation(Long delegationId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage())));

        Delegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Ủy quyền không tồn tại."));

        // Chỉ người tạo ra ủy quyền (delegator) hoặc admin mới có quyền thu hồi
        if (!delegation.getDelegator().getId().equals(currentUser.getId()) && !currentUser.getIsAdmin()) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "Bạn không có quyền thu hồi ủy quyền này.");
        }

        delegationRepository.delete(delegation);
    }

    // --- Helper Methods ---
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "Người được ủy quyền không tồn tại."));
    }

    private Document findDocById(Long docId) {
        return documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND,
                        ErrorMessage.DOCUMENT_NOT_FOUND.getMessage()));
    }
}