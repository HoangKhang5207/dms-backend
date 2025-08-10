# KỊCH BẢN KIỂM THỬ PHÂN QUYỀN ABAC, RBAC, GHI LOG & ỦY QUYỀN – DỰ ÁN DMS-BACKEND

**Phiên bản:** 1.5

**Người tạo:** 

**Ngày tạo:** 05/08/2025

**Ngày cập nhật:** 08/08/2025

## Phần 1: Bối Cảnh và Thiết Lập Môi Trường Kiểm Thử

### 1.1. Mục tiêu

- **Phân quyền ABAC**: Xác minh quyền chia sẻ tài liệu (documents:share:*) được gán và thực thi chính xác dựa trên thuộc tính người dùng, phòng ban, danh mục tài liệu, và tài liệu cụ thể. Đảm bảo người dùng (bao gồm Văn thư, Pháp chế, Visitor) chỉ truy cập hoặc chia sẻ tài liệu theo quyền được cấp và quy tắc phòng ban.
- **Phân quyền RBAC Toàn cục**: Xác minh người dùng chỉ thực hiện các thao tác tương ứng với vai trò chung trong tổ chức (Hiệu trưởng, Trưởng khoa, Chuyên viên, v.v.).
- **Phân quyền RBAC & ABAC trong Dự án**: 
  - **RBAC trong Dự án**: Xác minh quyền truy cập của thành viên dự án dựa trên vai trò dự án (Project Role), chỉ áp dụng cho tài liệu thuộc dự án.
  - **ABAC trong Dự án**: Đảm bảo quyền truy cập bị giới hạn bởi thuộc tính dự án (thời gian hiệu lực, tư cách thành viên).
- **Ủy quyền (Delegation)**: Xác minh người dùng có thể ủy quyền vai trò hoặc hành động cụ thể trên tài liệu cho người khác, với thời hạn và phạm vi được kiểm soát chặt chẽ.
- **Ghi log (Audit Log)**: Đảm bảo mọi hành động (chia sẻ, đọc, phê duyệt, từ chối, truy cập, ủy quyền) được ghi lại đầy đủ trong bảng audit_logs, bao gồm cả ngữ cảnh thực hiện (vai trò, dự án, ủy quyền).
- **Truy cập Visitor**: Đảm bảo Visitor chỉ xem được 1/3 số trang tài liệu PUBLIC (hoặc với watermark) khi chưa trả phí, và phải trả phí để xem toàn bộ hoặc tải xuống.
- **Kiểm tra trong và ngoài quyền**: Đảm bảo hành động trong quyền thành công, hành động ngoài quyền bị từ chối với mã lỗi 403 Forbidden và thông báo rõ ràng.

### 1.2. Quy tắc Phân quyền

#### Quy tắc ABAC theo Phòng ban
- **Nguyên tắc nội bộ**: Phòng ban có toàn quyền xem (documents:read) và sửa (documents:edit) tài liệu do mình tạo (documents.dept_id = users.dept_id) hoặc được giao xử lý (qua chia sẻ).
- **Nguyên tắc liên quan nghiệp vụ**: Phòng ban được xem tài liệu của đơn vị khác nếu liên quan đến nhiệm vụ phối hợp hoặc được chia sẻ (documents:share:readonly).
- **Nguyên tắc hạn chế hành chính**: Không được xem tài liệu của phòng ban khác nếu không có lý do nghiệp vụ chính đáng.
- **Nguyên tắc bảo mật**: Tài liệu PRIVATE chỉ người trong danh sách private_docs hoặc có quyền documents:share:timebound được truy cập.
- **Nguyên tắc cấp bậc quản lý**: Ban Giám hiệu (BGH) có quyền xem tất cả tài liệu trong tổ chức (organization_id = 1), trừ tài liệu PRIVATE không được cấp phép.

#### Quy tắc RBAC Toàn cục
- Quyền được gán dựa trên vai trò (RoleId) trong tổ chức, áp dụng cho tất cả tài liệu hoặc hành động không giới hạn bởi dự án.
- Một số quyền (như documents:sign, documents:lock) chỉ dành cho vai trò cấp cao (Hiệu trưởng).

#### Quy tắc ABAC theo Device Type
- **Nguyên tắc thiết bị công ty**: Người dùng chỉ được phép truy cập tài liệu từ thiết bị thuộc tổ chức (device_type = COMPANY_DEVICE, xác minh qua device_id được đăng ký trong hệ thống). Truy cập từ thiết bị bên ngoài (device_type = EXTERNAL_DEVICE) bị từ chối, trừ khi tài liệu là PUBLIC và Visitor đã thanh toán hoặc tài liệu được chia sẻ với quyền documents:share:external.
- **Nguyên tắc tài liệu nhạy cảm**: Tài liệu có confidentiality = PRIVATE hoặc LOCKED chỉ được truy cập từ thiết bị device_type = COMPANY_DEVICE.

#### Quy tắc RBAC & ABAC trong Dự án
- **RBAC trong Dự án**: Quyền dựa trên vai trò dự án (Project Role), chỉ áp dụng cho tài liệu thuộc dự án.
- **ABAC trong Dự án**: Quyền bị giới hạn bởi:
  - **Thời gian hiệu lực**: Quyền chỉ có hiệu lực trong khoảng start_date và end_date của dự án.
  - **Tư cách thành viên**: Người không phải thành viên dự án không có quyền truy cập tài liệu dự án, bất kể vai trò toàn cục.

### 1.3. Cơ Cấu Tổ Chức & Dữ Liệu Kiểm Thử

#### a. Bảng Phòng ban (Departments)

| DepartmentId | Name                          | Description                                                                 |
|--------------|-------------------------------|-----------------------------------------------------------------------------|
| K.CNTT       | Khoa Công nghệ Thông tin     | Phụ trách đào tạo và nghiên cứu về CNTT                                    |
| P.DTAO       | Phòng Đào tạo                | Quản lý chương trình và kết quả học tập                                    |
| P.TCHC       | Phòng Tổ chức Hành chính     | Quản lý nhân sự, hành chính, và văn thư                                    |
| BGH          | Ban Giám hiệu                | Lãnh đạo và điều hành toàn bộ hoạt động của trường                         |
| BGH.PC       | Bộ phận Pháp chế             | Trực thuộc BGH, phụ trách kiểm tra pháp lý và tư vấn pháp lý                |
| P.LT         | Phòng Lưu trữ                | Quản lý lưu trữ tài liệu                                                   |

#### b. Bảng Vai trò / Chức vụ (Roles / Ranks)

| RoleId            | Name                   | Description                                                                                   |
|-------------------|------------------------|-----------------------------------------------------------------------------------------------|
| HIEU_TRUONG       | Hiệu trưởng           | Người đứng đầu BGH, có quyền cao nhất.                                                       |
| TRUONG_KHOA       | Trưởng khoa           | Phụ trách quản lý Khoa CNTT.                                                                 |
| PHO_KHOA          | Phó khoa              | Hỗ trợ Trưởng khoa CNTT.                                                                     |
| CHUYEN_VIEN       | Chuyên viên           | Nhân viên thuộc P.DTAO hoặc P.TCHC.                                                          |
| GIAO_VU           | Giáo vụ               | Nhân viên thuộc K.CNTT, xử lý công việc hành chính của khoa.                                 |
| PHO_PHONG         | Phó phòng             | Hỗ trợ quản lý P.DTAO.                                                                       |
| CAN_BO            | Cán bộ                | Nhân viên thuộc P.TCHC.                                                                      |
| VAN_THU           | Văn thư               | Phụ trách đóng dấu, ghi sổ, kiểm tra ký số, quản lý luồng trình ký.                         |
| PHAP_CHE          | Pháp chế              | Phụ trách kiểm tra tính hợp pháp, tư vấn pháp lý, và phê duyệt tài liệu.                    |
| VISITOR           | Visitor               | Người dùng chưa đăng ký, truy cập tài liệu công khai qua liên kết chia sẻ.                  |
| NHAN_VIEN_LUU_TRU | Nhân viên Lưu trữ     | Phụ trách lưu trữ và khôi phục tài liệu.                                                    |
| NGUOI_NHAN        | Người nhận            | Người được phân phối tài liệu (thuộc recipients).                                           |
| QUAN_TRI_VIEN     | Quản trị viên         | Quản lý toàn hệ thống, có quyền xem và quản lý log.                                        |

