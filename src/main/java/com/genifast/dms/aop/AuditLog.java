package com.genifast.dms.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để đánh dấu các phương thức cần được ghi log audit.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /**
     * Mô tả hành động được thực hiện.
     * Ví dụ: "CREATE_DOCUMENT", "APPROVE_DOCUMENT", "DELEGATE_PERMISSION"
     */
    String action();
}