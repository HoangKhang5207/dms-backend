package com.genifast.dms.repository;

import com.genifast.dms.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    
    boolean existsByUserIdAndDocIdAndPermissionContaining(Long userId, Long docId, String permission);
    
    List<DocumentPermission> findByUserIdAndDocId(Long userId, Long docId);
    
    @Query("SELECT dp FROM DocumentPermission dp WHERE dp.userId = :userId AND dp.docId = :docId AND dp.permission = :permission")
    List<DocumentPermission> findByUserIdAndDocIdAndPermission(@Param("userId") Long userId, 
                                                               @Param("docId") Long docId, 
                                                               @Param("permission") String permission);
    
    void deleteByUserIdAndDocId(Long userId, Long docId);
}
