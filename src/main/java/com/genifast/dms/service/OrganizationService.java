package com.genifast.dms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.genifast.dms.dto.request.DeptManagerUpdateRequest;
import com.genifast.dms.dto.request.InviteUsersRequest;
import com.genifast.dms.dto.request.OrganizationCreateRequest;
import com.genifast.dms.dto.response.CheckOrgResponse;
import com.genifast.dms.dto.response.InviteUsersResponse;
import com.genifast.dms.dto.response.OrganizationResponse;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.dto.request.OrganizationUpdateRequest;
import com.genifast.dms.dto.request.UpdateOrganizationStatusRequest;
import com.genifast.dms.dto.request.UserActionRequest;

public interface OrganizationService {
    /**
     * Tạo một yêu cầu thành lập tổ chức mới.
     *
     * @param createDto DTO chứa thông tin tổ chức.
     * @param principal JWT principal của người dùng đang thực hiện.
     * @return DTO của tổ chức vừa được tạo (với trạng thái chờ duyệt).
     */
    OrganizationResponse createOrganization(OrganizationCreateRequest createDto);

    /**
     * Lấy thông tin chi tiết của một tổ chức theo ID.
     *
     * @param orgId     ID của tổ chức.
     * @param principal JWT principal của người dùng đang thực hiện.
     * @return DTO chứa thông tin chi tiết của tổ chức.
     */
    OrganizationResponse getOrganizationById(Long orgId);

    /**
     * Cập nhật thông tin của một tổ chức.
     *
     * @param orgId     ID của tổ chức cần cập nhật.
     * @param updateDto DTO chứa thông tin mới.
     * @param principal JWT principal của người dùng đang thực hiện.
     * @return DTO của tổ chức sau khi đã cập nhật.
     */
    OrganizationResponse updateOrganization(Long orgId, OrganizationUpdateRequest updateDto);

    /**
     * Cập nhật trạng thái của một tổ chức (thường do Admin thực hiện).
     */
    OrganizationResponse updateOrganizationStatus(Long orgId, UpdateOrganizationStatusRequest statusDto);

    /**
     * Kiểm tra xem người dùng hiện tại có yêu cầu tạo tổ chức nào đang chờ xử lý
     * không.
     */
    CheckOrgResponse checkUserHasPendingOrganizationRequest();

    /**
     * Mời người dùng vào một tổ chức.
     */
    InviteUsersResponse inviteUsers(Long orgId, InviteUsersRequest inviteDto);

    /**
     * Xử lý khi người dùng chấp nhận lời mời tham gia tổ chức.
     */
    void acceptInvitation(Long orgId, Long deptId, String userEmail);

    /**
     * Xóa một thành viên khỏi tổ chức.
     */
    void removeUserFromOrganization(Long orgId, UserActionRequest removeUserDto);

    /**
     * Gán vai trò Manager cho một thành viên.
     */
    void assignManagerRole(Long orgId, UserActionRequest assignDto);

    /**
     * Bãi nhiệm vai trò Manager của một thành viên.
     */
    void recallManagerRole(Long orgId, UserActionRequest recallDto);

    Page<UserResponse> getOrganizationMembers(Long orgId, Pageable pageable);

    Page<UserResponse> getDepartmentMembers(Long deptId, Pageable pageable);

    void updateDepartmentManagerRole(Long orgId, DeptManagerUpdateRequest deptManagerUpdateRequest);
}
