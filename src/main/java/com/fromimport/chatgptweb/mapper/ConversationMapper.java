package com.fromimport.chatgptweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fromimport.chatgptweb.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}