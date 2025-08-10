--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5
-- Dumped by pg_dump version 17.5

-- Started on 2025-08-06 22:33:02

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

--
-- TOC entry 270 (class 1255 OID 23514)
-- Name: unaccent(text); Type: FUNCTION; Schema: public; Owner: postgres
--

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


ALTER FUNCTION public.unaccent(text) OWNER TO postgres;

--
-- TOC entry 271 (class 1255 OID 23515)
-- Name: update_unaccented_title(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_unaccented_title() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  NEW.title_unaccent = unaccent(NEW.title);
  RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_unaccented_title() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 217 (class 1259 OID 23516)
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    user_id bigint,
    doc_id bigint,
    action character varying(50) NOT NULL,
    "timestamp" timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    details text,
    ip_address character varying(45),
    session_id character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.audit_logs OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 23523)
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.audit_logs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.audit_logs_id_seq OWNER TO postgres;

--
-- TOC entry 5197 (class 0 OID 0)
-- Dependencies: 218
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- TOC entry 219 (class 1259 OID 23524)
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text NOT NULL,
    parent_category_id bigint,
    organization_id bigint,
    created_by character varying(30) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status integer DEFAULT 1,
    department_id bigint NOT NULL
);


ALTER TABLE public.categories OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 23533)
-- Name: categories_department_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.categories_department_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categories_department_id_seq OWNER TO postgres;

--
-- TOC entry 5198 (class 0 OID 0)
-- Dependencies: 221
-- Name: categories_department_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.categories_department_id_seq OWNED BY public.categories.department_id;


--
-- TOC entry 220 (class 1259 OID 23532)
-- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.categories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categories_id_seq OWNER TO postgres;

--
-- TOC entry 5199 (class 0 OID 0)
-- Dependencies: 220
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;


--
-- TOC entry 222 (class 1259 OID 23534)
-- Name: comments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.comments (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    content text,
    parent_comment_id bigint,
    status integer DEFAULT 1 NOT NULL,
    doc_id bigint,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


ALTER TABLE public.comments OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 23540)
-- Name: comments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.comments_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comments_id_seq OWNER TO postgres;

--
-- TOC entry 5200 (class 0 OID 0)
-- Dependencies: 223
-- Name: comments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.comments_id_seq OWNED BY public.comments.id;


--
-- TOC entry 224 (class 1259 OID 23541)
-- Name: delegations; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.delegations OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 23545)
-- Name: delegations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.delegations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.delegations_id_seq OWNER TO postgres;

--
-- TOC entry 5201 (class 0 OID 0)
-- Dependencies: 225
-- Name: delegations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.delegations_id_seq OWNED BY public.delegations.id;


--
-- TOC entry 226 (class 1259 OID 23546)
-- Name: departments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.departments (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500),
    created_by character varying(100),
    created_at timestamp without time zone,
    status integer
);


ALTER TABLE public.departments OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 23551)
-- Name: departments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.departments_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.departments_id_seq OWNER TO postgres;

--
-- TOC entry 5202 (class 0 OID 0)
-- Dependencies: 227
-- Name: departments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.departments_id_seq OWNED BY public.departments.id;


--
-- TOC entry 269 (class 1259 OID 24791)
-- Name: document_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.document_permissions (
    id bigint NOT NULL,
    user_id bigint,
    doc_id bigint,
    permission character varying(255),
    version_number integer,
    expiry_date timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.document_permissions OWNER TO postgres;

--
-- TOC entry 268 (class 1259 OID 24790)
-- Name: document_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.document_permissions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.document_permissions_id_seq OWNER TO postgres;

--
-- TOC entry 5203 (class 0 OID 0)
-- Dependencies: 268
-- Name: document_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.document_permissions_id_seq OWNED BY public.document_permissions.id;


--
-- TOC entry 228 (class 1259 OID 23552)
-- Name: documents; Type: TABLE; Schema: public; Owner: postgres
--

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
    photo_id text
);


ALTER TABLE public.documents OWNER TO postgres;

--
-- TOC entry 5204 (class 0 OID 0)
-- Dependencies: 228
-- Name: COLUMN documents.access_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.documents.access_type IS '1 - Ngoại bộ, 2 - Công khai, 3 - Nội bộ, 4 - Riêng tư';


--
-- TOC entry 229 (class 1259 OID 23559)
-- Name: documents_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.documents_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.documents_id_seq OWNER TO postgres;

--
-- TOC entry 5205 (class 0 OID 0)
-- Dependencies: 229
-- Name: documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.documents_id_seq OWNED BY public.documents.id;


--
-- TOC entry 230 (class 1259 OID 23560)
-- Name: file_uploads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file_uploads (
    id bigint NOT NULL,
    document_id bigint NOT NULL,
    file_path character varying(255) NOT NULL,
    user_id bigint
);


ALTER TABLE public.file_uploads OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 23563)
-- Name: file_uploads_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.file_uploads_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.file_uploads_id_seq OWNER TO postgres;

--
-- TOC entry 5206 (class 0 OID 0)
-- Dependencies: 231
-- Name: file_uploads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.file_uploads_id_seq OWNED BY public.file_uploads.id;


--
-- TOC entry 232 (class 1259 OID 23564)
-- Name: logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.logs (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    action character varying(30),
    request_data character varying(255),
    created_at timestamp without time zone
);


ALTER TABLE public.logs OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 23569)
-- Name: logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.logs_id_seq OWNER TO postgres;

--
-- TOC entry 5207 (class 0 OID 0)
-- Dependencies: 233
-- Name: logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.logs_id_seq OWNED BY public.logs.id;


--
-- TOC entry 234 (class 1259 OID 23570)
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    title character varying(255),
    content text,
    created_at timestamp without time zone,
    doc_id bigint
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 23575)
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notifications_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notifications_id_seq OWNER TO postgres;

--
-- TOC entry 5208 (class 0 OID 0)
-- Dependencies: 235
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- TOC entry 236 (class 1259 OID 23576)
-- Name: organizations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.organizations (
    id bigint NOT NULL,
    name character varying(255),
    description character varying(255),
    status integer DEFAULT 1 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by character varying(50),
    limit_data bigint,
    data_used bigint,
    is_openai boolean,
    parent_organization_id bigint,
    limit_token bigint,
    token_used bigint
);


