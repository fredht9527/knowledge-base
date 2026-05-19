<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-orb orb-1"></div>
      <div class="bg-orb orb-2"></div>
      <div class="bg-orb orb-3"></div>
    </div>

    <div class="auth-container">
      <!-- 左侧品牌区 - 增强版设计（与Login.vue风格统一但更具特色） -->
      <div class="auth-brand">
        <!-- 装饰性大圆形光晕 -->
        <div class="brand-decoration">
          <div class="deco-circle deco-circle-1"></div>
          <div class="deco-circle deco-circle-2"></div>
          <div class="deco-circle deco-circle-3"></div>
          <!-- [FIX]: 新增第4个装饰圆环，增加视觉复杂度 -->
          <div class="deco-circle deco-circle-4"></div>
          <!-- [FIX]: 新增浮动光点粒子效果 -->
          <div class="deco-particles">
            <span class="particle particle-1"></span>
            <span class="particle particle-2"></span>
            <span class="particle particle-3"></span>
            <span class="particle particle-4"></span>
            <span class="particle particle-5"></span>
          </div>
        </div>

        <!-- [FIX]: 新增顶部渐入装饰线条 -->
        <div class="brand-accent-line"></div>

        <div class="brand-content">
          <!-- Logo区域 -->
          <!-- [FIX]: 全新AI主题Logo - "神经之星"：融合大脑神经网络+星云能量核+脉冲突触（与Login一致） -->
          <div class="brand-logo">
            <svg width="52" height="52" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <!-- 主渐变：亮白到金黄（品牌色系） -->
                <linearGradient id="logoGradMain" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#ffffff"/>
                  <stop offset="40%" stop-color="#fef3c7"/>
                  <stop offset="100%" stop-color="#fbbf24"/>
                </linearGradient>
                <!-- 能量核渐变：中心高亮向外衰减 -->
                <radialGradient id="logoGradCore" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#ffffff"/>
                  <stop offset="45%" stop-color="#fef9c3"/>
                  <stop offset="100%" stop-color="#fbbf24" stop-opacity="0.8"/>
                </radialGradient>
                <!-- 神经元节点渐变：冰蓝科技色 -->
                <radialGradient id="logoGradNode" cx="50%" cy="30%" r="60%">
                  <stop offset="0%" stop-color="#ffffff"/>
                  <stop offset="60%" stop-color:#a5f3fc/>
                  <stop offset="100%" stop-color="#67e8f9" stop-opacity="0.6"/>
                </radialGradient>
                <!-- 外圈光环渐变 -->
                <linearGradient id="logoGradRing" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="rgba(255,255,255,0)"/>
                  <stop offset="50%" stop-color="rgba(255,255,255,0.7)"/>
                  <stop offset="100%" stop-color="rgba(255,255,255,0)"/>
                </linearGradient>
                <!-- 大脑弧线渐变：半透明金色 -->
                <linearGradient id="logoGradBrain" x1="0%" y1="100%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="rgba(251,191,36,0.9)"/>
                  <stop offset="50%" stop-color="rgba(255,245,220,0.95)"/>
                  <stop offset="100%" stop-color="rgba(251,191,36,0.85)"/>
                </linearGradient>
                <!-- 内核发光滤镜 - 增强版 -->
                <filter id="logoGlow" x="-50%" y="-50%" width="200%" height="200%">
                  <feGaussianBlur stdDeviation="1.8" result="blur"/>
                  <feMerge>
                    <feMergeNode in="blur"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
                <!-- 强发光滤镜 - 用于核心 -->
                <filter id="logoGlowStrong" x="-80%" y="-80%" width="260%" height="260%">
                  <feGaussianBlur stdDeviation="2.5" result="blur1"/>
                  <feGaussianBlur stdDeviation="1" result="blur2"/>
                  <feMerge>
                    <feMergeNode in="blur1"/>
                    <feMergeNode in="blur2"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              </defs>

              <!-- 外圈脉冲环 - 保留动画类 -->
              <circle cx="24" cy="24" r="21" stroke="url(#logoGradRing)" stroke-width="0.8" fill="none" opacity="0.5" class="logo-pulse-ring"/>

              <!-- 外围轨道粒子环 -->
              <g opacity="0.35" class="logo-orbit">
                <!-- 上方轨道粒子 -->
                <circle cx="24" cy="3" r="1.2" fill="#fff" class="logo-sparkle"/>
                <!-- 右上粒子 -->
                <circle cx="39" cy="9" r="0.9" fill="#fef3c7"/>
                <!-- 右侧粒子 -->
                <circle cx="44" cy="24" r="1.1" fill="#fff" class="logo-sparkle"/>
                <!-- 右下粒子 -->
                <circle cx="38" cy="37" r="0.8" fill="#a5f3fc"/>
                <!-- 下方粒子 -->
                <circle cx="24" cy="43" r="1.2" fill="#fef3c7" class="logo-sparkle"/>
                <!-- 左下粒子 -->
                <circle cx="10" cy="37" r="0.9" fill="#fff"/>
                <!-- 左侧粒子 -->
                <circle cx="4" cy="24" r="1" fill="#a5f3fc" class="logo-sparkle"/>
                <!-- 左上粒子 -->
                <circle cx="9" cy="10" r="0.8" fill="#fef3c7"/>
              </g>

              <!-- ========== 主体：AI神经网络大脑 ========== -->
              <g filter="url(#logoGlow)">
                <!-- 大脑外轮廓 - 多层弧线构建半球形神经网络 -->
                <path d="M8 28 Q8 14, 18 11 Q24 9, 30 11 Q40 14, 40 28"
                      stroke="url(#logoGradBrain)" stroke-width="2.2" fill="none" stroke-linecap="round"
                      opacity="0.95"/>

                <!-- 大脑中层面弧线 - 左半球褶皱 -->
                <path d="M10 26 Q12 16, 19 13.5"
                      stroke="url(#logoGradBrain)" stroke-width="1.4" fill="none" stroke-linecap="round"
                      opacity="0.7"/>

                <!-- 大脑中层面弧线 - 右半球褶皱 -->
                <path d="M38 26 Q36 16, 29 13.5"
                      stroke="url(#logoGradBrain)" stroke-width="1.4" fill="none" stroke-linecap="round"
                      opacity="0.7"/>

                <!-- 大脑内部横贯弧线 - 左侧 -->
                <path d="M12 23 Q16 18, 21 16"
                      stroke="url(#logoGradBrain)" stroke-width="1" fill="none" stroke-linecap="round"
                      opacity="0.5"/>

                <!-- 大脑内部横贯弧线 - 右侧 -->
                <path d="M36 23 Q32 18, 27 16"
                      stroke="url(#logoGradBrain)" stroke-width="1" fill="none" stroke-linecap="round"
                      opacity="0.5"/>

                <!-- 中央分界纵线（大脑两半球分界） -->
                <path d="M24 12 L24 27"
                      stroke="url(#logoGradBrain)" stroke-width="1" fill="none" stroke-linecap="round"
                      opacity="0.55"/>
              </g>

              <!-- ========== 神经元节点网络 ========== -->
              <g filter="url(#logoGlow)" class="logo-circuit">
                <!-- 核心能量核 -->
                <circle cx="24" cy="22" r="4.5" fill="url(#logoGradCore)" filter="url(#logoGlowStrong)"
                        class="logo-core-pulse"/>

                <!-- 核心内亮点 -->
                <circle cx="24" cy="22" r="2" fill="#ffffff" opacity="0.95"/>

                <!-- ===== 左侧神经元网络 ===== -->
                <!-- 左上节点 -->
                <circle cx="15" cy="15" r="2.2" fill="url(#logoGradNode)" opacity="0.9"/>
                <circle cx="15" cy="15" r="1" fill="#fff" opacity="0.8"/>
                <!-- 连接线：左上→核心 -->
                <line x1="17" y1="16.5" x2="20.5" y2="20" stroke="rgba(165,243,252,0.5)" stroke-width="0.8"/>

                <!-- 左中节点 -->
                <circle cx="11" cy="22" r="1.8" fill="url(#logoGradNode)" opacity="0.85"/>
                <circle cx="11" cy="22" r="0.8" fill="#fff" opacity="0.7"/>
                <!-- 连接线：左中→核心 -->
                <line x1="12.8" y1="22" x2="19.5" y2="22" stroke="rgba(165,243,252,0.45)" stroke-width="0.7"/>
                <!-- 连接线：左中↔左上 -->
                <line x1="12.3" y1="20.5" x2="14" y2="16.5" stroke="rgba(165,243,252,0.35)" stroke-width="0.6"/>

                <!-- 左下节点 -->
                <circle cx="14" cy="29" r="1.5" fill="url(#logoGradNode)" opacity="0.75"/>
                <circle cx="14" cy="29" r="0.7" fill="#fff" opacity="0.65"/>
                <!-- 连接线：左下→核心 -->
                <line x1="15.8" y1="28" x2="20.5" y2="24" stroke="rgba(165,243,252,0.4)" stroke-width="0.6"/>

                <!-- 左边缘微节点 -->
                <circle cx="8" cy="26" r="1" fill="url(#logoGradNode)" opacity="0.55"/>
                <line x1="9.2" y1="25.5" x2="10" y2="23.5" stroke="rgba(165,243,252,0.3)" stroke-width="0.5"/>

                <!-- ===== 右侧神经元网络 ===== -->
                <!-- 右上节点 -->
                <circle cx="33" cy="15" r="2.2" fill="url(#logoGradNode)" opacity="0.9"/>
                <circle cx="33" cy="15" r="1" fill="#fff" opacity="0.8"/>
                <!-- 连接线：右上→核心 -->
                <line x1="31" y1="16.5" x2="27.5" y2="20" stroke="rgba(165,243,252,0.5)" stroke-width="0.8"/>

                <!-- 右中节点 -->
                <circle cx="37" cy="22" r="1.8" fill="url(#logoGradNode)" opacity="0.85"/>
                <circle cx="37" cy="22" r="0.8" fill="#fff" opacity="0.7"/>
                <!-- 连接线：右中→核心 -->
                <line x1="35.2" y1="22" x2="28.5" y2="22" stroke="rgba(165,243,252,0.45)" stroke-width="0.7"/>
                <!-- 连接线：右中↔右上 -->
                <line x1="35.7" y1="20.5" x2="34" y2="16.5" stroke="rgba(165,243,252,0.35)" stroke-width="0.6"/>

                <!-- 右下节点 -->
                <circle cx="34" cy="29" r="1.5" fill="url(#logoGradNode)" opacity="0.75"/>
                <circle cx="34" cy="29" r="0.7" fill="#fff" opacity="0.65"/>
                <!-- 连接线：右下→核心 -->
                <line x1="32.2" y1="28" x2="27.5" y2="24" stroke="rgba(165,243,252,0.4)" stroke-width="0.6"/>

                <!-- 右边缘微节点 -->
                <circle cx="40" cy="26" r="1" fill="url(#logoGradNode)" opacity="0.55"/>
                <line x1="38.8" y1="25.5" x2="38" y2="23.5" stroke="rgba(165,243,252,0.3)" stroke-width="0.5"/>
              </g>

              <!-- ========== 顶部AI光芒射线 ========== -->
              <g class="logo-sparkle" fill="#ffffff" filter="url(#logoGlow)">
                <!-- 中心主射线（最强） -->
                <ellipse cx="24" cy="6" rx="1.8" ry="4.5" opacity="0.95"/>
                <!-- 左侧射线组 -->
                <ellipse cx="16" cy="8.5" rx="1.3" ry="3" opacity="0.7" transform="rotate(-20 16 8.5)"/>
                <ellipse cx="10" cy="13" rx="1" ry="2.2" opacity="0.45" transform="rotate(-40 10 13)"/>
                <!-- 右侧射线组 -->
                <ellipse cx="32" cy="8.5" rx="1.3" ry="3" opacity="0.7" transform="rotate(20 32 8.5)"/>
                <ellipse cx="38" cy="13" rx="1" ry="2.2" opacity="0.45" transform="rotate(40 38 13)"/>
              </g>

              <!-- 底部数据流动指示器 -->
              <g opacity="0.4" class="logo-data-flow">
                <path d="M18 34 Q20 32, 22 34 Q24 36, 26 34 Q28 32, 30 34"
                      stroke="#a5f3fc" stroke-width="0.8" fill="none" stroke-linecap="round"/>
              </g>
            </svg>
            <!-- [FIX]: Logo角标，提示"新用户" -->
            <span class="logo-badge">NEW</span>
          </div>

          <!-- [FIX]: 更有吸引力的品牌标题和描述 -->
          <h1 class="brand-title">开启智能之旅</h1>
          <p class="brand-desc">注册即可解锁 AI 知识助手全部能力<br/>让团队协作更高效、知识管理更智能</p>

          <!-- [FIX]: 新增注册步骤指示器 -->
          <div class="brand-steps">
            <div class="step-item active">
              <span class="step-num">1</span>
              <span class="step-text">创建账号</span>
            </div>
            <div class="step-connector"></div>
            <div class="step-item">
              <span class="step-num">2</span>
              <span class="step-text">验证邮箱</span>
            </div>
            <div class="step-connector"></div>
            <div class="step-item">
              <span class="step-num">3</span>
              <span class="step-text">开始使用</span>
            </div>
          </div>

          <!-- 功能列表 - [FIX]: 补全至4项，与Login.vue一致 -->
          <div class="brand-features">
            <div class="brand-feature">
              <div class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </div>
              <div class="feature-text">
                <span class="feature-name">智能问答</span>
                <span class="feature-desc">快速获取准确答案</span>
              </div>
            </div>

            <div class="brand-feature">
              <div class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="7" height="7"/>
                  <rect x="14" y="3" width="7" height="7"/>
                  <rect x="3" y="14" width="7" height="7"/>
                  <rect x="14" y="14" width="7" height="7"/>
                </svg>
              </div>
              <div class="feature-text">
                <span class="feature-name">知识分类</span>
                <span class="feature-desc">高效管理结构化资料</span>
              </div>
            </div>

            <div class="brand-feature">
              <div class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
              </div>
              <div class="feature-text">
                <span class="feature-name">多轮对话</span>
                <span class="feature-desc">持续深入探讨交流</span>
              </div>
            </div>

            <div class="brand-feature">
              <div class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
              </div>
              <div class="feature-text">
                <span class="feature-name">安全可靠</span>
                <span class="feature-desc">数据加密保障隐私</span>
              </div>
            </div>
          </div>
        </div>

        <!-- [FIX]: 纯CSS实现的3D玻璃质感堆叠方块装饰（与Login.vue一致） -->
        <div class="brand-illustration">
          <div class="stack-stack">
            <div class="stack-layer stack-layer-1"></div>
            <div class="stack-layer stack-layer-2"></div>
            <div class="stack-layer stack-layer-3"></div>
            <div class="stack-layer stack-layer-4"></div>
          </div>
          <div class="stack-glow"></div>
          <div class="stack-ring stack-ring-1"></div>
          <div class="stack-ring stack-ring-2"></div>
          <!-- [FIX]: 新增第3个装饰圆环，增加层次感 -->
          <div class="stack-ring stack-ring-3"></div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-card-wrap">
        <div class="auth-card">
          <div class="auth-header">
            <h1 class="auth-title">注册账号</h1>
            <p class="auth-desc">填写以下信息创建你的账号</p>
          </div>

          <form class="auth-form" @submit.prevent="handleRegister">
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <div class="input-wrap" :class="{ focus: emailFocus, filled: email }">
                <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                <input v-model="email" type="email" placeholder="请输入邮箱" autocomplete="email"
                  @focus="emailFocus = true" @blur="emailFocus = false" @input="onEmailInput" />
                <span v-if="email && !validEmail" class="input-err">邮箱格式不正确</span>
              </div>
              <div v-if="showSuggestion && suggestList.length" class="email-suggest">
                <div v-for="s in suggestList" :key="s" class="suggest-item" @mousedown.prevent="selectSuggestion(s)">{{ s }}</div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">验证码</label>
                <div class="input-wrap" :class="{ focus: codeFocus, filled: code }">
                  <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m7 15 3-3-3-3"/><path d="m17 9-3 3 3 3"/></svg>
                  <input v-model="code" type="text" placeholder="验证码" maxlength="6"
                    @focus="codeFocus = true" @blur="codeFocus = false" />
                </div>
              </div>
              <div class="code-btn-wrap">
                <button type="button" class="code-btn" :disabled="countdown > 0" @click="sendCode">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </button>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">密码</label>
              <div class="input-wrap" :class="{ focus: pwdFocus, filled: password }">
                <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                <input v-model="password" :type="showPwd ? 'text' : 'password'" placeholder="至少6位密码" autocomplete="new-password"
                  @focus="pwdFocus = true" @blur="pwdFocus = false" />
                <button type="button" class="toggle-pwd" @click="showPwd = !showPwd">
                  <svg v-if="showPwd" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">确认密码</label>
              <div class="input-wrap" :class="{ focus: confirmFocus, filled: confirmPwd, error: confirmPwd && password !== confirmPwd }">
                <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                <input v-model="confirmPwd" :type="showPwd ? 'text' : 'password'" placeholder="再次输入密码" autocomplete="new-password"
                  @focus="confirmFocus = true" @blur="confirmFocus = false" />
              </div>
              <span v-if="confirmPwd && password !== confirmPwd" class="pwd-err">两次密码不一致</span>
            </div>

            <div class="form-group">
              <label class="form-label">安全验证</label>
              <SlideCaptcha ref="captchaRef" @verify="captchaPassed = true" />
            </div>

            <button type="submit" class="auth-btn" :disabled="!canSubmit">
              <span v-if="!submitting">注册</span>
              <span v-else class="btn-loading">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="31.4 31.4" stroke-linecap="round"/></svg>
                注册中...
              </span>
            </button>
          </form>

          <div class="auth-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="auth-link">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SlideCaptcha from '../components/SlideCaptcha.vue'
