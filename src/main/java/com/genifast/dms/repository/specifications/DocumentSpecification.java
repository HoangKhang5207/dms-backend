package com.genifast.dms.repository.specifications;

import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.PrivateDoc;
import com.genifast.dms.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DocumentSpecification {

    /**
     * Specification để kiểm tra quyền truy cập của user vào document.
     * Đây là cải tiến lớn nhất, đẩy logic phân quyền xuống CSDL.
     */
    public static Specification<Document> hasAccess(User user) {
        return (root, query, cb) -> {
            // Predicate cho quyền public (accessType = 1)
            Predicate publicAccess = cb.equal(root.get("accessType"), 1);

            // Predicate cho quyền trong tổ chức (accessType = 2)
            Predicate orgAccess = user.getOrganization() == null ? cb.disjunction()
                    : cb.and(
                            cb.equal(root.get("accessType"), 2),
                            cb.equal(root.get("organization").get("id"), user.getOrganization().getId()));

            // Predicate cho quyền trong phòng ban (accessType = 3)
            Predicate deptAccess = user.getDepartment() == null ? cb.disjunction()
                    : cb.and(
                            cb.equal(root.get("accessType"), 3),
                            cb.equal(root.get("department").get("id"), user.getDepartment().getId()));

            // Predicate cho quyền riêng tư (accessType = 4)
            // Bao gồm: user là người tạo, hoặc user được chia sẻ trong bảng private_docs
            Subquery<Long> privateDocsSubquery = query.subquery(Long.class);
            Root<PrivateDoc> privateDocRoot = privateDocsSubquery.from(PrivateDoc.class);
            privateDocsSubquery.select(privateDocRoot.get("document").get("id"))
                    .where(cb.equal(privateDocRoot.get("user").get("id"), user.getId()));

            Predicate privateAccess = cb.and(
                    cb.equal(root.get("accessType"), 4),
                    cb.or(
                            cb.equal(root.get("createdBy"), user.getEmail()),
                            root.get("id").in(privateDocsSubquery)));

            return cb.or(publicAccess, orgAccess, deptAccess, privateAccess);
        };
    }

    /**
     * Specification cho tìm kiếm AND/OR/NOT.
     */
    public static Specification<Document> matchesAndOrNot(SearchAndOrNotRequest dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // AND keywords: tất cả phải xuất hiện
            if (!CollectionUtils.isEmpty(dto.getAndKeywords())) {
                dto.getAndKeywords().forEach(keyword -> predicates
                        .add(cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%")));
            }

            // OR keywords: ít nhất một phải xuất hiện
            if (!CollectionUtils.isEmpty(dto.getOrKeywords())) {
                List<Predicate> orPredicates = new ArrayList<>();
                dto.getOrKeywords().forEach(keyword -> orPredicates
                        .add(cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%")));
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            // NOT keywords: không được có từ nào
            if (!CollectionUtils.isEmpty(dto.getNotKeywords())) {
                dto.getNotKeywords().forEach(keyword -> predicates
                        .add(cb.notLike(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Các specification cho việc lọc động
    public static Specification<Document> titleContains(String title) {
        return (root, query, cb) -> title == null ? cb.conjunction()
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Document> hasType(String type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Document> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null)
                return cb.conjunction();
            if (from != null && to != null)
                return cb.between(root.get("createdAt"), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}