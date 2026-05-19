<template>
  <div class="chat-layout">
    <!-- 对话历史侧边栏 -->
    <aside class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- 折叠按钮 -->
      <button class="collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧栏' : '折叠侧栏'">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline :points="sidebarCollapsed ? '9 18 15 12 9 6' : '15 18 9 12 15 6'"/></svg>
      </button>

      <!-- 展开态内容 -->
      <div class="sidebar-expanded">
        <div class="sidebar-header">
          <button class="new-chat-btn" @click="newSession">+ 新建对话</button>
        </div>
        <div class="search-box">
          <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchKeyword" placeholder="搜索对话..." @input="onSearch" />
          <button v-if="searchKeyword" class="clear-btn" @click="searchKeyword='';loadSessions()">×</button>
        </div>
        <div class="tab-bar">
          <span :class="{ on: tab === 'active' }" @click="tab='active';loadSessions()">全部</span>
          <span :class="{ on: tab === 'archived' }" @click="tab='archived';loadSessions()">已归档</span>
        </div>
        <div class="session-list" ref="sessionListRef">
          <div v-for="s in sessions" :key="s.id"
            class="session-item"
            :class="{ active: currentSessionId === s.id }"
            @click="switchSession(s.id)">
            <div class="session-main">
              <span class="session-title">{{ s.title }}</span>
              <span class="session-time">{{ timeAgo(s.updatedAt) }}</span>
            </div>
            <div class="session-actions" @click.stop>
              <el-dropdown trigger="hover" :hide-timeout="100" placement="bottom-end">
                <button class="more-btn" @click.stop>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/></svg>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="renameSession(s)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></svg> 重命名</el-dropdown-item>
                    <el-dropdown-item v-if="tab==='active'" @click="doArchive(s.id,true)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="21 8 21 21 3 21 3 8"/><rect x="1" y="3" width="22" height="5"/><line x1="10" y1="12" x2="14" y2="12"/></svg> 归档</el-dropdown-item>
                    <el-dropdown-item v-else @click="doArchive(s.id,false)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="21 8 21 21 3 21 3 8"/><rect x="1" y="3" width="22" height="5"/><line x1="10" y1="12" x2="14" y2="12"/></svg> 取消归档</el-dropdown-item>
                    <el-dropdown-item divided @click="doDelete(s.id)" style="color:#ef4444"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg> 删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <div v-if="!loadingSessions && sessions.length === 0" class="empty-sessions">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#d6cdc3" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <span>{{ tab==='archived' ? '暂无归档对话' : '暂无对话，开始一个新对话吧' }}</span>
          </div>
          <div v-if="loadingSessions" class="empty-sessions" style="padding:20px">加载中...</div>
          <div v-if="loadingMore" class="load-more-tip">加载中...</div>
          <div v-if="!sessionHasMore && sessions.length > 0 && !loadingMore" class="load-more-tip no-more">已全部加载（共 {{ sessions.length }} 条）</div>
        </div>
        <div v-if="sessionHasMore && !loadingMore && sessions.length > 0" class="load-more-fixed" @click="loadMoreSessions">↓ 加载更多（{{ sessions.length }}/{{ totalSessions }}）</div>
        <div v-if="loadingMore" class="load-more-fixed loading">加载中...</div>
        <div class="sidebar-footer">
          <div class="user-info" @click="openUserProfile" title="编辑个人资料">
            <div class="user-avatar">
              <!-- [FIX]: 使用 proxyAvatarUrl 代理外部CDN头像，解决 ERR_CONNECTION_RESET -->
              <img v-if="userStore.avatar" :src="proxyAvatarUrl(userStore.avatar)" class="user-avatar-img" referrerpolicy="no-referrer" @error="onAvatarError" />
              <span v-else class="user-avatar-text">{{ (userStore.nickname || '用户').charAt(0).toUpperCase() }}</span>
            </div>
            <span class="user-name">{{ userStore.nickname || '用户' }}</span>
          </div>
          <AiSettings />
        </div>
      </div>

      <!-- 折叠态：始终在 DOM 中，CSS 控制显隐 -->
      <div class="sidebar-collapsed-view">
        <div class="collapsed-icons">
          <button class="collapsed-icon-btn" @click="newSession" title="新建对话">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </button>
          <button class="collapsed-icon-btn" @click="focusSearch" title="搜索对话">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </button>
        </div>
        <div class="sidebar-footer collapsed-footer">
          <div class="user-info" @click="openUserProfile" title="编辑个人资料">
            <div class="user-avatar">
              <!-- [FIX]: 使用 proxyAvatarUrl 代理外部CDN头像，解决 ERR_CONNECTION_RESET -->
              <img v-if="userStore.avatar" :src="proxyAvatarUrl(userStore.avatar)" class="user-avatar-img" referrerpolicy="no-referrer" @error="onAvatarError" />
              <span v-else class="user-avatar-text">{{ (userStore.nickname || '用户').charAt(0).toUpperCase() }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <main class="chat-main" @drop.prevent="onDrop" @dragover.prevent>
      <div class="messages-area" ref="messagesRef">
        <div class="welcome" :class="{ compact: messages.length > 0 }">
          <div class="welcome-icon" v-if="messages.length === 0">
            <!-- [FIX]: 替换五角星为 AI 神经网络核心图标，与登录页"神经之星"风格协调 -->
            <svg width="88" height="88" viewBox="0 0 48 48" fill="none">
              <defs>
                <!-- 外围光晕 -->
                <radialGradient id="wcAura" cx="50%" cy="50%" r="55%">
                  <stop offset="0%" stop-color="#fed7aa" stop-opacity="0.35"/>
                  <stop offset="60%" stop-color="#fdba74" stop-opacity="0.12"/>
                  <stop offset="100%" stop-color="#f97316" stop-opacity="0"/>
                </radialGradient>
                <!-- 主背景渐变：深橙到琥珀 -->
                <linearGradient id="wcBg" x1="6" y1="6" x2="42" y2="42" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#c2410c"/>
                  <stop offset="50%" stop-color="#b45309"/>
                  <stop offset="100%" stop-color="#d97706"/>
                </linearGradient>
                <!-- 能量核渐变：亮白到金黄 -->
                <radialGradient id="wcCore" cx="45%" cy="42%" r="55%">
                  <stop offset="0%" stop-color="#ffffff"/>
                  <stop offset="50%" stop-color="#fef9c3"/>
                  <stop offset="100%" stop-color="#fbbf24" stop-opacity="0.85"/>
                </radialGradient>
                <!-- 神经元节点渐变：冰蓝科技色 -->
                <radialGradient id="wcNode" cx="45%" cy="35%" r="65%">
                  <stop offset="0%" stop-color="#ffffff"/>
                  <stop offset="55%" stop-color="#a5f3fc"/>
                  <stop offset="100%" stop-color="#67e8f9" stop-opacity="0.55"/>
                </radialGradient>
                <!-- 大脑轮廓渐变 -->
                <linearGradient id="wcBrain" x1="8%" y1="95%" x2="92%" y2="5%">
                  <stop offset="0%" stop-color="rgba(251,191,36,0.92)"/>
                  <stop offset="50%" stop-color="rgba(255,245,220,0.96)"/>
                  <stop offset="100%" stop-color="rgba(251,191,36,0.86)"/>
                </linearGradient>
                <!-- 发光滤镜 - 轻量级 -->
                <filter id="wcGlow" x="-40%" y="-40%" width="180%" height="180%">
                  <feGaussianBlur stdDeviation="1.5" result="blur"/>
                  <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
                </filter>
                <!-- 强发光滤镜 - 核心用 -->
                <filter id="wcGlowStrong" x="-70%" y="-70%" width="240%" height="240%">
                  <feGaussianBlur stdDeviation="2.2" result="blur1"/>
                  <feGaussianBlur stdDeviation="0.9" result="blur2"/>
                  <feMerge>
                    <feMergeNode in="blur1"/>
                    <feMergeNode in="blur2"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              </defs>

              <!-- 0) 最外层光晕 -->
              <circle cx="24" cy="24" r="24" fill="url(#wcAura)"/>

              <!-- 1) 主圆形背景 -->
              <circle cx="24" cy="24" r="21.5" fill="url(#wcBg)" stroke="rgba(255,255,255,0.12)" stroke-width="0.6"/>

              <!-- 2) 外围轨道粒子环 -->
              <g opacity="0.38">
                <circle cx="24" cy="3.5" r="1.15" fill="#fff"/>
                <circle cx="39" cy="9.5" r="0.85" fill="#fef3c7"/>
                <circle cx="44" cy="24" r="1.05" fill="#fff"/>
                <circle cx="38" cy="37.5" r="0.78" fill="#a5f3fc"/>
                <circle cx="24" cy="43.5" r="1.15" fill="#fef3c7"/>
                <circle cx="10" cy="37.5" r="0.85" fill="#fff"/>
                <circle cx="4" cy="24" r="0.95" fill="#a5f3fc"/>
                <circle cx="9" cy="10" r="0.78" fill="#fef3c7"/>
              </g>

              <!-- 3) 大脑神经网络轮廓 -->
              <g filter="url(#wcGlow)">
                <!-- 外层主弧线 -->
                <path d="M9 29 Q9 14.5, 18.5 11.5 Q24 9.5, 29.5 11.5 Q39 14.5, 39 29"
                      stroke="url(#wcBrain)" stroke-width="2.1" fill="none" stroke-linecap="round" opacity="0.94"/>
                <!-- 左半球褶皱弧线 -->
                <path d="M12 27 Q12 17, 18.5 14.5"
                      stroke="url(#wcBrain)" stroke-width="1.3" fill="none" stroke-linecap="round" opacity="0.68"/>
                <!-- 右半球褶皱弧线 -->
                <path d="M36 27 Q36 17, 29.5 14.5"
                      stroke="url(#wcBrain)" stroke-width="1.3" fill="none" stroke-linecap="round" opacity="0.68"/>
                <!-- 内部横贯线左 -->
                <path d="M13 23 Q17 20, 21 22"
                      stroke="url(#wcBrain)" stroke-width="0.95" fill="none" stroke-linecap="round" opacity="0.48"/>
                <!-- 内部横贯线右 -->
                <path d="M35 23 Q31 20, 27 22"
                      stroke="url(#wcBrain)" stroke-width="0.95" fill="none" stroke-linecap="round" opacity="0.48"/>
                <!-- 中央分界线 -->
                <path d="M24 14 L24 28"
                      stroke="url(#wcBrain)" stroke-width="0.95" fill="none" stroke-linecap="round" opacity="0.52"/>
              </g>

              <!-- 4) 神经元节点网络 + 连接线 -->
              <g filter="url(#wcGlow)">
                <!-- ===== 核心能量核 ===== -->
                <circle cx="24" cy="23" r="4.8" fill="url(#wcCore)" filter="url(#wcGlowStrong)"/>
                <circle cx="24" cy="23" r="2.1" fill="#ffffff" opacity="0.96"/>

                <!-- ===== 左侧神经元 ===== -->
                <!-- 左上节点 -->
                <circle cx="15.5" cy="15.5" r="2.3" fill="url(#wcNode)" opacity="0.92"/>
                <circle cx="15.5" cy="15.5" r="1.05" fill="#fff" opacity="0.82"/>
                <line x1="17.5" y1="17" x2="20.5" y2="20.5" stroke="rgba(165,243,252,0.48)" stroke-width="0.82"/>
                <!-- 左中节点 -->
                <circle cx="11.5" cy="23" r="1.9" fill="url(#wcNode)" opacity="0.86"/>
                <circle cx="11.5" cy="23" r="0.82" fill="#fff" opacity="0.72"/>
                <line x1="13.4" y1="23" x2="19.2" y2="23" stroke="rgba(165,243,252,0.43)" stroke-width="0.72"/>
                <!-- 左下节点 -->
                <circle cx="14.5" cy="30.5" r="1.58" fill="url(#wcNode)" opacity="0.76"/>
                <circle cx="14.5" cy="30.5" r="0.7" fill="#fff" opacity="0.64"/>
                <line x1="16.3" y1="29.5" x2="20.3" y2="25.5" stroke="rgba(165,243,252,0.38)" stroke-width="0.62"/>
                <!-- 左外节点 -->
                <circle cx="8.5" cy="27.5" r="1.08" fill="url(#wcNode)" opacity="0.56"/>
                <line x1="9.7" y1="27" x2="10.7" y2="25" stroke="rgba(165,243,252,0.3)" stroke-width="0.5"/>

                <!-- ===== 右侧神经元 ===== -->
                <!-- 右上节点 -->
                <circle cx="32.5" cy="15.5" r="2.3" fill="url(#wcNode)" opacity="0.92"/>
                <circle cx="32.5" cy="15.5" r="1.05" fill="#fff" opacity="0.82"/>
                <line x1="30.5" y1="17" x2="27.5" y2="20.5" stroke="rgba(165,243,252,0.48)" stroke-width="0.82"/>
                <!-- 右中节点 -->
                <circle cx="36.5" cy="23" r="1.9" fill="url(#wcNode)" opacity="0.86"/>
                <circle cx="36.5" cy="23" r="0.82" fill="#fff" opacity="0.72"/>
                <line x1="34.6" y1="23" x2="28.8" y2="23" stroke="rgba(165,243,252,0.43)" stroke-width="0.72"/>
                <!-- 右下节点 -->
                <circle cx="33.5" cy="30.5" r="1.58" fill="url(#wcNode)" opacity="0.76"/>
                <circle cx="33.5" cy="30.5" r="0.7" fill="#fff" opacity="0.64"/>
                <line x1="31.7" y1="29.5" x2="27.7" y2="25.5" stroke="rgba(165,243,252,0.38)" stroke-width="0.62"/>
                <!-- 右外节点 -->
                <circle cx="39.5" cy="27.5" r="1.08" fill="url(#wcNode)" opacity="0.56"/>
                <line x1="38.3" y1="27" x2="37.3" y2="25" stroke="rgba(165,243,252,0.3)" stroke-width="0.5"/>
              </g>

              <!-- 5) 顶部高光反射 -->
              <ellipse cx="18" cy="16" rx="6.5" ry="4" fill="#ffffff" opacity="0.07"/>
            </svg>
          </div>
          <h2 class="welcome-title" :class="{ sm: messages.length > 0 }">AI 知识助手</h2>
          <p class="welcome-desc" v-if="messages.length === 0">基于知识库的智能问答系统，帮你快速获取答案</p>
          <div class="quick-cards" v-if="messages.length === 0">
            <div class="quick-card" @click="quickQuestion('帮我总结知识库的核心功能')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#c2410c" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span>总结知识库功能</span>
            </div>
            <div class="quick-card" @click="quickQuestion('如何组织和管理知识条目？')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#c2410c" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/></svg>
              <span>如何管理知识</span>
            </div>
            <div class="quick-card" @click="quickQuestion('最新的人工智能技术趋势有哪些？')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#c2410c" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
              <span>AI 技术趋势</span>
            </div>
          </div>
        </div>

        <!-- [FIX]: 已移除 AI 知识助手跑马灯状态栏（Aurora Marquee），用户不再需要此加载指示器 -->

        <div v-for="(msg, i) in messages" :key="msg.id || i" class="msg-row" :class="msg.role">
          <div class="msg-avatar" :class="msg.role">
            <div v-if="msg.role==='assistant'" class="ai-avatar">
              <!-- [FIX]: 替换五角星为 AI 神经网络核心图标，与欢迎区图标风格一致 -->
              <svg width="24" height="24" viewBox="0 0 48 48" fill="none">
                <defs>
                  <!-- 主背景渐变 -->
                  <linearGradient id="aiBg" x1="6" y1="6" x2="42" y2="42" gradientUnits="userSpaceOnUse">
                    <stop offset="0%" stop-color="#c2410c"/>
                    <stop offset="50%" stop-color="#b45309"/>
                    <stop offset="100%" stop-color="#d97706"/>
                  </linearGradient>
                  <!-- 能量核渐变 -->
                  <radialGradient id="aiCore" cx="45%" cy="42%" r="55%">
                    <stop offset="0%" stop-color="#ffffff"/>
                    <stop offset="50%" stop-color="#fef9c3"/>
                    <stop offset="100%" stop-color="#fbbf24" stop-opacity="0.85"/>
                  </radialGradient>
                  <!-- 神经元节点渐变 -->
                  <radialGradient id="aiNode" cx="45%" cy="35%" r="65%">
                    <stop offset="0%" stop-color="#ffffff"/>
                    <stop offset="55%" stop-color="#a5f3fc"/>
                    <stop offset="100%" stop-color="#67e8f9" stop-opacity="0.55"/>
                  </radialGradient>
                  <!-- 大脑轮廓渐变 -->
                  <linearGradient id="aiBrain" x1="8%" y1="95%" x2="92%" y2="5%">
                    <stop offset="0%" stop-color="rgba(251,191,36,0.92)"/>
                    <stop offset="50%" stop-color="rgba(255,245,220,0.96)"/>
                    <stop offset="100%" stop-color="rgba(251,191,36,0.86)"/>
                  </linearGradient>
                  <!-- 发光滤镜 -->
                  <filter id="aiGlow" x="-40%" y="-40%" width="180%" height="180%">
                    <feGaussianBlur stdDeviation="1.2" result="blur"/>
                    <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
                  </filter>
                  <filter id="aiGlowStrong" x="-60%" y="-60%" width="220%" height="220%">
                    <feGaussianBlur stdDeviation="1.8" result="blur1"/>
                    <feGaussianBlur stdDeviation="0.7" result="blur2"/>
                    <feMerge><feMergeNode in="blur1"/><feMergeNode in="blur2"/><feMergeNode in="SourceGraphic"/></feMerge>
                  </filter>
                </defs>

                <!-- 主圆形背景 -->
                <circle cx="24" cy="24" r="22" fill="url(#aiBg)"/>

                <!-- 大脑神经网络轮廓 -->
                <g filter="url(#aiGlow)">
                  <!-- 外层主弧线 -->
                  <path d="M9 29 Q9 14.5, 18.5 11.5 Q24 9.5, 29.5 11.5 Q39 14.5, 39 29"
                        stroke="url(#aiBrain)" stroke-width="2.1" fill="none" stroke-linecap="round" opacity="0.94"/>
                  <!-- 左半球褶皱弧线 -->
                  <path d="M12 27 Q12 17, 18.5 14.5"
                        stroke="url(#aiBrain)" stroke-width="1.3" fill="none" stroke-linecap="round" opacity="0.68"/>
                  <!-- 右半球褶皱弧线 -->
                  <path d="M36 27 Q36 17, 29.5 14.5"
                        stroke="url(#aiBrain)" stroke-width="1.3" fill="none" stroke-linecap="round" opacity="0.68"/>
                  <!-- 内部横贯线左 -->
                  <path d="M13 23 Q17 20, 21 22"
                        stroke="url(#aiBrain)" stroke-width="0.95" fill="none" stroke-linecap="round" opacity="0.48"/>
                  <!-- 内部横贯线右 -->
                  <path d="M35 23 Q31 20, 27 22"
                        stroke="url(#aiBrain)" stroke-width="0.95" fill="none" stroke-linecap="round" opacity="0.48"/>
                  <!-- 中央分界线 -->
                  <path d="M24 14 L24 28"
                        stroke="url(#aiBrain)" stroke-width="0.95" fill="none" stroke-linecap="round" opacity="0.52"/>
                </g>

                <!-- 神经元节点网络 -->
                <g filter="url(#aiGlow)">
                  <!-- 核心能量核 -->
                  <circle cx="24" cy="23" r="4.8" fill="url(#aiCore)" filter="url(#aiGlowStrong)"/>
                  <circle cx="24" cy="23" r="2.1" fill="#ffffff" opacity="0.96"/>

                  <!-- 左侧神经元 -->
                  <circle cx="15.5" cy="15.5" r="2.3" fill="url(#aiNode)" opacity="0.92"/>
                  <circle cx="15.5" cy="15.5" r="1.05" fill="#fff" opacity="0.82"/>
                  <line x1="17.5" y1="17" x2="20.5" y2="20.5" stroke="rgba(165,243,252,0.48)" stroke-width="0.82"/>

                  <circle cx="11.5" cy="23" r="1.9" fill="url(#aiNode)" opacity="0.86"/>
                  <circle cx="11.5" cy="23" r="0.82" fill="#fff" opacity="0.72"/>
                  <line x1="13.4" y1="23" x2="19.2" y2="23" stroke="rgba(165,243,252,0.43)" stroke-width="0.72"/>

                  <circle cx="14.5" cy="30.5" r="1.58" fill="url(#aiNode)" opacity="0.76"/>
                  <circle cx="14.5" cy="30.5" r="0.7" fill="#fff" opacity="0.64"/>
                  <line x1="16.3" y1="29.5" x2="20.3" y2="25.5" stroke="rgba(165,243,252,0.38)" stroke-width="0.62"/>

                  <!-- 右侧神经元 -->
                  <circle cx="32.5" cy="15.5" r="2.3" fill="url(#aiNode)" opacity="0.92"/>
                  <circle cx="32.5" cy="15.5" r="1.05" fill="#fff" opacity="0.82"/>
                  <line x1="30.5" y1="17" x2="27.5" y2="20.5" stroke="rgba(165,243,252,0.48)" stroke-width="0.82"/>

                  <circle cx="36.5" cy="23" r="1.9" fill="url(#aiNode)" opacity="0.86"/>
                  <circle cx="36.5" cy="23" r="0.82" fill="#fff" opacity="0.72"/>
                  <line x1="34.6" y1="23" x2="28.8" y2="23" stroke="rgba(165,243,252,0.43)" stroke-width="0.72"/>

                  <circle cx="33.5" cy="30.5" r="1.58" fill="url(#aiNode)" opacity="0.76"/>
                  <circle cx="33.5" cy="30.5" r="0.7" fill="#fff" opacity="0.64"/>
                  <line x1="31.7" y1="29.5" x2="27.7" y2="25.5" stroke="rgba(165,243,252,0.38)" stroke-width="0.62"/>
                </g>
              </svg>
            </div>
            <!-- [FIX]: 用户头像 — 使用 userStore.avatar 显示真实头像，而非通用SVG占位符 -->
            <div v-else class="user-msg-avatar">
              <img v-if="userStore.avatar" :src="proxyAvatarUrl(userStore.avatar)" class="user-msg-avatar-img" referrerpolicy="no-referrer" @error="onAvatarError" />
              <span v-else class="user-msg-avatar-text">{{ (userStore.nickname || '用户').charAt(0).toUpperCase() }}</span>
            </div>
          </div>
            <div class="msg-content">
            <div class="msg-time" v-if="msg.createdAt || msg._localTime">{{ formatMsgTime(msg.createdAt || msg._localTime) }}</div>
            <div v-if="msg.imageUrls" class="msg-images">
              <img v-for="(url,j) in parseImages(msg.imageUrls)" :key="j" :src="url" class="msg-img" @click="previewImage(url)" />
            </div>
            <!-- [FIX]: 附件列表 -->
            <div v-if="msg.attachmentIds" class="msg-attachments">
              <div v-for="attId in parseAttachmentIds(msg.attachmentIds)" :key="attId" class="msg-attachment-item" @click="previewFile(attId)">
                <div class="att-icon" :class="fileIcon(attachmentCache[attId]?.fileName)">
                  <svg v-if="fileIcon(attachmentCache[attId]?.fileName)==='pdf'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                  <svg v-else-if="fileIcon(attachmentCache[attId]?.fileName)==='word'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                  <svg v-else-if="fileIcon(attachmentCache[attId]?.fileName)==='excel'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><rect x="8" y="12" width="8" height="6" rx="1"/></svg>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#c2410c" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                </div>
                <div class="att-info">
                  <span class="att-name">{{ attachmentCache[attId]?.fileName || '附件 #' + attId }}</span>
                  <span class="att-size" v-if="attachmentCache[attId]?.fileSize">{{ fmtSize(attachmentCache[attId].fileSize) }}</span>
                </div>
                <svg class="att-download" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              </div>
            </div>
            <!-- 思考过程（折叠区域） -->
            <div v-if="msg.thinking" class="thinking-section" :class="{ noContent: loading && !msg.content }">
              <div class="thinking-header" @click="toggleThinking(msg)">
                <svg :class="{ rotate: msg._showThinking }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                <span>思考过程</span>
                <span v-if="loading && !msg.content" class="thinking-status going">思考中</span>
                <span v-else class="thinking-status done">已完成</span>
              </div>
              <div class="thinking-body" v-show="msg._showThinking" v-html="sanitizeRenderedHtml(md.render(msg.thinking))"></div>
            </div>
            <div v-else-if="loading && msg===messages[messages.length-1] && !msg.content && !msg.thinking" class="msg-thinking-status">
              <div class="thinking-mini-spinner"></div>
              <span>AI 正在思考...</span>
            </div>
            <!-- 回答区域：思考中或内容为空时完全隐藏 -->
            <div class="msg-text" v-show="msg.content" v-html="renderMsgContent(msg.content)"></div>
            <!-- AI 回复操作按钮 -->
            <div v-if="msg.role==='assistant' && msg.content" class="msg-actions">
              <button class="act-btn" @click="copyText(msg.content)" title="复制">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              </button>
              <button class="act-btn" @click="deleteMessage(msg)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
            <!-- 用户消息操作按钮 -->
            <div v-if="msg.role==='user'" class="msg-actions">
              <button v-if="editingMsg !== msg" class="act-btn" @click="copyText(msg.content)" title="复制">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              </button>
              <button v-if="editingMsg !== msg" class="act-btn" @click="startInlineEdit(msg)" title="编辑">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></svg>
              </button>
              <button v-if="editingMsg !== msg" class="act-btn" @click="deleteMessage(msg)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
            <!-- 内联编辑框 -->
            <div v-if="editingMsg === msg" class="inline-edit">
              <textarea v-model="editText" ref="editInputRef" @keydown.enter.exact.prevent="submitInlineEdit" @keydown.escape="cancelInlineEdit" rows="1"></textarea>
              <div class="inline-edit-tips">
                <span>按 Enter 发送 · Esc 取消</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <div class="input-tools">
          <button class="tool-btn-sm" @click="triggerImageUpload" title="上传图片">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          </button>
          <input ref="imgInput" type="file" accept="image/*" multiple style="display:none" @change="onImageSelect" />
          <button class="tool-btn-sm" @click="triggerFileUpload" title="上传附件">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
          </button>
          <input ref="fileInput" type="file" multiple style="display:none" @change="onFileSelect" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.json,.log,.csv,.xml,.md,.yaml,.yml,.zip,.rar,.7z,.tar,.gz,.html,.htm,.ini,.conf,.cfg" />
        </div>
        <div v-if="pendingImages.length > 0 || pendingFiles.length > 0" class="preview-bar">
          <div v-for="(img,i) in pendingImages" :key="'img-'+i" class="preview-item img" @mouseenter="hoverImgIdx = i" @mouseleave="hoverImgIdx = -1">
            <img :src="img" />
            <button v-show="hoverImgIdx === i" class="remove-btn img-remove" @click="pendingImages.splice(i,1)">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div v-for="(f,i) in pendingFiles" :key="'file-'+i" class="preview-item file">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <span class="fname">{{ f.name }}</span><span class="fsize">{{ fmtSize(f.size) }}</span>
            <button class="remove-btn" @click="pendingFiles.splice(i,1)">×</button>
          </div>
        </div>
        <div class="input-toolbar" :class="{ sending: loading }">
          <div class="input-editor">
            <textarea v-model="inputText" ref="inputRef" placeholder="输入消息..." :disabled="loading" @keydown.enter.exact.prevent="sendMsg" @paste="onPaste" rows="1"></textarea>
          </div>
          <button class="send-btn" :class="{ spinning: loading }" :disabled="!canSend && !loading" @click="loading ? stopGeneration() : sendMsg()">
            <svg v-if="!loading" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M2.21 21.75L22.5 12 2.21 2.25 2 10.5l18 1.5-18 1.5-.21 8.25z"/></svg>
            <!-- [FIX]: 高速旋转动画 - 三叶涡轮 -->
            <svg v-else width="18" height="18" viewBox="0 0 24 24" class="turbo-spin">
              <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2.5" stroke-dasharray="6 8" stroke-linecap="round"/>
              <circle cx="12" cy="3.5" r="2" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>
    </main>



    <el-dialog v-model="previewVisible" width="80%" append-to-body class="preview-dialog">
      <img :src="previewUrl" style="width:100%;border-radius:8px" />
    </el-dialog>
    <!-- [FIX]: 文件预览弹窗 - JSON 带颜色高亮、MD 渲染预览 -->
    <el-dialog v-model="filePreviewVisible" width="85%" append-to-body class="file-preview-dialog" :title="filePreviewName">
      <div class="file-preview-content">
        <!-- PDF 用 iframe -->
        <iframe v-if="filePreviewType === 'pdf'" :src="filePreviewUrl" class="file-preview-iframe" />
        <!-- 图片 -->
        <img v-else-if="filePreviewType === 'image'" :src="filePreviewUrl" class="file-preview-img" />
        <!-- JSON 带颜色高亮 -->
        <pre v-else-if="filePreviewType === 'json'" class="file-preview-code json-highlight" v-html="highlightJson(filePreviewContent)"></pre>
        <!-- Markdown 渲染预览 -->
        <div v-else-if="filePreviewType === 'markdown'" class="file-preview-md" v-html="sanitizeRenderedHtml(md.render(filePreviewContent))"></div>
        <!-- [FIX]: LOG 文件带颜色高亮 + 换行保留 -->
        <pre v-else-if="filePreviewType === 'log'" class="file-preview-code log-highlight" v-html="highlightLog(filePreviewContent)"></pre>
        <!-- 其他文本（txt/csv/xml/yaml）带行号 -->
        <pre v-else-if="filePreviewType === 'text'" class="file-preview-code"><code>{{ filePreviewContent }}</code></pre>
        <!-- 不可预览 -->
        <div v-else class="file-preview-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d6cdc3" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <p>此文件格式不支持在线预览</p>
          <a :href="`/api/files/download/${filePreviewUrl.split('/').pop()}`" target="_blank" class="download-link">下载文件</a>
        </div>
      </div>
    </el-dialog>
    <el-dialog v-model="renameVisible" title="重命名对话" width="400px" append-to-body>
      <el-input v-model="renameText" placeholder="请输入新名称" @keyup.enter="doRename" />
      <template #footer>
        <el-button @click="renameVisible=false">取消</el-button>
        <el-button type="primary" @click="doRename">确定</el-button>
      </template>
    </el-dialog>
    <UserProfile ref="userProfileRef" />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  // [FIX]: 启用 typographer + GFM 表格支持
  typographer: true
}).set({
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' + hljs.highlight(str, { language: lang, ignoreIllegals: true }).value + '</code></pre>'
      } catch {}
    }
    // [FIX]: log 文件自定义高亮
    if (lang === 'log') {
      return '<pre class="hljs log-code"><code>' + highlightLog(str) + '</code></pre>'
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import AiSettings from '../components/AiSettings.vue'
import UserProfile from '../components/UserProfile.vue'
import { useAiConfig } from '../stores/aiConfig'
import { createSession, listSessions, searchSessions, updateSessionTitle, deleteSession, archiveSession, getMessages, sendMessage, saveUserMsg, saveAssistantMsg, ocrImage, uploadChatImages, uploadChatFiles, getAttachmentInfos, deleteMessage as apiDeleteMessage } from '../api/chat'
import { searchKnowledgeForChat } from '../api/knowledge'
import { proxyAvatarUrl, onAvatarError } from '../composables/useAvatarProxy'

/** 统一 HTML 消毒函数，防止 XSS */
function sanitizeRenderedHtml(html) {
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'h1','h2','h3','h4','h5','h6','p','br','hr','blockquote','pre','code',
      'ul','ol','li','a','strong','em','b','i','s','del','ins','mark','sub','sup',
      'table','thead','tbody','tr','th','td','img','details','summary','span','div'
    ],
    ALLOWED_ATTR: ['src','alt','title','width','height','href','target','rel','class','align'],
    ALLOW_DATA_ATTR: false
  })
}