ALTER TABLE public.organizations OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 23584)
-- Name: organizations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.organizations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.organizations_id_seq OWNER TO postgres;

--
-- TOC entry 5209 (class 0 OID 0)
-- Dependencies: 237
-- Name: organizations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.organizations_id_seq OWNED BY public.organizations.id;


--
-- TOC entry 238 (class 1259 OID 23585)
-- Name: permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.permissions (
    id bigint NOT NULL,
    name character varying(100),
    description character varying(255),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


ALTER TABLE public.permissions OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 23588)
-- Name: permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.permissions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.permissions_id_seq OWNER TO postgres;

--
-- TOC entry 5210 (class 0 OID 0)
-- Dependencies: 239
-- Name: permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.permissions_id_seq OWNED BY public.permissions.id;


--
-- TOC entry 240 (class 1259 OID 23589)
-- Name: positions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.positions (
    id bigint NOT NULL,
    name character varying(50)
);


ALTER TABLE public.positions OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 23592)
-- Name: positions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.positions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.positions_id_seq OWNER TO postgres;

--
-- TOC entry 5211 (class 0 OID 0)
-- Dependencies: 241
-- Name: positions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.positions_id_seq OWNED BY public.positions.id;


--
-- TOC entry 242 (class 1259 OID 23593)
-- Name: private_docs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.private_docs (
    id bigint NOT NULL,
    doc_id bigint NOT NULL,
    user_id bigint NOT NULL,
    status integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.private_docs OWNER TO postgres;

--
-- TOC entry 5212 (class 0 OID 0)
-- Dependencies: 242
-- Name: COLUMN private_docs.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.private_docs.status IS '1 - được chia sẻ, 2 - đã xóa';


--
-- TOC entry 243 (class 1259 OID 23597)
-- Name: private_docs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.private_docs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.private_docs_id_seq OWNER TO postgres;

--
-- TOC entry 5213 (class 0 OID 0)
-- Dependencies: 243
-- Name: private_docs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.private_docs_id_seq OWNED BY public.private_docs.id;


--
-- TOC entry 244 (class 1259 OID 23598)
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.products OWNER TO postgres;

--
-- TOC entry 245 (class 1259 OID 23606)
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.products_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.products_id_seq OWNER TO postgres;

--
-- TOC entry 5214 (class 0 OID 0)
-- Dependencies: 245
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- TOC entry 246 (class 1259 OID 23607)
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    refresh_token character varying NOT NULL
);


ALTER TABLE public.refresh_tokens OWNER TO postgres;

--
-- TOC entry 247 (class 1259 OID 23612)
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.refresh_tokens_id_seq OWNER TO postgres;

--
-- TOC entry 5215 (class 0 OID 0)
-- Dependencies: 247
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- TOC entry 248 (class 1259 OID 23613)
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role_permissions (
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL
);


ALTER TABLE public.role_permissions OWNER TO postgres;

--
-- TOC entry 249 (class 1259 OID 23616)
-- Name: role_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.role_permissions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.role_permissions_id_seq OWNER TO postgres;

--
-- TOC entry 5216 (class 0 OID 0)
-- Dependencies: 249
-- Name: role_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.role_permissions_id_seq OWNED BY public.role_permissions.role_id;


--
-- TOC entry 250 (class 1259 OID 23617)
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    organization_id bigint,
    name character varying(50) NOT NULL,
    description character varying(255),
    is_inheritable boolean,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- TOC entry 251 (class 1259 OID 23620)
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.roles_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.roles_id_seq OWNER TO postgres;

--
-- TOC entry 5217 (class 0 OID 0)
-- Dependencies: 251
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- TOC entry 252 (class 1259 OID 23621)
-- Name: search_histories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.search_histories (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    keywords character varying(255),
    type integer,
    updated_at timestamp without time zone
);


ALTER TABLE public.search_histories OWNER TO postgres;

--
-- TOC entry 253 (class 1259 OID 23625)
-- Name: search_histories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.search_histories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.search_histories_id_seq OWNER TO postgres;

--
-- TOC entry 5218 (class 0 OID 0)
-- Dependencies: 253
-- Name: search_histories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.search_histories_id_seq OWNED BY public.search_histories.id;


--
-- TOC entry 254 (class 1259 OID 23626)
-- Name: starred_docs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.starred_docs (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    doc_id bigint NOT NULL,
    updated_at timestamp without time zone,
    status integer
);


ALTER TABLE public.starred_docs OWNER TO postgres;

--
-- TOC entry 5219 (class 0 OID 0)
-- Dependencies: 254
-- Name: COLUMN starred_docs.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.starred_docs.status IS '1-đánh dấu, 2-bỏ đánh dấu';


--
-- TOC entry 255 (class 1259 OID 23629)
-- Name: starred_docs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.starred_docs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.starred_docs_id_seq OWNER TO postgres;

--
-- TOC entry 5220 (class 0 OID 0)
-- Dependencies: 255
-- Name: starred_docs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.starred_docs_id_seq OWNED BY public.starred_docs.id;


--
-- TOC entry 256 (class 1259 OID 23630)
-- Name: transactions; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.transactions OWNER TO postgres;

--
-- TOC entry 5221 (class 0 OID 0)
-- Dependencies: 256
-- Name: COLUMN transactions.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.transactions.status IS '0 - đã tạo, 1 - thành công, 2 - thất bại';


--
-- TOC entry 5222 (class 0 OID 0)
-- Dependencies: 256
-- Name: COLUMN transactions.ref_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.transactions.ref_id IS 'Mã giao dịch của ngân hàng';


--
-- TOC entry 257 (class 1259 OID 23636)
-- Name: transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.transactions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transactions_id_seq OWNER TO postgres;

--
-- TOC entry 5223 (class 0 OID 0)
-- Dependencies: 257
-- Name: transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.transactions_id_seq OWNED BY public.transactions.id;


--
-- TOC entry 258 (class 1259 OID 23637)
-- Name: user_documents; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.user_documents OWNER TO postgres;

