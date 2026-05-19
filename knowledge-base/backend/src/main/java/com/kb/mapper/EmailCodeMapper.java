package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.EmailCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱验证码 Mapper - 继承 MyBatis-Plus BaseMapper，提供基础 CRUD
 */
@Mapper
public interface EmailCodeMapper extends BaseMapper<EmailCode> {
}
