# Hệ thống Quản lý Tài liệu (DMS) - Genifast

## Giới thiệu

**DMS Genifast** là một hệ thống quản lý tài liệu mạnh mẽ và linh hoạt, được xây dựng để giúp các tổ chức và doanh nghiệp sắp xếp, lưu trữ, và truy xuất tài liệu một cách hiệu quả. Hệ thống tích hợp các tính năng tìm kiếm nâng cao bằng từ khóa và ngôn ngữ tự nhiên, giải quyết vấn đề thiếu hiệu quả trong các phương pháp quản lý tài liệu truyền thống.

## Tính năng chính

Hệ thống cung cấp một loạt các tính năng được phân chia theo vai trò người dùng:

### 👤 **Quản trị viên (Admin)**

  * **Quản lý Hệ thống:** Toàn quyền cấu hình và giám sát hệ thống.
  * **Quản lý Tổ chức:** Phê duyệt/từ chối yêu cầu tạo tổ chức mới, vô hiệu hóa hoặc xóa tổ chức.
  * **Quản lý Người dùng:** Quản lý tài khoản người dùng, phân quyền quản trị. (Chưa thực hiện)
  * **Quản lý Sản phẩm:** Tạo và quản lý các gói dịch vụ (ví dụ: dung lượng lưu trữ) để bán cho các tổ chức.
  * **Quản lý Giao dịch:** Theo dõi và quản lý các giao dịch mua bán trong hệ thống. (Chưa thực hiện)

### 🏢 **Quản lý Tổ chức (Organization Manager)**

  * **Quản lý Thông tin Tổ chức:** Chỉnh sửa thông tin chi tiết của tổ chức.
  * **Quản lý Phòng ban:** Tạo, sửa, xóa phòng ban và gán/bãi nhiệm vai trò quản lý phòng ban.
  * **Quản lý Thành viên:** Mời/xóa thành viên khỏi tổ chức, gán/bãi nhiệm vai trò quản lý tổ chức cho thành viên khác.
  * **Mua Dịch vụ:** Mua thêm các gói sản phẩm (ví dụ: dung lượng) cho tổ chức. (Chưa thực hiện)

### 👥 **Quản lý Phòng ban (Department Manager)**

  * **Quản lý Thư mục (Category):** Tạo, sửa, xóa các thư mục trong phòng ban của mình.
  * **Quản lý Tài liệu:** Tải lên, chỉnh sửa, chia sẻ và quản lý các tài liệu thuộc phòng ban.

### 🧑‍💻 **Người dùng (User)**

  * **Xác thực:** Đăng ký, đăng nhập (qua email/mật khẩu hoặc tài khoản social), đổi mật khẩu, và đặt lại mật khẩu.
  * **Quản lý Hồ sơ:** Xem và cập nhật thông tin cá nhân. (Chưa thực hiện)
  * **Quản lý Tài liệu:**
      * Xem, tải xuống, bình luận, và đánh dấu sao các tài liệu mà họ có quyền truy cập. (Chưa thực hiện)
      * Chia sẻ tài liệu với người dùng khác. (Chưa thực hiện)
      * Tìm kiếm tài liệu bằng từ khóa, siêu dữ liệu (metadata), và ngôn ngữ tự nhiên.
  * **Tham gia Tổ chức:** Chấp nhận lời mời để tham gia vào một tổ chức.

## Công nghệ sử dụng

  * **Backend:** Java 17, Spring Boot 3.3
  * **Database:** PostgreSQL
  * **Security:** Spring Security, JWT (JSON Web Tokens)
  * **ORM:** Spring Data JPA (Hibernate)
  * **Mapping:** MapStruct
  * **API Documentation:** OpenAPI (Swagger)
  * **Build Tool:** Maven

## Hướng dẫn Cài đặt và Chạy dự án

### Yêu cầu

  * JDK 17 hoặc cao hơn
  * Maven 3.8+
  * PostgreSQL

### Các bước cài đặt

1.  **Clone repository:**

    ```bash
    git clone https://github.com/hoangkhang5207/dms-backend.git
    cd dms-backend
    ```

2.  **Cấu hình Database:**

      * Tạo một database mới trong PostgreSQL (ví dụ: `genifast`).
      * Mở file `src/main/resources/application.properties` và cập nhật các thông tin kết nối cho phù hợp:
        ```properties
        spring.datasource.url=jdbc:postgresql://localhost:5432/genifast
        spring.datasource.username=your_postgres_username
        spring.datasource.password=your_postgres_password
        ```