#### c. Bảng Người dùng (Users)

| UserId        | Full Name           | Email                                                                     | DeptId | RoleId            | is_admin | is_organization_manager | is_dept_manager | status |
|---------------|---------------------|---------------------------------------------------------------------------|--------|-------------------|----------|-------------------------|-----------------|--------|
| user-ht       | Nguyễn Văn A        | [hieutruong@genifast.edu.vn](mailto:hieutruong@genifast.edu.vn)           | BGH    | HIEU_TRUONG       | true     | true                    | false           | 1      |
| user-tk       | Trần Thị B          | [truongkhoa.cntt@genifast.edu.vn](mailto:truongkhoa.cntt@genifast.edu.vn) | K.CNTT | TRUONG_KHOA       | false    | false                   | true            | 1      |
| user-pk       | Lê Văn C            | [phokhoa.cntt@genifast.edu.vn](mailto:phokhoa.cntt@genifast.edu.vn)       | K.CNTT | PHO_KHOA          | false    | false                   | false           | 1      |
| user-cv       | Phạm Thị D          | [chuyenvien.dtao@genifast.edu.vn](mailto:chuyenvien.dtao@genifast.edu.vn) | P.DTAO | CHUYEN_VIEN       | false    | false                   | false           | 1      |
| user-gv       | Đỗ Văn E            | [giaovu.cntt@genifast.edu.vn](mailto:giaovu.cntt@genifast.edu.vn)         | K.CNTT | GIAO_VU           | false    | false                   | false           | 1      |
| user-pp       | Nguyễn Thị F        | [phophong.dtao@genifast.edu.vn](mailto:phophong.dtao@genifast.edu.vn)     | P.DTAO | PHO_PHONG         | false    | false                   | true            | 1      |
| user-cb       | Trần Văn G          | [canbo.tchc@genifast.edu.vn](mailto:canbo.tchc@genifast.edu.vn)           | P.TCHC | CAN_BO            | false    | false                   | false           | 1      |
| user-vt       | Nguyễn Thị Văn      | [vanthu.tchc@genifast.edu.vn](mailto:vanthu.tchc@genifast.edu.vn)         | P.TCHC | VAN_THU           | false    | false                   | false           | 1      |
| user-pc       | Lê Thị Pháp         | [phapche.bgh@genifast.edu.vn](mailto:phapche.bgh@genifast.edu.vn)         | BGH.PC | PHAP_CHE          | false    | false                   | true            | 1      |
| user-lt       | Nguyễn Thị F        | [luutru@genifast.edu.vn](mailto:luutru@genifast.edu.vn)                   | P.LT   | NHAN_VIEN_LUU_TRU | false    | false                   | false           | 1      |
| user-nn       | Trần Văn G          | [nguoinhan@genifast.edu.vn](mailto:nguoinhan@genifast.edu.vn)             | K.CNTT | NGUOI_NHAN        | false    | false                   | false           | 1      |
| user-qtv      | Lê Thị H            | [quantri@genifast.edu.vn](mailto:quantri@genifast.edu.vn)                 | BGH    | QUAN_TRI_VIEN     | true     | false                   | false           | 1      |
| user-ext      | Hoàng Văn H         | [external@other.org](mailto:external@other.org)                           | NULL   | NONE              | false    | false                   | false           | 1      |
| user-inactive | Vũ Thị I            | [inactive@genifast.edu.vn](mailto:inactive@genifast.edu.vn)               | P.TCHC | CAN_BO            | false    | false                   | false           | 2      |
| user-visitor  | Guest Visitor       | NULL                                                                      | NULL   | VISITOR           | false    | false                   | false           | 1      |

#### d. Bảng Tài liệu (Documents)

| DocId       | Title                                         | AccessType | Status   | OrganizationId | DeptId | CreatedBy                                                                 | Category                 | Recipients         | Confidentiality | Version |
|-------------|-----------------------------------------------|------------|----------|----------------|--------|---------------------------------------------------------------------------|--------------------------|--------------------|-----------------|---------|
| doc-01      | Quy chế tuyển sinh 2026                   | INTERNAL   | PENDING  | 1              | P.DTAO | [chuyenvien.dtao@genifast.edu.vn](mailto:chuyenvien.dtao@genifast.edu.vn) | Quy chế                | [user-ht, user-cv] | INTERNAL        | 1       |
| doc-02      | Kế hoạch đào tạo ngành CNTT               | INTERNAL   | APPROVED | 1              | K.CNTT | [giaovu.cntt@genifast.edu.vn](mailto:giaovu.cntt@genifast.edu.vn)         | Kế hoạch đào tạo | [user-tk, user-gv] | PUBLIC          | 1       |
| doc-03      | Kế hoạch hợp tác quốc tế 2025             | EXTERNAL   | APPROVED | 1              | BGH    | [hieutruong@genifast.edu.vn](mailto:hieutruong@genifast.edu.vn)           | Hợp tác quốc tế   | [user-tk, user-pk] | INTERNAL        | 1       |
| doc-04      | Báo cáo tài chính 2025                    | INTERNAL   | APPROVED | 1              | P.TCHC | [canbo.tchc@genifast.edu.vn](mailto:canbo.tchc@genifast.edu.vn)           | Báo cáo tài chính    | [user-ht, user-lt] | LOCKED          | 1       |
| doc-05      | Hợp đồng đào tạo liên kết                 | EXTERNAL   | APPROVED | 1              | BGH    | [hieutruong@genifast.edu.vn](mailto:hieutruong@genifast.edu.vn)           | Hợp đồng            | []                 | EXTERNAL        | 1       |
| doc-06      | Kế hoạch đào tạo 2025                     | PUBLIC     | APPROVED | 1              | P.DTAO | [phophong.dtao@genifast.edu.vn](mailto:phophong.dtao@genifast.edu.vn)     | Kế hoạch đào tạo | []                 | PUBLIC          | 1       |
| doc-07      | Danh sách sinh viên                       | PRIVATE    | APPROVED | 1              | P.DTAO | [chuyenvien.dtao@genifast.edu.vn](mailto:chuyenvien.dtao@genifast.edu.vn) | Danh sách               | [user-pp]          | PRIVATE         | 1       |
| doc-08      | Bản nháp kế hoạch 2026                    | INTERNAL   | DRAFT    | 1              | P.DTAO | [chuyenvien.dtao@genifast.edu.vn](mailto:chuyenvien.dtao@genifast.edu.vn) | Kế hoạch đào tạo | []                 | INTERNAL        | 1       |
| doc-09      | Danh sách học bổng                        | PUBLIC     | PENDING  | 1              | P.DTAO | [phophong.dtao@genifast.edu.vn](mailto:phophong.dtao@genifast.edu.vn)     | Danh sách               | []                 | PUBLIC          | 1       |
| doc-proj-01 | Kế hoạch chi tiết triển khai DMS GĐ2      | PROJECT    | PENDING  | 1              | K.CNTT | [truongkhoa.cntt@genifast.edu.vn](mailto:truongkhoa.cntt@genifast.edu.vn) | Kế hoạch dự án    | [user-tk, user-cv] | PROJECT         | 1       |

