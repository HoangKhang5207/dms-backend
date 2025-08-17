package com.genifast.dms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    // Thêm các phương thức để hỗ trợ logic "xóa mềm" (soft-delete)
    // Spring Data JPA sẽ tự động tạo ra các câu truy vấn dựa trên tên phương thức

    List<T> findByIsDeletedFalse();

    Optional<T> findByIdAndIsDeletedFalse(ID id);

    boolean existsByIdAndIsDeletedFalse(ID id);

    List<T> findAllByIdInAndIsDeletedFalse(List<ID> ids);
}