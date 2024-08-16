package com.fromimport.chatgptweb.service;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface OpenAIService {
    Mono<String> chatgpt(String message);
}