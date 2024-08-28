package com.fromimport.chatgptweb.aspect;

import com.fromimport.chatgptweb.model.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Aspect
@Component
@Slf4j
public class MessageFilterAspect {

    // 定义正则表达式，用于过滤敏感词或不良信息
    private static final Pattern ILLEGAL_CONTENT_PATTERN = Pattern.compile("badword|anotherbadword", Pattern.CASE_INSENSITIVE);

    @Around("execution(* com.fromimport.chatgptweb.controller.ChatController.chat(..))")
    public Object filterMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && args[0] instanceof ChatRequest) {
            ChatRequest chatRequest = (ChatRequest) args[0];
            String message = chatRequest.getMessage();

            // 检查消息内容是否匹配敏感词正则表达式
            if (ILLEGAL_CONTENT_PATTERN.matcher(message).find()) {
                throw new RuntimeException("消息包含不允许的内容");
            }

            // 使用 PersonalInfoPatterns 中的正则表达式来检查个人敏感信息
            if (Pattern.compile(PersonalInfoPatterns.ID_CARD_PATTERN).matcher(message).find()) {
                throw new RuntimeException("消息包含身份证号等敏感信息");
            }
            if (Pattern.compile(PersonalInfoPatterns.PHONE_NUMBER_PATTERN).matcher(message).find()) {
                throw new RuntimeException("消息包含手机号等敏感信息");
            }
            if (Pattern.compile(PersonalInfoPatterns.EMAIL_PATTERN).matcher(message).find()) {
                throw new RuntimeException("消息包含邮箱地址等敏感信息");
            }
            if (Pattern.compile(PersonalInfoPatterns.BANK_CARD_PATTERN).matcher(message).find()) {
                throw new RuntimeException("消息包含银行卡号等敏感信息");
            }

            // 如果没有敏感词，可以对消息进行处理，比如去除不必要的空格
            log.info("消息内容合法: {}", message);
            chatRequest.setMessage(message.trim());
        }

        // 执行原方法
        return joinPoint.proceed();
    }
}
