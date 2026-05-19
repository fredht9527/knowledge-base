<template>
  <div class="edit">
    <div class="h">
      <button class="b" @click="goBack">← 返回</button>
      <span class="t">{{ isNew ? '新建' : '编辑' }}</span>
      <div class="as">
        <button class="s" @click="handleSave(0)">草稿</button>
        <button class="p" @click="handleSave(1)">发布</button>
      </div>
    </div>

    <div class="body">
      <div class="f"><label>标题</label><input v-model="form.title" placeholder="输入标题..." /></div>

      <div class="row">
        <div class="f f1"><label>分类</label>
          <select v-model="form.categoryId">
            <option :value="null">选择分类</option>
            <option v-for="c in flatCategories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="f f2"><label>标签</label>
          <div class="tw">
            <span v-for="(t,i) in form.tags" :key="i" class="tg">{{ t }}<span @click="form.tags.splice(i,1)">×</span></span>
            <input v-model="tagInput" placeholder="回车添加" @keydown.enter.prevent="addTag" />
          </div>
        </div>
      </div>

      <div class="f"><label>摘要</label><textarea v-model="form.summary" rows="2" placeholder="可选..." /></div>

      <div class="f">
        <div class="el"><label>内容</label>
          <div class="ts">
            <span :class="{ on: mode==='edit'}" @click="mode='edit'">编辑</span>
            <span :class="{ on: mode==='preview'}" @click="mode='preview'">预览</span>
            <span :class="{ on: mode==='split'}" @click="mode='split'">分屏</span>
          </div>
        </div>
        <div class="ed">
          <div class="ed-t"><span class="md">Markdown</span><span class="up" @click="triggerUpload">上传附件</span></div>
          <div class="ed-b">
            <textarea v-show="mode!=='preview'" v-model="form.content" class="code" placeholder="# 编写内容..."></textarea>
            <div v-show="mode!=='edit'" class="pr"><MarkdownViewer :content="form.content" /></div>
          </div>
        </div>
      </div>

      <div class="f"><label>附件</label>
        <div class="ua" @dragover.prevent @drop.prevent="handleDrop">
          <div class="ui" @click="triggerUpload"><p>拖拽或点击上传文件</p><p class="uh">支持任意格式</p></div>
          <input ref="fileInputRef" type="file" multiple accept=".doc,.docx,.pdf,.txt,.md" style="display:none" @change="handleFileChange" />
        </div>
        <div v-if="fileList.length" class="fl">
          <div v-for="(f,i) in fileList" :key="f.id||i" class="fm">
            <span class="fn">{{ f.fileName }} · {{ formatFileSize(f.size) }}</span>
            <span class="fa"><button class="fins" @click="insert(f)">插入</button><button class="frm" @click="removeFile(i)">×</button></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useKnowledgeStore } from '../stores/knowledge'
import { useCategoryStore } from '../stores/category'
import { uploadFiles, linkFilesToKnowledge } from '../api/upload'
import request from '../api/request'
import MarkdownViewer from '../components/MarkdownViewer.vue'

const props = defineProps({ id: [String, Number] })
const router = useRouter()
const store = useKnowledgeStore()
const categoryStore = useCategoryStore()
const isNew = computed(() => props.id === 'new')
const mode = ref('split')
const fileInputRef = ref(null)
const fileList = ref([])
const tagInput = ref('')
const form = reactive({
  title: '',
  content: '',
  summary: '',
  categoryId: null,
  tags: [],
  status: 1
})

/** 扁平化分类树为列表 */
const flatCategories = computed(() => {
  const result = []
  const flatten = (nodes) => {
    for (const node of nodes) {
      result.push(node)
      if (node.children) flatten(node.children)
    }
  }
  flatten(categoryStore.tree)
  return result
})

/** 添加标签 */
const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !form.tags.includes(tag)) {
    form.tags.push(tag)
  }
  tagInput.value = ''
}

/** 触发文件上传 */
const triggerUpload = () => fileInputRef.value?.click()

/** 文件选择变更 */
const handleFileChange = async (e) => {
  const files = Array.from(e.target.files)
  if (files.length) {
    await doUpload(files)
    e.target.value = ''
  }
}

/** 拖拽上传 */
const handleDrop = async (e) => {
  const files = Array.from(e.dataTransfer.files)
  if (files.length) await doUpload(files)
}

/** 执行文件上传 */
const doUpload = async (files) => {
  try {
    ElMessage.info('上传中...')
    const result = await uploadFiles(files)
    fileList.value.push(...result)
    ElMessage.success(result.length + ' 文件上传成功')
  } catch {
    ElMessage.error('上传失败')
  }
}

/** 移除文件 */
const removeFile = (index) => fileList.value.splice(index, 1)

/** 格式化文件大小 */
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

/** 插入附件到内容 */
const insert = async (file) => {
  const ext = file.fileName?.split('.').pop()?.toLowerCase()
  // .md 和 .txt 文件直接读取内容插入编辑器
  if (ext === 'md' || ext === 'txt') {
    try {
      // [FIX]: 使用 request 模块替代原始 fetch，确保携带 Auth Token
      const text = await request.get(`/files/download/${file.id}`, { responseType: 'text' })
      form.content += (form.content ? '\n\n' : '') + text
      ElMessage.success('文件内容已导入')
    } catch {
      ElMessage.error('读取文件内容失败')
    }
  } else {
    form.content += (form.content ? '\n\n' : '') + `[${file.fileName}](/api/files/download/${file.id})`
    ElMessage.success('已插入文件链接')
  }
}

