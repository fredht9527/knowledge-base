import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// Token Key
const TOKEN_KEY = 'kb_token'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// ========== 请求拦截器 ==========
request.interceptors.request.use(
  config => {
    // 添加 JWT Token 到请求头
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 上传文件增加超时时间
    if (config.headers['Content-Type']?.includes('multipart/form-data')) {
      config.timeout = 120000
    }
    return config
  },
  error => {
    ElMessage.error('请求发送失败')
    return Promise.reject(error)
  }
)

// ========== 响应拦截器 ==========
request.interceptors.response.use(
  response => {
    const res = response.data

    // 后端返回 Result 格式：{ code, message, data }
    if (res.code === 200) {
      return res.data
    }

    // 业务错误（code 不为 200）
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.code === 'ERR_NETWORK' || error.message?.includes('ECONNREFUSED') || error.message?.includes('Network Error')) {
      ElMessage({ message: '后端服务未启动，请先执行: cd backend && mvn spring-boot:run', type: 'error', duration: 5000 })
    } else if (error.response) {
      const status = error.response.status
      const msg = error.response.data?.message

      // Token 过期或无效，跳转登录
      if (status === 401) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem('kb_user_info')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      }

      switch (status) {
        case 400: ElMessage.error(msg || '请求参数错误'); break
        case 401: break // 已处理 Token 过期
        case 404: ElMessage.error('请求的资源不存在'); break
        case 500: ElMessage.error(msg || '服务器内部错误'); break
        default:  ElMessage.error(msg || `请求失败(${status})`)
      }
    } else if (error.request) {
      ElMessage.error('网络异常，无法连接到服务器')
    } else {
      ElMessage.error(error.message || '未知错误')
    }

    return Promise.reject(error)
  }
)

export default request
