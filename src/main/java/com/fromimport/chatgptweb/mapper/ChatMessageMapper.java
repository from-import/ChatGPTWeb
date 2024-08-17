package com.fromimport.chatgptweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fromimport.chatgptweb.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}