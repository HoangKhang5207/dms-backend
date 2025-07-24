package com.genifast.dms.repository;

import com.genifast.dms.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface cho Category entity.
 * Chuyển đổi từ ICategoryRepository của Golang.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Tìm category theo tên và department ID. Tương ứng FindCategoryByName.
     */
    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.department.id = :deptId AND c.status = 1")
    Optional<Category> findByNameAndDepartmentId(@Param("name") String name, @Param("deptId") Long deptId);

    /**
     * Tìm category theo tên (không phân biệt hoa thường) và department ID.
     * Tương ứng FindCategoryByNameLike.
     */
    @Query("""
              SELECT c
              FROM Category c
              WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
                AND c.department.id = :deptId
                AND c.status = 1
            """)
    List<Category> findByNameContainingIgnoreCaseAndDepartmentId(
            @Param("name") String name,
            @Param("deptId") Long deptId);

    /**
     * Lấy danh sách category theo department ID có phân trang.
     * Tương ứng ListCategoryByDepartment.
     * Sử dụng Pageable của Spring Data JPA để xử lý phân trang một cách thanh lịch.
     */
    Page<Category> findByDepartmentIdAndStatus(Long departmentId, int status, Pageable pageable);

    List<Category> findByDepartmentId(Long departmentId);

    /**
     * Cập nhật trạng thái cho một category. Tương ứng UpdateCategoryStatusByID.
     */
    @Modifying
    @Query("UPDATE Category c SET c.status = :status WHERE c.id = :id")
    void updateStatusById(@Param("id") Long id, @Param("status") int status);

    /**
     * Cập nhật trạng thái cho tất cả category thuộc một department.
     * Tương ứng UpdateCategoriesStatusByDepartmentID.
     */
    @Modifying
    @Query("UPDATE Category c SET c.status = :status WHERE c.department.id = :departmentId")
    void updateStatusByDepartmentId(@Param("departmentId") Long departmentId, @Param("status") int status);

    /**
     * Cập nhật trạng thái cho tất cả category thuộc một organization.
     * Tương ứng UpdateCategoriesStatusByOrganizationID.
     */
    @Modifying
    @Query("UPDATE Category c SET c.status = :status WHERE c.organization.id = :organizationId")
    void updateStatusByOrganizationId(@Param("organizationId") Long organizationId, @Param("status") int status);
}