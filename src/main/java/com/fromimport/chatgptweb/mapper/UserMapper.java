package com.fromimport.chatgptweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fromimport.chatgptweb.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
