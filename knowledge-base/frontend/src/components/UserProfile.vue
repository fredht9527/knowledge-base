<template>
  <el-dialog v-model="visible" title="个人资料" width="440px" :close-on-click-modal="false" append-to-body custom-class="profile-dialog-box">
    <div class="profile-body">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar-wrap" @click="triggerAvatarUpload" :title="'点击更换头像'">
          <!-- [FIX]: 使用 proxyAvatarUrl 代理外部CDN头像，解决 ERR_CONNECTION_RESET -->
          <img v-if="localAvatar" :src="proxyAvatarUrl(localAvatar)" class="avatar-img" referrerpolicy="no-referrer" @error="onAvatarError" />
          <div v-else class="avatar-placeholder">{{ displayInitial }}</div>
          <div class="avatar-overlay">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
          </div>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarSelect" />
        <div class="avatar-name">{{ localNickname || '用户' }}</div>
      </div>

      <!-- 表单 -->
      <div class="form-fields">
        <div class="field-row">
          <label class="field-label">邮箱</label>
          <div class="field-value readonly">{{ localEmail }}</div>
        </div>

        <div class="field-row">
          <label class="field-label">昵称</label>
          <el-input v-model="localNickname" placeholder="设置昵称" maxlength="50" class="field-input" />
        </div>

        <div class="field-row">
          <label class="field-label">性别</label>
          <el-radio-group v-model="localGender" class="gender-group">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
            <el-radio value="保密">保密</el-radio>
          </el-radio-group>
        </div>

        <div class="field-row">
          <label class="field-label">手机号</label>
          <el-input v-model="localPhone" placeholder="关联手机号" maxlength="20" class="field-input" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button class="logout-btn" @click="handleLogout">退出登录</el-button>
        <div class="footer-right">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { updateUserProfile, uploadAvatar } from '../api/user'
import { proxyAvatarUrl, onAvatarError } from '../composables/useAvatarProxy'

const router = useRouter()
const userStore = useUserStore()
const visible = ref(false)
const saving = ref(false)
const avatarInput = ref(null)

// 本地编辑数据，从 store 初始化
const localNickname = ref('')
const localGender = ref('保密')
const localPhone = ref('')
const localAvatar = ref('')
const localEmail = ref('')

// 显示首字作为占位头像
const displayInitial = computed(() => {
  const name = localNickname.value || '用户'
  return name.charAt(0).toUpperCase()
})

/** 打开弹窗时从 store 加载最新数据 */
function open() {
  localNickname.value = userStore.nickname || ''
  localGender.value = userStore.gender || '保密'
  localPhone.value = userStore.phone || ''
  localAvatar.value = userStore.avatar || ''
  localEmail.value = userStore.email || ''
  visible.value = true
}

/** 触发文件选择 */
function triggerAvatarUpload() {
  avatarInput.value?.click()
}

/** 选择头像文件 */
async function onAvatarSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // 预览本地文件
  const reader = new FileReader()
  reader.onload = (ev) => {
    localAvatar.value = ev.target.result
  }
  reader.readAsDataURL(file)

  // 上传头像
  try {
    const url = await uploadAvatar(file)
    localAvatar.value = url
    userStore.updateProfile({ avatar: url })
    ElMessage.success('头像已更新')
  } catch (err) {
    ElMessage.error('头像上传失败')
    console.error('头像上传失败', err)
  }

  e.target.value = ''
}

/** 保存资料 */
async function handleSave() {
  saving.value = true
  try {
    const profile = await updateUserProfile({
      nickname: localNickname.value,
      gender: localGender.value,
      phone: localPhone.value
    })
    userStore.updateProfile(profile)
    ElMessage.success('资料已保存')
    visible.value = false
  } catch (err) {
    console.error('保存资料失败', err)
  } finally {
    saving.value = false
  }
}

