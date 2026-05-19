package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper - 继承 MyBatis-Plus BaseMapper，提供基础 CRUD
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