--
-- TOC entry 259 (class 1259 OID 23641)
-- Name: user_documents_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_documents_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_documents_id_seq OWNER TO postgres;

--
-- TOC entry 5224 (class 0 OID 0)
-- Dependencies: 259
-- Name: user_documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_documents_id_seq OWNED BY public.user_documents.id;


--
-- TOC entry 260 (class 1259 OID 23642)
-- Name: user_notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    noti_id bigint NOT NULL,
    status integer
);


ALTER TABLE public.user_notifications OWNER TO postgres;

--
-- TOC entry 261 (class 1259 OID 23645)
-- Name: user_notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_notifications_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_notifications_id_seq OWNER TO postgres;

--
-- TOC entry 5225 (class 0 OID 0)
-- Dependencies: 261
-- Name: user_notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_notifications_id_seq OWNED BY public.user_notifications.id;


--
-- TOC entry 262 (class 1259 OID 23646)
-- Name: user_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_permissions (
    user_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    action character varying(50)
);


ALTER TABLE public.user_permissions OWNER TO postgres;

--
-- TOC entry 263 (class 1259 OID 23649)
-- Name: user_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_permissions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_permissions_id_seq OWNER TO postgres;

--
-- TOC entry 5226 (class 0 OID 0)
-- Dependencies: 263
-- Name: user_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_permissions_id_seq OWNED BY public.user_permissions.user_id;


--
-- TOC entry 264 (class 1259 OID 23650)
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- TOC entry 265 (class 1259 OID 23653)
-- Name: user_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_roles_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_roles_id_seq OWNER TO postgres;

--
-- TOC entry 5227 (class 0 OID 0)
-- Dependencies: 265
-- Name: user_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_roles_id_seq OWNED BY public.user_roles.user_id;


--
-- TOC entry 266 (class 1259 OID 23654)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

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
    last_login_at timestamp without time zone
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 5228 (class 0 OID 0)
-- Dependencies: 266
-- Name: COLUMN users.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.status IS '1 - hoạt động, 2 - không hoạt động';


--
-- TOC entry 267 (class 1259 OID 23667)
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- TOC entry 5229 (class 0 OID 0)
-- Dependencies: 267
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- TOC entry 4823 (class 2604 OID 23952)
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- TOC entry 4826 (class 2604 OID 23983)
-- Name: categories id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);


--
-- TOC entry 4830 (class 2604 OID 24002)
-- Name: categories department_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories ALTER COLUMN department_id SET DEFAULT nextval('public.categories_department_id_seq'::regclass);


--
-- TOC entry 4831 (class 2604 OID 24036)
-- Name: comments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments ALTER COLUMN id SET DEFAULT nextval('public.comments_id_seq'::regclass);


--
-- TOC entry 4833 (class 2604 OID 24083)
-- Name: delegations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegations ALTER COLUMN id SET DEFAULT nextval('public.delegations_id_seq'::regclass);


--
-- TOC entry 4835 (class 2604 OID 24108)
-- Name: departments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments ALTER COLUMN id SET DEFAULT nextval('public.departments_id_seq'::regclass);


--
-- TOC entry 4878 (class 2604 OID 24808)
-- Name: document_permissions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_permissions ALTER COLUMN id SET DEFAULT nextval('public.document_permissions_id_seq'::regclass);


--
-- TOC entry 4836 (class 2604 OID 24138)
-- Name: documents id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents ALTER COLUMN id SET DEFAULT nextval('public.documents_id_seq'::regclass);


--
-- TOC entry 4839 (class 2604 OID 24231)
-- Name: file_uploads id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_uploads ALTER COLUMN id SET DEFAULT nextval('public.file_uploads_id_seq'::regclass);


--
-- TOC entry 4840 (class 2604 OID 23676)
-- Name: logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logs ALTER COLUMN id SET DEFAULT nextval('public.logs_id_seq'::regclass);


--
-- TOC entry 4841 (class 2604 OID 24269)
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- TOC entry 4842 (class 2604 OID 24294)
-- Name: organizations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.organizations ALTER COLUMN id SET DEFAULT nextval('public.organizations_id_seq'::regclass);


--
-- TOC entry 4846 (class 2604 OID 24339)
-- Name: permissions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions ALTER COLUMN id SET DEFAULT nextval('public.permissions_id_seq'::regclass);


--
-- TOC entry 4847 (class 2604 OID 24356)
-- Name: positions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.positions ALTER COLUMN id SET DEFAULT nextval('public.positions_id_seq'::regclass);


--
-- TOC entry 4848 (class 2604 OID 24368)
-- Name: private_docs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_docs ALTER COLUMN id SET DEFAULT nextval('public.private_docs_id_seq'::regclass);


--
-- TOC entry 4850 (class 2604 OID 24400)
-- Name: products id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- TOC entry 4854 (class 2604 OID 24414)
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- TOC entry 4855 (class 2604 OID 24438)
-- Name: role_permissions role_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions ALTER COLUMN role_id SET DEFAULT nextval('public.role_permissions_id_seq'::regclass);


--
-- TOC entry 4856 (class 2604 OID 24461)
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- TOC entry 4857 (class 2604 OID 24487)
-- Name: search_histories id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.search_histories ALTER COLUMN id SET DEFAULT nextval('public.search_histories_id_seq'::regclass);


--
-- TOC entry 4859 (class 2604 OID 24503)
-- Name: starred_docs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.starred_docs ALTER COLUMN id SET DEFAULT nextval('public.starred_docs_id_seq'::regclass);


--
-- TOC entry 4860 (class 2604 OID 24528)
-- Name: transactions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions ALTER COLUMN id SET DEFAULT nextval('public.transactions_id_seq'::regclass);


--
-- TOC entry 4864 (class 2604 OID 24553)
-- Name: user_documents id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_documents ALTER COLUMN id SET DEFAULT nextval('public.user_documents_id_seq'::regclass);


--
-- TOC entry 4866 (class 2604 OID 24582)
-- Name: user_notifications id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_notifications ALTER COLUMN id SET DEFAULT nextval('public.user_notifications_id_seq'::regclass);


