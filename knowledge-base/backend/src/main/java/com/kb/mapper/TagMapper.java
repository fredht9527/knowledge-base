package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签 Mapper - 继承 MyBatis-Plus BaseMapper，提供基础 CRUD
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
