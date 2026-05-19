<template>
  <div class="ai-settings-wrapper">
    <el-button class="ai-settings-btn" @click="open" :title="configLabel">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
      </svg>
    </el-button>

    <el-dialog v-model="visible" title="AI 配置" width="520px" destroy-on-close append-to-body class="ai-config-dialog">
      <div class="config-body">
        <!-- 服务商 -->
        <div class="config-section">
          <label class="config-label">AI 服务商</label>
          <el-select v-model="form.provider" class="config-select" @change="onProviderChange">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Anthropic Claude" value="anthropic" />
            <el-option label="Azure OpenAI" value="azure" />
            <el-option label="自定义 (兼容 OpenAI)" value="custom" />
          </el-select>
        </div>

        <!-- API Key -->
        <div class="config-section">
          <label class="config-label">API Key</label>
          <div class="api-key-input-wrap">
            <input v-model="form.apiKey" :type="showKey ? 'text' : 'password'" placeholder="sk-..." class="config-input api-key-input" />
            <button class="eye-btn" @click="showKey = !showKey" type="button">
              <svg v-if="showKey" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
          </div>
        </div>

        <!-- 模型 -->
        <div class="config-section">
          <label class="config-label">模型</label>
          <el-select v-if="form.provider !== 'custom'" v-model="form.model" class="config-select">
            <el-option v-for="m in models" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
          <input v-else v-model="form.model" placeholder="填写模型 ID，如 gpt-4、deepseek-chat 等" class="config-input" />
        </div>

        <!-- 自定义 API 地址 -->
        <div class="config-section" v-if="form.provider === 'custom' || form.provider === 'azure'">
          <label class="config-label">API 地址</label>
          <input v-model="form.apiUrl" placeholder="https://open.bigmodel.cn/api/paas/v4/chat/completions" class="config-input" />
        </div>

        <!-- 温度 -->
        <div class="config-section">
          <label class="config-label">温度 (Temperature) <span class="val">{{ form.temperature }}</span></label>
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </div>

        <!-- 最大 Token -->
        <div class="config-section">
          <label class="config-label">最大 Token</label>
          <el-input-number v-model="form.maxTokens" :min="100" :max="128000" :step="512" controls-position="right" style="width:100%" />
        </div>

        <!-- 状态 -->
        <div class="config-status" :class="{ ok: isConfigured }">
          <span class="dot"></span>
          {{ isConfigured ? '已配置 · 可以开始对话' : '未配置 · 请填写 API Key' }}
        </div>
      </div>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, saveAiConfig } from '../api/aiConfig'
import { useAiConfig } from '../stores/aiConfig'

const { config: sharedConfig, updateConfig } = useAiConfig()

const visible = ref(false)
const saving = ref(false)
const showKey = ref(false)

const modelsMap = {
  openai: [
    { value: 'gpt-4o', label: 'GPT-4o' },
    { value: 'gpt-4o-mini', label: 'GPT-4o Mini' },
    { value: 'gpt-4-turbo', label: 'GPT-4 Turbo' },
    { value: 'gpt-3.5-turbo', label: 'GPT-3.5 Turbo' }
  ],
  anthropic: [
    { value: 'claude-3-opus-20240229', label: 'Claude 3 Opus' },
    { value: 'claude-3-sonnet-20240229', label: 'Claude 3 Sonnet' },
    { value: 'claude-3-haiku-20240307', label: 'Claude 3 Haiku' }
  ],
  azure: [
    { value: 'gpt-4o', label: 'GPT-4o' },
    { value: 'gpt-35-turbo', label: 'GPT-3.5 Turbo' }
  ],
  custom: []
}

const form = ref({
  provider: 'openai',
  apiKey: '',
  model: 'gpt-4o-mini',
  apiUrl: '',
  temperature: 0.7,
  maxTokens: 4096
})

const models = computed(() => modelsMap[form.value.provider] || [])

const isConfigured = computed(() => !!sharedConfig.apiKey)

const configLabel = computed(() => sharedConfig.apiKey ? `AI: ${sharedConfig.model}` : '未配置AI')

