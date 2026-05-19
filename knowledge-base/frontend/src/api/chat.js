import request from './request'

/** 添加 noLoading 标记的请求，不触发全屏 loading */
function chatRequest(config) {
  return request({ ...config, noLoading: true })
}

/** 创建新会话 */
export function createSession(model) {
  return chatRequest({ url: '/chat/session', method: 'post', data: model ? { model } : {} })
}

/** 获取会话列表 */
export function listSessions(params) {
  return chatRequest({ url: '/chat/sessions', method: 'get', params })
}

/** 搜索会话 */
export function searchSessions(params) {
  return chatRequest({ url: '/chat/sessions/search', method: 'get', params })
}

/** 更新会话标题 */
export function updateSessionTitle(id, title) {
  return chatRequest({ url: `/chat/session/${id}/title`, method: 'put', data: { title } })
}

/** 删除会话 */
export function deleteSession(id) {
  return chatRequest({ url: `/chat/session/${id}`, method: 'delete' })
}

/** 归档/取消归档会话 */
export function archiveSession(id, archive) {
  return chatRequest({ url: `/chat/session/${id}/archive`, method: 'put', params: { archive } })
}

/** 获取会话消息列表 */
export function getMessages(sessionId) {
  return chatRequest({ url: `/chat/session/${sessionId}/messages`, method: 'get' })
}

/** 发送消息（后端代理方式，非流式） */
export function sendMessage(sessionId, message, imageUrls, extra = {}) {
  return chatRequest({
    url: `/chat/session/${sessionId}/send`,
    method: 'post',
    data: { message, imageUrls, model: extra.model, apiUrl: extra.apiUrl }
  })
}

/** 保存用户消息到数据库（含图片URL和附件ID列表） */
export function saveUserMsg(sessionId, message, imageUrls, attachmentIds) {
  return chatRequest({
    url: `/chat/session/${sessionId}/message/user`,
    method: 'post',
    data: { message, imageUrls, attachmentIds }
  })
}

/** 保存AI回复消息到数据库（用于前端流式调用时记录消息） */
export function saveAssistantMsg(sessionId, reply, thinking) {
  return chatRequest({
    url: `/chat/session/${sessionId}/message/assistant`,
    method: 'post',
    data: { reply, thinking }
  })
}

/** [FIX]: OCR 识别图片文字 - 用于不支持图片输入的模型 */
export function ocrImage(base64) {
  return chatRequest({
    url: '/chat/ocr',
    method: 'post',
    data: { image: base64 }
  })
}

/** [FIX]: 上传聊天图片到服务器，返回图片 URL */
export function uploadChatImages(formData) {
  return request({
    url: '/chat/images/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    noLoading: true
  })
}

/** [FIX]: 上传聊天文件到服务器，提取文本+二进制存储，返回附件信息列表 */
export function uploadChatFiles(formData) {
  return request({
    url: '/files/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    noLoading: true
  })
}

/** [FIX]: 批量获取附件元信息 */
export function getAttachmentInfos(ids) {
  return request({
    url: '/files/info',
    method: 'post',
    data: ids,
    noLoading: true
  })
}

/** [FIX]: 删除指定消息（从数据库真实删除） */
export function deleteMessage(msgId) {
  return chatRequest({
    url: `/chat/message/${msgId}`,
    method: 'delete'
  })
}