const { config: aiConfig } = useAiConfig()
const userStore = useUserStore()
const userProfileRef = ref(null)
const route = useRoute()
const router = useRouter()
const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const loading = ref(false)
const loadingSessions = ref(false)
const sessionPage = ref(1)          // [FIX]: 会话列表当前页码
const sessionHasMore = ref(true)    // [FIX]: 是否还有更多会话可加载
const loadingMore = ref(false)      // [FIX]: 正在加载更多
const totalSessions = ref(0)        // [FIX]: 会话总数
const inputText = ref('')
const inputRef = ref(null)
const messagesRef = ref(null)
const imgInput = ref(null)
const fileInput = ref(null)
const editInputRef = ref(null)
const pendingImages = ref([])
const pendingFiles = ref([])
const hoverImgIdx = ref(-1)
const abortController = ref(null)
const editingMsg = ref(null)
const editText = ref('')
const previewVisible = ref(false)
const previewUrl = ref('')
const renameVisible = ref(false)
const renameText = ref('')
const renameTarget = ref(null)
const searchKeyword = ref('')
const tab = ref('active')
const sidebarCollapsed = ref(localStorage.getItem('sidebar_collapsed') === 'true')
const filePreviewVisible = ref(false)
const filePreviewUrl = ref('')
const filePreviewName = ref('')
const filePreviewContent = ref('')   // [FIX]: 文本内容（用于 JSON 高亮 / MD 渲染）
const filePreviewType = ref('')      // [FIX]: 预览类型：pdf / image / json / markdown / text / unknown
const attachmentCache = ref({})  // id -> { id, fileName, fileSize, fileType, previewable }
let searchTimer = null

