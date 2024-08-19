package com.fromimport.chatgptweb.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExceptionHandlingAspect {

    @Around("execution(* com.fromimport.chatgptweb.service..*(..))")  // 拦截指定包下的所有方法
    public Object handleException(ProceedingJoinPoint joinPoint) {
        try {
            // 执行目标方法
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 处理异常
            System.err.println("发现异常存在于方法中 : " + joinPoint.getSignature().getName());
            System.err.println("异常名称: " + ex.getMessage());

            // 可以进行更多操作，如日志记录、通知等
            // 使用日志框架记录异常
            log.error("Exception in method: " + joinPoint.getSignature().getName(), ex);
            // 重新抛出异常或返回默认值
            throw new RuntimeException("Exception handled: " + ex.getMessage(), ex);
        }
    }
}