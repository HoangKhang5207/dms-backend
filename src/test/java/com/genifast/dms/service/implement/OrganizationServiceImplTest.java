// package com.genifast.dms.service.implement;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.util.Collections;
// import java.util.List;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.genifast.dms.dto.request.organization.SearchOrgRequest;
// import com.genifast.dms.dto.response.organization.OrganizationResponse;
// import
// com.genifast.dms.dto.response.organization.OrganizationResponse.OrganizationDetails;
// import com.genifast.dms.entity.Organization;
// import com.genifast.dms.mapper.OrganizationMapper;
// import com.genifast.dms.repository.OrganizationRepository;
// import com.genifast.dms.service.impl.OrganizationServiceImpl;

// @ExtendWith(MockitoExtension.class) // Kích hoạt Mockito
// class OrganizationServiceImplTest {

// @Mock // Tạo một đối tượng giả (mock) cho Repository
// private OrganizationRepository organizationRepository;

// @Mock // Tạo một đối tượng giả cho Mapper
// private OrganizationMapper organizationMapper;

// @InjectMocks // Tạo một instance của ServiceImpl và inject các mock trên vào
// đó
// private OrganizationServiceImpl organizationService;

// private SearchOrgRequest request;
// private Organization organization;
// private OrganizationDetails organizationDetails;

// @BeforeEach
// void setUp() {
// // Thiết lập dữ liệu mẫu chạy trước mỗi test case
// request = new SearchOrgRequest();
// request.setPage(1);
// request.setLimit(10);

// organization = Organization.builder().id(1L).name("Test Corp").build();
// organizationDetails = new OrganizationDetails();
// organizationDetails.setId(1L);
// organizationDetails.setName("Test Corp");
// }

// @Test
// @DisplayName("Lấy danh sách Org thành công - trả về dữ liệu")
// void getOrgs_WhenDataExists_ShouldReturnCorrectResponse() {
// // --- ARRANGE (Given) ---
// // Dạy cho Mockito biết: khi repository được gọi, nó nên trả về cái gì
// List<Organization> mockOrgList = Collections.singletonList(organization);
// when(organizationRepository.getListOrganization(request)).thenReturn(mockOrgList);
// when(organizationRepository.count(request)).thenReturn(1);
// when(organizationMapper.toOrganizationDetailsList(any()))
// .thenReturn(Collections.singletonList(organizationDetails));

// // --- ACT (When) ---
// // Gọi phương thức cần test
// OrganizationResponse response = organizationService.getOrgs(request);

// // --- ASSERT (Then) ---
// // Kiểm tra kết quả
// assertThat(response).isNotNull();
// assertThat(response.getTotal()).isEqualTo(1);
// assertThat(response.getList()).hasSize(1);
// assertThat(response.getList().get(0).getName()).isEqualTo("Test Corp");

// // Kiểm tra xem các phương thức mock đã được gọi đúng hay không
// verify(organizationRepository).getListOrganization(request);
// verify(organizationRepository).count(request);
// verify(organizationMapper).toOrganizationDetailsList(mockOrgList);
// }

// @Test
// @DisplayName("Lấy danh sách Org thành công - không có dữ liệu")
// void getOrgs_WhenNoData_ShouldReturnEmptyResponse() {
// // --- ARRANGE (Given) ---
// when(organizationRepository.getListOrganization(request)).thenReturn(Collections.emptyList());
// when(organizationRepository.count(request)).thenReturn(0);

// // --- ACT (When) ---
// OrganizationResponse response = organizationService.getOrgs(request);

// // --- ASSERT (Then) ---
// assertThat(response).isNotNull();
// assertThat(response.getTotal()).isEqualTo(0);
// assertThat(response.getList()).isEmpty();
// }
// }
