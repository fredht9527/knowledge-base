import request from './request'

/**
 * AI 配置相关 API
 */

/** 获取 AI 配置 */
export function getAiConfig() {
  return request({
    url: '/ai-config',
    method: 'get'
  })
}

/** 保存 AI 配置 */
export function saveAiConfig(data) {
  return request({
    url: '/ai-config',
    method: 'post',
    data
  })
}
