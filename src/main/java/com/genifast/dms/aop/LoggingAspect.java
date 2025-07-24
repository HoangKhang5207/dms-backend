package com.genifast.dms.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut này định nghĩa "nơi" áp dụng aspect: tất cả các class
     * trong package service và controller.
     */
    @Pointcut("within(com.genifast.dms.service.impl..*) || within(com.genifast.dms.controller..*)")
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the
        // advices.
    }

    /**
     * Advice này sẽ "bọc" quanh các phương thức được định nghĩa bởi pointcut.
     * Nó sẽ thực thi trước và sau khi phương thức gốc chạy.
     */
    @Around("applicationPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // Log khi bắt đầu vào phương thức
        log.info("Enter: {}.{}() with argument[s] = {}", className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result;
        try {
            // Thực thi phương thức gốc
            result = joinPoint.proceed();
        } catch (IllegalArgumentException e) {
            // Log lỗi và re-throw exception
            log.error("Illegal argument: {} in {}.{}()", Arrays.toString(joinPoint.getArgs()), className, methodName);
            throw e;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Log khi kết thúc phương thức
        log.info("Exit: {}.{}(); Execution time = {} ms", className, methodName, executionTime);

        return result;
    }
}