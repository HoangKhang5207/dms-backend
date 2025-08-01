package com.genifast.dms.repository;

import com.genifast.dms.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    // Tìm permission theo tên (name là unique)
    Optional<Permission> findByName(String name);

    List<Permission> findAllByNameIn(List<String> names);
}