--
-- TOC entry 4867 (class 2604 OID 24618)
-- Name: user_permissions user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_permissions ALTER COLUMN user_id SET DEFAULT nextval('public.user_permissions_id_seq'::regclass);


--
-- TOC entry 4868 (class 2604 OID 24630)
-- Name: user_roles user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles ALTER COLUMN user_id SET DEFAULT nextval('public.user_roles_id_seq'::regclass);


--
-- TOC entry 4869 (class 2604 OID 24653)
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- TOC entry 5139 (class 0 OID 23516)
-- Dependencies: 217
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, user_id, doc_id, action, "timestamp", details, ip_address, session_id, created_at) FROM stdin;
\.


--
-- TOC entry 5141 (class 0 OID 23524)
-- Dependencies: 219
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.categories (id, name, description, parent_category_id, organization_id, created_by, created_at, updated_at, status, department_id) FROM stdin;
2	Hành chính	 Tài liệu cho phòng ban A	\N	1	123	2025-08-03 11:25:55.411819+07	2025-08-03 11:25:55.411819+07	1	1
5	Hợp đồng	Các loại hợp đồng và thỏa thuận	\N	8		2025-08-03 18:36:15.735606+07	2025-08-03 18:36:15.735606+07	1	8
6	Báo cáo	Báo cáo định kỳ và đặc biệt	\N	8		2025-08-03 18:36:15.738608+07	2025-08-03 18:36:15.738608+07	1	8
7	Chính sách	Chính sách nội bộ và quy định	\N	8		2025-08-03 18:36:15.740604+07	2025-08-03 18:36:15.740604+07	1	8
8	Hướng dẫn	Sách hướng dẫn và tài liệu đào tạo	\N	8		2025-08-03 18:36:15.744117+07	2025-08-03 18:36:15.744117+07	1	8
9	Tài liệu đối tác	Tài liệu chia sẻ với đối tác	\N	8		2025-08-03 18:36:15.747117+07	2025-08-03 18:36:15.747117+07	1	8
\.


--
-- TOC entry 5144 (class 0 OID 23534)
-- Dependencies: 222
-- Data for Name: comments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.comments (id, user_id, content, parent_comment_id, status, doc_id, created_at, updated_at) FROM stdin;
\.


--
-- TOC entry 5146 (class 0 OID 23541)
-- Dependencies: 224
-- Data for Name: delegations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delegations (id, delegator_id, delegatee_id, doc_id, permission, expiry_date, created_at, expiry_at, document_id) FROM stdin;
\.


--
-- TOC entry 5148 (class 0 OID 23546)
-- Dependencies: 226
-- Data for Name: departments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.departments (id, organization_id, name, description, created_by, created_at, status) FROM stdin;
1	1	123456	123456	\N	\N	1
2	2	IT Department	\N		2025-08-03 18:23:36.73925	1
3	3	External Dept	\N		2025-08-03 18:23:36.741252	1
4	4	IT Department	\N		2025-08-03 18:31:52.466871	1
5	5	External Dept	\N		2025-08-03 18:31:52.470873	1
6	6	IT Department	\N		2025-08-03 18:33:24.20419	1
7	7	External Dept	\N		2025-08-03 18:33:24.207188	1
8	8	IT Department	\N		2025-08-03 18:36:15.728326	1
9	9	External Dept	\N		2025-08-03 18:36:15.731329	1
\.


--
-- TOC entry 5191 (class 0 OID 24791)
-- Dependencies: 269
-- Data for Name: document_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.document_permissions (id, user_id, doc_id, permission, version_number, expiry_date, created_at) FROM stdin;
76	11	62	documents:share:readonly	\N	\N	2025-08-04 15:30:24.167231+07
77	11	62	documents:share:forwardable	\N	\N	2025-08-04 15:30:24.17272+07
\.


--
-- TOC entry 5150 (class 0 OID 23552)
-- Dependencies: 228
-- Data for Name: documents; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.documents (id, title, content, category_id, status, created_by, type, total_page, description, created_at, updated_at, file_path, file_id, storage_capacity, storage_unit, access_type, organization_id, dept_id, title_unaccent, password, photo_id) FROM stdin;
62	External.docx	Encrypted file content stored at C:\\Users\\namdo\\Downloads\\dms\\dms-backend\\uploads\\116dbf43-fd34-4bfb-8ffb-5fc127df1b55.docx.enc	5	1	user@company.com	docx	\N	\N	2025-08-04 15:26:18.016635+07	2025-08-04 15:26:18.016635+07	116dbf43-fd34-4bfb-8ffb-5fc127df1b55.docx.enc	116dbf43-fd34-4bfb-8ffb-5fc127df1b55	6450666	bytes	4	8	8	External.docx	iqejUVPg5SPFn8eJixb82ixMPccfB3SrkqiuSuPOrMA=	\N
63	External.docx	Encrypted file content stored at C:\\Users\\namdo\\Downloads\\dms\\dms-backend\\uploads\\52deebd4-a0db-42e3-9058-b6f515591f17.docx.enc	5	1	admin@company.com	docx	\N	\N	2025-08-04 16:01:39.126023+07	2025-08-04 16:01:39.126023+07	52deebd4-a0db-42e3-9058-b6f515591f17.docx.enc	52deebd4-a0db-42e3-9058-b6f515591f17	6450666	bytes	1	8	8	External.docx	Wfu68VAsoiWDe1QCPbHgUGmrY3HuDnBoG/VFI0DbGC8=	\N
\.


--
-- TOC entry 5152 (class 0 OID 23560)
-- Dependencies: 230
-- Data for Name: file_uploads; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file_uploads (id, document_id, file_path, user_id) FROM stdin;
61	62	116dbf43-fd34-4bfb-8ffb-5fc127df1b55.docx.enc	13
62	63	52deebd4-a0db-42e3-9058-b6f515591f17.docx.enc	11
\.


--
-- TOC entry 5154 (class 0 OID 23564)
-- Dependencies: 232
-- Data for Name: logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.logs (id, user_id, action, request_data, created_at) FROM stdin;
\.