import { sendCode as sendCodeApi, register } from '../api/auth'
import { useUserStore } from '../stores/user'
import { useEmailSuggest } from '../composables/useEmailSuggest'

const router = useRouter()
const userStore = useUserStore()
const code = ref('')
const password = ref('')
const confirmPwd = ref('')
const showPwd = ref(false)
const emailFocus = ref(false)
const codeFocus = ref(false)
const pwdFocus = ref(false)
const confirmFocus = ref(false)
const submitting = ref(false)
const countdown = ref(0)
const captchaPassed = ref(false)
const captchaRef = ref(null)

const { email, showSuggestion, suggestList, validEmail, onEmailInput, selectSuggestion } = useEmailSuggest()

const canSubmit = computed(() =>
  validEmail.value && code.value.length >= 4 && password.value.length >= 6 &&
  password.value === confirmPwd.value && captchaPassed.value && !submitting.value
)

// [FIX]: 发送验证码 - 点击后立即开始60秒倒计时，去掉loading状态
let timer = null

async function sendCode() {
  if (!validEmail.value) { ElMessage.warning('请输入正确的邮箱'); return }
  if (countdown.value > 0) return

  // 立即开始倒计时
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)

  try {
    await sendCodeApi(email.value, 'register')
    ElMessage.success(`验证码已发送至 ${email.value}`)
  } catch (error) {
    // 发送失败时重置倒计时，让用户可以重新发送
    clearInterval(timer)
    timer = null
    countdown.value = 0
    console.error('发送验证码失败:', error)
  }
}

