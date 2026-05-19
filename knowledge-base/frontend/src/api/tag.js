import request from './request'

export const getTags = () => request.get('/tags')

export const deleteTag = (id) => request.delete(`/tags/${id}`)
