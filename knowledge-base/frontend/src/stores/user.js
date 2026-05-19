import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'kb_token'
const USER_INFO_KEY = 'kb_user_info'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复状态
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userId = ref(0)
  const nickname = ref('')
  const email = ref('')
  const avatar = ref('')
  const gender = ref('保密')
  const phone = ref('')

  // 加载本地存储的用户信息
  function loadUserInfo() {
    try {
      const saved = localStorage.getItem(USER_INFO_KEY)
      if (saved) {
        const info = JSON.parse(saved)
        userId.value = info.userId || 0
        nickname.value = info.nickname || '用户'
        email.value = info.email || ''
        avatar.value = info.avatar || ''
        gender.value = info.gender || '保密'
        phone.value = info.phone || ''
      }
    } catch (e) {
      console.warn('加载用户信息失败', e)
    }
  }

  // 初始化时加载用户信息
  loadUserInfo()

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 登录成功，保存 Token 和用户信息
   */
  function login(authData) {
    token.value = authData.token
    userId.value = authData.userId
    nickname.value = authData.nickname || '用户'
    email.value = authData.email || ''
    avatar.value = authData.avatar || ''
    gender.value = authData.gender || '保密'
    phone.value = authData.phone || ''

    // [FIX]: 持久化 token + 用户信息
    localStorage.setItem(TOKEN_KEY, authData.token)
    saveToLocal()
  }

  /**
   * 更新本地用户信息（从服务器同步后调用）
   */
  function updateProfile(profile) {
    if (profile.nickname !== undefined) nickname.value = profile.nickname
    if (profile.avatar !== undefined) avatar.value = profile.avatar
    if (profile.gender !== undefined) gender.value = profile.gender
    if (profile.phone !== undefined) phone.value = profile.phone
    saveToLocal()
  }

  /** 保存到 localStorage */
  function saveToLocal() {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify({
      userId: userId.value,
      nickname: nickname.value,
      email: email.value,
      avatar: avatar.value,
      gender: gender.value,
      phone: phone.value
    }))
  }

  /**
   * 登出
   */
  function logout() {
    token.value = ''
    userId.value = 0
    nickname.value = ''
    email.value = ''
    avatar.value = ''
    gender.value = '保密'
    phone.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  /**
   * 获取 Token（用于请求头）
   */
  function getToken() {
    return token.value
  }

  return {
    token,
    userId,
    nickname,
    email,
    avatar,
    gender,
    phone,
    isLoggedIn,
    login,
    updateProfile,
    logout,
    getToken
  }
})
