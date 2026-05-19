<template>
  <div class="app-root">
    <!-- 登录/注册页：无顶部栏，全屏展示 -->
    <template v-if="isAuthPage">
      <router-view />
    </template>

    <!-- 主应用：带顶部栏 + 炫酷过渡 -->
    <template v-else>
      <div class="app-layout">
        <!-- 顶部导航栏 -->
        <header class="topbar">
          <div class="topbar-left">
            <!-- 移动端侧栏切换按钮 -->
            <button v-if="!isHomePage" class="mobile-menu-btn" @click="toggleSidebar" title="展开/收起侧栏">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            </button>
            <div class="topbar-logo">
              <div class="logo-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                  <line x1="8" y1="7" x2="16" y2="7"/><line x1="8" y1="11" x2="14" y2="11"/>
                </svg>
              </div>
              <span class="topbar-brand">AI 知识助手</span>
            </div>
          </div>

          <div class="topbar-center">
            <!-- AI 助手 / 控制台 切换 -->
            <button
              class="nav-btn"
              :class="{ active: isHomePage }"
              @click="goTo('Home')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              <span>AI 助手</span>
            </button>
            <button
              class="nav-btn"
              :class="{ active: !isHomePage }"
              @click="goTo('KnowledgeList')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><line x1="8" y1="7" x2="16" y2="7"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
              <span>控制台</span>
            </button>
          </div>
        </header>

        <!-- 主体区域：知识库页面带侧栏，AI页面全屏 -->
        <div class="app-body">
          <el-container class="body-container">
            <!-- 移动端侧栏遮罩 -->
            <div v-if="!isHomePage && isMobile && !sidebarCollapsed" class="sidebar-backdrop" @click="sidebarCollapsed = true" />

            <!-- 知识库侧栏（仅控制台页面显示） -->
            <el-aside v-if="!isHomePage" width="240px" class="app-sidebar" :class="{ collapsed: sidebarCollapsed, 'sidebar-mobile': isMobile }">
              <!-- 折叠按钮 -->
              <button class="app-collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧栏' : '折叠侧栏'">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <polyline :points="sidebarCollapsed ? '9 18 15 12 9 6' : '15 18 9 12 15 6'"/>
                </svg>
              </button>

              <!-- 展开态 -->
              <div class="sidebar-expanded">
                <div class="sidebar-inner">
                  <div class="sidebar-search">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                    <input v-model="searchKeyword" placeholder="搜索知识..." @keyup.enter="handleSearch" />
                  </div>
                  <CategoryTree />
                </div>
                <div class="sidebar-foot">
                  <div class="user-info" @click="openUserProfile" title="编辑个人资料">
                    <div class="user-avatar">
                      <!-- [FIX]: 使用 proxyAvatarUrl 代理外部CDN头像，解决 ERR_CONNECTION_RESET -->
                      <img v-if="user.avatar" :src="proxyAvatarUrl(user.avatar)" class="user-avatar-img" referrerpolicy="no-referrer" @error="onAvatarError" />
                      <span v-else class="user-avatar-text">{{ (user.nickname || '用户').charAt(0).toUpperCase() }}</span>
                    </div>
                    <span class="user-name">{{ user.nickname || '用户' }}</span>
                  </div>
                  <AiSettings />
                </div>
              </div>

              <!-- 折叠态 -->
              <div class="sidebar-collapsed-view">
                <div class="collapsed-icons">
                  <button class="collapsed-icon-btn" @click="goTo('KnowledgeList')" title="全部知识">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                      <line x1="8" y1="7" x2="16" y2="7"/>
                      <line x1="8" y1="11" x2="14" y2="11"/>
                    </svg>
                  </button>
                  <button class="collapsed-icon-btn" @click="goTo('CategoryManage')" title="分类管理">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                      <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                    </svg>
                  </button>
                </div>
                <div class="sidebar-foot collapsed-foot">
                  <div class="user-info" @click="openUserProfile" title="编辑个人资料">
                    <div class="user-avatar">
                      <!-- [FIX]: 使用 proxyAvatarUrl 代理外部CDN头像，解决 ERR_CONNECTION_RESET -->
                      <img v-if="user.avatar" :src="proxyAvatarUrl(user.avatar)" class="user-avatar-img" referrerpolicy="no-referrer" @error="onAvatarError" />
                      <span v-else class="user-avatar-text">{{ (user.nickname || '用户').charAt(0).toUpperCase() }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-aside>

            <el-main class="app-content" :class="{ full: isHomePage, 'content-shifted': !isHomePage && !isMobile && !sidebarCollapsed }">
              <!-- 炫酷页面过渡 -->
              <router-view v-slot="{ Component }">
                <transition name="portal" mode="out-in">
                  <component :is="Component" :key="$route.path" />
                </transition>
              </router-view>
            </el-main>
          </el-container>
        </div>
      </div>
      <UserProfile ref="userProfileRef" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import CategoryTree from './components/CategoryTree.vue'
import AiSettings from './components/AiSettings.vue'
import UserProfile from './components/UserProfile.vue'
import { useKnowledgeStore } from './stores/knowledge'
import { useUserStore } from './stores/user'
import { proxyAvatarUrl, onAvatarError } from './composables/useAvatarProxy'


const router = useRouter()
const route = useRoute()
const knowledgeStore = useKnowledgeStore()
const user = useUserStore()

const searchKeyword = ref('')

const isHomePage = computed(() => route.name === 'Home')
const isAuthPage = computed(() => ['Login', 'Register'].includes(route.name))

// ========== 侧栏响应式 ==========
const MOBILE_BREAKPOINT = 768
const isMobile = ref(window.innerWidth <= MOBILE_BREAKPOINT)
const sidebarCollapsed = ref(isMobile.value) // 移动端默认折叠，PC端默认展开

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function handleResize() {
  const mobile = window.innerWidth <= MOBILE_BREAKPOINT
  if (mobile !== isMobile.value) {
    isMobile.value = mobile
    // 切换断点时自动设置折叠状态
    sidebarCollapsed.value = mobile
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

function goTo(name) {
  if (route.name === name) return
  router.push({ name })
}

const handleSearch = () => {
  knowledgeStore.setFilter({ keyword: searchKeyword.value })
  router.push({ name: 'KnowledgeList' })
}
const userProfileRef = ref(null)
function openUserProfile() {
  userProfileRef.value?.open()
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; width: 100%; }

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  background: #faf8f5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-size: 14px;
}

/* ========== 根容器 ========== */
.app-root { height: 100%; width: 100%; }

/* ========== 主应用布局 ========== */
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ========== 顶部栏 ========== */
.topbar {
  height: 48px;
  min-height: 48px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(0,0,0,.06);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 16px;
  z-index: 200;
  box-shadow: 0 1px 3px rgba(0,0,0,.03);
}

.topbar-left { flex-shrink: 0; }
.topbar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo-icon {
  width: 26px; height: 26px; border-radius: 7px;
  background: linear-gradient(135deg, #c2410c, #d97706);
  display: flex; align-items: center; justify-content: center; color: white;
  box-shadow: 0 2px 6px rgba(194,65,12,.2);
}
.topbar-brand {
  font-size: 14px; font-weight: 700; color: #92400e;
  letter-spacing: -0.2px;
}

.topbar-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  background: rgba(0,0,0,.04);
  padding: 2px;
  border-radius: 8px;
  max-width: 240px;
  margin: 0 auto;
}

.nav-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #57534e;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all .2s;
}
.nav-btn.active {
  background: #fff;
  color: #c2410c;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
}
.nav-btn:not(.active):hover { color: #1c1917; }

/* ========== 顶部栏右区 ========== */
.topbar-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

/* ========== 移动端菜单按钮 ========== */
.mobile-menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #57534e;
  cursor: pointer;
  transition: all .15s;
  flex-shrink: 0;
}
.mobile-menu-btn:hover { background: rgba(0,0,0,.05); color: #1c1917; }

/* ========== 主体区域 ========== */
.app-body { flex: 1; overflow: hidden; }
.body-container { height: 100%; }

/* ========== 知识库侧栏 ========== */
.app-sidebar {
  background: #f5f0eb;
  border-right: 1px solid #e7e0d8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  position: relative;
  transition: width .25s ease;
}
.app-sidebar.collapsed:not(.sidebar-mobile) {
  width: 52px !important;
  min-width: 52px !important;
}

/* 折叠按钮 - 仿 AI 助手样式 */
.app-collapse-btn {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px; height: 40px;
  border-radius: 0 6px 6px 0;
  background: #f5f0eb;
  border: 1px solid rgba(0,0,0,.06);
  border-left: none;
  color: #8c847c;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color .15s, background .15s;
}
.app-collapse-btn:hover { color: #c2410c; background: #ede7e0; }

/* 展开态/折叠态内容显示控制 */
.app-sidebar.collapsed .sidebar-expanded { display: none; }
.app-sidebar:not(.collapsed) .sidebar-collapsed-view { display: none; }

/* 展开态容器 */
.sidebar-expanded { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* 折叠态视图 - 图标导航 */
.sidebar-collapsed-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.collapsed-icons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 0;
  flex-shrink: 0;
}
.collapsed-icon-btn {
  width: 36px; height: 36px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #57534e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;
}
.collapsed-icon-btn:hover { background: rgba(0,0,0,.06); color: #c2410c; }
.collapsed-foot { margin-top: auto; border-top: none; justify-content: center; padding: 8px 0; }
.collapsed-foot .user-info { padding: 0; }
.collapsed-foot .user-name { display: none; }

/* ========== 侧栏移动端遮罩 ========== */
.sidebar-backdrop {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,.35);
  z-index: 299;
  animation: fadeIn .2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ========== 内容区偏移（PC侧栏展开） ========== */
.content-shifted {
  margin-left: 0;
}
.sidebar-inner { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.sidebar-search {
  display: flex; align-items: center; gap: 8px;
  margin: 12px 12px 8px;
  padding: 7px 12px;
  background: #fdf8f3;
  border: 1px solid #e7e0d8;
  border-radius: 8px;
  color: #8c847c;
}
.sidebar-search:focus-within { border-color: #d97706; }
.sidebar-search input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 13px; color: #1c1917;
}
.sidebar-search input::placeholder { color: #8c847c; }

.sidebar-foot {
  border-top: 1px solid #e7e0d8;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  flex-shrink: 0;
}
.sidebar-foot .user-info {
  display: flex; align-items: center; gap: 7px;
  cursor: pointer; border-radius: 8px;
  padding: 3px 6px 3px 3px;
  transition: background .15s;
}
.sidebar-foot .user-info:hover { background: rgba(0,0,0,.04); }
.sidebar-foot .user-avatar {
  width: 28px; height: 28px; border-radius: 50%; overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #c2410c, #d97706);
  display: flex; align-items: center; justify-content: center;
}
.sidebar-foot .user-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.sidebar-foot .user-avatar-text { color: #fff; font-size: 12px; font-weight: 700; line-height: 1; }
.sidebar-foot .user-name { font-size: 13px; font-weight: 600; color: #1c1917; }

/* ========== 内容区 ========== */
.app-content {
  padding: 0;
  height: 100%;
  overflow-y: auto;
  background: #faf8f5;
  position: relative;
}
.app-content.full { padding: 0; }
.app-content::-webkit-scrollbar { width: 6px; }
.app-content::-webkit-scrollbar-thumb { background: #d6cdc3; border-radius: 3px; }

/* ========== 普通页面切换 ========== */
.portal-enter-active,
.portal-leave-active {
  transition: none;
}

/* ========== 全局组件覆盖 ========== */
.el-dialog {
  border-radius: 12px !important;
  border: 1px solid #e7e0d8 !important;
  box-shadow: 0 20px 60px rgba(41,37,36,0.08) !important;
}
.el-dialog__title { color: #1c1917 !important; font-weight: 600 !important; }
.el-dialog__body .el-input__wrapper,
.el-dialog__body .el-textarea__inner {
  background: #fdf8f3 !important;
  border: 1px solid #e7e0d8 !important;
  box-shadow: none !important;
}
.el-dialog__body .el-input__wrapper:hover { border-color: #d6cdc3 !important; }
.el-dialog__body .el-input__wrapper.is-focus { border-color: #d97706 !important; }
.el-dialog__body .el-input__inner { color: #1c1917 !important; }
.el-dialog__body .el-textarea__inner { color: #1c1917 !important; }
.el-dialog__footer { border-top: 1px solid #ede7e0 !important; }
.el-message-box { border: 1px solid #e7e0d8 !important; border-radius: 12px !important; }
.el-message-box__title { color: #1c1917 !important; }
.el-message-box__message { color: #57534e !important; }
.el-button--primary {
  background: linear-gradient(135deg, #d97706, #b45309) !important;
  border: none !important;
  font-weight: 600 !important;
}
.el-button--primary:hover { opacity: 0.9 !important; }
.el-button.is-round { padding: 8px 22px !important; }

/* ======================================================
   移动端响应式适配 (<= 768px)
   ====================================================== */
@media (max-width: 768px) {
  .mobile-menu-btn { display: flex; }

  .topbar-left { display: flex; align-items: center; gap: 4px; }

  /* 移动端隐藏内部折叠按钮 */
  .app-collapse-btn { display: none; }
  .app-sidebar.sidebar-mobile {
    position: fixed;
    top: 0; left: 0;
    width: 260px !important;
    height: 100vh !important;
    z-index: 300;
    transform: translateX(-100%);
    transition: transform .25s ease;
    box-shadow: 4px 0 24px rgba(0,0,0,.12);
  }
  .app-sidebar.sidebar-mobile.collapsed {
    transform: translateX(-100%);
  }
  .app-sidebar.sidebar-mobile:not(.collapsed) {
    transform: translateX(0);
  }
  /* 移动端展开时始终显示展开内容 */
  .app-sidebar.sidebar-mobile.collapsed .sidebar-expanded { display: flex; }
}

/* 平板/小屏桌面适配 (768px ~ 1024px) */
@media (min-width: 769px) and (max-width: 1024px) {
  .app-sidebar:not(.collapsed) {
    width: 200px !important;
  }
}
</style>
