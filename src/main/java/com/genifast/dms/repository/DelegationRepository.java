package com.genifast.dms.repository;

import com.genifast.dms.entity.Delegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {

        /**
         * Tìm kiếm một ủy quyền còn hiệu lực cho một người dùng, trên một tài liệu và
         * cho một quyền cụ thể.
         * 
         * @param delegateeId ID của người được ủy quyền
         * @param documentId  ID của tài liệu
         * @param permission  Tên quyền
         * @param now         Thời điểm hiện tại để kiểm tra hết hạn
         * @return Optional chứa Delegation nếu tìm thấy
         */
        @Query("SELECT d FROM Delegation d WHERE d.delegatee.id = :delegateeId " +
                        "AND d.document.id = :documentId AND d.permission = :permission " +
                        "AND d.startAt <= :now " +
                        "AND (d.expiryAt IS NULL OR d.expiryAt > :now)")
        Optional<Delegation> findActiveDelegation(@Param("delegateeId") Long delegateeId,
                        @Param("documentId") Long documentId,
                        @Param("permission") String permission,
                        @Param("now") Instant now);

        List<Delegation> findByDocumentId(Long documentId);
}