package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper - 继承 MyBatis-Plus BaseMapper，提供基础 CRUD
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
