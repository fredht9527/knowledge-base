import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { getKnowledgePage, getKnowledgeById, createKnowledge, updateKnowledge, deleteKnowledge } from '../api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const current = ref(null)
  const filter = reactive({
    page: 1,
    size: 6,
    keyword: '',
    categoryId: null,
    tagId: null,
    status: null
  })

  const setFilter = (newFilter) => {
    Object.assign(filter, newFilter, { page: 1 })
  }

  const fetchList = async () => {
    loading.value = true
    try {
      const res = await getKnowledgePage(filter)
      list.value = res.content
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  const fetchById = async (id) => {
    current.value = await getKnowledgeById(id)
    return current.value
  }

  const save = async (data) => {
    if (data.id) {
      return updateKnowledge(data.id, data)
    }
    return createKnowledge(data)
  }

  const remove = async (id) => {
    await deleteKnowledge(id)
    await fetchList()
  }

  return { list, total, loading, current, filter, setFilter, fetchList, fetchById, save, remove }
})