#### e. Bảng Quyền ABAC

| Quyền                     | Mô tả                                                                  | Thuộc tính và Giá trị Yêu cầu                                                                                                                                                                                                                                              | Ghi chú                                                                                            |
|---------------------------|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| documents:share:readonly    | Chia sẻ tài liệu với quyền chỉ đọc (documents:read).                 | - Người dùng: users.organization_id = documents.organization_id, users.status = 1. - Tài liệu: access_type IN (PUBLIC, INTERNAL), status = APPROVED.                                                                                                                          | Gán cho người dùng trong tổ chức để chia sẻ tài liệu công khai hoặc nội bộ. |
| documents:share:forwardable | Chia sẻ tài liệu với quyền documents:forward.                        | - Người dùng: is_dept_manager = true hoặc is_organization_manager = true, users.dept_id = documents.dept_id hoặc users.organization_id = documents.organization_id, users.status = 1. - Tài liệu: access_type IN (PUBLIC, INTERNAL), status = APPROVED.                   | Giới hạn cho quản lý để kiểm soát lan truyền tài liệu.                            |
| documents:share:timebound   | Chia sẻ tài liệu với thời hạn cụ thể (from_date, to_date).           | - Người dùng: is_admin = true, is_organization_manager = true, hoặc is_dept_manager = true, users.status = 1. - Tài liệu: access_type IN (EXTERNAL, INTERNAL, PRIVATE), status = APPROVED.                                                                                  | Dành cho tài liệu nhạy cảm, thời hạn từ from_date đến to_date.                     |
| documents:share:external    | Chia sẻ tài liệu ra ngoài tổ chức.                                   | - Người dùng: is_admin = true hoặc is_organization_manager = true, users.status = 1. - Tài liệu: access_type = EXTERNAL, status = APPROVED.                                                                                                                                 | Giới hạn cho quản trị viên hoặc quản lý tổ chức để đảm bảo bảo mật.   |
| documents:share:orgscope    | Chia sẻ tài liệu trong phạm vi tổ chức.                              | - Người dùng: users.organization_id = documents.organization_id, users.status = 1. - Tài liệu: access_type IN (PUBLIC, INTERNAL), status = APPROVED.                                                                                                                          | Áp dụng cho người dùng trong tổ chức để chia sẻ nội bộ.                         |
| documents:share:shareable   | Chia sẻ tài liệu với quyền documents:share.                          | - Người dùng: is_admin = true, is_organization_manager = true, hoặc is_dept_manager = true, users.dept_id = documents.dept_id hoặc users.organization_id = documents.organization_id, users.status = 1. - Tài liệu: access_type IN (PUBLIC, INTERNAL), status = APPROVED. | Giới hạn cho quản lý để kiểm soát việc cấp quyền chia sẻ tiếp.               |

#### f. Bảng Quyền RBAC Toàn cục

| Permission             | Description                                   | HIEU_TRUONG | TRUONG_KHOA | PHO_KHOA | CHUYEN_VIEN | GIAO_VU | NHAN_VIEN_LUU_TRU | NGUOI_NHAN | QUAN_TRI_VIEN | VAN_THU | PHAP_CHE |
|------------------------|-----------------------------------------------|-------------|-------------|----------|-------------|---------|-------------------|------------|---------------|---------|----------|
| documents:create       | Tạo mới tài liệu                              | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:read         | Xem tài liệu (trong phạm vi)                  | ✅         | ✅         | ✅      | ✅         | ✅     | ✅               | ✅        | ✅           | ✅     | ✅      |
| documents:update       | Chỉnh sửa tài liệu (do mình tạo)              | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:delete       | Xóa tài liệu (do mình tạo)                    | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:approve      | Phê duyệt tài liệu                            | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ✅      |
| documents:reject       | Từ chối tài liệu                              | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ✅     | ✅      |
| documents:submit       | Trình duyệt tài liệu                          | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:distribute   | Phân phối tài liệu sau khi duyệt              | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ❌      |
| documents:download     | Tải xuống tài liệu                            | ✅         | ✅         | ✅      | ✅         | ✅     | ✅               | ✅        | ✅           | ✅     | ✅      |
| documents:upload       | Tải lên tệp đính kèm                          | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:sign         | Ký điện tử tài liệu                           | ✅         | ❌         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ✅     | ❌      |
| documents:lock         | Khóa tài liệu                                 | ✅         | ❌         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ❌      |
| documents:unlock       | Mở khóa tài liệu                              | ✅         | ❌         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ❌      |
| documents:comment      | Thêm nhận xét vào tài liệu                    | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ✅        | ✅           | ✅     | ✅      |
| documents:history      | Xem lịch sử thay đổi tài liệu                 | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:archive      | Lưu trữ tài liệu                              | ✅         | ✅         | ❌      | ❌         | ❌     | ✅               | ❌        | ✅           | ❌     | ❌      |
| documents:restore      | Khôi phục tài liệu đã lưu trữ                 | ✅         | ✅         | ❌      | ❌         | ❌     | ✅               | ❌        | ✅           | ❌     | ❌      |
| documents:publish      | Công khai tài liệu                            | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ❌      |
| documents:track        | Theo dõi trạng thái tài liệu                  | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ✅        | ✅           | ✅     | ✅      |
| documents:version:read | Xem phiên bản cụ thể của tài liệu             | ✅         | ✅         | ✅      | ✅         | ✅     | ✅               | ✅        | ✅           | ✅     | ✅      |
| documents:notify       | Gửi thông báo (email, SMS)                    | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ❌      |
| documents:report       | Tạo báo cáo tài liệu                          | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:export       | Xuất tài liệu ra định dạng khác               | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ✅           | ❌     | ❌      |
| documents:forward      | Chuyển tiếp tài liệu                          | ✅         | ✅         | ✅      | ✅         | ✅     | ❌               | ❌        | ✅           | ✅     | ✅      |
| delegate_process       | Ủy quyền thao tác                             | ✅         | ✅         | ❌      | ❌         | ❌     | ❌               | ❌        | ❌           | ❌     | ❌      |
| audit:log              | Xem và ghi log hệ thống                       | ✅         | ❌         | ❌      | ❌         | ❌     | ❌               | ❌        | ✅           | ❌     | ❌      |

#### g. Bảng Quyền hạn cho Quản lý Dự án

| Tên Quyền            | Mô tả                                                                                       |
|----------------------|---------------------------------------------------------------------------------------------|
| project:read         | Xem thông tin và tài nguyên (tài liệu, task) của dự án.                                    |
| project:manage       | Quản lý thông tin chung của dự án (sửa tên, mô tả, thời gian, trạng thái).                |
| project:member:manage| Quản lý thành viên (thêm, xóa, thay đổi vai trò thành viên).                              |
| project:role:manage  | Quản lý các vai trò tùy chỉnh trong dự án (tạo, sửa, xóa vai trò).                        |
| project:document:upload | Tải lên tài liệu mới cho dự án.                                                        |
| project:task:create  | Tạo công việc (task) mới trong dự án.                                                     |
| project:task:update  | Cập nhật công việc (thay đổi trạng thái, người thực hiện...).                             |
| project:task:delete  | Xóa công việc trong dự án.                                                               |
| project:comment      | Thảo luận, bình luận trên dự án hoặc task thuộc dự án.                                    |

