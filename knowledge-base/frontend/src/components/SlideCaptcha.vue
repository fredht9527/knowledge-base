<template>
  <div class="captcha-card" :class="{ success: verified }">
    <!-- [FIX]: 移除"安全验证"标题和 .captcha-top 容器；刷新按钮改为绝对定位浮在视口右上方 -->
    <button v-if="!verified && imageLoaded" class="captcha-refresh" @click="reset" title="换一张">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
    </button>

    <!-- 背景图 + 缺口 -->
    <div class="captcha-viewport" ref="viewportRef">
      <div v-if="!imageLoaded" class="captcha-skeleton">
        <svg class="spinner" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10" stroke-dasharray="31.4 31.4" stroke-linecap="round"/></svg>
      </div>

      <template v-else>
        <img :src="imageUrl" class="captcha-bg-img" :width="W" :height="H" alt="" draggable="false"/>

        <!-- 缺口位置（虚线框 + 暗化） -->
        <div v-if="!verified" class="captcha-hole" :style="holeStyle">
          <div class="hole-frame"></div>
        </div>

        <!-- 可拖动的拼图块 -->
        <div
          v-if="!verified"
          class="captcha-piece"
          :class="{ snapping: snapping }"
          :style="pieceStyle"
          @mousedown.prevent="startDrag"
          @touchstart.prevent="startDrag"
        >
          <img :src="imageUrl" class="piece-img" :width="W" :height="H" alt="" draggable="false"/>
          <div class="piece-shine"></div>
        </div>

        <!-- 成功状态 -->
        <div v-if="verified" class="captcha-success-mask">
          <div class="success-ring">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <span>验证通过</span>
        </div>
      </template>
    </div>

    <!-- 轨道 -->
    <div v-if="!verified" class="captcha-rail">
      <div class="rail-fill" :style="{ width: trackPercent + '%' }"></div>
      <div
        class="rail-knob"
        :class="{ isDrag: dragging, isError: hasError }"
        :style="{ left: trackPercent + '%' }"
        @mousedown.prevent="startDrag"
        @touchstart.prevent="startDrag"
      >
        <!-- [FIX]: 改为向右双箭头">>"图标，匹配参考设计；尺寸 14x14 适配 36px 滑块 -->
        <svg v-if="hasError" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><polyline points="8 7 12 12 8 17"/><polyline points="13 7 17 12 13 17"/></svg>
      </div>
      <span class="rail-hint" :class="{ isHidden: dragging || pieceX > 8 }">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
        向右拖动滑块完成拼图
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['verify'])

const W = 320
const H = 200    // [FIX]: 从 180 增至 200，匹配视口新高度让图片更饱满
const P = 44     // piece size
const TOL = 5    // tolerance

const imageUrl = ref('')
const imageLoaded = ref(false)

const verified = ref(false)
const dragging = ref(false)
const hasError = ref(false)
const startX0 = ref(0)
const pieceX = ref(0)

// [FIX]: RAF 节流相关变量，避免高频 mousemove 触发过多 Vue 响应式更新
const snapping = ref(false)     // 回弹过渡标记
let rafId = 0                   // RAF 请求 ID
let pendingX = 0                // 暂存最新拖拽位置

const targetX = ref(80)
const targetY = ref(40)

const trackPercent = computed(() => {
  const max = W - P - 10
  return Math.min((pieceX.value / max) * 100, 100)
})

const holeStyle = computed(() => ({
  left: targetX.value + 'px',
  top: targetY.value + 'px',
  width: P + 'px',
  height: P + 'px'
}))

const pieceStyle = computed(() => ({
  transform: `translateX(${pieceX.value}px)`,
  top: targetY.value + 'px',
  width: P + 'px',
  height: P + 'px',
  '--tx': targetX.value + 'px',
  '--ty': targetY.value + 'px'
}))

