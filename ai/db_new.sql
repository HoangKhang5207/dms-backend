-- =====================================================
-- DMS Database Enhancement Script
-- Thêm các bảng và cột mới để hỗ trợ ABAC/RBAC
-- Phiên bản: 1.0
-- Ngày tạo: 09/08/2025
-- =====================================================

-- =====================================================
-- 1. TẠO CÁC BẢNG MỚI
-- =====================================================

-- Bảng projects - Quản lý dự án
CREATE TABLE public.projects (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    organization_id bigint NOT NULL,
    start_date timestamp with time zone,
    end_date timestamp with time zone,
    status integer DEFAULT 1, -- 1: ACTIVE, 2: COMPLETED, 3: SUSPENDED
    created_by bigint,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

-- Sequence cho projects
CREATE SEQUENCE public.projects_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.projects_id_seq OWNER TO postgres;
ALTER SEQUENCE public.projects_id_seq OWNED BY public.projects.id;
ALTER TABLE ONLY public.projects ALTER COLUMN id SET DEFAULT nextval('public.projects_id_seq'::regclass);

-- Primary key và constraints cho projects
ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);

-- Foreign keys cho projects
ALTER TABLE ONLY public.projects
    ADD CONSTRAINT fk_projects_organization FOREIGN KEY (organization_id) REFERENCES public.organizations(id);

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);

-- Comments cho projects
COMMENT ON COLUMN public.projects.status IS '1 - ACTIVE, 2 - COMPLETED, 3 - SUSPENDED';

-- =====================================================

-- Bảng project_members - Thành viên dự án
CREATE TABLE public.project_members (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role integer DEFAULT 1, -- 1: PROJECT_MANAGER, 2: MEMBER, 3: VIEWER
    joined_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status integer DEFAULT 1 -- 1: ACTIVE, 2: INACTIVE
);

-- Sequence cho project_members
CREATE SEQUENCE public.project_members_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.project_members_id_seq OWNER TO postgres;
ALTER SEQUENCE public.project_members_id_seq OWNED BY public.project_members.id;
ALTER TABLE ONLY public.project_members ALTER COLUMN id SET DEFAULT nextval('public.project_members_id_seq'::regclass);

-- Primary key và constraints cho project_members
ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT project_members_pkey PRIMARY KEY (id);

-- Unique constraint để tránh duplicate member trong cùng project
ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT uk_project_members_project_user UNIQUE (project_id, user_id);

-- Foreign keys cho project_members
ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES public.projects(id);

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES public.users(id);

-- Comments cho project_members
COMMENT ON COLUMN public.project_members.role IS '1 - PROJECT_MANAGER, 2 - MEMBER, 3 - VIEWER';
COMMENT ON COLUMN public.project_members.status IS '1 - ACTIVE, 2 - INACTIVE';

-- =====================================================

-- Bảng devices - Quản lý thiết bị
CREATE TABLE public.devices (
    id character varying(50) NOT NULL,
    device_type integer NOT NULL, -- 1: COMPANY_DEVICE, 2: EXTERNAL_DEVICE
    user_id bigint,
    device_name character varying(255),
    registered_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status integer DEFAULT 1 -- 1: ACTIVE, 2: INACTIVE
);

-- Primary key cho devices
ALTER TABLE ONLY public.devices
    ADD CONSTRAINT devices_pkey PRIMARY KEY (id);

-- Foreign key cho devices
ALTER TABLE ONLY public.devices
    ADD CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES public.users(id);

-- Comments cho devices
COMMENT ON COLUMN public.devices.device_type IS '1 - COMPANY_DEVICE, 2 - EXTERNAL_DEVICE';
COMMENT ON COLUMN public.devices.status IS '1 - ACTIVE, 2 - INACTIVE';

-- =====================================================
-- 2. BỔ SUNG CỘT VÀO CÁC BẢNG HIỆN CÓ
-- =====================================================

-- Bổ sung cột cho bảng documents
ALTER TABLE public.documents 
ADD COLUMN project_id bigint,
ADD COLUMN confidentiality integer DEFAULT 2, -- 1: PUBLIC, 2: INTERNAL, 3: PRIVATE, 4: LOCKED, 5: PROJECT
ADD COLUMN recipients text, -- JSON array của user IDs
ADD COLUMN version_number integer DEFAULT 1,
ADD COLUMN signed_at timestamp with time zone,
ADD COLUMN signed_by bigint;

-- Foreign Keys cho documents
ALTER TABLE public.documents 
ADD CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES public.projects(id),
ADD CONSTRAINT fk_documents_signed_by FOREIGN KEY (signed_by) REFERENCES public.users(id);

-- Comments cho documents
COMMENT ON COLUMN public.documents.confidentiality IS '1 - PUBLIC, 2 - INTERNAL, 3 - PRIVATE, 4 - LOCKED, 5 - PROJECT';
COMMENT ON COLUMN public.documents.recipients IS 'JSON array chứa danh sách user IDs được phân phối tài liệu';

-- =====================================================

-- Bổ sung cột cho bảng users
ALTER TABLE public.users 
ADD COLUMN current_device_id character varying(50),
ADD COLUMN full_name character varying(255);

