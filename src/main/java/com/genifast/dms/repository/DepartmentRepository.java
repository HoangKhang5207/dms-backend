package com.genifast.dms.repository;

import com.genifast.dms.entity.Department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface cho Department entity.
 * Chuyển đổi từ các hàm liên quan đến Department trong category_repository.go
 * của Golang.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Cập nhật trạng thái cho một department cụ thể.
     * Tương ứng UpdateDepartmentStatusByID [cite: 158-159].
     */
    @Modifying
    @Query("UPDATE Department d SET d.status = :status WHERE d.id = :id")
    void updateStatusById(@Param("id") Long id, @Param("status") int status);

    /**
     * Cập nhật trạng thái cho tất cả department thuộc một organization.
     * Tương ứng UpdateDepartmentsStatusByOrganizationID[cite: 160].
     */
    @Modifying
    @Query("UPDATE Department d SET d.status = :status WHERE d.organization.id = :organizationId")
    void updateStatusByOrganizationId(@Param("organizationId") Long organizationId, @Param("status") int status);

    @Query("SELECT d FROM Department d WHERE d.name = :name AND d.organization.id = :orgId AND d.status = 1")
    Optional<Department> findByNameAndOrganizationId(@Param("name") String name, @Param("orgId") Long orgId);

    /**
     * Lấy tất cả các phòng ban của một tổ chức (trừ những phòng ban đã bị xóa).
     */
    @Query("SELECT d FROM Department d WHERE d.organization.id = :orgId AND d.status = 1")
    List<Department> findByOrganizationIdAndStatusNot(@Param("orgId") Long orgId);
}