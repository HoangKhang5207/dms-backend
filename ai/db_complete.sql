--
-- PostgreSQL database dump - COMPLETE VERSION
-- Bao gồm tất cả bảng cũ + bảng mới + cột bổ sung cho DMS Backend
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- =====================================================
-- FUNCTIONS
-- =====================================================

CREATE FUNCTION public.unaccent(text) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $_$
BEGIN
  RETURN translate(
    $1,
    'áàạảãâấầậẩẫăắằặẳẵéèẹẻẽêếềệểễíìịỉĩóòọỏõôốồộổỗơớờợởỡúùụủũưứừựửữýỳỵỷỹđ',
    'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd'
  );
END;
$_$;

CREATE FUNCTION public.update_unaccented_title() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  NEW.title_unaccent = unaccent(NEW.title);
  RETURN NEW;
END;
$$;

SET default_tablespace = '';
SET default_table_access_method = heap;

-- =====================================================
-- TABLES
-- =====================================================

-- Audit Logs
CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    user_id bigint,
    doc_id bigint,
    action character varying(50) NOT NULL,
    "timestamp" timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    details text,
    ip_address character varying(45),
    session_id character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    device_id character varying(255),
    device_type integer,
    resource_type character varying(50),
    resource_id bigint,
    result character varying(20)
);

CREATE SEQUENCE public.audit_logs_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;

-- Categories
CREATE TABLE public.categories (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500),
    parent_category_id bigint,
    created_by character varying(100),
    created_at timestamp without time zone,
    status integer,
    department_id bigint NOT NULL,
    organization_id bigint
);

CREATE SEQUENCE public.categories_id_seq AS integer START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE public.categories_department_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;
ALTER SEQUENCE public.categories_department_id_seq OWNED BY public.categories.department_id;

-- Comments
CREATE TABLE public.comments (
    id bigint NOT NULL,
    doc_id bigint NOT NULL,
    user_id bigint NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    parent_comment_id bigint,
    status integer DEFAULT 1
);

CREATE SEQUENCE public.comments_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.comments_id_seq OWNED BY public.comments.id;

-- Delegations
CREATE TABLE public.delegations (
    id bigint NOT NULL,
    delegator_id bigint,
    delegatee_id bigint,
    doc_id integer,
    permission character varying(50),
    expiry_date timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    expiry_at timestamp(6) with time zone,
    document_id bigint NOT NULL
);

CREATE SEQUENCE public.delegations_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.delegations_id_seq OWNED BY public.delegations.id;

-- Departments
CREATE TABLE public.departments (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500),
    created_by character varying(100),
    created_at timestamp without time zone,
    status integer
);

CREATE SEQUENCE public.departments_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.departments_id_seq OWNED BY public.departments.id;

-- Document Permissions
CREATE TABLE public.document_permissions (
    id bigint NOT NULL,
    user_id bigint,
    doc_id bigint,
    permission character varying(255),
    version_number integer,
    expiry_date timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.document_permissions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.document_permissions_id_seq OWNED BY public.document_permissions.id;

-- Documents (với các cột mới)
CREATE TABLE public.documents (
    id bigint NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    category_id bigint,
    status integer NOT NULL,
    created_by character varying(255) NOT NULL,
    type character varying(30) NOT NULL,
    total_page integer,
    description character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    file_path character varying(255),
    file_id character varying(255),
    storage_capacity bigint,
    storage_unit character varying(255),
    access_type integer,
    organization_id bigint,
    dept_id bigint,
    title_unaccent text,
    password character varying(255),
    photo_id text,
    project_id bigint,
    confidentiality integer DEFAULT 2,
    recipients text,
    version_number integer DEFAULT 1,
    signed_at timestamp with time zone,
    signed_by bigint
);

CREATE SEQUENCE public.documents_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.documents_id_seq OWNED BY public.documents.id;

-- Notifications
CREATE TABLE public.notifications (
    id bigint NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    type character varying(50) NOT NULL,
    doc_id bigint,
    created_by character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status integer DEFAULT 1
);

CREATE SEQUENCE public.notifications_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;

-- Organizations
CREATE TABLE public.organizations (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500),
    created_by character varying(100),
    created_at timestamp without time zone,
    status integer
);

