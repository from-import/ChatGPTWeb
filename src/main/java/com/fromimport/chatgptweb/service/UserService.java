package com.fromimport.chatgptweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fromimport.chatgptweb.entity.User;

public interface UserService extends IService<User> {
    void registerUser(String username, String password);
    boolean authenticate(String username, String password);
    Long getUserIdByUsername(String username);
}