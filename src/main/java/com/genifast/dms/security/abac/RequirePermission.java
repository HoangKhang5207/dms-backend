package com.genifast.dms.security.abac;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();                 // ví dụ: "documents:track"
    String[] abacConditions() default {}; // ví dụ: {"recipients", "department"}
}
