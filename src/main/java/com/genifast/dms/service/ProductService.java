package com.genifast.dms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.genifast.dms.dto.request.ProductCreateUpdateRequest;
import com.genifast.dms.dto.response.ProductResponse;

public interface ProductService {
    ProductResponse createProduct(ProductCreateUpdateRequest createDto);

    ProductResponse getProductById(Long productId);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    List<ProductResponse> searchProductsByName(String name);

    ProductResponse updateProduct(Long productId, ProductCreateUpdateRequest updateDto);

    void deleteProduct(Long productId);
}
