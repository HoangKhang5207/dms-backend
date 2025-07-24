package com.genifast.dms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.genifast.dms.dto.request.ProductCreateUpdateRequest;
import com.genifast.dms.dto.response.ProductResponse;
import com.genifast.dms.entity.Product;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);

    Product toProduct(ProductCreateUpdateRequest createDto);

    void updateProductFromDto(ProductCreateUpdateRequest updateDto, @MappingTarget Product product);
}