3.  **Cấu hình Email Server:**

      * Để các tính năng như xác thực email, mời thành viên, đặt lại mật khẩu hoạt động, cần cấu hình SMTP server. Dự án đã được cấu hình sẵn để sử dụng Gmail.
      * Mở file `src/main/resources/application.properties` và thay đổi thông tin:
        ```properties
        spring.mail.username=your_email@gmail.com
        spring.mail.password=your_gmail_app_password
        ```
      * **Lưu ý:** Cần tạo "Mật khẩu ứng dụng" (App Password) cho tài khoản Gmail thay vì sử dụng mật khẩu đăng nhập thông thường.

4.  **Build và Chạy ứng dụng:**

      * Sử dụng Maven Wrapper được cung cấp sẵn để build và chạy dự án:
        ```bash
        # Trên Windows
        ./mvnw spring-boot:run

        # Trên macOS/Linux
        ./mvnw spring-boot:run
        ```
      * Ứng dụng sẽ khởi động và chạy tại `http://localhost:8080`.

5.  **Truy cập API Documentation:**

      * Sau khi khởi động thành công, truy cập giao diện Swagger UI để xem tài liệu API chi tiết và tương tác với các endpoint:
        `http://localhost:8080/swagger-ui.html`

## Ví dụ về các Endpoint API

Dưới đây là một số ví dụ về các request/response JSON cho các tính năng chính.

### 1\. Xác thực (Authentication)

#### Đăng ký tài khoản

  * **Endpoint:** `POST /api/v1/auth/signup`
  * **Request Body:**
    ```json
    {
      "first_name": "Khang",
      "last_name": "Hoang",
      "email": "khang@example.com",
      "password": "Password@123",
      "gender": false
    }
    ```
  * **Success Response (201 CREATED):**
    ```
    User registered successfully. Please check your email to verify your account.
    ```

#### Đăng nhập

  * **Endpoint:** `POST /api/v1/auth/login`
  * **Request Body:**
    ```json
    {
      "email": "khang@example.com",
      "password": "Password@123"
    }
    ```
  * **Success Response (200 OK):**
    ```json
    {
      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refresh_token": "aBcDeFgHiJkLmNoPqRsTuVwXyZ..."
    }
    ```

### 2\. Quản lý Tổ chức (Organization)

#### Tạo yêu cầu thành lập Tổ chức

  * **Endpoint:** `POST /api/v1/organizations`
  * **Yêu cầu:** Cần có `Authorization: Bearer <access_token>` trong header.
  * **Request Body:**
    ```json
    {
      "name": "My New Tech Corp",
      "description": "A corporation for developing new technology.",
      "is_openai": false
    }
    ```
  * **Success Response (201 CREATED):**
    ```json
    {
      "id": 1,
      "name": "My New Tech Corp",
      "description": "A corporation for developing new technology.",
      "status": 0,
      "is_openai": false,
      "limit_data": 10737418240,
      "data_used": 0,
      "limit_token": null,
      "token_used": null,
      "percent_data_used": 0,
      "created_at": "2025-07-24T15:30:00.123456",
      "created_by": "khang@example.com",
      "updated_at": "2025-07-24T15:30:00.123456",
      "updated_by": null
    }
    ```

### 3\. Quản lý Tài liệu (Document)

#### Tải lên Tài liệu mới

  * **Endpoint:** `POST /api/v1/documents`
  * **Yêu cầu:** `Content-Type: multipart/form-data` và `Authorization: Bearer <access_token>` header.
  * **Form Data:**
      * `metadata` (part):
        ```json
        {
          "title": "Project Proposal Q3",
          "description": "Detailed proposal for the third quarter.",
          "category_id": 5,
          "access_type": 2
        }
        ```
      * `file` (part): Nội dung file (ví dụ: `proposal.pdf`)
  * **Success Response (201 CREATED):**
    ```json
    {
      "id": 101,
      "title": "Project Proposal Q3",
      "description": "Detailed proposal for the third quarter.",
      "status": 1,
      "type": "pdf",
      "access_type": 2,
      "category_id": 5,
      "department_id": 2,
      "organization_id": 1,
      "original_filename": "proposal.pdf",
      "storage_unit": "1.2 MB",
      "file_path": "unique_file_id_proposal.pdf",
      "created_by": "khang@example.com",
      "created_at": "2025-07-24T15:45:00.987654Z",
      "updated_at": "2025-07-24T15:45:00.987654Z"
    }
    ```

## Tác giả

  * **Tên:** Hoàng Khang
  * **Email:** hoangkhang16112003@gmail.com
  * **GitHub:** [https://github.com/HoangKhang5207](https://github.com/HoangKhang5207)