// [FIX]: 已移除 Aurora 跑马灯相关状态变量（marqueePause、marqueeMessages）



onMounted(async () => {
  await loadSessions()
  // [FIX]: 从 URL 恢复当前会话（刷新不丢页面）
  const sid = route.query.sid
  if (sid) {
    const id = Number(sid)
    if (sessions.value.some(s => s.id === id)) {
      await switchSession(id)
    }
  }
})

// [FIX]: 组件卸载时清理定时器，防止内存泄漏
onBeforeUnmount(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
  // 取消正在进行的流式请求
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
})

// [FIX]: 会话列表分页+懒加载
const SESSION_PAGE_SIZE = 15

async function loadSessions(append = false) {
  if (!append) {
    sessionPage.value = 1
    sessionHasMore.value = true
  }
  loadingSessions.value = !append
  loadingMore.value = append
  try {
    const status = tab.value === 'archived' ? 1 : 0
    const data = await listSessions({ page: sessionPage.value, size: SESSION_PAGE_SIZE, status })
    const list = data?.content || []
    totalSessions.value = data?.total ?? 0  // [FIX]: 记录总条数
    if (append) {
      // 追加模式：去重后追加到现有列表
      const existIds = new Set(sessions.value.map(s => s.id))
      const newItems = list.filter(s => !existIds.has(s.id))
      sessions.value = [...sessions.value, ...newItems]
    } else {
      sessions.value = list
    }
    // 判断是否还有更多（返回数量不足一页说明到底了）
    sessionHasMore.value = list.length >= SESSION_PAGE_SIZE
  } catch { if (!append) sessions.value = [] }
  finally { loadingSessions.value = false; loadingMore.value = false }
}

// [FIX]: 加载更多会话（仅用户点击触发）
async function loadMoreSessions() {
  if (loadingMore.value || !sessionHasMore.value) return
  sessionPage.value++
  await loadSessions(true)
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!searchKeyword.value.trim()) { loadSessions(); return }
    try {
      const data = await searchSessions({ page: 1, size: 100, keyword: searchKeyword.value })
      sessions.value = data?.content || []
      sessionHasMore.value = false  // 搜索结果不需要懒加载
    } catch { sessions.value = [] }
  }, 300)
}

function openUserProfile() {
  userProfileRef.value?.open()
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar_collapsed', sidebarCollapsed.value)
}