/* ── 加载图片 ── */
function loadImage() {
  // [FIX]: 不再先设 imageLoaded=false，加载期间保留旧图，避免刷新闪烁
  const id = Math.floor(Math.random() * 80) + 10
  const url = `https://picsum.photos/id/${id}/${W}/${H}`

  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    // [FIX]: 新图加载成功后才更新，旧图始终可见直到替换
    imageUrl.value = url
    imageLoaded.value = true
  }
  img.onerror = () => {
    imageUrl.value = `https://picsum.photos/${W}/${H}?random=${Date.now()}`
    imageLoaded.value = true
  }
  img.src = url
}

function randomPos() {
  targetX.value = 30 + Math.floor(Math.random() * (W - P - 70))
  targetY.value = 10 + Math.floor(Math.random() * (H - P - 20))
  pieceX.value = 0
}

/* ── 拖拽 ── */
function startDrag(e) {
  if (verified.value) return
  dragging.value = true
  hasError.value = false
  const cx = e.touches ? e.touches[0].clientX : e.clientX
  startX0.value = cx - pieceX.value

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', endDrag)
}

function onDrag(e) {
  if (!dragging.value) return
  e.preventDefault()
  const cx = e.touches ? e.touches[0].clientX : e.clientX
  let dx = cx - startX0.value
  dx = Math.max(0, Math.min(dx, W - P - 10))
  // [FIX]: 使用 RAF 节流，只记录最新位置，每帧更新一次 pieceX
  pendingX = dx
  if (!rafId) {
    rafId = requestAnimationFrame(() => {
      pieceX.value = pendingX
      rafId = 0
    })
  }
}

function endDrag() {
  if (!dragging.value) return
  dragging.value = false
  // [FIX]: 取消未执行的 RAF，避免残留更新
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)

  if (Math.abs(pieceX.value - targetX.value) <= TOL) {
    pieceX.value = targetX.value
    verified.value = true
    emit('verify', true)
  } else {
    hasError.value = true
    // [FIX]: 验证失败回弹时添加过渡 class，实现平滑回弹动画
    snapping.value = true
    setTimeout(() => {
      hasError.value = false
      pieceX.value = 0
      // 过渡结束后移除 class
      setTimeout(() => { snapping.value = false }, 300)
    }, 500)
  }
}

function reset() {
  verified.value = false
  hasError.value = false
  pieceX.value = 0
  loadImage()
  randomPos()
}

onMounted(() => {
  loadImage()
  randomPos()
})

// [FIX]: 组件卸载时取消残留的 RAF
onBeforeUnmount(() => {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
})
</script>

<style scoped>
.captcha-card {
  width: 100%;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e7e0d8;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  overflow: hidden;
  transition: border-color .35s, box-shadow .35s;
}
.captcha-card.success {
  border-color: #86efac;
  box-shadow: 0 2px 10px rgba(16,185,129,0.06);
}

/* [FIX]: 移除 .captcha-top 和 .captcha-label 样式（标题已删除）；刷新按钮改为绝对定位浮在视口右上方 */
.captcha-refresh {
  position: absolute;
  top: 30px;
  right: 8px;
  z-index: 10;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(4px);
  border: none;
  color: #57534e;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  display: flex;
  transition: all .2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}