/** 退出登录 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    visible.value = false
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {}
}

defineExpose({ open })
</script>

<style scoped>
.profile-dialog-box :deep(.el-dialog__body) { padding: 0; }
.profile-body { padding: 0 24px; }

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 20px;
}

.avatar-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 3px solid #ede7e0;
  transition: border-color .2s;
}
.avatar-wrap:hover { border-color: #c2410c; }

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #c2410c, #d97706);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity .2s;
}
.avatar-wrap:hover .avatar-overlay { opacity: 1; }

.avatar-name {
  margin-top: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1c1917;
}

/* 表单 */
.form-fields { padding-bottom: 8px; }
.field-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0ebe5;
}
.field-row:last-child { border-bottom: none; }

.field-label {
  width: 64px;
  font-size: 13px;
  color: #57534e;
  flex-shrink: 0;
}
.field-value.readonly {
  flex: 1;
  font-size: 13px;
  color: #8c847c;
}
.field-input { flex: 1; }
.field-input :deep(.el-input__wrapper) {
  background: #fdf8f3;
  box-shadow: 0 0 0 1px #e7e0d8 inset;
  border-radius: 8px;
}
.field-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c2410c inset;
}
.field-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #c2410c inset, 0 0 0 3px rgba(194,65,12,.08);
}

.gender-group { display: flex; gap: 8px; }
.gender-group :deep(.el-radio) { margin-right: 0; }

.dialog-footer { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.footer-right { display: flex; gap: 8px; }
.logout-btn { color: #ef4444 !important; border-color: #fecaca !important; }
.logout-btn:hover { background: #fef2f2 !important; border-color: #ef4444 !important; color: #dc2626 !important; }
</style>

<!-- 非 scoped block：确保 append-to-body teleport 后样式仍生效 -->
<style>
/* 所有屏幕下自适应：宽度/高度/位置 */
.profile-dialog-box {
  width: min(440px, calc(100vw - 32px)) !important;
  max-height: 85vh !important;
  border-radius: 12px !important;
  display: flex !important;
  flex-direction: column !important;
}
.profile-dialog-box .el-dialog__header { flex-shrink: 0; }
.profile-dialog-box .el-dialog__body {
  flex: 1;
  overflow-y: auto !important;
}
.profile-dialog-box .el-dialog__footer { flex-shrink: 0; }

@media (max-width: 768px) {
  .profile-dialog-box .el-dialog__header {
    padding: 12px 16px 8px;
    margin-right: 0;
  }
  .profile-dialog-box .el-dialog__header .el-dialog__title {
    font-size: 15px;
    font-weight: 600;
  }
  .profile-dialog-box .el-dialog__body {
    padding: 0 16px;
  }
  .profile-dialog-box .el-dialog__footer {
    padding: 10px 16px 12px;
  }

  /* 头像区域缩小 */
  .profile-dialog-box .avatar-section {
    padding: 12px 0 10px;
  }
  .profile-dialog-box .avatar-wrap {
    width: 56px;
    height: 56px;
    border-width: 2px;
  }
  .profile-dialog-box .avatar-placeholder {
    font-size: 20px;
  }
  .profile-dialog-box .avatar-name {
    font-size: 14px;
    margin-top: 6px;
  }

  /* 表单区域紧凑 */
  .profile-dialog-box .form-fields {
    padding-bottom: 4px;
  }
  .profile-dialog-box .field-row {
    padding: 6px 0;
    gap: 8px;
  }
  .profile-dialog-box .field-label {
    width: 48px;
    font-size: 12px;
  }
  .profile-dialog-box .field-value.readonly {
    font-size: 12px;
  }
  .profile-dialog-box .field-input .el-input__wrapper {
    padding: 0 10px;
  }
  .profile-dialog-box .field-input .el-input__inner {
    height: 32px;
    font-size: 12px;
  }
  .profile-dialog-box .gender-group .el-radio {
    margin-right: 12px;
    font-size: 12px;
  }
  .profile-dialog-box .gender-group .el-radio__input {
    transform: scale(0.9);
  }

  /* 底部按钮横向紧凑排列 */
  .profile-dialog-box .dialog-footer {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }
  .profile-dialog-box .footer-right {
    gap: 6px;
  }
  .profile-dialog-box .dialog-footer .el-button {
    padding: 6px 12px;
    font-size: 12px;
    height: 32px;
  }
  .profile-dialog-box .logout-btn {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>
