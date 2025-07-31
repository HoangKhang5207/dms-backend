package com.genifast.dms.repository;

import com.genifast.dms.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    List<UserDocument> findByDocumentId(Long documentId);

    @Modifying
    @Query("DELETE FROM UserDocument ud WHERE ud.documentId = :documentId")
    void deleteByDocumentId(Long documentId);
}