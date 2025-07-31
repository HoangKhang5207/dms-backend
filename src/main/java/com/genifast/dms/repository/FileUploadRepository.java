package com.genifast.dms.repository;

import com.genifast.dms.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    List<FileUpload> findByDocumentId(Long documentId);

    @Modifying
    @Query("DELETE FROM FileUpload fu WHERE fu.documentId = :documentId")
    void deleteByDocumentId(Long documentId);
}