import { ref, computed } from 'vue'

/**
 * 邮箱输入建议 Composable
 * 从 Login 和 Register 中提取的共用逻辑
 */
const EMAIL_SUFFIXES = [
  '@qq.com', '@163.com', '@126.com', '@gmail.com',
  '@sina.com', '@sohu.com', '@foxmail.com', '@outlook.com'
]

export function useEmailSuggest() {
  const email = ref('')
  const showSuggestion = ref(false)

  const emailParts = computed(() => {
    const idx = email.value.indexOf('@')
    return idx <= 0
      ? { name: email.value, suffix: '' }
      : { name: email.value.slice(0, idx), suffix: email.value.slice(idx) }
  })

  const suggestList = computed(() => {
    const { name, suffix } = emailParts.value
    if (!name || suffix.includes('.')) return []
    return EMAIL_SUFFIXES
      .filter(s => s.startsWith(suffix))
      .map(s => name + s)
  })

  const validEmail = computed(() =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)
  )

  function onEmailInput() {
    showSuggestion.value = email.value.length > 0 && !email.value.includes('.')
  }

  function selectSuggestion(suggestion) {
    email.value = suggestion
    showSuggestion.value = false
  }

  return {
    email,
    showSuggestion,
    suggestList,
    validEmail,
    onEmailInput,
    selectSuggestion
  }
}