function focusSearch() {
  sidebarCollapsed.value = false
  localStorage.setItem('sidebar_collapsed', 'false')
  nextTick(() => {
    document.querySelector('.search-box input')?.focus()
  })
}

async function newSession() {
  // [FIX]: 已选中一个会话且该会话无消息时，不重复新建
  if (currentSessionId.value && messages.value.length === 0) {
    ElMessage.info('当前已是最新对话')
    return
  }
  try {
    const s = await createSession(aiConfig.model)
    sessions.value.unshift(s)
    await switchSession(s.id)
    inputText.value = ''
    messages.value = []
  } catch {
    const local = { id: Date.now(), title: '新对话', status: 0 }
    sessions.value.unshift(local)
    await switchSession(local.id)
    messages.value = []
  }
}

async function switchSession(id) {
  currentSessionId.value = id
  messages.value = []
  // [FIX]: 将当前会话 ID 写入 URL，刷新时可恢复
  router.replace({ query: { ...route.query, sid: id } })
  try {
    const msgs = await getMessages(id)
    messages.value = (msgs || []).map(m => ({ ...m, _showThinking: false }))
    // [FIX]: 缓存历史消息中的附件信息
    const attIds = new Set()
    for (const m of messages.value) {
      if (m.attachmentIds) {
        try { JSON.parse(m.attachmentIds).forEach(id => attIds.add(id)) } catch {}
      }
    }
    if (attIds.size > 0) {
      try {
        const infos = await getAttachmentInfos([...attIds])
        if (infos) for (const info of infos) attachmentCache.value[info.id] = info
      } catch {}
    }
  } catch {}
  nextTick(() => scrollToBottom())
}

// ==================== 流式发送消息 ====================
const canSend = computed(() => !loading.value && (inputText.value.trim().length > 0 || pendingImages.value.length > 0 || pendingFiles.value.length > 0))

/** 粘贴截图 */
function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const blob = item.getAsFile()
      if (!blob) continue
      const r = new FileReader()
      r.onload = (ev) => { if (ev.target?.result) pendingImages.value.push(ev.target.result) }
      r.readAsDataURL(blob)
    }
  }
}

/** 停止AI生成 */
function stopGeneration() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  loading.value = false
}

async function sendMsg() {
  const text = inputText.value.trim()
  const images = [...pendingImages.value]
  const files = [...pendingFiles.value]
  if (!text && images.length === 0 && files.length === 0) return

  if (!currentSessionId.value) {
    await newSession()
    await nextTick()
  }

  const fileDesc = files.map(f => `[附件] ${f.name}`).join('\n')
  
  // [FIX]: 处理附件文件：1.上传到服务器 2.获取提取的文本内容 3.保存附件ID用于历史回显
  let attachmentIds = []  // 服务器返回的附件 ID，用于保存到数据库和历史回显
  let fileText = ''        // 从文件提取的文本内容（完整，不截断）
  if (files.length > 0) {
    const formData = new FormData()
    for (const f of files) {
      formData.append('files', f.file || f)
    }
    try {
      const uploadResult = await uploadChatFiles(formData)
      if (uploadResult && uploadResult.length > 0) {
        attachmentIds = uploadResult.map(r => r.id)
        // 缓存附件信息
        for (const r of uploadResult) {
          attachmentCache.value[r.id] = { id: r.id, fileName: r.fileName, fileSize: r.size, fileType: r.fileType, previewable: isPreviewable(r.fileName) }
        }
        // [FIX]: 获取每个附件提取的完整文本内容
        for (const r of uploadResult) {
          try {
            const resp = await fetch(`/api/files/text/${r.id}`)
            if (resp.ok) {
              const data = await resp.json()
              if (data?.data?.text) {
                fileText += (fileText ? '\n\n' : '') + `[文件: ${r.fileName}]\n${data.data.text}`
              }
            }
          } catch (e) {
            console.warn('获取文件文本失败', r.id, e)
          }
        }
      }
    } catch (e) {
      console.warn('文件上传失败', e)
    }
  }
  
  // [FIX]: 处理图片：1.上传到服务器获取URL 2.OCR识别文字 3.保存URL用于历史回显
  let imageUrls = []  // 服务器返回的图片 URL，用于保存到数据库和历史回显
  let ocrText = ''
  if (images.length > 0) {
    // 将 base64 图片转为 File 对象上传到服务器
    const formData = new FormData()
    for (let i = 0; i < images.length; i++) {
      const base64 = images[i]
      const blob = dataURLtoBlob(base64)
      formData.append('images', blob, `image_${i}.png`)
    }
    try {
      const uploadResult = await uploadChatImages(formData)
      if (uploadResult && uploadResult.length > 0) {
        imageUrls = uploadResult
      }
    } catch (e) {
      console.warn('图片上传失败', e)
    }
    
    // OCR 识别图片文字
    for (const imgBase64 of images) {
      try {
        const ocrResult = await ocrImage(imgBase64)
        if (ocrResult?.text) {
          ocrText += (ocrText ? '\n\n' : '') + ocrResult.text
        }
      } catch (e) {
        console.warn('OCR 识别失败', e)
      }
    }
  }
  
  // [FIX]: 展示内容（完整，不截断，用于存储和历史回显）
  const displayContent = [text, ocrText ? '[图片识别内容]\n' + ocrText : '', fileDesc, fileText].filter(Boolean).join('\n\n')

  // [FIX]: 发给 AI 的内容（超长文本分片摘要，避免 token 超限）
  let aiContent = text
  if (ocrText) aiContent += '\n\n[图片识别内容]\n' + ocrText
  if (fileDesc) aiContent += '\n\n' + fileDesc
  if (fileText) {
    if (fileText.length <= 6000) {
      aiContent += '\n\n' + fileText
    } else {
      // 分片策略：取前4000字+末尾2000字，中间标注省略
      aiContent += '\n\n' + fileText.substring(0, 4000)
      aiContent += '\n\n...[文件内容较长，已省略中间部分，共 ' + fileText.length + ' 字]...\n\n'
      aiContent += fileText.substring(fileText.length - 2000)
    }
  }

  // 添加用户消息（展示完整内容，_localTime 立即显示时间）
  const now = new Date().toISOString()
  messages.value.push({ id: 'user-' + Date.now(), role: 'user', content: displayContent, imageUrls: imageUrls.length > 0 ? JSON.stringify(imageUrls) : null, attachmentIds: attachmentIds.length > 0 ? JSON.stringify(attachmentIds) : null, _localTime: now })
  inputText.value = ''
  pendingImages.value = []
  pendingFiles.value = []
  loading.value = true
  nextTick(() => scrollToBottom())

  // AI 回复占位（_localTime 立即显示时间）
  const aiNow = new Date().toISOString()
  const aiMsg = { id: 'ai-' + Date.now(), role: 'assistant', content: '', thinking: '', _showThinking: true, _folded: false, _localTime: aiNow }
  messages.value.push(aiMsg)
  nextTick(() => scrollToBottom())

  // 保存用户消息到数据库（传服务器图片 URL 和附件 ID，而非 base64）
  saveUserMsg(currentSessionId.value, displayContent, imageUrls.length > 0 ? imageUrls : null, attachmentIds.length > 0 ? attachmentIds : null).catch(() => {})

  // [FIX]: 将 systemPrompt 声明提升到 try/catch 外部，确保 catch 块中也能访问
  // 原因：const 是块级作用域，在 try 块内声明后 catch 块无法引用，导致回退非流式时报 ReferenceError
  let systemPrompt = ''

  // [FIX]: 强制门禁 — 拦截身份类问题（中英文），直接自我介绍，完全不走知识库检索
  const IDENTITY_PATTERNS = [
    // 中文
    '你是谁', '你是什么', '介绍一下自己', '自我介绍', '你叫什么名字', '你的身份', '你是干什么的', '你是做什么的', '你是哪位', '你叫啥',
    '你有什么功能', '你能做什么', '你会什么', '你会做什么', '你是谁的助手', '你是哪个ai',
    // 英文
    'who are you', 'what are you', 'who r u', 'who ru', 'introduce yourself', 'tell me about yourself',
    'what can you do', 'what do you do', 'what are you for', 'what is your purpose',
    'are you an ai', 'are you chatgpt', 'are you claude', 'are you gpt',
    'your name', 'what is your name', "what's your name", 'who built you', 'who created you',
    // 中英混合
    '介绍下自己', '介绍自己', '自我介绍一下', '介绍一下'
  ]
  const rawQuestion = (displayContent.trim() || text.trim()).toLowerCase()
  const isIdentityQuestion = IDENTITY_PATTERNS.some(p => rawQuestion.startsWith(p.toLowerCase()))

  if (isIdentityQuestion) {
    // [FIX]: 身份问题走完整流式流程：5秒思考动画 → 流式输出答案 → 保存数据库
    const answer = '我是你的个人知识库助手，我可以帮你查询、整理和解读知识库中的内容。请告诉我你想了解什么。'
    const thinkingContent = '这是一个关于身份确认的问题。根据我的职责设定，我是用户的个人知识库助手，专门帮助用户查询、整理和解读知识库中的内容。我不具备其他身份或功能。'

    // 等待5秒思考动画（模拟真实思考）
    await new Promise(resolve => setTimeout(resolve, 5000))

    // 流式输出思考内容（100ms/段）
    const thinkingChunks = thinkingContent.match(/.{1,8}/g) || [thinkingContent]
    aiMsg._showThinking = true
    aiMsg._folded = false
    for (const chunk of thinkingChunks) {
      await new Promise(r => setTimeout(r, 80))
      aiMsg.thinking += chunk
      messages.value = [...messages.value]
      nextTick(() => scrollToBottom())
    }

    // 流式输出答案
    const answerChunks = answer.match(/.{1,5}/g) || [answer]
    for (const chunk of answerChunks) {
      await new Promise(r => setTimeout(r, 50))
      aiMsg.content += chunk
      messages.value = [...messages.value]
      nextTick(() => scrollToBottom())
    }

    // 保存到数据库
    saveAssistantMsg(currentSessionId.value, answer, thinkingContent).catch(() => {})
    loading.value = false
    aiMsg._showThinking = false
    loadSessions()
    nextTick(() => scrollToBottom())
    return
  }

  try {
    // [FIX]: 先从知识库检索相关内容
    let knowledgeContext = ''
    let hasKnowledge = false
    try {
      const searchKeyword = displayContent.trim().slice(0, 50) || text.trim().slice(0, 50)
      if (searchKeyword) {
        const results = await searchKnowledgeForChat(searchKeyword, 5)
        if (results && results.length > 0) {
          hasKnowledge = true
          knowledgeContext = '\n\n===== 知识库检索结果 =====\n以下是知识库中与用户问题相关的内容，你必须仅基于以下知识库内容回答用户的问题。严禁使用你自身训练数据中的知识进行补充或编造。如果以下内容不足以完整回答用户的问题，你必须在回答中明确指出"知识库中未找到完整答案"。不要猜测、不要编造、不要补充知识库中不存在的信息。\n'
          results.forEach((item, idx) => {
            knowledgeContext += `\n【知识条目${idx + 1}】标题：${item.title}`
            if (item.categoryName) knowledgeContext += ` | 分类：${item.categoryName}`
            if (item.summary) knowledgeContext += `\n摘要：${item.summary}`
            if (item.content) knowledgeContext += `\n内容：${item.content}`
            knowledgeContext += '\n---'
          })
          knowledgeContext += '\n===== 知识库检索结果结束 =====\n'
        }
      }
    } catch (e) { /* 知识库检索失败不影响主流程 */ }

    // 【强制门禁】两步判断：第一步判断身份问题 → 直接回复；第二步才查知识库
    systemPrompt = hasKnowledge
      ? `=== 【第一步：身份拦截检查】 ===
你收到用户消息后，第一步必须先判断：这是在问你的身份/自我介绍吗？
包括但不限于："你是谁"、"who are you"、"介绍一下自己"、"what are you"、"你叫什么名字"、"who built you"等。

如果用户在问身份，你必须且只能回复以下固定内容，不允许改动一个字，不允许多说，不允许查知识库：
"我是你的个人知识库助手，我可以帮你查询、整理和解读知识库中的内容。请告诉我你想了解什么。"

如果不是在问身份，才进入下面的知识库问答流程。

=== 【第二步：知识库问答】 ===
你是用户的【个人知识库助手】，没有其他任何身份。

【绝对禁止】❌"作为AI助手"❌"我可以帮你"❌"我是一个AI"❌"当然可以"❌"让我来"❌

【唯一回答来源】你的所有知识唯一来源于下方「知识库检索结果」。禁止使用自身训练数据。禁止编造、推测、联想。

【知识库有答案时】严格基于检索结果回答，禁止补充任何额外信息。

【知识库无答案时】你必须且只能回复这一句，不允许多说一个字：
"抱歉，在知识库中未找到与您问题相关的内容，请先添加相关知识条目。"

【问题与知识库无关时】（天气、新闻、计算器等）直接回复：
"抱歉，我只能回答知识库中已有的内容，请先在知识库中添加相关知识。"

知识库检索结果：
${knowledgeContext}`
      : `=== 【第一步：身份拦截检查】 ===
你收到用户消息后，第一步必须先判断：这是在问你的身份/自我介绍吗？
包括但不限于："你是谁"、"who are you"、"介绍一下自己"、"what are you"、"你叫什么名字"、"who built you"等。

如果用户在问身份，你必须且只能回复以下固定内容，不允许改动一个字，不允许多说，不允许查知识库：
"我是你的个人知识库助手，我可以帮你查询、整理和解读知识库中的内容。请告诉我你想了解什么。"

如果不是在问身份，才进入下面的知识库问答流程。

=== 【第二步：知识库问答】 ===
你是用户的【个人知识库助手】，没有其他任何身份。

【绝对禁止】❌"作为AI助手"❌"我可以帮你"❌"我是一个AI"❌"当然可以"❌"让我来"❌

【唯一回答来源】你只能回答知识库中已有的内容。你的知识完全来自知识库检索结果，不使用任何其他来源。

【知识库无答案时】你必须且只能回复这一句，不允许多说一个字：
"抱歉，在知识库中未找到与您问题相关的内容，请先添加相关知识条目。"

【问题与知识库无关时】（天气、新闻、计算器等）直接回复：
"抱歉，我只能回答知识库中已有的内容，请先在知识库中添加相关知识。"`

    // 构建消息数组
    const msgs = [{ role: 'system', content: systemPrompt }]
    for (const m of messages.value.slice(0, -2)) {
      if (m.role === 'user' || m.role === 'assistant') msgs.push({ role: m.role, content: m.content })
    }
    // [FIX]: 发送分片摘要内容给 AI（避免超长文本 token 超限）
    msgs.push({ role: 'user', content: aiContent })

    // [FIX]: 通过后端代理调用 AI API，不再从前端直接调用外部 API
    abortController.value = new AbortController()
    const resp = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ model: aiConfig.model, apiUrl: aiConfig.apiUrl, messages: msgs, temperature: aiConfig.temperature || 0.7, maxTokens: aiConfig.maxTokens || 4096 }),
      signal: abortController.value.signal
    })
    if (!resp.ok) throw new Error(`API ${resp.status}: ${(await resp.text()).substring(0,100)}`)

    // 解析 SSE
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    let isErrorEvent = false
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        const trimmed = line.trim()
        // [FIX]: 处理后端推送的 error 事件（SSE 格式：event: error\ndata: msg）
        if (trimmed === 'event: error') { isErrorEvent = true; continue }
        if (!trimmed || !trimmed.startsWith('data:')) continue
        const data = trimmed.slice(5).trim()
        if (isErrorEvent) { isErrorEvent = false; throw new Error(data) }
        if (data === '[DONE]') { break }
        try {
          const parsed = JSON.parse(data)
          const delta = parsed?.choices?.[0]?.delta || {}
          if (delta.reasoning_content) { aiMsg.thinking += delta.reasoning_content; messages.value = [...messages.value]; scrollToBottom() }
          if (delta.content) {
            // 首次收到 content → 立刻折叠思考，再追加回答
            if (!aiMsg._folded && aiMsg.thinking) { aiMsg._showThinking = false; aiMsg._folded = true }
            aiMsg.content += delta.content; messages.value = [...messages.value]
          }
          scrollToBottom()
        } catch {}
      }
    }
  } catch (e) {
    console.warn('流式失败，回退非流式', e)
    if (aiMsg.content === '' && aiMsg.thinking === '') {
      // [FIX]: 判断是否为 API Key 错误（401），直接显示友好提示，不回退
      const is401 = e?.message?.includes('API Key') || e?.message?.includes('401')
      if (is401) {
        aiMsg.content = e.message
        loading.value = false
        messages.value = [...messages.value]
        return
      }
      // [FIX]: 判断是否为 429 限流错误，是则走 ES 知识库兜底
      const is429 = e?.message?.includes('429') || e?.message?.includes('API 429')
      if (is429) {
        // [FIX]: 429 限流兜底 — 从 ES 知识库搜索答案，模拟 AI 思考+回答
        try {
          const searchKeyword = displayContent.trim().slice(0, 50) || text.trim().slice(0, 50)
          const results = searchKeyword ? await searchKnowledgeForChat(searchKeyword, 5) : []

          if (results && results.length > 0) {
            // [FIX]: 在 thinking 区域显示"基于知识库检索"
            aiMsg.thinking = '当前 AI 服务请求受限，正在基于知识库检索相关内容为您解答...\n\n检索关键词：' + searchKeyword + '\n匹配到 ' + results.length + ' 条相关知识条目'
            aiMsg._showThinking = true
            messages.value = [...messages.value]
            nextTick(() => scrollToBottom())

            // [FIX]: 将搜索结果格式化为自然的 Markdown 内容
            let fullContent = ''
            results.forEach((item, idx) => {
              if (idx > 0) fullContent += '\n\n---\n\n'
              fullContent += `### ${item.title || '知识条目 ' + (idx + 1)}`
              if (item.categoryName) fullContent += `\n*分类：${item.categoryName}*`
              fullContent += '\n\n'
              if (item.summary) fullContent += item.summary + '\n\n'
              if (item.content) fullContent += item.content
            })

            // [FIX]: 模拟打字机效果，逐字输出内容
            await new Promise(resolve => {
              let charIdx = 0
              const speed = 20 // 每字符间隔 ms
              const timer = setInterval(() => {
                // 每次输出 1-3 个字符，模拟自然阅读速度
                const chunkSize = Math.min(Math.floor(Math.random() * 3) + 1, fullContent.length - charIdx)
                aiMsg.content += fullContent.substring(charIdx, charIdx + chunkSize)
                charIdx += chunkSize
                messages.value = [...messages.value]
                scrollToBottom()
                if (charIdx >= fullContent.length) {
                  clearInterval(timer)
                  resolve()
                }
              }, speed)
            })

            // [FIX]: 输出完成后折叠思考区域
            aiMsg._showThinking = false
            aiMsg._folded = true
            messages.value = [...messages.value]
          } else {
            // [FIX]: 知识库也无结果，显示友好提示
            aiMsg.thinking = '当前 AI 服务请求受限，已尝试从知识库检索，但未找到相关内容。'
            aiMsg._showThinking = true
            aiMsg.content = '抱歉，当前 AI 服务暂时不可用，且知识库中未找到与您问题相关的内容。请稍后重试，或先添加相关知识条目。'
            aiMsg._showThinking = false
            aiMsg._folded = true
            messages.value = [...messages.value]
          }
        } catch (esErr) {
          // [FIX]: ES 搜索也失败，显示友好提示而非原始 429 错误
          console.warn('429 兜底：ES 知识库搜索也失败', esErr)
          aiMsg.content = '抱歉，当前 AI 服务暂时不可用，请稍后重试。'
          messages.value = [...messages.value]
        }
      } else {
        // [FIX]: 非 429 错误，通过后端代理调用 AI（非流式回退）
        try {
          const msgs = [{ role: 'system', content: systemPrompt }]
          for (const m of messages.value.slice(0, -2)) {
            if (m.role === 'user' || m.role === 'assistant') msgs.push({ role: m.role, content: m.content })
          }
          msgs.push({ role: 'user', content: aiContent })
          // [FIX]: 通过后端代理调用，不再直接调用外部 API
          const resp = await fetch('/api/chat/stream', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ model: aiConfig.model, apiUrl: aiConfig.apiUrl, messages: msgs, temperature: aiConfig.temperature || 0.7, maxTokens: aiConfig.maxTokens || 4096 })
          })
          if (resp.ok) {
            // 读取 SSE 流，提取最终内容
            const text = await resp.text()
            const dataLines = text.split('\n').filter(l => l.startsWith('data: ') && !l.includes('[DONE]'))
            let fullContent = ''
            for (const line of dataLines) {
              try {
                const parsed = JSON.parse(line.slice(6))
                fullContent += parsed?.choices?.[0]?.delta?.content || ''
              } catch {}
            }
            aiMsg.content = fullContent || '抱歉，未收到回复。'
          } else {
            aiMsg.content = 'AI 服务调用失败（' + resp.status + '）'
          }
          messages.value = [...messages.value]
        } catch (e2) { aiMsg.content = 'AI 异常：' + e2.message; messages.value = [...messages.value] }
      }
    }
  }

  // 流结束：自动折叠思考 + 保存AI回答到数据库
  loading.value = false
  aiMsg._showThinking = false
  // [FIX]: thinking 截断保护，防止超长内容存入数据库报错
  const savedThinking = aiMsg.thinking && aiMsg.thinking.length > 50000
    ? aiMsg.thinking.substring(0, 50000) + '\n...(思考内容过长，已截断)'
    : aiMsg.thinking
  saveAssistantMsg(currentSessionId.value, aiMsg.content, savedThinking).catch(() => {})
  loadSessions()
  nextTick(() => scrollToBottom())
}

