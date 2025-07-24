package com.genifast.dms.repository;

import com.genifast.dms.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface cho User entity.
 * Kế thừa JpaRepository để có các phương thức CRUD cơ bản.
 * Các phương thức được chuyển đổi từ IUserRepository của Golang. [cite: 175]
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByEmail(String email);

        Optional<User> findByEmailAndPassword(String email, String password);

        @Query("SELECT u FROM User u WHERE u.email = :email AND u.organization.id = :orgId")
        Optional<User> findByEmailAndOrganization(String email, Long orgId);

        @Query("SELECT u.email FROM User u WHERE u.email IN :emails AND u.organization IS NOT NULL")
        List<String> findEmailsOfUsersInOrganization(@Param("emails") List<String> emails);

        @Query("SELECT u FROM User u WHERE u.email IN :emails AND u.organization IS NULL AND status = 1")
        List<User> findEmailsOfUsersNotInOrganization(@Param("emails") List<String> emails);

        /**
         * Tìm tất cả user thuộc một tổ chức, có phân trang.
         */
        Page<User> findByOrganizationId(Long organizationId, Pageable pageable);

        /**
         * Tìm tất cả user thuộc một phòng ban, có phân trang.
         */
        Page<User> findByDepartmentId(Long departmentId, Pageable pageable);

        /**
         * Cập nhật trạng thái của user. Tương ứng UpdateUserStatus. [cite: 180]
         * 
         * @Modifying và @Transactional (ở tầng service) là bắt buộc cho các query
         *            UPDATE/DELETE.
         */

        /**
         * Cập nhật trạng thái của user. Tương ứng UpdateUserStatus.
         */
        @Modifying
        @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
        void updateUserStatus(@Param("userId") Long userId, @Param("status") int status);

        /**
         * Đánh dấu user là một tài khoản social. Tương ứng UpdateUserSocial.
         */
        @Modifying
        @Query("UPDATE User u SET u.isSocial = true WHERE u.id = :userId")
        void updateUserAsSocial(@Param("userId") Long userId);

        /**
         * Gán user vào một tổ chức và phòng ban. Tương ứng AddPeopleOrganization.
         * Refactored: Cập nhật trực tiếp vào đối tượng liên kết.
         */
        @Modifying
        @Query("UPDATE User u SET u.organization.id = :orgId, u.department.id = :deptId WHERE u.id = :userId")
        void addUserToOrganizationAndDepartment(
                        @Param("userId") Long userId,
                        @Param("orgId") Long org,
                        @Param("deptId") Long dept);

        /**
         * Cập nhật vai trò quản lý tổ chức cho user. Tương ứng
         * UpdateUserOrganizationRole.
         */
        @Modifying
        @Query("UPDATE User u SET u.organization.id = :orgId, u.isOrganizationManager = :isManager WHERE u.id = :userId")
        void updateUserOrganizationRole(@Param("userId") Long userId, @Param("orgId") Long orgId,
                        @Param("isManager") boolean isManager);

        /**
         * Cập nhật vai trò quản lý (chung). Tương ứng UpdateUserRoleManager.
         */
        @Modifying
        @Query("UPDATE User u SET u.isOrganizationManager = :isManager WHERE u.id = :userId")
        void updateUserRoleManager(@Param("userId") Long userId, @Param("isManager") boolean isManager);

        /**
         * Reset mật khẩu cho user. Tương ứng ResetPassword.
         */
        @Modifying
        @Query("UPDATE User u SET u.password = :password WHERE u.id = :userId")
        void resetPassword(@Param("userId") Long userId, @Param("password") String password);

        /**
         * Xóa user khỏi tổ chức. Tương ứng RemoveUserFromOrganization.
         * Refactored: Set các đối tượng liên quan thành NULL.
         */
        @Modifying
        @Query("UPDATE User u SET u.organization = NULL, u.department = NULL, u.isOrganizationManager = false, u.isDeptManager = false WHERE u.id = :userId")
        void removeUserFromOrganization(@Param("userId") Long userId);

        @Modifying
        @Query("UPDATE User u SET u.isDeptManager = :isManager WHERE u.id = :userId")
        void updateUserDeptManagerRole(@Param("userId") Long userId, @Param("isManager") boolean isManager);
}