function onProviderChange() {
  const m = modelsMap[form.value.provider]
  if (m && m.length > 0) {
    form.value.model = m[0].value
  } else {
    form.value.model = 'gpt-4'
  }
  if (form.value.provider === 'custom' || form.value.provider === 'azure') {
    form.value.apiUrl = form.value.provider === 'custom' ? 'https://open.bigmodel.cn/api/paas/v4/chat/completions' : ''
  } else {
    form.value.apiUrl = ''
  }
}

async function open() {
  try {
    // [FIX]: 从后端获取最新配置（含解密后的 API Key）
    const remoteConfig = await getAiConfig()
    if (remoteConfig) {
      form.value.provider = remoteConfig.provider || 'openai'
      form.value.apiKey = remoteConfig.apiKey || ''
      form.value.model = remoteConfig.model || 'gpt-4o-mini'
      form.value.apiUrl = remoteConfig.apiUrl || ''
      form.value.temperature = remoteConfig.temperature ?? 0.7
      form.value.maxTokens = remoteConfig.maxTokens || 4096
      // 同步更新 store，保证按钮状态一致
      updateConfig({ ...remoteConfig })
    } else {
      // 后端无配置，使用本地 store 兜底
      loadFromStore()
    }
  } catch {
    // 后端不可用时，从本地 store 读取
    loadFromStore()
  }
  visible.value = true
}

/** 从本地 store 加载配置（API Key 可能为空） */
function loadFromStore() {
  form.value.provider = sharedConfig.provider || 'openai'
  form.value.apiKey = sharedConfig.apiKey || ''
  form.value.model = sharedConfig.model || 'gpt-4o-mini'
  form.value.apiUrl = sharedConfig.apiUrl || ''
  form.value.temperature = sharedConfig.temperature ?? 0.7
  form.value.maxTokens = sharedConfig.maxTokens || 4096
}

async function save() {
  if (!form.value.apiKey) {
    ElMessage.warning('请填写 API Key')
    return
  }
  saving.value = true
  // 立即更新共享配置（HomeChat 立即可见）
  updateConfig({ ...form.value })
  try {
    await saveAiConfig(form.value)
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.success('配置已保存到本地')
  } finally {
    saving.value = false
    visible.value = false
  }
}
</script>

<style scoped>
.ai-settings-btn {
  background: transparent !important;
  border: none !important;
  color: #8c847c !important;
  padding: 4px !important;
  height: auto !important;
  cursor: pointer;
  transition: transform .2s, color .2s;
}
.ai-settings-btn:hover { color: #c2410c !important; transform: rotate(60deg); }

.config-body { padding: 8px 0; }
.config-section { margin-bottom: 18px; }
.config-label { display: block; font-size: 13px; font-weight: 600; color: #1c1917; margin-bottom: 6px; }
.config-label .val { font-weight: 400; color: #8c847c; margin-left: 6px; }
.config-select { width: 100%; }
.config-input {
  width: 100%; padding: 8px 12px; border: 1px solid #e7e0d8; border-radius: 8px;
  font-size: 13px; outline: none; box-sizing: border-box; background: #fdf8f3;
  transition: border-color .2s;
}
.config-input:focus { border-color: #c2410c; }
.api-key-input-wrap { position: relative; display: flex; }
.api-key-input { padding-right: 36px !important; }
.eye-btn {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  background: none; border: none; cursor: pointer; padding: 4px;
  color: #8c847c; display: flex; align-items: center; justify-content: center;
  line-height: 1; border-radius: 4px; transition: color .15s;
}
.eye-btn:hover { color: #c2410c; }
.config-status { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #8c847c; margin-top: 12px; }
.config-status.ok { color: #16a34a; }
.config-status .dot { width: 6px; height: 6px; border-radius: 50%; background: #8c847c; }
.config-status.ok .dot { background: #16a34a; }

:deep(.el-dialog) { border-radius: 16px !important; background: #fdf8f3 !important; }
:deep(.el-dialog__title) { color: #1c1917; font-weight: 700; }
:deep(.el-button--primary) { background: linear-gradient(135deg, #c2410c, #d97706) !important; border: none !important; }
</style>