--
-- TOC entry 5156 (class 0 OID 23570)
-- Dependencies: 234
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, title, content, created_at, doc_id) FROM stdin;
\.


--
-- TOC entry 5158 (class 0 OID 23576)
-- Dependencies: 236
-- Data for Name: organizations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.organizations (id, name, description, status, created_at, created_by, updated_at, updated_by, limit_data, data_used, is_openai, parent_organization_id, limit_token, token_used) FROM stdin;
1	Test	123	1	2025-07-30 21:29:32.496398	admin@gmail.com	2025-07-30 21:29:32.496398	\N	10737418240	0	t	\N	\N	\N
2	Company A	\N	1	2025-08-03 18:23:36.687613		2025-08-03 18:23:36.687613	\N	\N	\N	\N	\N	\N	\N
3	Partner Company	\N	1	2025-08-03 18:23:36.736263		2025-08-03 18:23:36.736263	\N	\N	\N	\N	\N	\N	\N
4	Company A	\N	1	2025-08-03 18:31:52.45339		2025-08-03 18:31:52.45339	\N	\N	\N	\N	\N	\N	\N
5	Partner Company	\N	1	2025-08-03 18:31:52.464871		2025-08-03 18:31:52.464871	\N	\N	\N	\N	\N	\N	\N
6	Company A	\N	1	2025-08-03 18:33:24.149393		2025-08-03 18:33:24.150465	\N	\N	\N	\N	\N	\N	\N
7	Partner Company	\N	1	2025-08-03 18:33:24.200672		2025-08-03 18:33:24.200672	\N	\N	\N	\N	\N	\N	\N
8	Company A	\N	1	2025-08-03 18:36:15.676536		2025-08-03 18:36:15.676536	\N	\N	\N	\N	\N	\N	\N
9	Partner Company	\N	1	2025-08-03 18:36:15.725344		2025-08-03 18:36:15.725344	\N	\N	\N	\N	\N	\N	\N
\.


--
-- TOC entry 5160 (class 0 OID 23585)
-- Dependencies: 238
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.permissions (id, name, description, created_at, updated_at) FROM stdin;
1	documents:share:readonly	Chia sẻ tài liệu với quyền chỉ đọc	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
2	documents:share:forwardable	Chia sẻ tài liệu với quyền chuyển tiếp	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
3	documents:share:timebound	Chia sẻ tài liệu với thời hạn	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
4	documents:share:external	Chia sẻ tài liệu ra ngoài tổ chức	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
5	documents:share:orgscope	Chia sẻ tài liệu trong phạm vi tổ chức	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
\.


--
-- TOC entry 5162 (class 0 OID 23589)
-- Dependencies: 240
-- Data for Name: positions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.positions (id, name) FROM stdin;
\.


--
-- TOC entry 5164 (class 0 OID 23593)
-- Dependencies: 242
-- Data for Name: private_docs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.private_docs (id, doc_id, user_id, status) FROM stdin;
\.


--
-- TOC entry 5166 (class 0 OID 23598)
-- Dependencies: 244
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.products (id, name, description, status, type, price, quantity, created_at, updated_at) FROM stdin;
\.


--
-- TOC entry 5168 (class 0 OID 23607)
-- Dependencies: 246
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.refresh_tokens (id, user_id, refresh_token) FROM stdin;
5	13	aDwvnzbGnWUFEsfzZzdLOZFGpSOLavBKPpDkaYIAASqvzudEmtBHcKyMRFDgLgbmsQJxHBVbkwiZtJNKfGVOnGZdtniaJogeAVrCdVDeIzECWEOseqWxjHtQNOrEwixL
4	11	FLloeOYLETXWLPNuWTvhbFfoUslUAJxGiVlSoNnncrcjPvKLtFVPowrUacPVFUCxtCtvdkOYkndvCvemdoAKEVGjdOXsKQvTSIkBnuTGpzEApjbQOUEVVsgzBPlpxBbf
\.


--
-- TOC entry 5170 (class 0 OID 23613)
-- Dependencies: 248
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role_permissions (role_id, permission_id) FROM stdin;
1	1
1	2
1	3
1	4
1	5
2	1
2	5
\.


--
-- TOC entry 5172 (class 0 OID 23617)
-- Dependencies: 250
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (id, organization_id, name, description, is_inheritable, created_at, updated_at) FROM stdin;
1	1	Document Manager	Quản lý tài liệu với quyền chia sẻ đầy đủ	t	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
2	1	Document Viewer	Xem và chia sẻ tài liệu với quyền hạn chế	t	2025-08-01 21:22:59.341566	2025-08-01 21:22:59.341566
\.


--
-- TOC entry 5174 (class 0 OID 23621)
-- Dependencies: 252
-- Data for Name: search_histories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.search_histories (id, user_id, created_at, keywords, type, updated_at) FROM stdin;
\.


--
-- TOC entry 5176 (class 0 OID 23626)
-- Dependencies: 254
-- Data for Name: starred_docs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.starred_docs (id, user_id, doc_id, updated_at, status) FROM stdin;
\.


--
-- TOC entry 5178 (class 0 OID 23630)
-- Dependencies: 256
-- Data for Name: transactions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.transactions (id, user_id, product_id, amount, status, bank_response_code, created_at, updated_at, ref_id) FROM stdin;
\.


--
-- TOC entry 5180 (class 0 OID 23637)
-- Dependencies: 258
-- Data for Name: user_documents; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_documents (id, user_id, document_id, type, status, decentralized_by, updated_at, viewed_at, created_at, move_to_trash_at) FROM stdin;
44	13	62	1	1	\N	2025-08-04 15:26:18.036107+07	\N	2025-08-04 08:26:18.036107	\N
45	11	63	1	1	\N	2025-08-04 16:01:39.151109+07	\N	2025-08-04 09:01:39.151109	\N
\.


--
-- TOC entry 5182 (class 0 OID 23642)
-- Dependencies: 260
-- Data for Name: user_notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_notifications (id, user_id, noti_id, status) FROM stdin;
\.


--
-- TOC entry 5184 (class 0 OID 23646)
-- Dependencies: 262
-- Data for Name: user_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_permissions (user_id, permission_id, action) FROM stdin;
\.


