package com.fromimport.chatgptweb.serviceImpl;

import com.fromimport.chatgptweb.service.AuthService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public Mono<String> login(String username, String password) {
        // 这里实现实际的验证逻辑，例如与数据库对比
        if ("admin".equals(username) && "password".equals(password)) {
            return Mono.just("Login successful");
        } else {
            return Mono.just("Invalid username or password");
        }
    }
}
