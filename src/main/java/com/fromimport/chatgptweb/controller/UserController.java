package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.annotation.LoadConversationsToRedis;
import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
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
            user.setPassword(hashPassword(user.getPassword()));
            log.info("用户密码加密成功: {}", user);
            userService.save(user);
            log.info("用户注册成功: {}", user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户注册成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("用户注册失败: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "用户名已存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @PostMapping("/login")
    @LoadConversationsToRedis
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user, HttpSession session) {
        Map<String, String> responseMap = new HashMap<>();
        try {
            log.info("用户正在尝试登录：username={}", user.getUsername());
            // 直接在此处对密码进行验证
            boolean authenticatedUser = userService.authenticate(user.getUsername(), user.getPassword());

            if (authenticatedUser) {
                User loggedInUser = userService.getUserByUsername(user.getUsername());
                session.setAttribute("user", loggedInUser); // 存储完整用户信息到 session
                responseMap.put("message", "登录成功");
                responseMap.put("userId", loggedInUser.getId().toString()); // 添加 userId 到响应中
                log.info("用户登录成功: {}", loggedInUser);
                return ResponseEntity.ok(responseMap);
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

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logoutUser(HttpSession session) {
        session.invalidate(); // 使用户会话无效
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "用户已登出");
        return ResponseEntity.ok(responseMap);
    }
}