// [FIX]: 注册
async function handleRegister() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const authData = await register({
      email: email.value,
      code: code.value,
      password: password.value
    })
    userStore.login(authData)
    ElMessage.success('注册成功')
    // [FIX]: 使用 router.push 替代 window.location.reload，保持 SPA 体验
    router.push({ name: 'Home' })
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    submitting.value = false
  }
}

// [FIX]: 组件卸载时清理验证码倒计时定时器，防止内存泄漏
onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #faf8f5;
}

.auth-bg { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.25;
}
.orb-1 { width: 500px; height: 500px; background: #c2410c; top: -150px; right: -100px; animation: floatSlow 10s ease-in-out infinite; }
.orb-2 { width: 400px; height: 400px; background: #d97706; bottom: -120px; left: -80px; animation: floatSlow 12s ease-in-out infinite reverse; }
.orb-3 { width: 300px; height: 300px; background: #b45309; top: 40%; left: 50%; animation: floatSlow 14s ease-in-out infinite 2s; }
@keyframes floatSlow {
  0%,100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 15px) scale(0.95); }
}

.auth-container {
  position: relative;
  display: flex;
  width: 100%;
  max-width: 860px;
  min-height: 560px;
  margin: 20px;
  z-index: 1;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(24px);
  box-shadow: 0 24px 80px rgba(41,37,36,0.10), 0 8px 32px rgba(194,65,12,0.06);
  border: 1px solid rgba(231,224,216,0.4);
}

.auth-brand {
  /* [FIX]: 改为百分比宽度(45%)，使左右两侧视觉均衡对称 */
  width: 45%;
  max-width: 420px;
  background: linear-gradient(160deg, #9a3412 0%, #c2410c 50%, #d97706 100%);
  /* [FIX]: 压缩padding使页面更紧凑（原48px 40px） */
  padding: 36px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
/* [FIX]: 品牌区光效叠加 - 与Login.vue一致 */
.auth-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 20% 50%, rgba(255,255,255,0.15) 0%, transparent 60%),
              radial-gradient(ellipse at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 50%);
}

/* [FIX]: 装饰性大圆形光晕 - 与Login.vue完全一致 */
.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
}
.deco-circle-1 {
  width: 280px;
  height: 280px;
  background: rgba(255,255,255,0.4);
  top: -80px;
  right: -60px;
}
.deco-circle-2 {
  width: 180px;
  height: 180px;
  background: rgba(255,255,255,0.25);
  bottom: 40px;
  left: -40px;
}
.deco-circle-3 {
  width: 120px;
  height: 120px;
  background: rgba(255,220,180,0.35);
  top: 45%;
  right: 20%;
}
/* [FIX]: 新增第4个装饰圆环 */
.deco-circle-4 {
  width: 90px;
  height: 90px;
  background: transparent;
  border: 2px solid rgba(255,255,255,0.15);
  top: 15%;
  left: 25%;
  animation: pulseRing 6s ease-in-out infinite;
}

