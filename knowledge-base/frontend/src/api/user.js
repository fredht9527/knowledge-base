import request from './request'

/**
 * 获取当前登录用户资料
 */
export const getUserProfile = () => request.get('/user/profile')

/**
 * 更新用户资料
 * @param {object} data - { nickname, gender, phone }
 */
export const updateUserProfile = (data) => request.put('/user/profile', data)

/**
 * 上传头像
 * @param {File} file - 图片文件
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