function quickQuestion(text) { inputText.value = text; sendMsg() }

// 图片
function triggerImageUpload() { imgInput.value?.click() }
function onImageSelect(e) {
  const files = e.target.files; if (!files) return
  for (const f of files) { const r = new FileReader(); r.onload = (ev) => pendingImages.value.push(ev.target.result); r.readAsDataURL(f) }
  e.target.value = ''
}

// 附件
function triggerFileUpload() { fileInput.value?.click() }
function onFileSelect(e) {
  const files = e.target.files; if (!files) return
  for (const f of files) pendingFiles.value.push({ name: f.name, size: f.size, file: f })
  e.target.value = ''
}

function fmtSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1048576).toFixed(1) + 'MB'
}

function onDrop(e) {
  const files = e.dataTransfer.files; if (!files) return
  for (const f of files) {
    if (f.type.startsWith('image/')) { const r = new FileReader(); r.onload = (ev) => pendingImages.value.push(ev.target.result); r.readAsDataURL(f) }
    else { pendingFiles.value.push({ name: f.name, size: f.size, file: f }) }
  }
}

function parseImages(urls) { if (!urls) return []; try { return JSON.parse(urls) } catch { return [] } }
function parseAttachmentIds(ids) { if (!ids) return []; try { return JSON.parse(ids) } catch { return [] } }
function previewImage(url) { previewUrl.value = url; previewVisible.value = true }

/** [FIX]: 判断文件是否可预览 */
function isPreviewable(fileName) {
  if (!fileName) return false
  const lower = fileName.toLowerCase()
  return lower.endsWith('.pdf') || lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg')
    || lower.endsWith('.gif') || lower.endsWith('.webp') || lower.endsWith('.svg') || lower.endsWith('.txt')
    || lower.endsWith('.json') || lower.endsWith('.log') || lower.endsWith('.csv') || lower.endsWith('.xml')
    || lower.endsWith('.md') || lower.endsWith('.yaml') || lower.endsWith('.yml')
}

