package com.genifast.dms.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.dto.request.ProductCreateUpdateRequest;
import com.genifast.dms.dto.response.ProductResponse;
import com.genifast.dms.entity.Product;
import com.genifast.dms.mapper.ProductMapper;
import com.genifast.dms.repository.ProductRepository;
import com.genifast.dms.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateUpdateRequest createDto) {
        Product product = productMapper.toProduct(createDto);
        product.setStatus(1); // Active
        Product savedProduct = productRepository.save(product);
        log.info("Product '{}' (ID: {}) created.", savedProduct.getName(), savedProduct.getId());
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        return productRepository.findById(productId)
                .map(productMapper::toProductResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_EXIST,
                        ErrorMessage.PRODUCT_NOT_EXIST.getMessage()));
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toProductResponse);
    }

    @Override
    public List<ProductResponse> searchProductsByName(String name) {
        return productMapper.toProductResponseList(productRepository.findByNameContainingIgnoreCase(name));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductCreateUpdateRequest updateDto) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_EXIST,
                        ErrorMessage.PRODUCT_NOT_EXIST.getMessage()));

        productMapper.updateProductFromDto(updateDto, existingProduct);

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product ID {} updated.", updatedProduct.getId());
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ApiException(ErrorCode.PRODUCT_NOT_EXIST,
                    ErrorMessage.PRODUCT_NOT_EXIST.getMessage());
        }
        productRepository.deleteById(productId);
        log.warn("Product ID {} deleted.", productId);
    }
}