/* [FIX]: 浮动光点粒子效果 */
.deco-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255,255,255,0.6);
  border-radius: 50%;
  animation: floatParticle linear infinite;
}
.particle-1 { width: 3px; height: 3px; top: 20%; left: 15%; animation-duration: 8s; animation-delay: 0s; }
.particle-2 { width: 5px; height: 5px; top: 60%; left: 70%; animation-duration: 10s; animation-delay: 2s; }
.particle-3 { width: 3px; height: 3px; top: 35%; left: 55%; animation-duration: 7s; animation-delay: 4s; }
.particle-4 { width: 4px; height: 4px; top: 75%; left: 30%; animation-duration: 9s; animation-delay: 1s; opacity: 0.4; }
.particle-5 { width: 2px; height: 2px; top: 10%; left: 80%; animation-duration: 11s; animation-delay: 3s; opacity: 0.5; }

@keyframes floatParticle {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.8; }
  50% { transform: translateY(-40px) translateX(15px); opacity: 0.6; }
  90% { opacity: 0.3; }
  100% { transform: translateY(-80px) translateX(-10px); opacity: 0; }
}

@keyframes pulseRing {
  0%,100% { transform: scale(1); opacity: 0.15; }
  50% { transform: scale(1.08); opacity: 0.22; }
}