.captcha-refresh:hover {
  background: rgba(255,255,255,0.95);
  color: #292524;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

/* ── 视口 ── */
.captcha-viewport {
  position: relative;
  width: auto;
  height: 200px;
  /* [FIX]: 高度从 180px 增至 200px，利用移除标题栏后释放的空间让图片更饱满 */
  margin: 0;
  border-radius: 14px 14px 0 0;
  overflow: hidden;
  background: #faf8f5;
}

.captcha-skeleton {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.spinner {
  animation: spin 1s linear infinite;
  color: #e7e0d8;
}
@keyframes spin { to { transform: rotate(360deg); } }

.captcha-bg-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 缺口 */
.captcha-hole {
  position: absolute;
  left: 0;
  top: 0;
  border-radius: 4px;
  pointer-events: none;
  z-index: 2;
}
.hole-frame {
  width: 100%;
  height: 100%;
  border-radius: 4px;
  border: 1.5px dashed rgba(255,255,255,0.65);
  /* [FIX]: 用 rgba 叠加替代 backdrop-filter，零 GPU 合成开销 */
  background: rgba(0,0,0,0.15);
  box-shadow: inset 0 0 0 9999px rgba(0,0,0,0.1);
}

/* 拼图块 */
.captcha-piece {
  position: absolute;
  left: 0;
  border-radius: 4px;
  overflow: hidden;
  cursor: grab;
  z-index: 3;
  box-shadow: 0 4px 16px rgba(0,0,0,0.28), 0 0 0 1px rgba(255,255,255,0.35) inset;
  /* [FIX]: 移除拖拽期间的 transition，改为仅在回弹时通过 .snapping class 启用过渡 */
  will-change: transform;
}
/* [FIX]: 回弹时的过渡动画，仅在 snapping 状态下生效 */
.captcha-piece.snapping {
  transition: transform .3s ease;
}
.captcha-piece:active { cursor: grabbing; }

.piece-img {
  position: absolute;
  left: 0;
  top: 0;
  width: 320px;
  height: 200px;
  /* [FIX]: 高度从 180px 更新为 200px，匹配 .captcha-viewport 新高度 */
  object-fit: cover;
  pointer-events: none;
  /* 让图片偏移到正确位置 */
  margin-left: calc(-1 * var(--tx));
  margin-top: calc(-1 * var(--ty));
}
.piece-shine {
  position: absolute;
  inset: 0;
  border-radius: 4px;
  box-shadow: inset 0 0 0 1.5px rgba(255,255,255,0.45);
  pointer-events: none;
}

/* 成功遮罩 */
.captcha-success-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(3px);
  z-index: 5;
  color: #059669;
  font-size: 14px;
  font-weight: 600;
  animation: fadeIn .3s ease;
}
.success-ring {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #d1fae5;
  display: flex;
  align-items: center;
  justify-content: center;
}
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

/* ── 轨道 ── */
.captcha-rail {
  position: relative;
  height: 40px;
  margin: 5px 0px 5px;
  border-radius: 8px;
  background: #fdf8f3;
  border: 1px solid #e7e0d8;
  display: flex;
  align-items: center;
  overflow: hidden;
  user-select: none;
}

.rail-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, #d97706, #c2410c);
  border-radius: 8px;
  /* [FIX]: 移除拖拽期间的 transition，轨道填充跟随 pieceX 实时更新 */
  pointer-events: none;
}

.rail-knob {
  position: absolute;
  top: 1px;
  /* [FIX]: 尺寸调整为 36x36（小于 rail 高度 40px），配合 margin-left: -18px
   * 确保滑块在 overflow:hidden 容器中完整显示，不被裁剪左半部分 */
  width: 36px;
  height: 36px;
  margin-left: 1px;
  /* [FIX]: 改为橙色圆角方形滑块，匹配参考设计（向右箭头风格） */
  background: linear-gradient(135deg, #f97316, #ea580c);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: grab;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(234,88,12,0.35), 0 1px 2px rgba(234,88,12,0.15);
  will-change: transform;
  transition: box-shadow .15s;
}
.rail-knob:hover {
  box-shadow: 0 4px 14px rgba(234,88,12,0.45), 0 2px 4px rgba(234,88,12,0.2);
  transform: scale(1.06);
}
.rail-knob.isDrag {
  cursor: grabbing;
  box-shadow: 0 6px 20px rgba(234,88,12,0.5), 0 2px 6px rgba(234,88,12,0.25);
  transform: scale(1.08);
}
.rail-knob.isError {
  color: #ef4444;
  animation: shake .45s ease-in-out;
}

.rail-hint {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  color: #8c847c;
  font-size: 12px;
  pointer-events: none;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: opacity .25s;
}
.rail-hint.isHidden { opacity: 0; }
.rail-hint svg { opacity: 0.45; }

@keyframes shake {
  0%, 100% { transform: translateX(0) scale(1.12); }
  20% { transform: translateX(-6px) scale(1.12); }
  60% { transform: translateX(6px) scale(1.12); }
}
</style>