CREATE SEQUENCE public.organizations_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.organizations_id_seq OWNED BY public.organizations.id;

-- Permissions
CREATE TABLE public.permissions (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(255),
    created_by character varying(100) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.permissions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.permissions_id_seq OWNED BY public.permissions.id;

-- Positions
CREATE TABLE public.positions (
    id bigint NOT NULL,
    name character varying(50)
);

CREATE SEQUENCE public.positions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.positions_id_seq OWNED BY public.positions.id;

-- Private Docs
CREATE TABLE public.private_docs (
    id bigint NOT NULL,
    doc_id bigint NOT NULL,
    user_id bigint NOT NULL,
    status integer DEFAULT 1 NOT NULL
);

CREATE SEQUENCE public.private_docs_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.private_docs_id_seq OWNED BY public.private_docs.id;

-- Products
CREATE TABLE public.products (
    id bigint NOT NULL,
    name character varying(255),
    description character varying(255),
    status integer DEFAULT 1 NOT NULL,
    type character varying(50) NOT NULL,
    price bigint,
    quantity bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.products_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;

-- Projects (BẢNG MỚI)
CREATE TABLE public.projects (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    organization_id bigint NOT NULL,
    start_date timestamp with time zone,
    end_date timestamp with time zone,
    status integer DEFAULT 1,
    created_by character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE public.projects_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.projects_id_seq OWNED BY public.projects.id;

-- Project Members (BẢNG MỚI)
CREATE TABLE public.project_members (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role integer DEFAULT 3,
    joined_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status integer DEFAULT 1
);

CREATE SEQUENCE public.project_members_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.project_members_id_seq OWNED BY public.project_members.id;

-- Devices (BẢNG MỚI)
CREATE TABLE public.devices (
    id bigint NOT NULL,
    device_type integer NOT NULL,
    user_id bigint NOT NULL,
    device_name character varying(255),
    registered_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status integer DEFAULT 1
);

CREATE SEQUENCE public.devices_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.devices_id_seq OWNED BY public.devices.id;

-- Refresh Tokens
CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    refresh_token character varying NOT NULL
);

CREATE SEQUENCE public.refresh_tokens_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;

-- Roles
CREATE TABLE public.roles (
    id bigint NOT NULL,
    organization_id bigint,
    name character varying(50) NOT NULL,
    description character varying(255),
    is_inheritable boolean,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.roles_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;

-- Role Permissions
CREATE TABLE public.role_permissions (
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL
);

CREATE SEQUENCE public.role_permissions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.role_permissions_id_seq OWNED BY public.role_permissions.role_id;

-- Search Histories
CREATE TABLE public.search_histories (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    keywords character varying(255),
    type integer,
    updated_at timestamp without time zone
);

CREATE SEQUENCE public.search_histories_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.search_histories_id_seq OWNED BY public.search_histories.id;

-- Starred Docs
CREATE TABLE public.starred_docs (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    doc_id bigint NOT NULL,
    updated_at timestamp without time zone,
    status integer
);

CREATE SEQUENCE public.starred_docs_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.starred_docs_id_seq OWNED BY public.starred_docs.id;

-- Transactions
CREATE TABLE public.transactions (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    product_id bigint,
    amount bigint DEFAULT 0 NOT NULL,
    status integer DEFAULT 0 NOT NULL,
    bank_response_code character varying(50),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    ref_id character varying(50)
);

CREATE SEQUENCE public.transactions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.transactions_id_seq OWNED BY public.transactions.id;

-- User Documents
CREATE TABLE public.user_documents (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    document_id bigint NOT NULL,
    type integer,
    status integer DEFAULT 1 NOT NULL,
    decentralized_by character varying(255),
    updated_at timestamp with time zone,
    viewed_at timestamp with time zone,
    created_at timestamp without time zone,
    move_to_trash_at timestamp without time zone
);

CREATE SEQUENCE public.user_documents_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_documents_id_seq OWNED BY public.user_documents.id;

-- User Notifications
CREATE TABLE public.user_notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    noti_id bigint NOT NULL,
    status integer
);

CREATE SEQUENCE public.user_notifications_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_notifications_id_seq OWNED BY public.user_notifications.id;

-- User Permissions
CREATE TABLE public.user_permissions (
    user_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    action character varying(50)
);

CREATE SEQUENCE public.user_permissions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_permissions_id_seq OWNED BY public.user_permissions.user_id;

-- User Roles
CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL
);

CREATE SEQUENCE public.user_roles_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_roles_id_seq OWNED BY public.user_roles.user_id;

-- Users (với các cột mới)
CREATE TABLE public.users (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    document_id bigint NOT NULL,
    type integer,
    status integer DEFAULT 1 NOT NULL,
    decentralized_by character varying(255),
    updated_at timestamp with time zone,
    viewed_at timestamp with time zone,
    created_at timestamp without time zone,
    move_to_trash_at timestamp without time zone
);

CREATE SEQUENCE public.user_documents_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_documents_id_seq OWNED BY public.user_documents.id;

-- User Notifications
CREATE TABLE public.user_notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    noti_id bigint NOT NULL,
    status integer
);

