package com.genifast.dms.repository;

import com.genifast.dms.entity.Document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface cho Document entity.
 * Kế thừa JpaSpecificationExecutor để hỗ trợ xây dựng query động.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    /**
     * Lấy danh sách document theo danh sách tiêu đề.
     * Tương ứng GetDocumentByTitles.
     */
    List<Document> findByTitleIn(List<String> titles);

    /**
     * Lấy danh sách tất cả tiêu đề document.
     * Tương ứng SearchDocumentTitles.
     */
    @Query("SELECT d.title FROM Document d")
    List<String> findAllTitles();

    /**
     * Lấy danh sách document theo category. Tương ứng GetDocumentsByCategoryID.
     */
    Page<Document> findByCategoryIdAndStatus(Long categoryId, int status, Pageable pageable);

    /**
     * Lấy danh sách document theo department. Tương ứng GetDocumentsByDepartmentID.
     */
    List<Document> findByDepartmentId(Long departmentId);

    Optional<Document> findByShareToken(String shareToken);

    /**
     * Cập nhật trạng thái cho tất cả document thuộc một organization.
     * Tương ứng UpdateDocumentStatusByOrganizationID.
     */
    @Modifying
    @Query("UPDATE Document d SET d.status = :status WHERE d.organization.id = :organizationId")
    void updateStatusByOrganizationId(@Param("organizationId") Long organizationId, @Param("status") int status);

    /**
     * Cập nhật trạng thái cho tất cả document thuộc một category.
     * Tương ứng UpdateDocumentStatusByCategoryID.
     */
    @Modifying
    @Query("UPDATE Document d SET d.status = :status WHERE d.category.id = :categoryId")
    void updateStatusByCategoryId(@Param("categoryId") Long categoryId, @Param("status") int status);

    /**
     * Cập nhật trạng thái cho tất cả document thuộc một department.
     * Tương ứng UpdateDocumentStatusByDepartmentID.
     */
    @Modifying
    @Query("UPDATE Document d SET d.status = :status WHERE d.department.id = :departmentId")
    void updateStatusByDepartmentId(@Param("departmentId") Long departmentId, @Param("status") int status);

    Optional<Document> findByFileId(String fileId);

    List<Document> findAllByOrderByCreatedAtDesc();

    Optional<Document> findByFilePath(String filePath);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.organization ORDER BY d.createdAt DESC")
    List<Document> findAllWithCategoryAndOrganization();
}