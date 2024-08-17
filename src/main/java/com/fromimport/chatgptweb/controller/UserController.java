package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody User user) {
        try {
            userService.save(user);
            log.info("用户注册成功: " + user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户注册成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "用户名已存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user, HttpSession session, HttpServletResponse response) {
        Map<String, String> responseMap = new HashMap<>();
        try {
            boolean authenticatedUser = userService.authenticate(user.getUsername(), user.getPassword());

            if (authenticatedUser) {
                // 将用户信息存储到 session
                session.setAttribute("user", user);

                responseMap.put("message", "登录成功");
                // 重定向到 index.html
                response.sendRedirect("/index.html");
                return null; // 因为重定向后不再需要返回JSON
            } else {
                responseMap.put("message", "用户名或密码错误");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseMap);
            }
        } catch (Exception e) {
            log.error("登录失败: ", e);
            responseMap.put("message", "登录失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMap);
        }
    }
}