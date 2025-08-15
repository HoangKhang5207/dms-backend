package com.genifast.dms.repository;

import com.genifast.dms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Tìm role theo tên trong một tổ chức cụ thể (truy cập thuộc tính lồng nhau bằng Organization_Id)
    Optional<Role> findByNameAndOrganization_Id(String name, Long organizationId);

    // Tìm role theo tên (cho các role không thuộc tổ chức nào - role hệ thống)
    Optional<Role> findByNameAndOrganizationIsNull(String name);

    // Lấy tất cả role của một tổ chức
    List<Role> findByOrganization_Id(Long organizationId);
}