/** [FIX]: 获取文件图标名称 */
function fileIcon(fileName) {
  if (!fileName) return 'file'
  const lower = fileName.toLowerCase()
  if (lower.endsWith('.pdf')) return 'pdf'
  if (lower.endsWith('.doc') || lower.endsWith('.docx')) return 'word'
  if (lower.endsWith('.xls') || lower.endsWith('.xlsx')) return 'excel'
  if (lower.endsWith('.ppt') || lower.endsWith('.pptx')) return 'ppt'
  if (lower.endsWith('.zip') || lower.endsWith('.rar') || lower.endsWith('.7z') || lower.endsWith('.tar') || lower.endsWith('.gz')) return 'zip'
  if (lower.endsWith('.json')) return 'json'
  if (lower.endsWith('.txt') || lower.endsWith('.log') || lower.endsWith('.md')) return 'text'
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.png') || lower.endsWith('.gif') || lower.endsWith('.webp') || lower.endsWith('.svg')) return 'image'
  if (lower.endsWith('.csv')) return 'csv'
  return 'file'
}

/** [FIX]: 预览文件 */
function previewFile(attId) {
  const info = attachmentCache.value[attId]
  if (!info) {
    // 从服务器获取附件信息
    getAttachmentInfos([attId]).then(list => {
      if (list && list.length > 0) {
        attachmentCache.value[attId] = list[0]
        openFilePreview(list[0])
      }
    }).catch(() => {})
    return
  }
  openFilePreview(info)
}

function openFilePreview(info) {
  const lower = (info.fileName || '').toLowerCase()

  // 确定预览类型
  if (lower.endsWith('.pdf')) {
    filePreviewType.value = 'pdf'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
  } else if (lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.gif') || lower.endsWith('.webp') || lower.endsWith('.svg')) {
    filePreviewType.value = 'image'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
  } else if (lower.endsWith('.json')) {
    filePreviewType.value = 'json'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
    // 获取文本内容用于高亮
    fetch(`/api/files/text/${info.id}`).then(r => r.json()).then(d => {
      if (d?.data?.text) {
        try {
          filePreviewContent.value = JSON.stringify(JSON.parse(d.data.text), null, 2)
        } catch { filePreviewContent.value = d.data.text }
      }
    }).catch(() => {})
  } else if (lower.endsWith('.log')) {
    // [FIX]: LOG 文件单独类型，带颜色高亮
    filePreviewType.value = 'log'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
    fetch(`/api/files/text/${info.id}`).then(r => r.json()).then(d => {
      if (d?.data?.text) filePreviewContent.value = d.data.text
    }).catch(() => {})
  } else if (lower.endsWith('.md')) {
    filePreviewType.value = 'markdown'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
    // 获取文本内容用于 markdown 渲染
    fetch(`/api/files/text/${info.id}`).then(r => r.json()).then(d => {
      if (d?.data?.text) {
        // [FIX]: 统一换行符为 \n，否则 md.render 无法正确解析标题/列表等
        filePreviewContent.value = d.data.text.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      }
    }).catch(() => {})
  } else if (lower.endsWith('.txt') || lower.endsWith('.log') || lower.endsWith('.csv') || lower.endsWith('.xml') || lower.endsWith('.yaml') || lower.endsWith('.yml') || lower.endsWith('.ini') || lower.endsWith('.conf') || lower.endsWith('.cfg')) {
    filePreviewType.value = 'text'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
    // 获取文本内容
    fetch(`/api/files/text/${info.id}`).then(r => r.json()).then(d => {
      if (d?.data?.text) filePreviewContent.value = d.data.text
    }).catch(() => {})
  } else if (info.previewable) {
    filePreviewType.value = 'text'
    filePreviewUrl.value = `/api/files/preview/${info.id}`
    filePreviewContent.value = ''
    fetch(`/api/files/text/${info.id}`).then(r => r.json()).then(d => {
      if (d?.data?.text) filePreviewContent.value = d.data.text
    }).catch(() => {})
  } else {
    // 不可预览：直接下载
    window.open(`/api/files/download/${info.id}`, '_blank')
    return
  }
  filePreviewName.value = info.fileName
  filePreviewVisible.value = true
}

