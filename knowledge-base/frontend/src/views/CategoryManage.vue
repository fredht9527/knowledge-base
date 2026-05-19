<template>
  <div class="cat-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <button class="add-btn" @click="showDialog(null)">+ 新增主分类</button>
    </div>

    <!-- 主分类列表 -->
    <div class="cat-list">
      <template v-for="cat in parentList" :key="cat.id">
        <div class="cat-row parent-row">
          <div class="cat-name" @click="toggleChild(cat.id)">
            <svg :class="{ rotated: expanded.has(cat.id) }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
            <span class="name-text">{{ cat.name }}</span>
            <span class="child-count" v-if="cat.children?.length">({{ cat.children.length }})</span>
          </div>
          <div class="cat-actions">
            <button class="act" @click="showDialog(cat)">新增子分类</button>
            <button class="act" @click="showDialog(cat, true)">编辑</button>
            <button class="act del" @click="handleDelete(cat)">删除</button>
          </div>
        </div>

        <!-- 子分类列表 -->
        <transition name="slide">
          <div v-if="expanded.has(cat.id)" class="children">
            <div v-if="cat.children?.length" class="child-list">
              <div v-for="ch in cat.children" :key="ch.id" class="cat-row child-row">
                <div class="cat-name">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                  <span class="name-text">{{ ch.name }}</span>
                </div>
                <div class="cat-actions">
                  <button class="act" @click="showDialog(ch, true)">编辑</button>
                  <button class="act del" @click="handleDelete(ch)">删除</button>
                </div>
              </div>
            </div>
            <div v-else class="no-child">暂无子分类</div>
          </div>
        </transition>
      </template>

      <div v-if="!parentList.length" class="empty">暂无分类，点击上方按钮新增</div>
    </div>

    <!-- 分页 -->
    <div class="pg" v-if="total > 0">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
        v-model:current-page="currentPage" @current-change="loadData" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="输入分类名称" maxlength="100" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ editingId ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '../api/category'

const pageSize = 10
const currentPage = ref(1)
const total = ref(0)
const allCategories = ref([])
const expanded = ref(new Set())

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const editingId = ref(null)
const form = reactive({ name: '', description: '', parentId: null })
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

// 当前页的主分类
const parentList = ref([])

const loadData = async () => {
  try {
    const tree = await getCategoryTree()
    allCategories.value = tree

    // 展平主分类
    const all = []
    const flatten = (nodes) => {
      for (const n of nodes) {
        all.push(n)
      }
    }
    flatten(tree)

    total.value = all.length

    // 分页
    const start = (currentPage.value - 1) * pageSize
    parentList.value = all.slice(start, start + pageSize)
  } catch (e) {
    ElMessage.error('加载分类失败')
    console.error('加载分类失败:', e)
  }
}

const toggleChild = (id) => {
  const s = new Set(expanded.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  expanded.value = s
}

const showDialog = (parentNode, isEdit = false) => {
  if (isEdit && parentNode) {
    // 编辑模式
    editingId.value = parentNode.id
    dialogTitle.value = '编辑分类'
    form.name = parentNode.name
    form.description = parentNode.description || ''
    form.parentId = parentNode.parentId || null
  } else {
    // 新增模式
    editingId.value = null
    if (parentNode) {
      dialogTitle.value = '新增子分类'
      form.parentId = parentNode.id
    } else {
      dialogTitle.value = '新增主分类'
      form.parentId = null
    }
    form.name = ''
    form.description = ''
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const data = { ...form }
    if (editingId.value) data.id = editingId.value
    const api = editingId.value ? updateCategory(editingId.value, data) : createCategory(data)
    await api
    ElMessage.success(editingId.value ? '修改成功' : '创建成功')
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    if (e !== false) {  // 表单校验失败返回 false，不提示
      ElMessage.error('操作失败')
      console.error('分类操作失败:', e)
    }
  }
}

const handleDelete = async (node) => {
  try {
    await ElMessageBox.confirm(`确定删除「${node.name}」吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消' })
    await deleteCategory(node.id)
    ElMessage.success('已删除')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('删除分类失败:', e)
    }
  }
}

onMounted(loadData)
</script>

<style scoped>
.cat-page { max-width: 800px; margin: 24px auto 0; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #1c1917; }

.add-btn {
  padding: 8px 18px; border-radius: 8px; border: none;
  background: linear-gradient(135deg, #d97706, #b45309); color: white;
  font-size: 13px; font-weight: 600; cursor: pointer;
}
.add-btn:hover { opacity: 0.9; }

.cat-list { background: #fff; border: 1px solid #e7e0d8; border-radius: 10px; overflow: hidden; }

.cat-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; transition: background 120ms;
}
.parent-row { border-bottom: 1px solid #ede7e0; }
.child-row { padding: 10px 20px 10px 48px; }
.child-row + .child-row { border-top: 1px solid #fdf8f3; }

.cat-name {
  display: flex; align-items: center; gap: 8px; cursor: pointer; flex: 1;
}
.cat-name svg { flex-shrink: 0; color: #8c847c; transition: transform 200ms; }
.cat-name svg.rotated { transform: rotate(90deg); }
.name-text { font-size: 14px; font-weight: 500; color: #1c1917; }
.child-count { font-size: 12px; color: #8c847c; }

.cat-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 120ms; }
.cat-row:hover .cat-actions { opacity: 1; }

.act {
  padding: 4px 10px; border-radius: 4px; border: none;
  font-size: 12px; cursor: pointer;
  background: #faf8f5; color: #57534e;
}
.act:hover { background: #ede7e0; }
.act.del { color: #ef4444; }
.act.del:hover { background: #fef2f2; }

.children { background: #fdf8f3; }
.child-list { border-top: 1px solid #ede7e0; }

.no-child {
  padding: 16px 20px 16px 48px; font-size: 13px; color: #8c847c;
  border-top: 1px solid #ede7e0;
}

.empty { padding: 60px; text-align: center; font-size: 14px; color: #8c847c; }

.pg { display: flex; justify-content: center; margin-top: 24px; }

.slide-enter-active, .slide-leave-active { transition: all 200ms ease; }
.slide-enter-from { opacity: 0; max-height: 0; }
.slide-enter-to { opacity: 1; max-height: 500px; }
.slide-leave-from { opacity: 1; max-height: 500px; }
.slide-leave-to { opacity: 0; max-height: 0; }

:deep(.el-pagination.is-background .el-pager li) { background: #fff !important; color: #57534e !important; border: 1px solid #e7e0d8 !important; border-radius: 6px !important; }
:deep(.el-pagination.is-background .el-pager li.is-active) { background: linear-gradient(135deg, #d97706, #b45309) !important; color: white !important; border: none !important; }
:deep(.el-pagination button) { background: #fff !important; color: #57534e !important; border: 1px solid #e7e0d8 !important; border-radius: 6px !important; }
</style>