--
-- TOC entry 5186 (class 0 OID 23650)
-- Dependencies: 264
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_id, role_id) FROM stdin;
\.


--
-- TOC entry 5188 (class 0 OID 23654)
-- Dependencies: 266
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, first_name, last_name, user_name, email, password, gender, status, is_admin, created_at, updated_at, is_organization_manager, organization_id, is_social, dept_id, is_dept_manager, position_id, last_login_at) FROM stdin;
12	Dept	Manager	\N	dept.manager@company.com	$2a$10$zO7cmEnt9iN4eRRr3nPMIeav3STSAIhrYWTxp1we2r6cJnfEPPxgO	f	1	f	2025-08-03 18:36:15.895604	2025-08-03 18:36:15.895604	f	8	\N	8	t	\N	\N
13	Normal	User	\N	user@company.com	$2a$10$sfOFPARs5BtboZv25ZLF1uOj.8H61vb2HBLPe/fG6K43Js9T5ms2.	f	1	f	2025-08-03 18:36:15.966283	2025-08-03 18:36:15.966283	f	8	\N	8	f	\N	\N
14	External	User	\N	external@partner.com	$2a$10$nMOX611XwthJzXXPsMV8w.6lArqlAw2TqgjvjVRPvp2wrgTzof9Dm	f	1	f	2025-08-03 18:36:16.037734	2025-08-03 18:36:16.037734	f	9	\N	9	f	\N	\N
15	Target	User	\N	target@company.com	$2a$10$Zxqun84i1NurEWjsW4STveo9e3e6KiSFr6JfzBsNpDZ8.GaYIblEW	f	1	f	2025-08-03 18:36:16.10797	2025-08-03 18:36:16.10797	f	8	\N	8	f	\N	\N
11	Admin	User	\N	admin@company.com	$2a$10$xlgkHGXfdRfIjKKqURYfNuqs2PiSkDF52jXu9OOoZSqA2hkY.PoMK	f	1	t	2025-08-03 18:36:15.822774	2025-08-04 15:11:02.34252	t	8	\N	8	\N	\N	2025-08-04 15:11:02.339992
\.


--
-- TOC entry 5230 (class 0 OID 0)
-- Dependencies: 218
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 1, false);


--
-- TOC entry 5231 (class 0 OID 0)
-- Dependencies: 221
-- Name: categories_department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categories_department_id_seq', 1, false);


--
-- TOC entry 5232 (class 0 OID 0)
-- Dependencies: 220
-- Name: categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categories_id_seq', 9, true);


--
-- TOC entry 5233 (class 0 OID 0)
-- Dependencies: 223
-- Name: comments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.comments_id_seq', 1, false);


--
-- TOC entry 5234 (class 0 OID 0)
-- Dependencies: 225
-- Name: delegations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.delegations_id_seq', 1, false);


--
-- TOC entry 5235 (class 0 OID 0)
-- Dependencies: 227
-- Name: departments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.departments_id_seq', 9, true);


--
-- TOC entry 5236 (class 0 OID 0)
-- Dependencies: 268
-- Name: document_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.document_permissions_id_seq', 77, true);


--
-- TOC entry 5237 (class 0 OID 0)
-- Dependencies: 229
-- Name: documents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.documents_id_seq', 63, true);


--
-- TOC entry 5238 (class 0 OID 0)
-- Dependencies: 231
-- Name: file_uploads_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.file_uploads_id_seq', 62, true);


--
-- TOC entry 5239 (class 0 OID 0)
-- Dependencies: 233
-- Name: logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.logs_id_seq', 1, false);


--
-- TOC entry 5240 (class 0 OID 0)
-- Dependencies: 235
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notifications_id_seq', 1, false);


--
-- TOC entry 5241 (class 0 OID 0)
-- Dependencies: 237
-- Name: organizations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.organizations_id_seq', 9, true);


--
-- TOC entry 5242 (class 0 OID 0)
-- Dependencies: 239
-- Name: permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.permissions_id_seq', 5, true);


--
-- TOC entry 5243 (class 0 OID 0)
-- Dependencies: 241
-- Name: positions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.positions_id_seq', 1, false);


--
-- TOC entry 5244 (class 0 OID 0)
-- Dependencies: 243
-- Name: private_docs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.private_docs_id_seq', 1, false);


--
-- TOC entry 5245 (class 0 OID 0)
-- Dependencies: 245
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.products_id_seq', 1, false);


--
-- TOC entry 5246 (class 0 OID 0)
-- Dependencies: 247
-- Name: refresh_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.refresh_tokens_id_seq', 5, true);


--
-- TOC entry 5247 (class 0 OID 0)
-- Dependencies: 249
-- Name: role_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.role_permissions_id_seq', 1, false);


--
-- TOC entry 5248 (class 0 OID 0)
-- Dependencies: 251
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.roles_id_seq', 2, true);


--
-- TOC entry 5249 (class 0 OID 0)
-- Dependencies: 253
-- Name: search_histories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.search_histories_id_seq', 1, false);


--
-- TOC entry 5250 (class 0 OID 0)
-- Dependencies: 255
-- Name: starred_docs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.starred_docs_id_seq', 1, false);


--
-- TOC entry 5251 (class 0 OID 0)
-- Dependencies: 257
-- Name: transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.transactions_id_seq', 1, false);


--
-- TOC entry 5252 (class 0 OID 0)
-- Dependencies: 259
-- Name: user_documents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_documents_id_seq', 45, true);


--
-- TOC entry 5253 (class 0 OID 0)
-- Dependencies: 261
-- Name: user_notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_notifications_id_seq', 1, false);


--
-- TOC entry 5254 (class 0 OID 0)
-- Dependencies: 263
-- Name: user_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_permissions_id_seq', 1, false);


--
-- TOC entry 5255 (class 0 OID 0)
-- Dependencies: 265
-- Name: user_roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_roles_id_seq', 1, false);


--
-- TOC entry 5256 (class 0 OID 0)
-- Dependencies: 267
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 15, true);