#### h. Bảng Ủy Quyền (Delegations)

| DelegationId | DelegatorId | DelegateeId | Permission           | DocId  | FromDate             | ToDate               | CreatedAt                |
|--------------|-------------|-------------|----------------------|--------|----------------------|----------------------|--------------------------|
| del-01       | user-ht     | user-tk     | documents:approve    | doc-01 | 2025-08-07T00:00:00Z | 2025-08-14T23:59:59Z | Thời gian hiện tại |
| del-02       | user-tk     | user-pk     | documents:distribute | doc-02 | 2025-08-07T00:00:00Z | 2025-08-10T23:59:59Z | Thời gian hiện tại |

#### i. Dữ liệu Dự án

- **ProjectId**: project-dms
- **Tên**: Dự án Triển khai DMS Giai đoạn 2 
- **Tổ chức**: Khoa Công nghệ Thông tin (từ user-tk) 
- **Thời gian**: 07/08/2025 đến 30/11/2025 
- **Trạng thái**: ACTIVE 
- **Vai trò trong Dự án (Project Roles)**: 
  - **prole-lead** (Trưởng dự án): Quyền project:read, project:manage, project:member:manage, project:role:manage, tất cả documents:*, project:task:*, project:comment.
  - **prole-member** (Thành viên): Quyền project:read, documents:read, documents:upload, documents:comment, project:task:read, project:comment.
  - **prole-deputy** (Tổ phó): Quyền project:read, documents:read, documents:update, documents:comment, project:task:read, project:comment.
- **Thành viên Dự án**: 
  - user-tk (Trưởng dự án, prole-lead)
  - user-cv (Thành viên, prole-member, sau nâng cấp thành prole-deputy)
- **Tài liệu Dự án**: 
  - **DocId**: doc-proj-01
  - **Tên**: Kế hoạch chi tiết triển khai DMS GĐ2
  - **ProjectId**: project-dms
  - **AccessType**: PROJECT
  - **Status**: PENDING
  - **Recipients**: [user-tk, user-cv]

#### j. Bảng Thiết bị (Devices)

| DeviceId   | DeviceType      | UserId       | Description                               |
|------------|-----------------|--------------|-------------------------------------------|
| device-001 | COMPANY_DEVICE  | user-ht      | Laptop công ty của Hiệu trưởng           |
| device-002 | COMPANY_DEVICE  | user-cv      | Máy tính bàn của Chuyên viên             |
| device-003 | EXTERNAL_DEVICE | user-ext     | Điện thoại cá nhân ngoài công ty         |
| device-004 | COMPANY_DEVICE  | user-vt      | Máy tính bàn của Văn thư                 |
| device-005 | EXTERNAL_DEVICE | user-visitor | Thiết bị của Visitor                    |

## Phần 2: Kịch Bản Kiểm Thử Phân Quyền ABAC

### 2.1. Kịch Bản 1: Vai Trò "Chuyên viên" (user-cv)

**Người dùng**: Phạm Thị D ([chuyenvien.dtao@genifast.edu.vn](mailto:chuyenvien.dtao@genifast.edu.vn)), P.DTAO.

#### Tình huống 1.1: Chia sẻ tài liệu trong quyền (In-Scope)
- **Mô tả**: Chuyên viên chia sẻ "Quy chế tuyển sinh 2026" (doc-01, INTERNAL, PENDING) cho Giáo vụ (user-gv) với quyền chỉ đọc. 
- **Thiết lập**: user-cv có quyền documents:share:readonly, documents:share:orgscope. 
- **Các bước**: 
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-01/share với body
- **Kết quả mong đợi**:
  - API trả về 201 Created. 
  - Bản ghi mới trong document_permissions: 
    - user_id: user-gv, doc_id: doc-01, permission: documents:read, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-cv, doc_id: doc-01, action: SHARE_DOCUMENT, details: "Shared document ID doc-01 with user ID user-gv with permission documents:read", session_id: Session của user-cv.

#### Tình huống 1.2: Chia sẻ ngoài quyền (Out-of-Scope)
- **Mô tả**: Chuyên viên cố gắng chia sẻ doc-01 cho Giáo vụ với quyền documents:share:forwardable.
- **Thiết lập**: user-cv không có quyền documents:share:forwardable.
- **Các bước**: 
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-01/share với body
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Bạn không có quyền thực hiện hành động này."
  - Không có bản ghi mới trong document_permissions.
  - Log trong audit_logs: 
    - user_id: user-cv, doc_id: doc-01, action: SHARE_DOCUMENT_FAILED, details: "Failed to share document ID doc-01 with user ID user-gv: No permission documents:share:forwardable", session_id: Session của user-cv.

#### Tình huống 1.3: Chia sẻ tài liệu PRIVATE
- **Mô tả**: Chuyên viên chia sẻ "Danh sách sinh viên" (doc-07, PRIVATE, APPROVED) cho Phó phòng (user-pp) với quyền chỉ đọc, thời hạn 05/08/2025 đến 12/08/2025.
- **Thiết lập**: user-pp trong private_docs, user-cv có quyền documents:share:readonly, documents:share:timebound.
- **Các bước**: 
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-07/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created.
  - Bản ghi mới trong document_permissions: 
    - user_id: user-pp, doc_id: doc-07, permission: documents:read, from_date: 2025-08-05T00:00:00Z, to_date: 2025-08-12T23:59:59Z, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-cv, doc_id: doc-07, action: SHARE_DOCUMENT, details: "Shared document ID doc-07 with user ID user-pp with permission documents:read (timebound from 2025-08-05 to 2025-08-12)", session_id: Session của user-cv.

#### Tình huống 1.4: Chia sẻ tài liệu PRIVATE cho người không thuộc private_docs
- **Mô tả**: Chuyên viên cố gắng chia sẻ doc-07 cho Cán bộ (user-cb) không thuộc private_docs. 
- **Thiết lập**: user-cb không trong private_docs. 
- **Các bước**: 
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-07/share với body
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Recipient not authorized for private document."
  - Không có bản ghi mới trong document_permissions.
  - Log trong audit_logs: 
    - user_id: user-cv, doc_id: doc-07, action: SHARE_DOCUMENT_FAILED, details: "Failed to share document ID doc-07 with user ID user-cb: Recipient not in private_docs", session_id: Session của user-cv.

### 2.2. Kịch Bản 2: Vai Trò "Trưởng khoa" (user-tk)

**Người dùng**: Trần Thị B ([truongkhoa.cntt@genifast.edu.vn](mailto:truongkhoa.cntt@genifast.edu.vn)), K.CNTT.

#### Tình huống 2.1: Chia sẻ tài liệu với quyền forwardable và timebound
- **Mô tả**: Trưởng khoa chia sẻ "Kế hoạch đào tạo ngành CNTT" (doc-02, INTERNAL, APPROVED) cho Phó khoa (user-pk) với quyền documents:share:forwardable, documents:share:timebound, thời hạn 05/08/2025 đến 10/08/2025.
- **Thiết lập**: user-tk có quyền documents:share:forwardable, documents:share:timebound, documents:share:orgscope.
- **Các bước**: 
  - Đăng nhập: truongkhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-02/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created.
  - Bản ghi mới trong document_permissions: 
    - user_id: user-pk, doc_id: doc-02, permission: documents:read, documents:forward, from_date: 2025-08-05T00:00:00Z, to_date: 2025-08-10T23:59:59Z, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-tk, doc_id: doc-02, action: SHARE_DOCUMENT, details: "Shared document ID doc-02 with user ID user-pk with permissions documents:read, documents:forward (timebound from 2025-08-05 to 2025-08-10)", session_id: Session của user-tk.