/* [FIX]: 顶部渐入装饰线条 */
.brand-accent-line {
  position: absolute;
  top: 28px;
  left: 36px;
  right: 36px;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.4) 30%, rgba(255,255,255,0.6) 50%, rgba(255,255,255,0.4) 70%, transparent 100%);
  border-radius: 1px;
  z-index: 2;
}

.brand-content {
  position: relative;
  z-index: 2;
  color: #fff;
}
/* [FIX]: 重新设计Logo容器 - 增强发光效果和微动效（与Login.vue统一） */
.brand-logo {
  width: 80px; height: 80px;
  border-radius: 20px;
  background: rgba(255,255,255,0.22);
  backdrop-filter: blur(12px);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 20px;
  box-shadow:
    0 4px 16px rgba(0,0,0,0.12),
    0 0 24px rgba(251,191,36,0.15),
    inset 0 1px 0 rgba(255,255,255,0.3);
  position: relative;
  overflow: visible;
}

/* [FIX]: Logo内SVG脉冲环动画 */
.brand-logo .logo-pulse-ring {
  animation: logoPulse 3s ease-in-out infinite;
}
@keyframes logoPulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.08); opacity: 0.8; }
}

/* [FIX]: Logo星芒闪烁 */
.brand-logo .logo-sparkle {
  animation: sparkle 2.5s ease-in-out infinite;
  transform-origin: center;
}
@keyframes sparkle {
  0%, 100% { opacity: 0.85; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}

/* [FIX]: Logo电路连接线流动效果 */
.brand-logo .logo-circuit {
  animation: circuitFlow 4s ease-in-out infinite;
}
@keyframes circuitFlow {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.6; }
}
/* [FIX]: Logo角标样式 */
.logo-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  padding: 2px 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #c2410c;
  background: linear-gradient(135deg, #fff 0%, #fef3c7 100%);
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(194,65,12,0.25);
  animation: badgePulse 2s ease-in-out infinite;
}
@keyframes badgePulse {
  0%,100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.brand-title { font-size: 24px; font-weight: 700; margin: 0 0 8px; letter-spacing: -0.5px; }
.brand-desc { font-size: 13px; line-height: 1.7; opacity: 0.85; margin: 0 0 24px; }

/* [FIX]: 注册步骤指示器 */
.brand-steps {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 24px;
  padding: 14px 16px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.15);
}
.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.step-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  color: rgba(255,255,255,0.7);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .3s ease;
}
.step-text {
  font-size: 11px;
  color: rgba(255,255,255,0.7);
  font-weight: 500;
  white-space: nowrap;
  transition: all .3s ease;
}
.step-item.active .step-num {
  background: linear-gradient(135deg, #fff 0%, #fef3c7 100%);
  color: #c2410c;
  box-shadow: 0 2px 8px rgba(255,200,150,0.4);
}
.step-item.active .step-text {
  color: #fff;
  font-weight: 600;
}
.step-connector {
  flex: 1;
  height: 1.5px;
  background: rgba(255,255,255,0.15);
  min-width: 16px;
  margin: 0 8px;
}

/* [FIX]: 功能列表样式与Login.vue保持一致 */
.brand-features { display: flex; flex-direction: column; gap: 14px; }
.brand-feature { display: flex; align-items: center; gap: 12px; }
.feature-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.feature-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}
.feature-desc {
  font-size: 12px;
  opacity: 0.78;
  color: #fff;
}

