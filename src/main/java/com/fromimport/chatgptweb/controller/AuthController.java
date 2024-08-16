package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.model.LoginRequest;
import com.fromimport.chatgptweb.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody LoginRequest loginRequest) {
        log.info("正在尝试登陆");
        return authService.login(loginRequest.getUsername(), loginRequest.getPassword())
                .map(response -> ResponseEntity.status(response.equals("Login successful") ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                        .body(response));
    }
}