#### Tình huống 2.2: Chia sẻ ngoài phạm vi tổ chức
- **Mô tả**: Trưởng khoa cố gắng chia sẻ doc-02 cho user-ext (organization_id ≠ 1). 
- **Thiết lập**: user-tk có quyền documents:share:orgscope nhưng không có documents:share:external. 
- **Các bước**: 
  - Đăng nhập: truongkhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-02/share với body
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Recipient is not in the same organization."
  - Không có bản ghi mới trong document_permissions.
  - Log trong audit_logs: 
    - user_id: user-tk, doc_id: doc-02, action: SHARE_DOCUMENT_FAILED, details: "Failed to share document ID doc-02 with user ID user-ext: Recipient not in organization", session_id: Session của user-tk.

#### Tình huống 2.3: Chia sẻ tài liệu PUBLIC
- **Mô tả**: Trưởng khoa chia sẻ "Kế hoạch đào tạo 2025" (doc-06, PUBLIC, APPROVED) cho Cán bộ (user-cb) với quyền documents:share:shareable. 
- **Thiết lập**: user-tk có quyền documents:share:shareable, documents:share:orgscope. 
- **Các bước**: 
  - Đăng nhập: truongkhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-06/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created.
  - Bản ghi mới trong document_permissions: 
    - user_id: user-cb, doc_id: doc-06, permission: documents:share, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-tk, doc_id: doc-06, action: SHARE_DOCUMENT, details: "Shared document ID doc-06 with user ID user-cb with permission documents:share", session_id: Session của user-tk.

### 2.3. Kịch Bản 3: Vai Trò "Hiệu trưởng" (user-ht)

**Người dùng**: Nguyễn Văn A ([hieutruong@genifast.edu.vn](mailto:hieutruong@genifast.edu.vn)), BGH.

#### Tình huống 3.1: Chia sẻ tài liệu ra ngoài tổ chức
- **Mô tả**: Hiệu trưởng chia sẻ "Kế hoạch hợp tác quốc tế 2025" (doc-03, EXTERNAL, APPROVED) cho user-ext với quyền documents:share:readonly.
- **Thiết lập**: user-ht có quyền documents:share:external, documents:share:readonly.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-03/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created.
  - Bản ghi mới trong document_permissions: 
    - user_id: user-ext, doc_id: doc-03, permission: documents:read, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-03, action: SHARE_DOCUMENT, details: "Shared document ID doc-03 with user ID user-ext with permission documents:read (external)", session_id: Session của user-ht.

#### Tình huống 3.2: Chia sẻ với quyền shareable
- **Mô tả**: Hiệu trưởng chia sẻ "Hợp đồng đào tạo liên kết" (doc-05, EXTERNAL, APPROVED) cho Trưởng khoa (user-tk) với quyền documents:share:shareable.
- **Thiết lập**: user-ht có quyền documents:share:shareable.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-05/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created.
  - Bản ghi mới trong document_permissions: 
    - user_id: user-tk, doc_id: doc-05, permission: documents:share, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-05, action: SHARE_DOCUMENT, details: "Shared document ID doc-05 with user ID user-tk with permission documents:share", session_id: Session của user-ht.

#### Tình huống 3.3: Chia sẻ cho người dùng không hoạt động
- **Mô tả**: Hiệu trưởng cố gắng chia sẻ doc-03 cho user-inactive (status = 2).
- **Thiết lập**: user-inactive có status = 2.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-03/share với body
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Recipient is not active."
  - Không có bản ghi mới trong document_permissions.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-03, action: SHARE_DOCUMENT_FAILED, details: "Failed to share document ID doc-03 with user ID user-inactive: Recipient not active", session_id: Session của user-ht.

#### Tình huống 3.4: Chia sẻ tài liệu PUBLIC qua liên kết công khai cho Visitor
- **Mô tả**: Hiệu trưởng tạo liên kết công khai cho "Kế hoạch đào tạo 2025" (doc-06, PUBLIC, APPROVED) cho Visitor với quyền documents:share:readonly, documents:share:timebound, thời hạn 05/08/2025 đến 08/08/2025.
- **Thiết lập**: user-ht có quyền documents:share:readonly.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-06/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created, tạo liên kết công khai với access_token (giả sử token-abc123).
  - Bản ghi mới trong document_permissions: 
    - user_id: NULL, doc_id: doc-06, permission: documents:read, from_date: 2025-08-05T00:00:00Z, to_date: 2025-08-08T23:59:59Z, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-06, action: SHARE_DOCUMENT, details: "Created public link for document ID doc-06 with permission documents:read (timebound from 2025-08-05 to 2025-08-08)", session_id: Session của user-ht.

### 2.4. Kịch Bản 4: Vai Trò "Phó phòng" (user-pp)

**Người dùng**: Nguyễn Thị F ([phophong.dtao@genifast.edu.vn](mailto:phophong.dtao@genifast.edu.vn)), P.DTAO.

#### Tình huống 4.1: Chia sẻ tài liệu với quyền shareable và timebound
- **Mô tả**: Phó phòng chia sẻ "Báo cáo tài chính 2025" (doc-04, INTERNAL, APPROVED) cho Cán bộ (user-cb) với quyền documents:share:shareable, thời hạn 05/08/2025 đến 12/08/2025.
- **Thiết lập**: user-pp có quyền documents:share:shareable, documents:share:timebound.
- **Các bước**: 
  - Đăng nhập: phophong.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-04/share với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created.
  - Bản ghi mới trong document_permissions: 
    - user_id: user-cb, doc_id: doc-04, permission: documents:share, from_date: 2025-08-05T00:00:00Z, to_date: 2025-08-12T23:59:59Z, created_at: Thời gian hiện tại.
  - Log trong audit_logs: 
    - user_id: user-pp, doc_id: doc-04, action: SHARE_DOCUMENT, details: "Shared document ID doc-04 with user ID user-cb with permission documents:share (timebound from 2025-08-05 to 2025-08-12)", session_id: Session của user-pp.

### 2.5. Kịch Bản 5: Vai Trò "Văn thư" (user-vt)

**Người dùng**: Nguyễn Thị Văn ([vanthu.tchc@genifast.edu.vn](mailto:vanthu.tchc@genifast.edu.vn)), P.TCHC.

#### Tình huống 5.1: Đóng dấu tài liệu PUBLIC
- **Mô tả**: Văn thư đóng dấu "Kế hoạch đào tạo 2025" (doc-06, PUBLIC, APPROVED) trước khi chia sẻ qua liên kết công khai.
- **Thiết lập**: user-vt có quyền documents:share:readonly, documents:sign.
- **Các bước**: 
  - Đăng nhập: vanthu.tchc@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-06/sign với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK.
  - Tài liệu doc-06 cập nhật is_stamped = true.
  - Log trong audit_logs: 
    - user_id: user-vt, doc_id: doc-06, action: SIGN_DOCUMENT, details: "Stamped document ID doc-06: Official stamp applied", session_id: Session của user-vt.

#### Tình huống 5.2: Từ chối tài liệu DRAFT do thiếu ký số
- **Mô tả**: Văn thư từ chối "Bản nháp kế hoạch 2026" (doc-08, INTERNAL, DRAFT) do thiếu ký số.
- **Thiết lập**: user-vt có quyền documents:share:readonly, documents:reject.
- **Các bước**: 
  - Đăng nhập: vanthu.tchc@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-08/review với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK.
  - Tài liệu doc-08 cập nhật review_status = rejected.
  - Log trong audit_logs: 
    - user_id: user-vt, doc_id: doc-08, action: REJECT_DOCUMENT, details: "Rejected document ID doc-08: Missing digital signature", session_id: Session của user-vt.