CREATE SEQUENCE public.user_notifications_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_notifications_id_seq OWNED BY public.user_notifications.id;

-- User Permissions
CREATE TABLE public.user_permissions (
    user_id bigint NOT NULL,
    permission_id bigint NOT NULL
);

CREATE SEQUENCE public.user_permissions_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_permissions_id_seq OWNED BY public.user_permissions.user_id;

-- User Roles
CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL
);

CREATE SEQUENCE public.user_roles_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.user_roles_id_seq OWNED BY public.user_roles.user_id;

-- Users (với các cột mới)
CREATE TABLE public.users (
    id bigint NOT NULL,
    first_name character varying(30) NOT NULL,
    last_name character varying(30) NOT NULL,
    user_name character varying(50),
    email character varying(30) NOT NULL,
    password character varying(255),
    gender boolean DEFAULT false,
    status integer DEFAULT 1,
    is_admin boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    is_organization_manager boolean DEFAULT false,
    organization_id bigint,
    is_social boolean DEFAULT false,
    dept_id bigint,
    is_dept_manager boolean DEFAULT false,
    position_id bigint,
    last_login_at timestamp without time zone,
    current_device_id character varying(255),
    full_name character varying(255)
);

CREATE SEQUENCE public.users_id_seq AS integer START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;

