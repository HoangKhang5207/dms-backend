package com.genifast.dms.repository.specifications;

import org.springframework.data.jpa.domain.Specification;

import com.genifast.dms.entity.BaseEntity;

public class CommonSpecifications<M extends BaseEntity> {

    public Specification<M> unDeleted() {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("isDeleted"), false);
    }
}
