package com.genifast.dms.repository;

import com.genifast.dms.entity.PrivateDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateDocRepository extends JpaRepository<PrivateDoc, Long> {
    
    boolean existsByUser_IdAndDocument_IdAndStatus(Long userId, Long docId, Integer status);
    
        void deleteByUser_IdAndDocument_Id(Long userId, Long docId);
}