-- =====================================================
-- DEFAULT VALUES
-- =====================================================

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);
ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);
ALTER TABLE ONLY public.categories ALTER COLUMN department_id SET DEFAULT nextval('public.categories_department_id_seq'::regclass);
ALTER TABLE ONLY public.comments ALTER COLUMN id SET DEFAULT nextval('public.comments_id_seq'::regclass);
ALTER TABLE ONLY public.delegations ALTER COLUMN id SET DEFAULT nextval('public.delegations_id_seq'::regclass);
ALTER TABLE ONLY public.departments ALTER COLUMN id SET DEFAULT nextval('public.departments_id_seq'::regclass);
ALTER TABLE ONLY public.document_permissions ALTER COLUMN id SET DEFAULT nextval('public.document_permissions_id_seq'::regclass);
ALTER TABLE ONLY public.documents ALTER COLUMN id SET DEFAULT nextval('public.documents_id_seq'::regclass);
ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);
ALTER TABLE ONLY public.organizations ALTER COLUMN id SET DEFAULT nextval('public.organizations_id_seq'::regclass);
ALTER TABLE ONLY public.permissions ALTER COLUMN id SET DEFAULT nextval('public.permissions_id_seq'::regclass);
ALTER TABLE ONLY public.positions ALTER COLUMN id SET DEFAULT nextval('public.positions_id_seq'::regclass);
ALTER TABLE ONLY public.private_docs ALTER COLUMN id SET DEFAULT nextval('public.private_docs_id_seq'::regclass);
ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);
ALTER TABLE ONLY public.projects ALTER COLUMN id SET DEFAULT nextval('public.projects_id_seq'::regclass);
ALTER TABLE ONLY public.project_members ALTER COLUMN id SET DEFAULT nextval('public.project_members_id_seq'::regclass);
ALTER TABLE ONLY public.devices ALTER COLUMN id SET DEFAULT nextval('public.devices_id_seq'::regclass);
ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);
ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);
ALTER TABLE ONLY public.search_histories ALTER COLUMN id SET DEFAULT nextval('public.search_histories_id_seq'::regclass);
ALTER TABLE ONLY public.starred_docs ALTER COLUMN id SET DEFAULT nextval('public.starred_docs_id_seq'::regclass);
ALTER TABLE ONLY public.transactions ALTER COLUMN id SET DEFAULT nextval('public.transactions_id_seq'::regclass);
ALTER TABLE ONLY public.user_documents ALTER COLUMN id SET DEFAULT nextval('public.user_documents_id_seq'::regclass);
ALTER TABLE ONLY public.user_notifications ALTER COLUMN id SET DEFAULT nextval('public.user_notifications_id_seq'::regclass);
ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);

-- =====================================================
-- PRIMARY KEYS
-- =====================================================

ALTER TABLE ONLY public.audit_logs ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.categories ADD CONSTRAINT categories_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.comments ADD CONSTRAINT comments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.delegations ADD CONSTRAINT delegations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.departments ADD CONSTRAINT departments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.document_permissions ADD CONSTRAINT document_permissions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.documents ADD CONSTRAINT documents_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.notifications ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.organizations ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.permissions ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.positions ADD CONSTRAINT positions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.private_docs ADD CONSTRAINT private_docs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.products ADD CONSTRAINT products_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.projects ADD CONSTRAINT projects_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.project_members ADD CONSTRAINT project_members_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.devices ADD CONSTRAINT devices_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.refresh_tokens ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.roles ADD CONSTRAINT roles_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.role_permissions ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);
ALTER TABLE ONLY public.search_histories ADD CONSTRAINT search_histories_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.starred_docs ADD CONSTRAINT starred_docs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.transactions ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.user_documents ADD CONSTRAINT user_documents_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.user_notifications ADD CONSTRAINT user_notifications_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.user_permissions ADD CONSTRAINT user_permissions_pkey PRIMARY KEY (user_id, permission_id);
ALTER TABLE ONLY public.user_roles ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);
ALTER TABLE ONLY public.users ADD CONSTRAINT users_pkey PRIMARY KEY (id);

-- =====================================================
-- FOREIGN KEYS
-- =====================================================