/** [FIX]: JSON 语法高亮 - 纯 CSS 实现，轻量无依赖 */
function highlightJson(jsonStr) {
  if (!jsonStr) return ''
  // 先转义 HTML
  let escaped = jsonStr.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // JSON 语法高亮：字符串、数字、布尔、null、key
  return escaped.replace(
    /("(\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
    (match) => {
      let cls = 'json-number'       // 数字
      if (/^"/.test(match)) {
        if (/:$/.test(match)) {
          cls = 'json-key'          // key
        } else {
          cls = 'json-string'      // 字符串值
        }
      } else if (/true|false/.test(match)) {
        cls = 'json-boolean'       // 布尔
      } else if (/null/.test(match)) {
        cls = 'json-null'          // null
      }
      return `<span class="${cls}">${match}</span>`
    }
  )
}

// 会话管理
function renameSession(s) { renameTarget.value = s; renameText.value = s.title; renameVisible.value = true }
async function doRename() {
  if (!renameText.value.trim() || !renameTarget.value) return
  try { await updateSessionTitle(renameTarget.value.id, renameText.value) } catch {}
  renameTarget.value.title = renameText.value; renameVisible.value = false
}
async function doArchive(id, archive) {
  try { await archiveSession(id, archive); ElMessage.success(archive ? '已归档' : '已取消归档'); loadSessions(); if (currentSessionId.value === id) { currentSessionId.value = null; router.replace({ query: { ...route.query, sid: undefined } }) } } catch {}
}
async function doDelete(id) {
  try { await ElMessageBox.confirm('确定删除这个对话？', '确认', { type: 'warning' }); await deleteSession(id); ElMessage.success('已删除'); if (currentSessionId.value === id) { currentSessionId.value = null; router.replace({ query: { ...route.query, sid: undefined } }) }; loadSessions() } catch {}
}

function timeAgo(t) {
  if (!t) return ''
  const diff = (Date.now() - new Date(t).getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 2592000) return Math.floor(diff / 86400) + '天前'
  return new Date(t).toLocaleDateString()
}

/** [FIX]: base64 DataURL 转 Blob 对象，用于文件上传 */
function dataURLtoBlob(dataURL) {
  const parts = dataURL.split(',')
  const mime = parts[0].match(/:(.*?);/)[1]
  const b64 = atob(parts[1])
  const arr = new Uint8Array(b64.length)
  for (let i = 0; i < b64.length; i++) arr[i] = b64.charCodeAt(i)
  return new Blob([arr], { type: mime })
}

/** [FIX]: 切换思考内容折叠状态，触发响应式更新 */
function toggleThinking(msg) {
  msg._showThinking = !msg._showThinking
  messages.value = [...messages.value]
}

/** [FIX]: 格式化消息时间戳为 yyyy-MM-dd HH:mm:ss */
function formatMsgTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/**
 * [FIX]: 渲染消息内容 - 自动识别 JSON/LOG 等文件文本，带颜色高亮显示
 * 预处理流程：
 * 1. 检测 [文件: xxx.json] 或 [文件: xxx.log] 等标记
 * 2. 将其后的文本包裹为 ```json 或 ```log 代码块
 * 3. 再交给 markdown-it 渲染（配合 hljs 高亮）
 */
function renderMsgContent(content) {
  if (!content) return ''
  // 预处理：把 [文件: xxx.ext]\n 后的内容包裹为代码块
  let processed = content.replace(
    /\[文件:\s*([^\]]+\.(\w+))\]\n([\s\S]*?)(?=\n\n\[文件:|$)/g,
    (match, fileName, ext, text) => {
      const lang = ext === 'json' ? 'json' : ext === 'log' ? 'log' : ext === 'csv' ? 'csv' : ext === 'xml' ? 'xml' : ext === 'yaml' || ext === 'yml' ? 'yaml' : ext === 'md' ? 'markdown' : ''
      if (lang) {
        return `**📎 ${fileName}**\n\`\`\`${lang}\n${text.trim()}\n\`\`\``
      }
      return `**📎 ${fileName}**\n\`\`\`\n${text.trim()}\n\`\`\``
    }
  )
  const raw = md.render(processed)
  return sanitizeRenderedHtml(raw)
}

/**
 * [FIX]: LOG 文件语法高亮 - INFO 绿色 / WARN 黄色 / ERROR 红色 / DEBUG 蓝色
 * 每条日志按时间戳自动换行
 */
function highlightLog(logStr) {
  if (!logStr) return ''
  // [FIX]: 先转义 HTML
  let escaped = logStr
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // [FIX]: 在每个时间戳前插入换行（确保每条日志独占一行）
  escaped = escaped.replace(/(?<!^)(\d{4}-\d{2}-\d{2}[\sT]\d{2}:\d{2}:\d{2})/g, '\n$1')
  return escaped
    .replace(/\b(INFO)\b/g, '<span style="color:#98c379;font-weight:600">$1</span>')
    .replace(/\b(WARN|WARNING)\b/g, '<span style="color:#e5c07b;font-weight:600">$1</span>')
    .replace(/\b(ERROR|FATAL|SEVERE)\b/g, '<span style="color:#e06c75;font-weight:600">$1</span>')
    .replace(/\b(DEBUG|TRACE)\b/g, '<span style="color:#61afef;font-weight:600">$1</span>')
    // 日期时间高亮（橙色）
    .replace(/(\d{4}-\d{2}-\d{2}[\sT]\d{2}:\d{2}:\d{2}(?:\.\d+)?)/g, '<span class="log-time" style="color:#d19a66">$1</span>')
}

// ==================== 回复操作 ====================
function copyText(text) {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制')).catch(() => {})
}

/** 删除单条消息（从数据库真实删除） */
async function deleteMessage(msg) {
  // 如果是数据库已有消息（id 是数字），调用后端 API 删除
  if (msg.id && !String(msg.id).startsWith('user-') && !String(msg.id).startsWith('ai-')) {
    try {
      await apiDeleteMessage(msg.id)
    } catch (e) {
      console.warn('删除消息失败', e)
    }
  }
  // 从前端列表移除
  const idx = messages.value.indexOf(msg)
  if (idx >= 0) messages.value.splice(idx, 1)
}

/** 开始内联编辑用户消息 */
function startInlineEdit(msg) {
  if (loading.value) return
  editingMsg.value = msg
  editText.value = msg.content || ''
  nextTick(() => {
    const el = editInputRef.value
    if (el && typeof el.focus === 'function') {
      el.focus()
      if (typeof el.setSelectionRange === 'function') {
        el.setSelectionRange(editText.value.length, editText.value.length)
      }
    }
  })
}

/** 取消内联编辑 */
function cancelInlineEdit() {
  editingMsg.value = null
  editText.value = ''
}

/** 提交内联编辑 → 替换原消息 + 重新调用 AI */
async function submitInlineEdit() {
  const text = editText.value.trim()
  if (!text) { cancelInlineEdit(); return }
  const msg = editingMsg.value
  if (!msg) return

  // 更新用户消息内容
  msg.content = text
  editingMsg.value = null
  editText.value = ''

  // 删除该消息之后的所有后续消息（AI回答等）
  const idx = messages.value.indexOf(msg)
  if (idx >= 0) messages.value.splice(idx + 1)

  // 重新调用 AI
  inputText.value = text
  sendMsg()
}

function scrollToBottom() {
  nextTick(() => {
    // 外层容器滚动到底部
    const outer = messagesRef.value
    if (outer) outer.scrollTop = outer.scrollHeight
    // 思考内容内部也滚动到底部（查询所有thinking-body，最后一个最新）
    const allTb = document.querySelectorAll('.thinking-body')
    if (allTb.length > 0) {
      const lastTb = allTb[allTb.length - 1]
      lastTb.scrollTop = lastTb.scrollHeight
    }
  })
}


</script>

<style scoped>
/* ===== 全局布局 ===== */
.chat-layout { display: flex; height: 100%; position: relative; background: #faf8f5; overflow: hidden; font-size: 14px; }

/* ===== 侧边栏 ===== */
.chat-sidebar { width: 240px; min-width: 240px; background: #f5f0eb; border-right: 1px solid rgba(0,0,0,.06); display: flex; flex-direction: column; overflow: hidden; position: relative; }
.chat-sidebar.collapsed { width: 52px; min-width: 52px; }

/* 折叠按钮 - 分隔线位置 */
.collapse-btn {
  position: absolute; right: -12px; top: 50%; transform: translateY(-50%);
  width: 24px; height: 40px; border-radius: 0 6px 6px 0;
  background: #f5f0eb; border: 1px solid rgba(0,0,0,.06); border-left: none;
  color: #8c847c; cursor: pointer; z-index: 10;
  display: flex; align-items: center; justify-content: center;
  transition: color .15s, background .15s;
}
.collapse-btn:hover { color: #c2410c; background: #ede7e0; }
.chat-sidebar.collapsed .collapse-btn { right: -12px; }

/* 展开态/折叠态内容显隐控制 - 避免 v-if 销毁重建导致 layout 错乱 */
.chat-sidebar.collapsed .sidebar-expanded { display: none; }
.chat-sidebar:not(.collapsed) .sidebar-collapsed-view { display: none; }

/* 折叠态视图 - 独立 flex 布局，始终在 DOM 中 */
.sidebar-collapsed-view {
  display: flex; flex-direction: column; flex: 1;
  overflow: hidden;
}

.collapsed-icons {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px 0; flex-shrink: 0;
}
.collapsed-icon-btn {
  width: 36px; height: 36px; border-radius: 8px;
  background: transparent; border: none; color: #57534e;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .15s;
}
.collapsed-icon-btn:hover { background: rgba(0,0,0,.06); color: #c2410c; }

.sidebar-header { padding: 12px 12px 8px; }
.new-chat-btn { width: 100%; padding: 8px; background: linear-gradient(135deg, #c2410c, #d97706); color: #fff; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all .2s; letter-spacing: 0.3px; }
.new-chat-btn:hover { opacity: .9; transform: translateY(-0.5px); box-shadow: 0 2px 8px rgba(194,65,12,.25); }
.new-chat-btn:active { transform: translateY(0); }

.search-box { display: flex; align-items: center; gap: 5px; margin: 0 12px 8px; padding: 6px 10px; background: #fff; border: 1px solid rgba(0,0,0,.06); border-radius: 8px; color: #8c847c; transition: all .2s; }
.search-box:focus-within { border-color: #c2410c; box-shadow: 0 0 0 2px rgba(194,65,12,.08); }
.search-icon { flex-shrink: 0; }
.search-box input { flex: 1; border: none; outline: none; background: transparent; font-size: 13px; color: #1c1917; }
.search-box input::placeholder { color: #8c847c; }
.clear-btn { background: none; border: none; color: #8c847c; cursor: pointer; font-size: 14px; padding: 0 2px; line-height: 1; }
.clear-btn:hover { color: #1c1917; }

.tab-bar { display: flex; gap: 2px; margin: 0 12px 8px; padding: 2px; background: rgba(0,0,0,.04); border-radius: 6px; }
.tab-bar span { flex: 1; text-align: center; padding: 4px; font-size: 12px; color: #57534e; border-radius: 5px; cursor: pointer; transition: all .2s; letter-spacing: 0.2px; }
.tab-bar span.on { background: #fff; color: #1c1917; font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,.06); }

.session-list { flex: 1; overflow-y: auto; padding: 0 6px; }
.session-list::-webkit-scrollbar { width: 3px; }
.session-list::-webkit-scrollbar-thumb { background: #d6cdc3; border-radius: 2px; }
.session-item { display: flex; align-items: center; padding: 8px 10px; margin-bottom: 1px; border-radius: 7px; cursor: pointer; transition: background .15s; position: relative; }
.session-item:hover { background: rgba(0,0,0,.04); }
.session-item.active { background: rgba(0,0,0,.07); }
.session-main { flex: 1; min-width: 0; }
.session-title { display: block; font-size: 13px; color: #1c1917; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4; }
.session-time { font-size: 11px; color: #8c847c; letter-spacing: 0.2px; }
.session-actions { opacity: 0; transition: opacity .15s; flex-shrink: 0; }
.session-item:hover .session-actions { opacity: 1; }
.more-btn { background: none; border: none; color: #57534e; cursor: pointer; padding: 3px; display: flex; border-radius: 4px; }
.more-btn:hover { background: rgba(0,0,0,.06); }
.empty-sessions { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 32px 12px; color: #8c847c; font-size: 12px; }
.load-more-tip { text-align: center; padding: 10px 0; font-size: 12px; color: #8c847c; }
.load-more-tip.no-more { color: #a8a29e; }
.load-more-fixed { text-align: center; padding: 7px 12px; margin: 0 6px 6px; font-size: 12px; color: #c2410c; font-weight: 500; border-radius: 6px; cursor: pointer; transition: all .15s; background: rgba(194,65,12,.05); border: 1px solid rgba(194,65,12,.12); flex-shrink: 0; }
.load-more-fixed:hover { background: rgba(194,65,12,.1); }
.load-more-fixed.loading { color: #8c847c; background: none; border: none; cursor: default; }

.sidebar-footer { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-top: 1px solid rgba(0,0,0,.06); margin-top: auto; flex-shrink: 0; }
.collapsed-footer { margin-top: auto; border-top: none; justify-content: center; padding: 8px 0; }
.collapsed-footer .user-info { padding: 0; }
.user-info { display: flex; align-items: center; gap: 7px; cursor: pointer; border-radius: 8px; padding: 3px 6px 3px 3px; transition: background .15s; }
.user-info:hover { background: rgba(0,0,0,.04); }
.user-avatar { width: 28px; height: 28px; border-radius: 50%; overflow: hidden; flex-shrink: 0; background: linear-gradient(135deg, #c2410c, #d97706); display: flex; align-items: center; justify-content: center; }
.user-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.user-avatar-text { color: #fff; font-size: 12px; font-weight: 700; line-height: 1; }
.user-name { font-size: 12px; font-weight: 600; color: #1c1917; }

/* ===== 主聊天区 ===== */
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; position: relative; background: #faf8f5; }
.messages-area { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; }
.messages-area::-webkit-scrollbar { width: 5px; }
.messages-area::-webkit-scrollbar-thumb { background: #d6cdc3; border-radius: 3px; }
.messages-area::-webkit-scrollbar-track { background: transparent; }
.messages-area:has(> .welcome:only-child) { justify-content: center; }

/* ===== 欢迎页 ===== */
.welcome { display: flex; flex-direction: column; align-items: center; padding: 48px 32px 24px; text-align: center; transition: padding .3s; }
.welcome.compact { padding: 16px 32px 8px; }
.welcome-icon { margin-bottom: 12px; filter: drop-shadow(0 4px 20px rgba(30,27,75,0.15)); }
.welcome.compact .welcome-icon { display: none; }
.welcome-title { font-size: 22px; font-weight: 700; color: #92400e; margin: 0 0 6px; transition: font-size .3s; letter-spacing: -0.3px; }
.welcome-title.sm { font-size: 15px; margin: 0; }
.welcome-desc { font-size: 13px; color: #57534e; margin: 0 0 24px; letter-spacing: 0.2px; }
.quick-cards { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }
.quick-card { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: #fff; border: 1px solid rgba(0,0,0,.06); border-radius: 10px; cursor: pointer; transition: all .2s; font-size: 13px; color: #1c1917; }
.quick-card:hover { border-color: #c2410c; box-shadow: 0 2px 12px rgba(194,65,12,.1); transform: translateY(-1px); }

/* ===== 消息 ===== */
.msg-row { display: flex; gap: 10px; margin-bottom: 16px; max-width: 92%; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; margin-left: auto; max-width: 58%; align-items: flex-start; }
/*
 * [FIX]: 头像对齐策略（参考用户截图红框标注）
 *   AI头像    → 与"思考过程/已收起"按钮同一水平线（跳过 msg-time 行）
 *   用户头像  → 与橙色消息气泡顶部同一水平线（跳过 msg-time 行）
 */
.msg-avatar { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: #ede7e0; }
/* AI头像：margin-top = msg-time高度(≈16px)，与thinking-section/msg-text顶部对齐 */
.msg-row.assistant .msg-avatar { margin-top: 16px; }
/* 用户头像：margin-top = msg-time高度(≈16px)，与msg-text(橙色气泡)顶部对齐 */
.msg-row.user .msg-avatar { background: #d6cdc3; margin-top: 16px; }
.msg-avatar.assistant { background: transparent; padding: 0; }
.ai-avatar { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 2px 6px rgba(30,27,75,0.15); }
/* [FIX]: 消息区用户真实头像样式 */
.user-msg-avatar { width: 30px; height: 30px; border-radius: 50%; overflow: hidden; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #c2410c, #d97706); }
.user-msg-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.user-msg-avatar-text { color: #fff; font-size: 14px; font-weight: 700; line-height: 1; }

.msg-content { max-width: 100%; min-width: 0; }
.msg-time { font-size: 11px; color: #8c847c; margin-bottom: 3px; letter-spacing: 0.3px; }
.msg-row.user .msg-content { max-width: 90%; }

.msg-text { padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.65; word-break: break-word; }
.msg-row.assistant .msg-text { background: #fff; border: 1px solid rgba(0,0,0,.06); color: #1e293b; }
.msg-row.user .msg-text { background: linear-gradient(135deg, #c2410c, #d97706); color: #fff; font-size: 14px; }

.msg-text :deep(pre) { background: #282c34; color: #abb2bf; padding: 12px 14px; border-radius: 8px; overflow-x: auto; font-size: 13px; margin: 6px 0; border: 1px solid rgba(0,0,0,.08); }
.msg-text :deep(code) { background: rgba(194,65,12,.08); padding: 1px 5px; border-radius: 4px; font-size: 13px; font-family: 'SF Mono','Fira Code','JetBrains Mono',monospace; color: #c2410c; }
.msg-text :deep(pre code) { background: none; padding: 0; color: inherit; }
.msg-text :deep(pre .hljs) { background: transparent; padding: 0; }
/* [FIX]: 用户消息中的代码块用深色背景+白色代码 */
.msg-row.user .msg-text :deep(pre) { background: rgba(0,0,0,.25); border-color: rgba(255,255,255,.1); }
.msg-row.user .msg-text :deep(code) { background: rgba(255,255,255,.15); color: #fff; }
.msg-row.user .msg-text :deep(pre code) { background: none; color: #e0e0e0; }
.msg-text :deep(p) { margin: 0 0 6px; }
.msg-text :deep(p:last-child) { margin-bottom: 0; }
.msg-text :deep(ul), .msg-text :deep(ol) { padding-left: 18px; margin: 3px 0 6px; }
.msg-text :deep(li) { margin: 1px 0; }
.msg-text :deep(blockquote) { border-left: 2px solid #c2410c; padding-left: 10px; color: #57534e; margin: 6px 0; }
.msg-text :deep(a) { color: #c2410c; text-decoration: underline; }
.msg-text :deep(h1), .msg-text :deep(h2), .msg-text :deep(h3), .msg-text :deep(h4) { margin: 10px 0 4px; font-weight: 600; color: #1e293b; font-size: 14px; }
.msg-text :deep(hr) { border: none; border-top: 1px solid rgba(0,0,0,.06); margin: 10px 0; }
.msg-text :deep(img) { max-width: 100%; border-radius: 6px; }
.msg-text :deep(table) { border-collapse: collapse; margin: 6px 0; width: 100%; font-size: 13px; }
.msg-text :deep(th), .msg-text :deep(td) { border: 1px solid rgba(0,0,0,.08); padding: 5px 10px; text-align: left; }
.msg-text :deep(th) { background: rgba(0,0,0,.03); font-weight: 600; }

.msg-images { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 6px; }
.msg-img { max-width: 180px; max-height: 180px; border-radius: 7px; cursor: pointer; border: 1px solid rgba(0,0,0,.06); transition: transform .2s; }
.msg-img:hover { transform: scale(1.02); }

/* ===== 附件列表 ===== */
.msg-attachments { display: flex; flex-direction: column; gap: 5px; margin-bottom: 6px; }
.msg-attachment-item { display: flex; align-items: center; gap: 8px; padding: 7px 10px; background: #fff; border: 1px solid rgba(0,0,0,.06); border-radius: 8px; cursor: pointer; transition: all .15s; }
.msg-attachment-item:hover { border-color: #c2410c; box-shadow: 0 1px 6px rgba(194,65,12,.08); }
.att-icon { width: 28px; height: 28px; border-radius: 6px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: #f5f3ff; }
.att-icon.pdf { background: #fef2f2; }
.att-icon.word { background: #eff6ff; }
.att-icon.excel { background: #f0fdf4; }
.att-icon.zip { background: #fefce8; }
.att-info { flex: 1; min-width: 0; }
.att-name { display: block; font-size: 13px; color: #1e293b; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.att-size { font-size: 11px; color: #8c847c; }
.att-download { flex-shrink: 0; color: #8c847c; transition: color .15s; }
.msg-attachment-item:hover .att-download { color: #c2410c; }

/* ===== 文件预览弹窗 ===== */
.file-preview-dialog .el-dialog__body { padding: 0; }
.file-preview-content { height: 70vh; display: flex; align-items: center; justify-content: center; background: #1e1e2e; border-radius: 0 0 8px 8px; overflow: hidden; }
.file-preview-iframe { width: 100%; height: 100%; border: none; background: #fff; }
.file-preview-img { max-width: 100%; max-height: 100%; object-fit: contain; }

/* JSON 高亮 */
.file-preview-code { width: 100%; height: 100%; margin: 0; padding: 20px; overflow: auto; font-family: 'SF Mono','Fira Code','JetBrains Mono',Consolas,monospace; font-size: 13px; line-height: 1.6; background: #282c34; color: #abb2bf; white-space: pre; tab-size: 2; }
.file-preview-code code { font-family: inherit; }

/* Markdown 渲染预览 */
/* Markdown 预览容器（子元素样式在非 scoped 块中定义） */
.file-preview-md { width: 100%; height: 100%; padding: 24px 32px; overflow: auto; background: #fff; color: #1e293b; font-size: 14px; line-height: 1.75; }
.file-preview-empty { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #8c847c; }
.file-preview-empty p { font-size: 14px; margin: 0; }
.download-link { display: inline-flex; align-items: center; gap: 5px; padding: 6px 16px; background: #c2410c; color: #fff; border-radius: 7px; font-size: 13px; text-decoration: none; }
.download-link:hover { opacity: .9; }

/* ===== 思考过程 ===== */
.thinking-section { margin-bottom: 6px; border: 1px solid rgba(0,0,0,.06); border-radius: 8px; overflow: hidden; background: #fff; }
.thinking-header { display: flex; align-items: center; gap: 5px; padding: 6px 10px; cursor: pointer; user-select: none; background: rgba(0,0,0,.02); font-size: 12px; color: #57534e; transition: background .15s; }
.thinking-header:hover { background: rgba(0,0,0,.04); }
.thinking-header svg { transition: transform .2s; }
.thinking-header svg.rotate { transform: rotate(90deg); }
.thinking-status { margin-left: auto; font-size: 11px; padding: 1px 6px; border-radius: 8px; }
.thinking-status.going { color: #c2410c; background: #fef7ed; }
.thinking-status.done { color: #16a34a; background: #f0fdf4; }
.thinking-body { padding: 8px 14px 8px 22px; font-size: 12px; color: #57534e; line-height: 1.55; border-top: 1px solid rgba(0,0,0,.05); max-height: 240px; overflow-y: scroll; scroll-behavior: smooth; }
.thinking-body :deep(p) { margin: 0 0 3px; }
.thinking-body :deep(code) { font-size: 11px; }
.thinking-body :deep(pre) { font-size: 11px; margin: 4px 0; padding: 6px 8px; }
.thinking-body::-webkit-scrollbar { width: 3px; }
.thinking-body::-webkit-scrollbar-track { background: transparent; }
.thinking-body::-webkit-scrollbar-thumb { background: #d6cdc3; border-radius: 2px; }

.thinking-section.noContent { margin-bottom: 0; }
.thinking-section.noContent + .msg-text { display: none; }

.msg-thinking-status { display: flex; align-items: center; gap: 6px; padding: 6px 0; }
.thinking-mini-spinner { width: 12px; height: 12px; border: 1.5px solid rgba(0,0,0,.06); border-top-color: #c2410c; border-radius: 50%; animation: miniSpin .8s linear infinite; }
@keyframes miniSpin { to { transform: rotate(360deg); } }
.msg-thinking-status span { font-size: 12px; color: #57534e; }

/* ===== 输入区 ===== */
.chat-input-area { padding: 0 20px 12px; background: #faf8f5; }
.input-tools { display: flex; gap: 3px; padding: 0 6px 4px; align-items: center; }
.tool-btn-sm { background: none; border: none; color: #8c847c; cursor: pointer; padding: 3px; border-radius: 5px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.tool-btn-sm:hover { color: #c2410c; background: rgba(194,65,12,.06); }
.tool-btn-sm svg { display: block; }

.preview-bar { display: flex; gap: 6px; padding: 8px 0 6px; overflow-x: auto; }
.preview-item { position: relative; flex-shrink: 0; border-radius: 7px; overflow: hidden; }
.preview-item.img { width: 48px; height: 48px; }
.preview-item.img img { width: 100%; height: 100%; object-fit: cover; }
.preview-item.img .img-remove { position: absolute; top: 1px; right: 1px; width: 16px; height: 16px; border-radius: 50%; background: rgba(0,0,0,.5); color: #fff; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background .15s; z-index: 2; }
.preview-item.img .img-remove:hover { background: rgba(239,68,68,.8); }
.preview-item.file { display: flex; align-items: center; gap: 5px; padding: 5px 10px; background: #fff; border: 1px solid rgba(0,0,0,.06); border-radius: 7px; font-size: 12px; color: #1c1917; }
.preview-item.file .fname { max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.preview-item.file .fsize { color: #8c847c; }
.remove-btn { position: absolute; top: -5px; right: -5px; width: 16px; height: 16px; border-radius: 50%; background: #ef4444; color: #fff; border: none; font-size: 10px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.preview-item.file .remove-btn { position: static; width: 14px; height: 14px; font-size: 10px; }

.input-toolbar { display: flex; align-items: flex-end; gap: 5px; background: #fff; border: 1px solid rgba(0,0,0,.08); border-radius: 14px; padding: 7px 10px; transition: border-color .2s, box-shadow .2s; }
.input-toolbar:focus-within { border-color: #c2410c; box-shadow: 0 0 0 2px rgba(194,65,12,.08); }
.input-editor { flex: 1; min-width: 0; }
.input-editor textarea { width: 100%; border: none; outline: none; resize: none; font-size: 14px; font-family: inherit; color: #1e293b; background: transparent; max-height: 100px; line-height: 1.5; padding: 3px 0; }
.input-editor textarea::placeholder { color: #8c847c; }
.input-editor textarea:disabled { opacity: .5; cursor: not-allowed; }

.input-toolbar.sending { border-color: #c2410c; background: #fdf8f3; }
.input-tools.sending .tool-btn-sm { opacity: .35; pointer-events: none; }

/* ===== 发送按钮 ===== */
.send-btn { width: 34px; height: 34px; border-radius: 10px; border: none; background: linear-gradient(135deg, #c2410c, #b45309); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all .2s; }
.send-btn:disabled { opacity: .35; cursor: not-allowed; }
.send-btn:not(:disabled):hover { opacity: .9; transform: translateY(-0.5px); box-shadow: 0 2px 8px rgba(194,65,12,.3); }
.send-btn:active { transform: translateY(0); }

.send-btn.spinning { background: linear-gradient(135deg, #ef4444, #dc2626); animation: pulseStop 2s ease-in-out infinite; }
.send-btn.spinning:hover { opacity: .9; transform: scale(1.05); }
/* [FIX]: 三叶涡轮高速旋转动画 - 0.35s 一圈，超高速 */
.turbo-spin { animation: turboSpin .35s linear infinite; }
@keyframes turboSpin { to { transform: rotate(360deg); } }
@keyframes pulseStop { 0%,100% { box-shadow: 0 0 0 0 rgba(239,68,68,.3); } 50% { box-shadow: 0 0 0 8px rgba(239,68,68,0); } }

::deep(.el-dropdown-menu__item) { display: flex; align-items: center; gap: 5px; font-size: 13px; padding: 5px 14px; }

/* ===== 操作按钮 ===== */
.msg-actions { display: flex; gap: 1px; padding: 3px 0 0; opacity: 0; transition: opacity .15s; }
.msg-row:hover .msg-actions { opacity: 1; }
.act-btn { background: none; border: none; color: #8c847c; cursor: pointer; width: 24px; height: 24px; border-radius: 5px; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.act-btn:hover { background: rgba(0,0,0,.04); color: #c2410c; }

/* ===== 内联编辑 ===== */
.inline-edit { margin-top: 6px; }
.inline-edit textarea {
  width: 100%; border: 1.5px solid #c2410c; outline: none; resize: none;
  font-size: 14px; font-family: inherit; color: #1e293b; background: #fff;
  padding: 8px 12px; border-radius: 10px; line-height: 1.5; min-height: 38px;
  box-shadow: 0 0 0 2px rgba(194,65,12,.06);
}
.inline-edit-tips { font-size: 11px; color: #8c847c; padding: 3px 3px 0; }

/* [FIX]: 已移除 Aurora 跑马灯样式（.marquee-bar / .marquee-glow / .marquee-track / .marquee-content / .marquee-item / .marquee-spark 及所有 @keyframes 动画） */


</style>

<!-- [FIX]: 非 scoped 样式块 - 用于 v-html 渲染内容的语法高亮（scoped CSS 无法穿透 v-html） -->
<style>
/* ===== JSON 语法高亮（v-html 渲染内容，不能用 scoped）===== */
.json-highlight .json-key { color: #e06c75; }       /* key - 红色 */
.json-highlight .json-string { color: #98c379; }    /* 字符串值 - 绿色 */
.json-highlight .json-number { color: #d19a66; }    /* 数字 - 橙黄 */
.json-highlight .json-boolean { color: #c678dd; }   /* 布尔 - 紫色 */
.json-highlight .json-null { color: #e06c75; font-style: italic; }  /* null - 红色 */

/* ===== LOG 语法高亮 ===== */
.log-highlight .log-info { color: #98c379; font-weight: 600; }      /* INFO → 绿 */
.log-highlight .log-warn { color: #e5c07b; font-weight: 600; }      /* WARN → 黄 */
.log-highlight .log-error { color: #e06c75; font-weight: 600; }     /* ERROR → 红 */
.log-highlight .log-debug { color: #61afef; font-weight: 600; }     /* DEBUG → 蓝 */
.log-highlight .log-time { color: #d19a66; }                        /* 时间戳 → 橫 */

/* ===== Markdown 预览渲染（v-html 渲染内容，不能用 scoped）===== */
.file-preview-md h1 { font-size: 22px; font-weight: 700; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; margin: 20px 0 12px; }
.file-preview-md h2 { font-size: 18px; font-weight: 700; border-bottom: 1px solid #e2e8f0; padding-bottom: 6px; margin: 18px 0 10px; }
.file-preview-md h3 { font-size: 16px; font-weight: 600; margin: 14px 0 8px; }
.file-preview-md h4 { font-size: 14px; font-weight: 600; margin: 12px 0 6px; }
.file-preview-md p { margin: 0 0 10px; }
.file-preview-md code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 14px; font-family: 'SF Mono','Fira Code','JetBrains Mono',Consolas,monospace; color: #c2410c; }
.file-preview-md pre { background: #282c34; color: #abb2bf; padding: 14px; border-radius: 8px; overflow-x: auto; margin: 12px 0; }
.file-preview-md pre code { background: none; padding: 0; color: inherit; }
.file-preview-md blockquote { border-left: 4px solid #c2410c; padding: 8px 16px; margin: 12px 0; background: #fef7ed; color: #57534e; }
.file-preview-md ul, .file-preview-md ol { padding-left: 24px; margin: 8px 0; }
.file-preview-md li { margin: 4px 0; }
.file-preview-md table { border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 14px; }
.file-preview-md th, .file-preview-md td { border: 1px solid #d6cdc3; padding: 8px 12px; text-align: left; }
.file-preview-md th { background: #fdf8f3; font-weight: 600; }
.file-preview-md a { color: #c2410c; text-decoration: underline; }
.file-preview-md hr { border: none; border-top: 1px solid #e2e8f0; margin: 16px 0; }
.file-preview-md img { max-width: 100%; border-radius: 6px; }
</style>