--
-- TOC entry 4881 (class 2606 OID 23954)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 4883 (class 2606 OID 23985)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- TOC entry 4885 (class 2606 OID 24038)
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- TOC entry 4887 (class 2606 OID 24085)
-- Name: delegations delegations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegations
    ADD CONSTRAINT delegations_pkey PRIMARY KEY (id);


--
-- TOC entry 4889 (class 2606 OID 24110)
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- TOC entry 4891 (class 2606 OID 23707)
-- Name: documents documents_file_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_file_id_key UNIQUE (file_id);


--
-- TOC entry 4893 (class 2606 OID 24140)
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);


--
-- TOC entry 4896 (class 2606 OID 24233)
-- Name: file_uploads file_uploads_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_uploads
    ADD CONSTRAINT file_uploads_pkey PRIMARY KEY (id);


--
-- TOC entry 4948 (class 2606 OID 24810)
-- Name: document_permissions fk_document_permissions; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_permissions
    ADD CONSTRAINT fk_document_permissions PRIMARY KEY (id);


--
-- TOC entry 4898 (class 2606 OID 23711)
-- Name: logs logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logs
    ADD CONSTRAINT logs_pkey PRIMARY KEY (id);


--
-- TOC entry 4900 (class 2606 OID 24271)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 4902 (class 2606 OID 24296)
-- Name: organizations organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);


--
-- TOC entry 4904 (class 2606 OID 24341)
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- TOC entry 4906 (class 2606 OID 24358)
-- Name: positions positions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.positions
    ADD CONSTRAINT positions_pkey PRIMARY KEY (id);


--
-- TOC entry 4908 (class 2606 OID 24389)
-- Name: private_docs private_docs_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_docs
    ADD CONSTRAINT private_docs_pk UNIQUE (doc_id, user_id);


--
-- TOC entry 4910 (class 2606 OID 24370)
-- Name: private_docs private_docs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_docs
    ADD CONSTRAINT private_docs_pkey PRIMARY KEY (id);


--
-- TOC entry 4914 (class 2606 OID 24402)
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- TOC entry 4916 (class 2606 OID 24416)
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 4918 (class 2606 OID 24425)
-- Name: refresh_tokens refresh_tokens_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_key UNIQUE (user_id);


--
-- TOC entry 4920 (class 2606 OID 24451)
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- TOC entry 4922 (class 2606 OID 24463)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- TOC entry 4926 (class 2606 OID 24489)
-- Name: search_histories search_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.search_histories
    ADD CONSTRAINT search_histories_pkey PRIMARY KEY (id);


--
-- TOC entry 4928 (class 2606 OID 24505)
-- Name: starred_docs starred_docs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.starred_docs
    ADD CONSTRAINT starred_docs_pkey PRIMARY KEY (id);


--
-- TOC entry 4930 (class 2606 OID 24530)
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- TOC entry 4940 (class 2606 OID 24773)
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- TOC entry 4912 (class 2606 OID 24769)
-- Name: private_docs uk8an6neupw8575bud7rno353k9; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_docs
    ADD CONSTRAINT uk8an6neupw8575bud7rno353k9 UNIQUE (doc_id, user_id);


--
-- TOC entry 4924 (class 2606 OID 24771)
-- Name: roles ukogtyde3p678ej48s9ilm3wdj5; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT ukogtyde3p678ej48s9ilm3wdj5 UNIQUE (name, organization_id);


--
-- TOC entry 4932 (class 2606 OID 24555)
-- Name: user_documents user_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_documents
    ADD CONSTRAINT user_documents_pkey PRIMARY KEY (id);


--
-- TOC entry 4934 (class 2606 OID 24584)
-- Name: user_notifications user_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT user_notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 4936 (class 2606 OID 24620)
-- Name: user_permissions user_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT user_permissions_pkey PRIMARY KEY (user_id, permission_id);


--
-- TOC entry 4938 (class 2606 OID 24643)
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- TOC entry 4942 (class 2606 OID 23751)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 4944 (class 2606 OID 24655)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 4946 (class 2606 OID 23753)
-- Name: users users_user_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_user_name_key UNIQUE (user_name);


--
-- TOC entry 4894 (class 1259 OID 23754)
-- Name: unaccented_title_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX unaccented_title_idx ON public.documents USING btree (title_unaccent);


--
-- TOC entry 4993 (class 2620 OID 23755)
-- Name: documents trigger_update_unaccented_title; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_update_unaccented_title BEFORE INSERT OR UPDATE ON public.documents FOR EACH ROW EXECUTE FUNCTION public.update_unaccented_title();


--
-- TOC entry 4957 (class 2606 OID 24774)
-- Name: delegations fk5vs38nyuhh82txtwb1hc2llu5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegations
    ADD CONSTRAINT fk5vs38nyuhh82txtwb1hc2llu5 FOREIGN KEY (document_id) REFERENCES public.documents(id);


--
-- TOC entry 4949 (class 2606 OID 24166)
-- Name: audit_logs fk_audit_logs_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fk_audit_logs_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);


--
-- TOC entry 4950 (class 2606 OID 24656)
-- Name: audit_logs fk_audit_logs_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fk_audit_logs_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4951 (class 2606 OID 24025)
-- Name: categories fk_categories_categories; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fk_categories_categories FOREIGN KEY (parent_category_id) REFERENCES public.categories(id) NOT VALID;


--
-- TOC entry 4952 (class 2606 OID 24116)
-- Name: categories fk_categories_departments; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fk_categories_departments FOREIGN KEY (department_id) REFERENCES public.departments(id);


--
-- TOC entry 4953 (class 2606 OID 24307)
-- Name: categories fk_categories_organizations; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fk_categories_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- TOC entry 4954 (class 2606 OID 24061)
-- Name: comments fk_comments_comments; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comments_comments FOREIGN KEY (parent_comment_id) REFERENCES public.comments(id) NOT VALID;


--
-- TOC entry 4955 (class 2606 OID 24171)
-- Name: comments fk_comments_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comments_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);


