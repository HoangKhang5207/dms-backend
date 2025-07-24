package com.genifast.dms.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.genifast.dms.dto.request.CategoryCreateRequest;
import com.genifast.dms.dto.response.CategoryResponse;
import com.genifast.dms.entity.Category;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    // Map các trường ID từ object lồng nhau sang trường ID phẳng trong DTO
    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "organization.id", target = "organizationId")
    CategoryResponse toCategoryResponse(Category category);

    // Bỏ qua các trường quan hệ khi map từ DTO sang Entity
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "organization", ignore = true)
    Category toCategory(CategoryCreateRequest createDto);
}
