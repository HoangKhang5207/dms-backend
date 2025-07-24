package com.genifast.dms.repository;

import com.genifast.dms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface cho Product entity.
 * Chuyển đổi từ IProductRepository của Golang.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Tìm các sản phẩm theo tên, không phân biệt hoa thường.
     * Tương ứng với hàm GetProductsByName sử dụng LIKE của Golang.
     *
     * @param name Tên sản phẩm cần tìm kiếm
     * @return Danh sách sản phẩm phù hợp
     */
    List<Product> findByNameContainingIgnoreCase(String name);
}