package com.fromimport.chatgptweb.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface AuthService {
    Mono<String> login(String username, String password);
}