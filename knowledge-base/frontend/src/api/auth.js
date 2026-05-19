import request from './request'

/**
 * 发送验证码
 * @param {string} email - 邮箱地址
 * @param {string} type - 验证码类型：register/login/reset
 */
export const sendCode = (email, type = 'register') => request.post('/auth/send-code', { email, type }, { noLoading: true })

/**
 * 用户注册
 * @param {object} data - { email, code, password }
 */
export const register = (data) => request.post('/auth/register', data, { noLoading: true })

/**
 * 用户登录
 * @param {object} data - { email, password }
 */
export const login = (data) => request.post('/auth/login', data, { noLoading: true })