/** 返回上一页 */
const goBack = () => router.back()

/** 保存知识条目 */
const handleSave = async (status) => {
  if (!form.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.categoryId) { ElMessage.warning('请选择分类'); return }
  if (!form.content.trim()) { ElMessage.warning('请输入内容'); return }

  form.status = status
  const data = isNew.value ? { ...form } : { ...form, id: Number(props.id) }

  try {
    const saved = await store.save(data)
    // 保存后关联已上传的文件到知识条目
    const knowledgeId = saved?.id || Number(props.id)
    if (fileList.value.length > 0) {
      const ids = fileList.value.map(f => f.id).join(',')
      try {
        await linkFilesToKnowledge(knowledgeId, ids)
      } catch (e) {
        console.warn('附件关联失败', e)
      }
    }
    ElMessage.success(status === 1 ? '已发布' : '已保存')
    router.push({ name: 'KnowledgeList' })
    store.fetchList()
  } catch (e) {
    ElMessage.error('保存失败，请稍后重试')
    console.error('保存知识失败:', e)
  }
}

onMounted(async () => {
  await categoryStore.fetchTree()
  if (!isNew.value) {
    try {
      const data = await store.fetchById(props.id)
      form.title = data.title
      form.content = data.content
      form.summary = data.summary || ''
      form.categoryId = data.categoryId
      form.tags = [...(data.tags || [])]
      form.status = data.status
    } catch (e) {
      ElMessage.error('加载知识条目失败')
      console.error('加载知识条目失败:', e)
    }
  }
})
</script>

<style scoped>
.edit { max-width: 1000px; margin: 24px auto 0; }
.h { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.b { padding: 6px 12px; border-radius: 6px; border: 1px solid #e7e0d8; background: #fff; color: #57534e; font-size: 13px; cursor: pointer; }
.t { flex:1; font-size: 16px; font-weight: 600; color: #1c1917; }
.as { display: flex; gap: 8px; }
.s { padding: 6px 14px; border-radius: 6px; border: 1px solid #e7e0d8; background: #fff; color: #57534e; font-size: 13px; cursor: pointer; }
.p { padding: 6px 14px; border-radius: 6px; border: none; background: linear-gradient(135deg, #d97706, #b45309); color: white; font-size: 13px; font-weight: 600; cursor: pointer; }

.body { background: #fff; border: 1px solid #e7e0d8; border-radius: 8px; padding: 24px; }
.f { margin-bottom: 16px; }
.f label { display: block; font-size: 12px; font-weight: 600; color: #8c847c; margin-bottom: 6px; }
.row { display: flex; gap: 12px; }
.f1 { flex:1; }
.f2 { flex:2; }

input:not([type]), textarea, select { width:100%; padding: 8px 12px; border-radius: 6px; border: 1px solid #e7e0d8; font-size: 14px; color: #1c1917; outline: none; }
input:focus, textarea:focus, select:focus { border-color: #d97706; }
input::placeholder, textarea::placeholder { color: #8c847c; }
textarea { resize: vertical; }

.tw { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; padding: 6px 10px; border-radius: 6px; border: 1px solid #e7e0d8; min-height: 36px; }
.tw input { border:none; padding:0; min-width:80px; }
.tg { display: inline-flex; align-items:center; gap:4px; padding:2px 8px; border-radius:4px; background:#fef3c7; color:#92400e; font-size:12px; }
.tg span { cursor:pointer; color:#8c847c; }

.el { display:flex; align-items:center; justify-content:space-between; margin-bottom:6px; }
.el label { margin-bottom:0; }
.ts { display:flex; gap:2px; background:#faf8f5; border-radius:6px; padding:2px; }
.ts span { padding:3px 10px; border-radius:4px; font-size:12px; color:#8c847c; cursor:pointer; }
.ts span.on { background:#fff; color:#1c1917; }

.ed { border:1px solid #e7e0d8; border-radius:8px; overflow:hidden; }
.ed-t { display:flex; align-items:center; justify-content:space-between; padding:6px 12px; background:#fdf8f3; border-bottom:1px solid #ede7e0; }
.md { font-size:11px; color:#8c847c; }
.up { font-size:12px; color:#8c847c; cursor:pointer; }
.up:hover { color:#44403c; }

.ed-b { display:flex; min-height:360px; }
.code { flex:1; border:none; outline:none; resize:none; padding:16px; background:#fdf8f3; color:#1c1917; font-size:13px; font-family: 'JetBrains Mono', monospace; line-height:1.7; }
.pr { flex:1; padding:16px 20px; overflow-y:auto; border-left:1px solid #ede7e0; }

.ua { border:1px dashed #e7e0d8; border-radius:8px; }
.ui { display:flex; flex-direction:column; align-items:center; padding:20px; cursor:pointer; }
.ui p { font-size:13px; color:#8c847c; margin:0; }
.uh { font-size:11px; color:#a8a29e; margin-top:4px !important; }

.fl { margin-top:8px; display:flex; flex-direction:column; gap:4px; }
.fm { display:flex; align-items:center; justify-content:space-between; padding:6px 12px; background:#fdf8f3; border:1px solid #ede7e0; border-radius:6px; }
.fn { font-size:13px; color:#57534e; }
.fa { display:flex; gap:8px; }
.fins { background:none; border:none; color:#d97706; cursor:pointer; font-size:12px; }
.frm { background:none; border:none; color:#8c847c; cursor:pointer; }
</style>