-- Existing foreign keys
ALTER TABLE ONLY public.categories ADD CONSTRAINT fk_categories_categories FOREIGN KEY (parent_category_id) REFERENCES public.categories(id);
ALTER TABLE ONLY public.categories ADD CONSTRAINT fk_categories_departments FOREIGN KEY (department_id) REFERENCES public.departments(id);
ALTER TABLE ONLY public.comments ADD CONSTRAINT fk_comments_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.comments ADD CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.delegations ADD CONSTRAINT fk_delegations_documents FOREIGN KEY (document_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.departments ADD CONSTRAINT fk_departments_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
ALTER TABLE ONLY public.document_permissions ADD CONSTRAINT fk_document_permissions_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.document_permissions ADD CONSTRAINT fk_document_permissions_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.documents ADD CONSTRAINT fk_documents_categories FOREIGN KEY (category_id) REFERENCES public.categories(id);
ALTER TABLE ONLY public.documents ADD CONSTRAINT fk_documents_departments FOREIGN KEY (dept_id) REFERENCES public.departments(id);
ALTER TABLE ONLY public.documents ADD CONSTRAINT fk_documents_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
ALTER TABLE ONLY public.notifications ADD CONSTRAINT fk_notifications_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.private_docs ADD CONSTRAINT fk_private_docs_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.private_docs ADD CONSTRAINT fk_private_docs_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.refresh_tokens ADD CONSTRAINT fk_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.role_permissions ADD CONSTRAINT fk_role_permissions_permissions FOREIGN KEY (permission_id) REFERENCES public.permissions(id);
ALTER TABLE ONLY public.role_permissions ADD CONSTRAINT fk_role_permissions_roles FOREIGN KEY (role_id) REFERENCES public.roles(id);
ALTER TABLE ONLY public.search_histories ADD CONSTRAINT fk_search_histories_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.transactions ADD CONSTRAINT fk_transactions_products FOREIGN KEY (product_id) REFERENCES public.products(id);
ALTER TABLE ONLY public.transactions ADD CONSTRAINT fk_transactions_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.user_documents ADD CONSTRAINT fk_user_documents_documents FOREIGN KEY (document_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.user_documents ADD CONSTRAINT fk_user_documents_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.user_notifications ADD CONSTRAINT fk_user_notifications_notifications FOREIGN KEY (noti_id) REFERENCES public.notifications(id);
ALTER TABLE ONLY public.user_notifications ADD CONSTRAINT fk_user_notifications_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.user_permissions ADD CONSTRAINT fk_user_permissions_permissions FOREIGN KEY (permission_id) REFERENCES public.permissions(id);
ALTER TABLE ONLY public.user_permissions ADD CONSTRAINT fk_user_permissions_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.user_roles ADD CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES public.roles(id);
ALTER TABLE ONLY public.user_roles ADD CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.users ADD CONSTRAINT fk_users_departments FOREIGN KEY (dept_id) REFERENCES public.departments(id);
ALTER TABLE ONLY public.users ADD CONSTRAINT fk_users_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
ALTER TABLE ONLY public.users ADD CONSTRAINT fk_users_positions FOREIGN KEY (position_id) REFERENCES public.positions(id);

-- New foreign keys for new tables and columns
ALTER TABLE ONLY public.projects ADD CONSTRAINT fk_projects_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
ALTER TABLE ONLY public.project_members ADD CONSTRAINT fk_project_members_projects FOREIGN KEY (project_id) REFERENCES public.projects(id);
ALTER TABLE ONLY public.project_members ADD CONSTRAINT fk_project_members_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.devices ADD CONSTRAINT fk_devices_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.documents ADD CONSTRAINT fk_documents_projects FOREIGN KEY (project_id) REFERENCES public.projects(id);
ALTER TABLE ONLY public.documents ADD CONSTRAINT fk_documents_signed_by FOREIGN KEY (signed_by) REFERENCES public.users(id);
ALTER TABLE ONLY public.roles ADD CONSTRAINT fk_roles_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);
ALTER TABLE ONLY public.search_histories ADD CONSTRAINT fk_search_histories_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.starred_docs ADD CONSTRAINT fk_starred_docs_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.starred_docs ADD CONSTRAINT fk_starred_docs_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);
ALTER TABLE ONLY public.transactions ADD CONSTRAINT fk_transactions_users FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.transactions ADD CONSTRAINT fk_transactions_products FOREIGN KEY (product_id) REFERENCES public.products(id);

-- =====================================================
-- INDEXES
-- =====================================================

-- Existing indexes
CREATE INDEX idx_documents_title_unaccent ON public.documents USING gin (to_tsvector('english'::regconfig, title_unaccent));
CREATE TRIGGER update_title_unaccent BEFORE INSERT OR UPDATE ON public.documents FOR EACH ROW EXECUTE FUNCTION public.update_unaccented_title();

