package com.genifast.dms.repository;

import com.genifast.dms.entity.Organization;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>,
                JpaSpecificationExecutor<Organization> {
        /**
         * Tìm organization bằng tên. Tương ứng FindOrganizationByName.
         */
        Optional<Organization> findByName(String name);

        /**
         * Tìm organization theo người tạo. Tương ứng FindOrganizationByAuthor.
         */
        Optional<Organization> findByCreatedBy(String createdBy);

        /**
         * Cập nhật trạng thái của organization. Tương ứng UpdateOrganizationStatus.
         */
        @Modifying
        @Query("UPDATE Organization o SET o.status = :status WHERE o.id = :orgId")
        void updateOrganizationStatus(@Param("orgId") Long orgId, @Param("status") int status);
}
