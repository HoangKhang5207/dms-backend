package com.genifast.dms.service.abac;

import com.genifast.dms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Đánh giá các policy cụ thể trong ABAC
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyEvaluator {

    private final DocumentPermissionRepository documentPermissionRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final PrivateDocRepository privateDocRepository;

    /**
     * Kiểm tra quyền chia sẻ tài liệu
     */
    public boolean hasSharedPermission(Long userId, Long docId, String action) {
        try {
            return documentPermissionRepository.existsByUserIdAndDocIdAndPermissionContaining(
                userId, docId, action);
        } catch (Exception e) {
            log.error("Error checking shared permission for user {} on doc {}", userId, docId, e);
            return false;
        }
    }

    /**
     * Kiểm tra tư cách thành viên dự án
     */
    public boolean isProjectMember(Long userId, Long projectId) {
        try {
            return projectMemberRepository.existsByUserIdAndProjectIdAndStatus(userId, projectId, 1);
        } catch (Exception e) {
            log.error("Error checking project membership for user {} in project {}", userId, projectId, e);
            return false;
        }
    }

    /**
     * Kiểm tra dự án có đang hoạt động không
     */
    public boolean isProjectActive(Long projectId, Instant currentTime) {
        try {
            return projectRepository.findById(projectId)
                .map(project -> {
                    Instant now = currentTime != null ? currentTime : Instant.now();
                    return project.getStatus() == 1 && // ACTIVE
                           (project.getStartDate() == null || !now.isBefore(project.getStartDate())) &&
                           (project.getEndDate() == null || !now.isAfter(project.getEndDate()));
                })
                .orElse(false);
        } catch (Exception e) {
            log.error("Error checking project active status for project {}", projectId, e);
            return false;
        }
    }

    /**
     * Kiểm tra quyền truy cập tài liệu PRIVATE
     */
    public boolean hasPrivateDocAccess(Long userId, Long docId) {
        try {
            return privateDocRepository.existsByUser_IdAndDocument_IdAndStatus(userId, docId, 1);
        } catch (Exception e) {
            log.error("Error checking private doc access for user {} on doc {}", userId, docId, e);
            return false;
        }
    }

    /**
     * Kiểm tra thời hạn chia sẻ tài liệu
     */
    public boolean isWithinShareTimebound(Long userId, Long docId, Instant currentTime) {
        try {
            Instant now = currentTime != null ? currentTime : Instant.now();
            return documentPermissionRepository.findByUserIdAndDocId(userId, docId)
                .stream()
                .anyMatch(permission -> 
                    (permission.getExpiryDate() == null || !now.isAfter(permission.getExpiryDate())));
        } catch (Exception e) {
            log.error("Error checking share timebound for user {} on doc {}", userId, docId, e);
            return true; // Default allow if no timebound specified
        }
    }
}