/* [FIX]: 纯CSS实现的3D玻璃质感堆叠方块 - 与Login.vue完全一致 */
.brand-illustration {
  position: absolute;
  bottom: -20px;
  right: -30px;
  width: 200px;
  height: 180px;
  pointer-events: none;
  z-index: 1;
}
.stack-stack {
  position: absolute;
  bottom: 10px;
  right: 30px;
  transform: perspective(500px) rotateX(55deg) rotateZ(-35deg);
  transform-style: preserve-3d;
}
.stack-layer {
  position: absolute;
  width: 80px;
  height: 80px;
  border-radius: 12px;
  backface-visibility: hidden;
}
.stack-layer-1 {
  background: linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(255,240,220,0.9) 100%);
  box-shadow: 0 8px 32px rgba(194,65,12,0.25), inset 0 1px 0 rgba(255,255,255,0.8);
  transform: translateZ(0);
}
.stack-layer-2 {
  background: linear-gradient(135deg, rgba(255,245,230,0.9) 0%, rgba(255,215,170,0.85) 100%);
  box-shadow: 0 6px 24px rgba(194,65,12,0.2), inset 0 1px 0 rgba(255,255,255,0.7);
  transform: translateZ(18px) translate(-4px, -4px);
}
.stack-layer-3 {
  background: linear-gradient(135deg, rgba(255,220,180,0.85) 0%, rgba(245,158,11,0.7) 100%);
  box-shadow: 0 4px 20px rgba(194,65,12,0.18), inset 0 1px 0 rgba(255,255,255,0.6);
  transform: translateZ(36px) translate(-8px, -8px);
}
.stack-layer-4 {
  background: linear-gradient(135deg, rgba(245,158,11,0.75) 0%, rgba(217,119,6,0.65) 100%);
  box-shadow: 0 3px 16px rgba(154,52,18,0.25), inset 0 1px 0 rgba(255,255,255,0.5);
  transform: translateZ(54px) translate(-12px, -12px);
}
.stack-glow {
  position: absolute;
  bottom: -10px;
  right: 20px;
  width: 140px;
  height: 40px;
  background: radial-gradient(ellipse, rgba(255,200,150,0.4) 0%, transparent 70%);
  filter: blur(8px);
  border-radius: 50%;
}
.stack-ring {
  position: absolute;
  border: 1.5px solid rgba(255,255,255,0.2);
  border-radius: 50%;
}
.stack-ring-1 {
  width: 120px;
  height: 120px;
  bottom: -20px;
  right: 10px;
}
.stack-ring-2 {
  width: 160px;
  height: 160px;
  bottom: -40px;
  right: -10px;
  border-color: rgba(255,255,255,0.1);
}
/* [FIX]: 新增第3个装饰圆环 */
.stack-ring-3 {
  width: 200px;
  height: 200px;
  bottom: -60px;
  right: -30px;
  border-color: rgba(255,255,255,0.06);
}

