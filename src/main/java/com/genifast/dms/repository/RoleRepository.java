package com.genifast.dms.repository;

import com.genifast.dms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Tìm role theo tên trong một tổ chức cụ thể
    Optional<Role> findByNameAndOrganizationId(String name, Long organizationId);

    // Tìm role theo tên (cho các role không thuộc tổ chức nào - role hệ thống)
    Optional<Role> findByNameAndOrganizationIdIsNull(String name);

    // Lấy tất cả role của một tổ chức
    List<Role> findByOrganizationId(Long organizationId);
}