-- Foreign Key cho users
ALTER TABLE public.users 
ADD CONSTRAINT fk_users_device FOREIGN KEY (current_device_id) REFERENCES public.devices(id);

-- =====================================================

-- Bổ sung cột cho bảng audit_logs
ALTER TABLE public.audit_logs 
ADD COLUMN project_id bigint,
ADD COLUMN device_id character varying(50),
ADD COLUMN delegation_id bigint;

-- Foreign Keys cho audit_logs
ALTER TABLE public.audit_logs 
ADD CONSTRAINT fk_audit_logs_project FOREIGN KEY (project_id) REFERENCES public.projects(id),
ADD CONSTRAINT fk_audit_logs_device FOREIGN KEY (device_id) REFERENCES public.devices(id),
ADD CONSTRAINT fk_audit_logs_delegation FOREIGN KEY (delegation_id) REFERENCES public.delegations(id);

-- =====================================================
-- 3. TẠO INDEX ĐỂ TỐI ƯU HIỆU SUẤT
-- =====================================================

-- Index cho projects
CREATE INDEX idx_projects_organization_id ON public.projects(organization_id);
CREATE INDEX idx_projects_status ON public.projects(status);
CREATE INDEX idx_projects_created_by ON public.projects(created_by);

-- Index cho project_members
CREATE INDEX idx_project_members_project_id ON public.project_members(project_id);
CREATE INDEX idx_project_members_user_id ON public.project_members(user_id);
CREATE INDEX idx_project_members_role ON public.project_members(role);

-- Index cho devices
CREATE INDEX idx_devices_user_id ON public.devices(user_id);
CREATE INDEX idx_devices_type ON public.devices(device_type);

-- Index cho documents (cột mới)
CREATE INDEX idx_documents_project_id ON public.documents(project_id);
CREATE INDEX idx_documents_confidentiality ON public.documents(confidentiality);

-- Index cho audit_logs (cột mới)
CREATE INDEX idx_audit_logs_project_id ON public.audit_logs(project_id);
CREATE INDEX idx_audit_logs_device_id ON public.audit_logs(device_id);
CREATE INDEX idx_audit_logs_delegation_id ON public.audit_logs(delegation_id);

-- =====================================================
-- 4. DỮ LIỆU MẪU CHO KIỂM THỬ
-- =====================================================

-- Thêm dữ liệu mẫu cho projects
INSERT INTO public.projects (id, name, description, organization_id, start_date, end_date, status, created_by) VALUES
(1, 'DMS Development Phase 2', 'Triển khai hệ thống quản lý tài liệu giai đoạn 2', 1, '2025-01-01 00:00:00+07', '2025-12-31 23:59:59+07', 1, 1);

-- Thêm dữ liệu mẫu cho project_members
INSERT INTO public.project_members (id, project_id, user_id, role, status) VALUES
(1, 1, 1, 1, 1), -- Hiệu trưởng là Project Manager
(2, 1, 2, 2, 1); -- Trưởng khoa là Member

-- Thêm dữ liệu mẫu cho devices
INSERT INTO public.devices (id, device_type, user_id, device_name, status) VALUES
('device-001', 1, 1, 'Laptop công ty của Hiệu trưởng', 1),
('device-002', 1, 2, 'Máy tính bàn của Trưởng khoa', 1),
('device-003', 2, 3, 'Điện thoại cá nhân ngoài công ty', 1),
('device-004', 1, 4, 'Máy tính bàn của Chuyên viên', 1),
('device-005', 2, 15, 'Thiết bị của Visitor', 1);

-- Cập nhật current_device_id cho users
UPDATE public.users SET current_device_id = 'device-001', full_name = 'Nguyễn Văn A' WHERE id = 1;
UPDATE public.users SET current_device_id = 'device-002', full_name = 'Trần Thị B' WHERE id = 2;
UPDATE public.users SET current_device_id = 'device-003', full_name = 'Lê Văn C' WHERE id = 3;
UPDATE public.users SET current_device_id = 'device-004', full_name = 'Phạm Thị D' WHERE id = 4;

-- Cập nhật project_id và confidentiality cho document dự án
UPDATE public.documents 
SET project_id = 1, confidentiality = 5, recipients = '[1, 2]'
WHERE title LIKE '%DMS%' OR title LIKE '%dự án%';

-- =====================================================
-- 5. TRIGGER ĐỂ TỰ ĐỘNG CẬP NHẬT TIMESTAMP
-- =====================================================

-- Trigger function để cập nhật updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Áp dụng trigger cho projects
CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON public.projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- KẾT THÚC SCRIPT
-- =====================================================

-- Cập nhật sequence values
SELECT pg_catalog.setval('public.projects_id_seq', 1, true);
SELECT pg_catalog.setval('public.project_members_id_seq', 2, true);

-- Thông báo hoàn thành
-- Script đã hoàn thành việc tạo các bảng và cột mới cho hệ thống ABAC/RBAC
-- Các bảng mới: projects, project_members, devices
-- Các cột mới: documents (project_id, confidentiality, recipients, version_number, signed_at, signed_by)
--              users (current_device_id, full_name)
--              audit_logs (project_id, device_id, delegation_id)
