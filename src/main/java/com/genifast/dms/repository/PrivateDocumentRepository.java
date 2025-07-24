package com.genifast.dms.repository;

import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.PrivateDoc;
import com.genifast.dms.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface cho PrivateDocument entity.
 * Chuyển đổi từ IPrivateDocumentRepository của Golang.
 */
@Repository
public interface PrivateDocumentRepository extends JpaRepository<PrivateDoc, Long> {

    /**
     * Tìm bản ghi quyền truy cập theo user, document và trạng thái active.
     * Tương ứng với hàm GetPrivateDocument bên Golang.
     * Sử dụng query derivation của Spring Data JPA.
     *
     * @param user     Đối tượng User
     * @param document Đối tượng Document
     * @param status   Trạng thái (ví dụ: 1 cho active)
     * @return Optional chứa bản ghi PrivateDocument nếu tồn tại
     */
    Optional<PrivateDoc> findByUserAndDocumentAndStatus(User user, Document document, int status);
}