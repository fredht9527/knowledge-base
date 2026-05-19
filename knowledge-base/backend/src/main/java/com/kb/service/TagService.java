package com.kb.service;

import com.kb.entity.Tag;
import com.kb.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签业务逻辑 - 获取标签列表、删除标签
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    /** 获取所有标签名称列表 */
    public List<String> getAllNames() {
        return tagMapper.selectList(null).stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

    /** 删除标签 */
    @Transactional
    public void delete(Long id) {
        tagMapper.deleteById(id);
    }
}