/* [FIX]: 压缩表单区域高度 - 减小padding和间距使布局更紧凑 */
.auth-card-wrap { flex: 1; min-width: 320px; display: flex; align-items: center; justify-content: center; padding: 32px 36px; }
.auth-card { width: 100%; max-width: 400px; }
/* [FIX]: 压缩header间距（原24px） */
.auth-header { margin-bottom: 18px; }
.auth-title { font-size: 22px; font-weight: 700; color: #1c1917; margin: 0 0 6px; }
.auth-desc { font-size: 14px; color: #8c847c; margin: 0; }

/* [FIX]: 压缩表单项间距（原16px） */
.auth-form { display: flex; flex-direction: column; gap: 12px; }
.form-group { display: flex; flex-direction: column; gap: 5px; position: relative; }
/* [FIX]: 压缩行间距 */
.form-row { display: flex; gap: 10px; align-items: flex-end; }
.flex-1 { flex: 1; }
.form-label { font-size: 13px; font-weight: 600; color: #1c1917; }

.input-wrap {
  display: flex; align-items: center; gap: 10px; padding: 0 14px; height: 48px;
  background: #fdf8f3; border: 1.5px solid #e7e0d8; border-radius: 12px;
  transition: all .2s; position: relative;
}
.input-wrap.focus { border-color: #c2410c; box-shadow: 0 0 0 3px rgba(194,65,12,0.08); background: #fff; }
.input-wrap.filled { background: #fff; }
.input-wrap.error { border-color: #ef4444; }
.input-icon { color: #8c847c; flex-shrink: 0; }
.input-wrap.focus .input-icon { color: #c2410c; }
.input-wrap input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: #1c1917; height: 100%; }
.input-wrap input::placeholder { color: #a8a29e; }
.input-err { position: absolute; right: 14px; font-size: 11px; color: #ef4444; white-space: nowrap; }
.pwd-err { font-size: 11px; color: #ef4444; padding-left: 4px; }

.toggle-pwd { background: none; border: none; color: #8c847c; cursor: pointer; padding: 6px; display: flex; border-radius: 6px; flex-shrink: 0; }
.toggle-pwd:hover { background: #ede7e0; color: #c2410c; }

.email-suggest {
  position: absolute; top: 100%; left: 0; right: 0;
  background: #fff; border: 1px solid #e7e0d8; border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06); z-index: 20; overflow: hidden;
}
.suggest-item { padding: 8px 14px; font-size: 13px; color: #1c1917; cursor: pointer; transition: background .1s; }
.suggest-item:hover { background: #ede7e0; color: #c2410c; }

.code-btn-wrap { flex-shrink: 0; padding-bottom: 1px; }
.code-btn {
  height: 48px; padding: 0 18px; border: 1.5px solid #c2410c; border-radius: 12px;
  background: #fff; color: #c2410c; font-size: 13px; font-weight: 600; cursor: pointer;
  white-space: nowrap; transition: all .15s;
}
.code-btn:hover:not(:disabled) { background: #c2410c; color: #fff; }
.code-btn:disabled { opacity: 0.5; cursor: not-allowed; border-color: #e7e0d8; color: #8c847c; }

.auth-btn {
  width: 100%; height: 48px; border: none; border-radius: 12px;
  background: linear-gradient(135deg, #c2410c, #d97706);
  color: #fff; font-size: 15px; font-weight: 600; cursor: pointer;
  transition: opacity .2s, transform .1s;
  display: flex; align-items: center; justify-content: center;
}
.auth-btn:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); }
.auth-btn:active:not(:disabled) { transform: translateY(0); }
.auth-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-loading { display: flex; align-items: center; gap: 8px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* [FIX]: 压缩footer间距（原22px） */
.auth-footer { text-align: center; margin-top: 16px; font-size: 13px; color: #8c847c; }
.auth-link { color: #c2410c; font-weight: 600; text-decoration: none; }
.auth-link:hover { text-decoration: underline; }

@media (max-width: 720px) {
  .auth-container { flex-direction: column; max-width: 480px; }
  .auth-brand { width: 100%; min-width: 0; padding: 28px 24px; }
  /* [FIX]: 移动端隐藏新增的装饰元素 */
  .brand-accent-line { display: none; }
  .brand-steps { display: none; }
  .logo-badge { display: none; }
  .deco-circle-4 { display: none; }
  .deco-particles { display: none; }
  .stack-ring-3 { display: none; }
  
  .brand-title { font-size: 22px; }
  .brand-desc { margin-bottom: 20px; }
  /* [FIX]: 移动端隐藏功能列表、3D插图和装饰光晕 */
  .brand-features { display: none; }
  .brand-illustration { display: none; }
  .brand-decoration { display: none; }
  .auth-card-wrap { padding: 24px 20px; }
  .form-row { flex-direction: column; gap: 12px; }
  .code-btn-wrap { width: 100%; }
  .code-btn { width: 100%; justify-content: center; display: flex; }
}
</style>
