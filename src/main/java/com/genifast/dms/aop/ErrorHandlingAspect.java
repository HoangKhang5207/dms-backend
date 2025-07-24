package com.genifast.dms.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ErrorHandlingAspect {

    @AfterThrowing(pointcut = "execution(* com.genifast.dms.service.*.*(..))", throwing = "ex")
    public void handleServiceException(JoinPoint joinPoint, Exception ex) {
        String methodName = joinPoint.getSignature().getName();
        String errorMsg = String.format("[ERROR] In %s: %s", methodName, ex.getMessage());

        log.error(errorMsg, ex);
        // Có thể thêm logic gửi cảnh báo/email tại đây
    }
}
