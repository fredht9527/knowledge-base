<template>
  <div class="list">
    <div class="head"><h2>{{ currentCategoryName || '全部知识' }}</h2><span class="c">{{ knowledgeStore.total }} 篇</span><button class="create-btn" @click="handleCreate">+ 新建知识</button></div>

    <div v-loading="knowledgeStore.loading">
      <template v-if="knowledgeStore.list.length">
        <div class="cards">
          <div v-for="item in knowledgeStore.list" :key="item.id" class="card" @click="goDetail(item.id)">
            <div class="card-head">
              <div class="dot" :class="{ d: item.status !== 1 }"></div>
              <span class="title">{{ item.title }}</span>
              <el-dropdown trigger="click" @command="(cmd) => handleCardAction(cmd, item)" @click.stop>
                <span class="m">⋯</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <p class="summary">{{ item.summary || item.content?.replace(/[#*`\[\]>\-]/g, '').slice(0, 120) + '...' }}</p>
            <div class="foot">
              <div class="tags">
                <span v-if="item.categoryName" class="t ct">{{ item.categoryName }}</span>
                <span v-for="t in (item.tags || []).slice(0,3)" :key="t" class="t">{{ t }}</span>
              </div>
              <div class="meta">
                <span>{{ item.viewCount }} 次</span>
                <span>{{ formatTime(item.updatedAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="empty">
        <p class="e1">还没有知识条目</p>
        <p class="e2">点击下方按钮创建第一条</p>
        <button class="btn" @click="handleCreate">+ 新建知识</button>
      </div>
    </div>

    <div class="pg" v-if="knowledgeStore.total > knowledgeStore.filter.size">
      <el-pagination v-model:current-page="knowledgeStore.filter.page" v-model:page-size="knowledgeStore.filter.size"
        :total="knowledgeStore.total" :page-sizes="[6,10,20,50]" layout="sizes, prev, pager, next" background
        @current-change="fetchData" @size-change="fetchData" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useKnowledgeStore } from '../stores/knowledge'
import { useCategoryStore } from '../stores/category'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()
const categoryStore = useCategoryStore()

const currentCategoryName = computed(() => {
  if (!categoryStore.selectedId) return ''
  const f = (l) => { for (const i of l) { if (i.id === categoryStore.selectedId) return i.name; if (i.children) { const r = f(i.children); if (r) return r } } }
  return f(categoryStore.tree)
})

const MS_PER_DAY = 86400000
const formatTime = (t) => { if (!t) return ''; const d = new Date(t); const df = Math.floor((Date.now()-d)/MS_PER_DAY); if (df===0) return '今天'; if (df===1) return '昨天'; if (df<7) return df+'天前'; return t.slice(0,10) }

const fetchData = () => knowledgeStore.fetchList()
const goDetail = (id) => router.push({ name: 'KnowledgeDetail', params: { id } })
const goEdit = (id) => router.push({ name: 'KnowledgeEdit', params: { id } })
const handleCreate = () => router.push({ name: 'KnowledgeEdit', params: { id: 'new' } })

const handleCardAction = async (cmd, item) => {
  if (cmd === 'edit') goEdit(item.id)
  else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(`删除「${item.title}」？`)
      await knowledgeStore.remove(item.id)
      ElMessage.success('已删除')
    } catch (e) {
      // 用户取消或删除失败
      if (e !== 'cancel') {
        ElMessage.error('删除失败')
        console.error('删除失败:', e)
      }
    }
  }
}

onMounted(() => fetchData())
watch(() => categoryStore.selectedId, fetchData)
</script>

<style scoped>
.list { max-width: 900px; margin: 24px auto 0; }
.head { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.head h2 { font-size: 18px; font-weight: 600; color: #1c1917; }
.c { font-size: 13px; color: #8c847c; margin-right: auto; }
.create-btn {
  padding: 6px 14px; border-radius: 8px; border: none;
  background: linear-gradient(135deg, #d97706, #b45309);
  color: white; font-size: 12px; font-weight: 600; cursor: pointer;
  transition: opacity .15s; flex-shrink: 0;
}
.create-btn:hover { opacity: 0.9; }

.cards { display: flex; flex-direction: column; gap: 8px; }

.card { background: #fff; border: 1px solid #e7e0d8; border-radius: 8px; padding: 16px 20px; cursor: pointer; }
.card:hover { border-color: #d6cdc3; background: #fdf8f3; }

.card-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #84cc16; flex-shrink: 0; }
.dot.d { background: #e7e0d8; }
.title { flex: 1; font-size: 15px; font-weight: 500; color: #1c1917; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.m { opacity: 0; color: #8c847c; cursor: pointer; }
.card:hover .m { opacity: 1; }

.summary { color: #8c847c; font-size: 13px; line-height: 1.6; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.foot { display: flex; align-items: center; justify-content: space-between; }
.tags { display: flex; gap: 4px; flex-wrap: wrap; }
.t { font-size: 11px; padding: 1px 8px; border-radius: 3px; background: #faf8f5; color: #8c847c; }
.ct { background: #fef3c7; color: #92400e; }
.meta { display: flex; gap: 12px; font-size: 12px; color: #a8a29e; }

.empty { text-align: center; padding: 100px 0; }
.e1 { font-size: 16px; font-weight: 500; color: #44403c; margin-bottom: 6px; }
.e2 { font-size: 13px; color: #8c847c; margin-bottom: 20px; }
.btn { padding: 8px 20px; border: none; border-radius: 8px; background: linear-gradient(135deg, #d97706, #b45309); color: white; font-size: 14px; font-weight: 600; cursor: pointer; }

.pg { display: flex; justify-content: center; margin-top: 24px; }
:deep(.el-pagination.is-background .el-pager li) { background: #fff !important; color: #8c847c !important; border: 1px solid #e7e0d8 !important; border-radius: 6px !important; }
:deep(.el-pagination.is-background .el-pager li.is-active) { background: linear-gradient(135deg, #d97706, #b45309) !important; color: white !important; border: none !important; }
:deep(.el-pagination button) { background: #fff !important; color: #8c847c !important; border: 1px solid #e7e0d8 !important; border-radius: 6px !important; }
:deep(.el-loading-spinner .path) { stroke: #d97706 !important; }
</style>
