<template>
  <div class="tree">
    <div class="sec-label">导航</div>

    <div class="item" :class="{ on: route.name === 'Home' }" @click="goHome">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
      <span>首页</span>
    </div>

    <div class="item" :class="{ on: route.name === 'KnowledgeList' || route.name === 'KnowledgeDetail' || route.name === 'KnowledgeEdit' }" @click="goAll">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
      <span>全部知识</span>
    </div>

    <div class="item" :class="{ on: route.name === 'CategoryManage' }" @click="goCategory">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
      <span>分类管理</span>
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useCategoryStore } from '../stores/category'
import { useKnowledgeStore } from '../stores/knowledge'

const router = useRouter()
const route = useRoute()
const categoryStore = useCategoryStore()
const knowledgeStore = useKnowledgeStore()

const goHome = () => { router.push({ name: 'Home' }) }

const goAll = () => {
  categoryStore.selectCategory(null)
  knowledgeStore.setFilter({ categoryId: null, keyword: '' })
  router.push({ name: 'KnowledgeList' })
  knowledgeStore.fetchList()
}

const goCategory = () => { router.push({ name: 'CategoryManage' }) }
</script>

<style scoped>
.tree { padding: 12px 0; }
.sec-label { padding: 8px 16px 6px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; color: #8c847c; }

.item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 16px; margin: 2px 8px; border-radius: 8px;
  font-size: 12px; color: #57534e;
  cursor: pointer; transition: all 120ms;
}
.item:hover { background: #ede7e0; color: #292524; }
.item.on { background: #ede7e0; color: #1c1917; font-weight: 500; }
.item svg { flex-shrink: 0; }
</style>
