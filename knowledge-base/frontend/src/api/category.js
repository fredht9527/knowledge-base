import request from './request'

export const getCategoryTree = () => request.get('/categories/tree')

export const getCategories = () => request.get('/categories')

export const getCategoryById = (id) => request.get(`/categories/${id}`)

export const createCategory = (data) => request.post('/categories', data)

export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)

export const deleteCategory = (id) => request.delete(`/categories/${id}`)
