import request from './request'

/** 上传文件到服务器 */
export const uploadFiles = (files) => {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/**
 * 将已上传的文件关联到知识条目
 * @param {number} knowledgeId - 知识条目ID
 * @param {string} fileIds - 逗号分隔的文件ID，如 "1,2,3"
 */
export const linkFilesToKnowledge = (knowledgeId, fileIds) => {
  return request.put('/files/link', null, {
    params: { knowledgeId, fileIds }
  })
}
