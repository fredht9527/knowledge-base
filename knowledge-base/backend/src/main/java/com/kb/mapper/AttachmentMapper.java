package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件 Mapper - 继承 MyBatis-Plus BaseMapper，提供基础 CRUD
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
}