#### Tình huống 5.3: Thao tác trong quyền và ngoài quyền - Ký tài liệu INTERNAL và PRIVATE
- **Mô tả**: Văn thư ký tài liệu “Quy chế tuyển sinh 2026” (doc-01, INTERNAL, PENDING) và cố gắng ký tài liệu “Danh sách sinh viên” (doc-07, PRIVATE, APPROVED).
- **Thiết lập**: user-vt có quyền documents:sign, doc-01 thuộc P.DTAO, doc-07 là PRIVATE và user-vt không trong private_docs.
- **Các bước**:
  - Đăng nhập: vanthu.tchc@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-01/sign với body
  - Gọi API: POST /api/v1/documents/doc-07/sign.
- **Kết quả mong đợi**: 
  - Bước 2: API trả về 200 OK, cập nhật is_signed = true cho doc-01.
  - Bước 3: API trả về 403 Forbidden, thông báo: "User not authorized for private document."
  - Log trong audit_logs: 
    - Bước 2: user_id: user-vt, doc_id: doc-01, action: SIGN_DOCUMENT, details: "Signed document ID doc-01: Official digital signature applied", session_id: Session của user-vt.
    - Bước 3: user_id: user-vt, doc_id: doc-07, action: SIGN_DOCUMENT_FAILED, details: "Failed to sign document ID doc-07: User not in private_docs", session_id: Session của user-vt.

### 2.6. Kịch Bản 6: Vai Trò "Pháp chế" (user-pc)

**Người dùng**: Lê Thị Pháp ([phapche.bgh@genifast.edu.vn](mailto:phapche.bgh@genifast.edu.vn)), BGH.PC.

#### Tình huống 6.1: Phê duyệt tài liệu EXTERNAL
- **Mô tả**: Pháp chế phê duyệt "Hợp đồng đào tạo liên kết" (doc-05, EXTERNAL, APPROVED).
- **Thiết lập**: user-pc có quyền documents:share:readonly, documents:approve.
- **Các bước**: 
  - Đăng nhập: phapche.bgh@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-05/review với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK.
  - Tài liệu doc-05 cập nhật legal_status = approved.
  - Log trong audit_logs: 
    - user_id: user-pc, doc_id: doc-05, action: APPROVE_DOCUMENT, details: "Approved document ID doc-05", session_id: Session của user-pc.

## Phần 3: Kịch Bản Kiểm Thử Phân Quyền RBAC Toàn cục

### 3.1. Kịch Bản 9: Vai Trò "Hiệu trưởng" (user-ht)

**Người dùng**: Nguyễn Văn A ([hieutruong@genifast.edu.vn](mailto:hieutruong@genifast.edu.vn)), BGH.

#### Tình huống 9.1: Thao tác trong quyền - Ký điện tử tài liệu
- **Mô tả**: Hiệu trưởng ký điện tử tài liệu doc-04.
- **Thiết lập**: user-ht có quyền documents:sign.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-04/sign.
- **Kết quả mong đợi**: 
  - API trả về 200 OK, cập nhật trạng thái tài liệu (thêm chữ ký điện tử).
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-04, action: SIGN, details: "Signed document doc-04", session_id: Session của user-ht.

#### Tình huống 9.2: Thao tác trong quyền - Khóa tài liệu
- **Mô tả**: Hiệu trưởng khóa tài liệu doc-01.
- **Thiết lập**: user-ht có quyền documents:lock.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-01/lock.
- **Kết quả mong đợi**: 
  - API trả về 200 OK, cập nhật confidentiality = LOCKED.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-01, action: LOCK, details: "Locked document doc-01", session_id: Session của user-ht.

#### Tình huống 9.3: Thao tác ngoài quyền - Xem log hệ thống
- **Mô tả**: Hiệu trưởng thử xem log toàn hệ thống (chỉ Quản trị viên có quyền).
- **Thiết lập**: user-ht không có quyền audit:log.
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: GET /api/v1/audit-logs.
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Bạn không có quyền thực hiện hành động này."
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: NULL, action: AUDIT_LOG_READ_FAILED, details: "Failed: No audit:log permission", session_id: Session của user-ht.

#### Tình huống 9.4: Thao tác trong quyền - Công khai tài liệu
- **Mô tả**: Hiệu trưởng công khai tài liệu “Danh sách học bổng” (doc-09, PUBLIC, PENDING).
- **Thiết lập**: user-ht có quyền documents:publish.
- **Các bước**:
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-09/publish.
- **Kết quả mong đợi**:
  - API trả về 200 OK, cập nhật access_type = PUBLIC, status = APPROVED.
  - Log trong audit_logs:
    - user_id: user-ht, doc_id: doc-09, action: PUBLISH_DOCUMENT, details: “Published document doc-09”, session_id: Session của user-ht.

#### Tình huống 9.5: Thao tác trong quyền - Gửi thông báo tài liệu
- **Mô tả**: Hiệu trưởng gửi thông báo về tài liệu doc-09 đến Trưởng khoa (user-tk).
- **Thiết lập**: user-ht có quyền documents:notify.
- **Các bước**:
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-09/notify với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK, gửi thông báo thành công.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-09, action: NOTIFY_DOCUMENT, details: "Notified user-tk about document doc-09", session_id: Session của user-ht.

#### Tình huống 9.6: Thao tác trong quyền - Mở khóa tài liệu
- **Mô tả**: Hiệu trưởng mở khóa tài liệu "Báo cáo tài chính 2025" (doc-04, LOCKED). 
- **Thiết lập**: user-ht có quyền documents:unlock, doc-04 ở trạng thái confidentiality = LOCKED. 
- **Các bước**: 
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-04/unlock.
- **Kết quả mong đợi**: 
  - API trả về 200 OK, cập nhật confidentiality = INTERNAL.
  - Log trong audit_logs: 
    - user_id: user-ht, doc_id: doc-04, action: UNLOCK_DOCUMENT, details: "Unlocked document doc-04", session_id: Session của user-ht.

#### Tình huống 9.7: Thao tác trong quyền với ABAC - Đọc tài liệu PRIVATE
- **Mô tả**: Hiệu trưởng (user-ht) đọc tài liệu “Danh sách sinh viên” (doc-07, PRIVATE, APPROVED) từ thiết bị công ty (device-001) và thử từ thiết bị bên ngoài (device-003).
- **Thiết lập**: user-ht có quyền documents:read, is_organization_manager = true, nhưng không trong recipients của doc-07. Quy tắc ABAC yêu cầu device_type = COMPANY_DEVICE cho tài liệu PRIVATE.
- **Các bước**:
  - Đăng nhập: hieutruong@genifast.edu.vn.
  - Gọi API: GET /api/v1/documents/doc-07 với header X-Device-Id: device-001.
  - Gọi API: GET /api/v1/documents/doc-07 với header X-Device-Id: device-003.
- **Kết quả mong đợi**:
  - Bước 2: API trả về 200 OK với nội dung doc-07 (do user-ht là organization manager và sử dụng thiết bị công ty).
  - Bước 3: API trả về 403 Forbidden, thông báo: "Access denied from external device for private document."
  - Log trong audit_logs:
    - Bước 2: user_id: user-ht, doc_id: doc-07, action: READ_DOCUMENT, details: "Read document ID doc-07 (private access by organization manager, device-001)", session_id: Session của user-ht, device_id: device-001.
    - Bước 3: user_id: user-ht, doc_id: doc-07, action: READ_DOCUMENT_FAILED, details: "Failed to read document ID doc-07: Access denied from external device", session_id: Session của user-ht, device_id: device-003.

