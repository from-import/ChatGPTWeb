package com.fromimport.chatgptweb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.ASSIGN_ID)  // 使用 MyBatis-Plus 的雪花算法生成 ID
    private Long id; // 对话 ID
    private Long userId; // 参与对话的用户 ID
    private LocalDateTime startTimestamp; // 对话开始时间
    private LocalDateTime endTimestamp; // 对话结束时间（可选）

    public Conversation() {
    }

    public Conversation(Long id, Long userId, LocalDateTime startTimestamp, LocalDateTime endTimestamp) {
        this.id = id;
        this.userId = userId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }
}