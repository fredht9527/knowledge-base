<template>
  <div class="detail" v-loading="loading">
    <template v-if="detail">
      <div class="bar">
        <button class="b" @click="goBack">← 返回</button>
        <button class="e" @click="goEdit">编辑</button>
      </div>

      <div class="article">
        <div class="hd">
          <h1>{{ detail.title }}</h1>
          <div class="meta">
            <span v-if="detail.categoryName" class="tag bg">{{ detail.categoryName }}</span>
            <span v-for="t in (detail.tags || [])" :key="t" class="tag">{{ t }}</span>
            <span>{{ detail.viewCount }} 浏览</span>
            <span>{{ formatTime(detail.updatedAt) }}</span>
          </div>
        </div>
        <div class="bd"><MarkdownViewer :content="detail.content" /></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useKnowledgeStore } from '../stores/knowledge'
import MarkdownViewer from '../components/MarkdownViewer.vue'

const props = defineProps({ id: [String, Number] })
const router = useRouter()
const knowledgeStore = useKnowledgeStore()
const loading = ref(false)
const detail = ref(null)

const formatTime = (t) => { if (!t) return ''; return new Date(t).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }
const fetchData = async () => { loading.value = true; try { detail.value = await knowledgeStore.fetchById(props.id) } finally { loading.value = false } }
const goBack = () => router.push({ name: 'KnowledgeList' })
const goEdit = () => router.push({ name: 'KnowledgeEdit', params: { id: props.id } })
onMounted(fetchData)
</script>

<style scoped>
.detail { max-width: 1000px; margin: 24px auto 0; }
.bar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.b { padding: 6px 14px; border-radius: 6px; border: 1px solid #e7e0d8; background: #fff; color: #57534e; font-size: 13px; cursor: pointer; }
.b:hover { border-color: #d6cdc3; }
.e { padding: 6px 18px; border-radius: 6px; border: none; background: linear-gradient(135deg, #d97706, #b45309); color: white; font-size: 13px; font-weight: 600; cursor: pointer; }

.article { background: #fff; border: 1px solid #e7e0d8; border-radius: 8px; padding: 20px 24px; }
.hd { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #ede7e0; }
.hd h1 { font-size: 18px; font-weight: 600; color: #1c1917; margin-bottom: 8px; }
.meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 12px; color: #8c847c; }
.tag { padding: 2px 10px; border-radius: 4px; background: #faf8f5; color: #57534e; font-size: 11px; }
.bg { background: #fef3c7; color: #92400e; }
</style>