### 3.2. Kịch Bản 10: Vai Trò "Trưởng khoa" (user-tk)

**Người dùng**: Trần Thị B ([truongkhoa.cntt@genifast.edu.vn](mailto:truongkhoa.cntt@genifast.edu.vn)), K.CNTT.

#### Tình huống 10.1: Thao tác trong quyền - Phê duyệt và phân phối tài liệu
- **Mô tả**: Trưởng khoa phê duyệt và phân phối tài liệu doc-02.
- **Thiết lập**: user-tk có quyền documents:approve, documents:distribute.
- **Các bước**: 
  - Đăng nhập: truongkhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-02/approve.
  - Gọi API: POST /api/v1/documents/doc-02/distribute với body
- **Kết quả mong đợi**: 
  - API approve trả về 200 OK, cập nhật status = APPROVED.
  - API distribute trả về 200 OK, cập nhật recipients.
  - Log trong audit_logs: 
    - user_id: user-tk, doc_id: doc-02, action: APPROVE, details: "Approved document doc-02", session_id: Session của user-tk.
    - user_id: user-tk, doc_id: doc-02, action: DISTRIBUTE, details: "Distributed document doc-02 to user-gv, user-nn", session_id: Session của user-tk.

#### Tình huống 10.2: Thao tác ngoài quyền - Ký điện tử tài liệu
- **Mô tả**: Trưởng khoa thử ký điện tử tài liệu doc-02.
- **Thiết lập**: user-tk không có quyền documents:sign.
- **Các bước**: 
  - Đăng nhập: truongkhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-02/sign.
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Bạn không có quyền thực hiện hành động này."
  - Log trong audit_logs: 
    - user_id: user-tk, doc_id: doc-02, action: SIGN_DOCUMENT_FAILED, details: "Failed: No documents:sign permission", session_id: Session của user-tk.

#### Tình huống 10.3: Thao tác trong quyền - Tải lên tài liệu
- **Mô tả**: Trưởng khoa tải lên tài liệu mới cho “Kế hoạch hợp tác quốc tế 2025” (doc-03).
- **Thiết lập**: user-tk có quyền documents:upload.
- **Các bước**:
  - Đăng nhập: truongkhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-03/upload với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK, cập nhật tệp đính kèm cho doc-03.
  - Log trong audit_logs: 
    - user_id: user-tk, doc_id: doc-03, action: UPLOAD_DOCUMENT, details: "Uploaded file to document doc-03", session_id: Session của user-tk.

### 3.3. Kịch Bản 11: Vai Trò "Phó khoa" (user-pk)

**Người dùng**: Lê Văn C ([phokhoa.cntt@genifast.edu.vn](mailto:phokhoa.cntt@genifast.edu.vn)), K.CNTT.

#### Tình huống 11.1: Thao tác trong quyền - Cập nhật tài liệu
- **Mô tả**: Phó khoa cập nhật tài liệu doc-03 (do mình tạo).
- **Thiết lập**: user-pk có quyền documents:update.
- **Các bước**: 
  - Đăng nhập: phokhoa.cntt@genifast.edu.vn.
  - Gọi API: PUT /api/v1/documents/doc-03 với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK, tạo bản ghi mới trong DocumentVersions với version_number = 2.
  - Log trong audit_logs: 
    - user_id: user-pk, doc_id: doc-03, action: UPDATE, details: "Updated document doc-03 to version 2", session_id: Session của user-pk.

#### Tình huống 11.2: Thao tác ngoài quyền - Phê duyệt tài liệu
- **Mô tả**: Phó khoa thử phê duyệt tài liệu doc-03.
- **Thiết lập**: user-pk không có quyền documents:approve.
- **Các bước**: 
  - Đăng nhập: phokhoa.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-03/approve.
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Bạn không có quyền thực hiện hành động này."
  - Log trong audit_logs: 
    - user_id: user-pk, doc_id: doc-03, action: APPROVE_DOCUMENT_FAILED, details: "Failed: No documents:approve permission", session_id: Session của user-pk.

#### Tình huống 11.3: Thao tác trong quyền và ngoài quyền với ABAC - Tải xuống tài liệu
- **Mô tả**: Phó khoa tải xuống tài liệu “Kế hoạch đào tạo 2025” (doc-06, PUBLIC, APPROVED) và cố gắng tải xuống “Quy chế tuyển sinh 2026” (doc-01, INTERNAL, PENDING) không thuộc recipients.
- **Thiết lập**: user-pk có quyền documents:download, doc-06 là PUBLIC, doc-01 thuộc P.DTAO và user-pk không trong recipients.
- **Các bước**:
  - Đăng nhập: phokhoa.cntt@genifast.edu.vn.
  - Gọi API: GET /api/v1/documents/doc-06/download.
  - Gọi API: GET /api/v1/documents/doc-01/download.
- **Kết quả mong đợi**:
  - Bước 2: API trả về 200 OK với file tải về cho doc-06.
  - Bước 3: API trả về 403 Forbidden, thông báo: “User not authorized to access document.”
  - Log trong audit_logs:
    - Bước 2: user_id: user-pk, doc_id: doc-06, action: DOWNLOAD_DOCUMENT, details: “Downloaded document doc-06 (public access)”, session_id: Session của user-pk.
    - Bước 3: user_id: user-pk, doc_id: doc-01, action: DOWNLOAD_DOCUMENT_FAILED, details: “Failed to download document doc-01: User not in recipients”, session_id: Session của user-pk.

### 3.4. Kịch Bản 12: Vai Trò "Chuyên viên" (user-cv)

**Người dùng**: Phạm Thị D ([chuyenvien.dtao@genifast.edu.vn](mailto:chuyenvien.dtao@genifast.edu.vn)), P.DTAO.

#### Tình huống 12.1: Thao tác trong quyền - Tạo và trình duyệt tài liệu
- **Mô tả**: Chuyên viên tạo và trình duyệt tài liệu doc-01.
- **Thiết lập**: user-cv có quyền documents:create, documents:submit.
- **Các bước**: 
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents với body
  - Gọi API: POST /api/v1/documents/doc-01/submit.
- **Kết quả mong đợi**: 
  - API POST /documents trả về 201 Created, tạo bản ghi trong Documents.
  - API POST /submit trả về 200 OK, cập nhật status = PENDING.
  - Log trong audit_logs: 
    - user_id: user-cv, doc_id: doc-01, action: CREATE, details: "Created document Quy chế tuyển sinh 2026", session_id: Session của user-cv.
    - user_id: user-cv, doc_id: doc-01, action: SUBMIT, details: "Submitted document doc-01 for approval", session_id: Session của user-cv.

#### Tình huống 12.2: Thao tác ngoài quyền - Phân phối tài liệu
- **Mô tả**: Chuyên viên thử phân phối tài liệu doc-01.
- **Thiết lập**: user-cv không có quyền documents:distribute.
- **Các bước**: 
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-01/distribute với body
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Bạn không có quyền thực hiện hành động này."
  - Log trong audit_logs: 
    - user_id: user-cv, doc_id: doc-01, action: DISTRIBUTE_DOCUMENT_FAILED, details: "Failed: No documents:distribute permission", session_id: Session của user-cv.

