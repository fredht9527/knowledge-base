import request from './request'

export const getKnowledgePage = (params) => request.get('/knowledge', { params, noLoading: true })

export const getKnowledgeById = (id) => request.get(`/knowledge/${id}`, { noLoading: true })

export const createKnowledge = (data) => request.post('/knowledge', data, { noLoading: true })

export const updateKnowledge = (id, data) => request.put(`/knowledge/${id}`, data, { noLoading: true })

export const deleteKnowledge = (id) => request.delete(`/knowledge/${id}`, { noLoading: true })

/** [FIX]: 供AI对话使用的知识库检索 */
export const searchKnowledgeForChat = (keyword, size = 5) =>
  request.get('/knowledge/search', { params: { keyword, size }, noLoading: true })