--
-- TOC entry 4956 (class 2606 OID 24661)
-- Name: comments fk_comments_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4958 (class 2606 OID 24666)
-- Name: delegations fk_delegations_users_delegatee; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegations
    ADD CONSTRAINT fk_delegations_users_delegatee FOREIGN KEY (delegatee_id) REFERENCES public.users(id);


--
-- TOC entry 4959 (class 2606 OID 24671)
-- Name: delegations fk_delegations_users_delegator; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegations
    ADD CONSTRAINT fk_delegations_users_delegator FOREIGN KEY (delegator_id) REFERENCES public.users(id);


--
-- TOC entry 4960 (class 2606 OID 24312)
-- Name: departments fk_departments_organizations; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT fk_departments_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- TOC entry 4991 (class 2606 OID 24815)
-- Name: document_permissions fk_document_permissions_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_permissions
    ADD CONSTRAINT fk_document_permissions_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);


--
-- TOC entry 4992 (class 2606 OID 24824)
-- Name: document_permissions fk_document_permissions_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_permissions
    ADD CONSTRAINT fk_document_permissions_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4961 (class 2606 OID 24192)
-- Name: documents fk_documents_categories; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk_documents_categories FOREIGN KEY (category_id) REFERENCES public.categories(id);


--
-- TOC entry 4962 (class 2606 OID 24205)
-- Name: documents fk_documents_departments; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk_documents_departments FOREIGN KEY (dept_id) REFERENCES public.departments(id);


--
-- TOC entry 4963 (class 2606 OID 24317)
-- Name: documents fk_documents_organizations; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk_documents_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- TOC entry 4964 (class 2606 OID 24238)
-- Name: file_uploads fk_file_uploads_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_uploads
    ADD CONSTRAINT fk_file_uploads_documents FOREIGN KEY (document_id) REFERENCES public.documents(id);


--
-- TOC entry 4965 (class 2606 OID 24676)
-- Name: file_uploads fk_file_uploads_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_uploads
    ADD CONSTRAINT fk_file_uploads_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4966 (class 2606 OID 24681)
-- Name: logs fk_logs_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.logs
    ADD CONSTRAINT fk_logs_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4967 (class 2606 OID 24283)
-- Name: notifications fk_notifications_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk_notifications_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);


--
-- TOC entry 4968 (class 2606 OID 24328)
-- Name: organizations fk_organizations_organizations; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT fk_organizations_organizations FOREIGN KEY (parent_organization_id) REFERENCES public.organizations(id) NOT VALID;


--
-- TOC entry 4969 (class 2606 OID 24378)
-- Name: private_docs fk_private_docs_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_docs
    ADD CONSTRAINT fk_private_docs_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);


--
-- TOC entry 4970 (class 2606 OID 24686)
-- Name: private_docs fk_private_docs_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_docs
    ADD CONSTRAINT fk_private_docs_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4971 (class 2606 OID 24691)
-- Name: refresh_tokens fk_refresh_tokens_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4972 (class 2606 OID 24452)
-- Name: role_permissions fk_role_permissions_permissions; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fk_role_permissions_permissions FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- TOC entry 4973 (class 2606 OID 24469)
-- Name: role_permissions fk_role_permissions_roles; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fk_role_permissions_roles FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- TOC entry 4974 (class 2606 OID 24478)
-- Name: roles fk_roles_organizations; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT fk_roles_organizations FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


--
-- TOC entry 4975 (class 2606 OID 24696)
-- Name: search_histories fk_search_histories_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.search_histories
    ADD CONSTRAINT fk_search_histories_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4976 (class 2606 OID 24510)
-- Name: starred_docs fk_starred_docs_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.starred_docs
    ADD CONSTRAINT fk_starred_docs_documents FOREIGN KEY (doc_id) REFERENCES public.documents(id);


--
-- TOC entry 4977 (class 2606 OID 24701)
-- Name: starred_docs fk_starred_docs_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.starred_docs
    ADD CONSTRAINT fk_starred_docs_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4978 (class 2606 OID 24535)
-- Name: transactions fk_transactions_products; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fk_transactions_products FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- TOC entry 4979 (class 2606 OID 24706)
-- Name: transactions fk_transactions_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fk_transactions_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4980 (class 2606 OID 24564)
-- Name: user_documents fk_user_documents_documents; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_documents
    ADD CONSTRAINT fk_user_documents_documents FOREIGN KEY (document_id) REFERENCES public.documents(id);


--
-- TOC entry 4981 (class 2606 OID 24711)
-- Name: user_documents fk_user_documents_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_documents
    ADD CONSTRAINT fk_user_documents_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4982 (class 2606 OID 24589)
-- Name: user_notifications fk_user_notifications_notifications; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT fk_user_notifications_notifications FOREIGN KEY (noti_id) REFERENCES public.notifications(id);


--
-- TOC entry 4983 (class 2606 OID 24716)
-- Name: user_notifications fk_user_notifications_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT fk_user_notifications_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4984 (class 2606 OID 24609)
-- Name: user_permissions fk_user_permissions_permissions; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT fk_user_permissions_permissions FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- TOC entry 4985 (class 2606 OID 24721)
-- Name: user_permissions fk_user_permissions_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_permissions
    ADD CONSTRAINT fk_user_permissions_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4986 (class 2606 OID 24644)
-- Name: user_roles fk_user_roles_roles; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- TOC entry 4987 (class 2606 OID 24726)
-- Name: user_roles fk_user_roles_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4988 (class 2606 OID 24757)
-- Name: users fk_users_positions; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_positions FOREIGN KEY (position_id) REFERENCES public.positions(id) NOT VALID;


--
-- TOC entry 4989 (class 2606 OID 24779)
-- Name: users fkny8a9c8evadqjfx8jvetc73gs; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkny8a9c8evadqjfx8jvetc73gs FOREIGN KEY (dept_id) REFERENCES public.departments(id);


--
-- TOC entry 4990 (class 2606 OID 24784)
-- Name: users fkqpugllwvyv37klq7ft9m8aqxk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkqpugllwvyv37klq7ft9m8aqxk FOREIGN KEY (organization_id) REFERENCES public.organizations(id);


-- Completed on 2025-08-06 22:33:03

--
-- PostgreSQL database dump complete
--

