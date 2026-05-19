package com.kb.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * [FIX]: Elasticsearch 仓库接口 - Spring Data Elasticsearch 自动实现
 */
public interface SearchRepository extends ElasticsearchRepository<SearchDocument, String> {
}
