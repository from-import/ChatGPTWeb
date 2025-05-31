package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.annotation.LoadConversationsToRedis;
import com.fromimport.chatgptweb.common.JwtUtils;
import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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

    @GetMapping("/session/userId")
    public ResponseEntity<?> getUserIdFromToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = JwtUtils.getUsernameFromToken(token);
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    return ResponseEntity.ok(Map.of("userId", user.getId()));
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
                }
            } catch (Exception e) {
                log.error("JWT解析失败：{}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
    }


    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @PostMapping("/login")
    @LoadConversationsToRedis
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user) {
        Map<String, String> responseMap = new HashMap<>();
        try {
            log.info("用户正在尝试登录：username={}", user.getUsername());

            boolean authenticatedUser = userService.authenticate(user.getUsername(), user.getPassword());

            if (authenticatedUser) {
                User loggedInUser = userService.getUserByUsername(user.getUsername());
                String token = JwtUtils.generateToken(loggedInUser.getUsername());

                // 不再使用 Session，完全使用 JWT
                responseMap.put("message", "登录成功");
                responseMap.put("userId", loggedInUser.getId().toString());
                responseMap.put("token", token);

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
    public ResponseEntity<Map<String, String>> logoutUser() {
        // JWT 是无状态的，服务端无法真正“使 token 失效”
        // 通常做法是前端清除本地 token（如从 localStorage 或 cookie 中删除）
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "JWT 模式下无需服务端登出，请前端删除本地 token");
        return ResponseEntity.ok(responseMap);
    }
}
