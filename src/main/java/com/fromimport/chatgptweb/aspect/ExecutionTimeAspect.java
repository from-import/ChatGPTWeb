package com.fromimport.chatgptweb.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExecutionTimeAspect {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    // 切面方法，记录方法的执行时间
    @Around("execution(* com.fromimport.chatgptweb.service.ConversationService.getConversationHistoryWithFirstMessage(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行目标方法
        Object proceed = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;

        // 记录耗时
        logger.info("{} 执行时间: {} ms", joinPoint.getSignature(), executionTime);

        return proceed;
    }
}