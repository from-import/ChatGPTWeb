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
    private Long conversationId; // 添加这个字段用于和conversation表关联
    private String content;
    private String sender;  // "user" or "chatgpt"
    private LocalDateTime timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String content) {
        this.content = content;
    }

    public ChatMessage(String sender, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public ChatMessage(Long id, Long userId, Long conversationId, String content, String sender, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.content = content;
        this.sender = sender;
        this.timestamp = timestamp;
    }
}
