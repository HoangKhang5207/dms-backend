package com.genifast.dms.repository;

import com.genifast.dms.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermission.UserPermissionId> {
    @Transactional
    void deleteByUserIdAndPermissionId(Long userId, Long permissionId);
}