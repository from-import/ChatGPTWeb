package com.fromimport.chatgptweb.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.mapper.UserMapper;
import com.fromimport.chatgptweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username); // 根据用户名查询

        return userMapper.selectOne(queryWrapper); // 使用 Mapper 查询单个用户
    }



    @Override
    public void registerUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 加密密码
        log.info("正在向数据库增加用户：" + user);
        userMapper.insert(user); // 插入用户到数据库
    }

    @Override
    public boolean authenticate(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();; // 根据用户名查询用户
        log.info("正在验证用户：" + user);
        if (user == null) {
            log.info("用户不存在");
            return false;
        }
        boolean correct = password.equals(user.getPassword()); // 验证密码
        if(correct){
            log.info("用户验证成功");}

        return correct; // 验证密码
    }

    @Override
    public Long getUserIdByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username); // 根据用户名查询

        User user = userMapper.selectOne(queryWrapper); // 使用 Mapper 查询单个用户
        if (user != null) {
            return user.getId();
        }
        return null; // 或者抛出异常，视情况而定
    }
}
