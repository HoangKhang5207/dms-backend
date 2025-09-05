package com.genifast.dms.repository;

import com.genifast.dms.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    // Tìm một phiên bản cụ thể của một tài liệu
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, Integer versionNumber);

    // Lấy tất cả các phiên bản của một tài liệu, sắp xếp theo phiên bản mới nhất
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
}