#### Tình huống 12.3: Thao tác trong quyền với ABAC - Trình duyệt tài liệu PRIVATE
- **Mô tả**: Chuyên viên trình duyệt tài liệu “Danh sách sinh viên” (doc-07, PRIVATE, APPROVED).
- **Thiết lập**: user-cv có quyền documents:submit, user-cv trong private_docs và là created_by của doc-07.
- **Các bước**:
  - Đăng nhập: chuyenvien.dtao@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-07/submit.
- **Kết quả mong đợi**:
  - API trả về 200 OK, cập nhật status = PENDING.
  - Log trong audit_logs:
    - user_id: user-cv, doc_id: doc-07, action: SUBMIT_DOCUMENT, details: “Submitted document doc-07 for approval (private document)”, session_id: Session của user-cv.

### 3.5. Kịch Bản 13: Vai Trò "Giáo vụ" (user-gv)

**Người dùng**: Đỗ Văn E ([giaovu.cntt@genifast.edu.vn](mailto:giaovu.cntt@genifast.edu.vn)), K.CNTT.

#### Tình huống 13.1: Thao tác trong quyền - Thêm nhận xét vào tài liệu
- **Mô tả**: Giáo vụ thêm nhận xét vào tài liệu doc-02.
- **Thiết lập**: user-gv có quyền documents:comment.
- **Các bước**: 
  - Đăng nhập: giaovu.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-02/comment với body
- **Kết quả mong đợi**: 
  - API trả về 201 Created, tạo bản ghi trong DocumentComments.
  - Log trong audit_logs: 
    - user_id: user-gv, doc_id: doc-02, action: COMMENT, details: "Added comment to document doc-02", session_id: Session của user-gv.

#### Tình huống 13.2: Thao tác ngoài quyền - Từ chối tài liệu
- **Mô tả**: Giáo vụ thử từ chối tài liệu doc-02.
- **Thiết lập**: user-gv không có quyền documents:reject.
- **Các bước**: 
  - Đăng nhập: giaovu.cntt@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-02/reject.
- **Kết quả mong đợi**: 
  - API trả về 403 Forbidden, thông báo: "Bạn không có quyền thực hiện hành động này."
  - Log trong audit_logs: 
    - user_id: user-gv, doc_id: doc-02, action: REJECT_DOCUMENT_FAILED, details: "Failed: No documents:reject permission", session_id: Session của user-gv.

#### Tình huống 13.3: Thao tác trong quyền - Tải xuống tài liệu
- **Mô tả**: Giáo vụ tải xuống tài liệu “Kế hoạch đào tạo ngành CNTT” (doc-02).
- **Thiết lập**: user-gv có quyền documents:download.
- **Các bước**:
  - Đăng nhập: giaovu.cntt@genifast.edu.vn.
  - Gọi API: GET /api/v1/documents/doc-02/download.
- **Kết quả mong đợi**:
  - API trả về 200 OK với file tải về.
  - Log trong audit_logs:
    - user_id: user-gv, doc_id: doc-02, action: DOWNLOAD_DOCUMENT, details: “Downloaded document doc-02”, session_id: Session của user-gv.

### 3.6. Kịch Bản 14: Vai Trò "Nhân viên Lưu trữ" (user-lt)

**Người dùng**: Nguyễn Thị F ([luutru@genifast.edu.vn](mailto:luutru@genifast.edu.vn)), phòng Lưu Trữ.

#### Tình huống 14.1: Thao tác trong quyền - Lưu trữ tài liệu
- **Mô tả**: Nhân viên Lưu trữ lưu trữ tài liệu “Kế hoạch đào tạo 2025” (doc-06, PUBLIC, APPROVED).
- **Thiết lập**: user-lt có quyền documents:archive.
- **Các bước**:
  - Đăng nhập: luutru@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-06/archive.
- **Kết quả mong đợi**:
  - API trả về 200 OK, cập nhật archive_status = ARCHIVED.
  - Log trong audit_logs:
    - user_id: user-lt, doc_id: doc-06, action: ARCHIVE_DOCUMENT, details: “Archived document doc-06”, session_id: Session của user-lt.

#### Tình huống 14.2: Thao tác trong quyền - Khôi phục tài liệu
- **Mô tả**: Nhân viên Lưu trữ khôi phục tài liệu doc-06 đã lưu trữ.
- **Thiết lập**: user-lt có quyền documents:restore, doc-06 ở trạng thái archive_status = ARCHIVED.
- **Các bước**:
  - Đăng nhập: luutru@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-06/restore.
- **Kết quả mong đợi**:
  - API trả về 200 OK, cập nhật archive_status = RESTORED.
  - Log trong audit_logs:
    - user_id: user-lt, doc_id: doc-06, action: RESTORE_DOCUMENT, details: “Restored document doc-06”, session_id: Session của user-lt.

#### Tình huống 14.3: Thao tác trong quyền - Tải xuống tài liệu
- **Mô tả**: Nhân viên Lưu trữ tải xuống tài liệu doc-06.
- **Thiết lập**: user-lt có quyền documents:download.
- **Các bước**:
  - Đăng nhập: luutru@genifast.edu.vn.
  - Gọi API: GET /api/v1/documents/doc-06/download.
- **Kết quả mong đợi**:
  - API trả về 200 OK với file tải về.
  - Log trong audit_logs:
    - user_id: user-lt, doc_id: doc-06, action: DOWNLOAD_DOCUMENT, details: “Downloaded document doc-06”, session_id: Session của user-lt.

#### Tình huống 14.4: Thao tác trong quyền - Xem phiên bản tài liệu
- **Mô tả**: Nhân viên Lưu trữ xem phiên bản cụ thể của tài liệu doc-04 (version = 1).
- **Thiết lập**: user-lt có quyền documents:version:read.
- **Các bước**:
  - Đăng nhập: luutru@genifast.edu.vn.
  - Gọi API: GET /api/v1/documents/doc-04/versions/1.
- **Kết quả mong đợi**:
  - API trả về 200 OK với nội dung phiên bản 1 của doc-04.
  - Log trong audit_logs:
    - user_id: user-lt, doc_id: doc-04, action: READ_VERSION, details: “Read version 1 of document doc-04”, session_id: Session của user-lt.

#### Tình huống 14.5: Thao tác ngoài quyền - Phê duyệt tài liệu
- **Mô tả**: Nhân viên Lưu trữ cố gắng phê duyệt tài liệu doc-06.
- **Thiết lập**: user-lt không có quyền documents:approve.
- **Các bước**:
  - Đăng nhập: luutru@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/doc-06/approve.
- **Kết quả mong đợi**:
  - API trả về 403 Forbidden, thông báo: “Bạn không có quyền thực hiện hành động này.”
  - Log trong audit_logs:
    - user_id: user-lt, doc_id: doc-06, action: APPROVE_DOCUMENT_FAILED, details: “Failed: No documents:approve permission”, session_id: Session của user-lt.

### 3.7. Kịch Bản 15: Vai Trò "Quản trị viên" (user-qtv)

**Người dùng**: Lê Thị H ([quantri@genifast.edu.vn](mailto:quantri@genifast.edu.vn)), Ban Giám Hiệu.

#### Tình huống 15.1: Thao tác trong quyền - Tạo báo cáo tài liệu
- **Mô tả**: Quản trị viên tạo báo cáo tài liệu cho tất cả tài liệu trong tổ chức.
- **Thiết lập**: user-qtv có quyền documents:report.
- **Các bước**:
  - Đăng nhập: quantri@genifast.edu.vn.
  - Gọi API: POST /api/v1/documents/report với body
- **Kết quả mong đợi**: 
  - API trả về 200 OK với nội dung báo cáo.
  - Log trong audit_logs: 
    - user_id