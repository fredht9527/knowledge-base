import { reactive } from 'vue'

/**
 * AI 配置全局共享状态
 * 所有组件（AiSettings、HomeChat）共享同一份配置，修改立即可见
 * [FIX]: apiKey 不再持久化到 localStorage，仅存内存，防止被 XSS 窃取
 */
const config = reactive({
  provider: 'openai',
  apiKey: '',
  model: 'gpt-3.5-turbo',
  apiUrl: '',
  temperature: 0.7,
  maxTokens: 2048
})

/** 从 localStorage 加载配置（不含 apiKey） */
function loadFromStorage() {
  try {
    const saved = localStorage.getItem('ai_config')
    if (saved) {
      const data = JSON.parse(saved)
      // [FIX]: 不从 localStorage 恢复 apiKey，仅恢复其他配置
      const { apiKey: _apiKey, ...safeData } = data
      Object.assign(config, safeData)
    }
  } catch {}
}

/** 保存配置到 localStorage（不含 apiKey） */
function saveToStorage() {
  const { apiKey: _apiKey, ...safeData } = { ...config }
  localStorage.setItem('ai_config', JSON.stringify(safeData))
}

/** 更新配置（合并部分字段） */
function updateConfig(partial) {
  Object.assign(config, partial)
  saveToStorage()
}

// 初始化时加载
loadFromStorage()

export function useAiConfig() {
  return { config, updateConfig, loadFromStorage }
}
