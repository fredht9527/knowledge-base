package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识条目 Mapper - 继承 MyBatis-Plus BaseMapper，提供基础 CRUD 和分页
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
}