-- New indexes for performance
CREATE INDEX idx_projects_organization_id ON public.projects(organization_id);
CREATE INDEX idx_projects_status ON public.projects(status);
CREATE INDEX idx_project_members_project_id ON public.project_members(project_id);
CREATE INDEX idx_project_members_user_id ON public.project_members(user_id);
CREATE UNIQUE INDEX idx_project_members_unique ON public.project_members(project_id, user_id);
CREATE INDEX idx_devices_user_id ON public.devices(user_id);
CREATE INDEX idx_devices_type ON public.devices(device_type);
CREATE INDEX idx_documents_project_id ON public.documents(project_id);
CREATE INDEX idx_documents_confidentiality ON public.documents(confidentiality);
CREATE INDEX idx_users_current_device_id ON public.users(current_device_id);
CREATE INDEX idx_audit_logs_device_id ON public.audit_logs(device_id);
CREATE INDEX idx_audit_logs_resource ON public.audit_logs(resource_type, resource_id);

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON COLUMN public.documents.access_type IS '1 - Ngoại bộ, 2 - Công khai, 3 - Nội bộ, 4 - Riêng tư';
COMMENT ON COLUMN public.documents.confidentiality IS '1 - PUBLIC, 2 - INTERNAL, 3 - PRIVATE, 4 - LOCKED, 5 - PROJECT';
COMMENT ON COLUMN public.documents.recipients IS 'JSON array chứa danh sách user IDs được phân phối tài liệu';
COMMENT ON COLUMN public.users.status IS '1 - hoạt động, 2 - không hoạt động';
COMMENT ON COLUMN public.users.current_device_id IS 'ID của thiết bị hiện tại đang sử dụng';
COMMENT ON COLUMN public.users.full_name IS 'Họ và tên đầy đủ của user';
COMMENT ON COLUMN public.private_docs.status IS '1 - được chia sẻ, 2 - đã xóa';
COMMENT ON COLUMN public.transactions.status IS '0 - đã tạo, 1 - thành công, 2 - thất bại';
COMMENT ON COLUMN public.transactions.ref_id IS 'Mã giao dịch của ngân hàng';
COMMENT ON COLUMN public.projects.status IS '1 - ACTIVE, 2 - COMPLETED, 3 - SUSPENDED';
COMMENT ON COLUMN public.project_members.role IS '1 - PROJECT_MANAGER, 2 - MEMBER, 3 - VIEWER';
COMMENT ON COLUMN public.project_members.status IS '1 - ACTIVE, 2 - INACTIVE';
COMMENT ON COLUMN public.devices.device_type IS '1 - COMPANY_DEVICE, 2 - EXTERNAL_DEVICE';
COMMENT ON COLUMN public.devices.status IS '1 - ACTIVE, 2 - INACTIVE';
COMMENT ON COLUMN public.audit_logs.device_id IS 'ID của thiết bị thực hiện hành động';
COMMENT ON COLUMN public.audit_logs.device_type IS '1 - COMPANY_DEVICE, 2 - EXTERNAL_DEVICE';
COMMENT ON COLUMN public.audit_logs.resource_type IS 'Loại tài nguyên (document, user, project, etc.)';
COMMENT ON COLUMN public.audit_logs.resource_id IS 'ID của tài nguyên được tác động';
COMMENT ON COLUMN public.audit_logs.result IS 'Kết quả hành động (SUCCESS, DENIED, ERROR)';
COMMENT ON COLUMN public.starred_docs.status IS '1-đánh dấu, 2-bỏ đánh dấu';
COMMENT ON COLUMN public.transactions.status IS '0 - đã tạo, 1 - thành công, 2 - thất bại';
COMMENT ON COLUMN public.transactions.ref_id IS 'Mã giao dịch của ngân hàng';
