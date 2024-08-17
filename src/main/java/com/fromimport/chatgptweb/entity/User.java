package com.fromimport.chatgptweb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 用户ID
    private String username; // 用户名
    private String password; // 密码（存储明文密码或加密后的密码）
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
