package com.fromimport.chatgptweb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId
    private Long id;
    private Long userId;
    private String content;
    private String sender;  // "user" or "chatgpt"
    private LocalDateTime timestamp;
}
