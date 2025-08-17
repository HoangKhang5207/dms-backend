package com.genifast.dms.repository;

import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentOrderByVersionNumberDesc(Document document);
    Optional<DocumentVersion> findByDocumentAndVersionNumber(Document document, Integer versionNumber);
}
