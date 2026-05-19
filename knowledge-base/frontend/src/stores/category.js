import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '../api/category'

export const useCategoryStore = defineStore('category', () => {
  const tree = ref([])
  const loading = ref(false)
  const selectedId = ref(null)

  const fetchTree = async () => {
    loading.value = true
    try {
      tree.value = await getCategoryTree()
    } finally {
      loading.value = false
    }
  }

  const save = async (data) => {
    if (data.id) {
      await updateCategory(data.id, data)
    } else {
      await createCategory(data)
    }
    await fetchTree()
  }

  const remove = async (id) => {
    await deleteCategory(id)
    await fetchTree()
  }

  const selectCategory = (id) => {
    selectedId.value = id
  }

  return { tree, loading, selectedId, fetchTree, save, remove, selectCategory }
})
