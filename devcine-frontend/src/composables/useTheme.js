import { ref, onMounted, watch } from 'vue'

export function useTheme() {
  const isLightMode = ref(false)

  const toggleTheme = () => {
    isLightMode.value = !isLightMode.value
  }

  // Watch for changes and apply to document, also save to localStorage
  watch(isLightMode, (newVal) => {
    if (newVal) {
      document.documentElement.classList.add('light')
      localStorage.setItem('theme', 'light')
    } else {
      document.documentElement.classList.remove('light')
      localStorage.setItem('theme', 'dark')
    }
  })

  // Initialize on mount
  onMounted(() => {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'light') {
      isLightMode.value = true
    } else if (!savedTheme) {
      // Default to dark mode if nothing saved
      isLightMode.value = false
    }
  })

  return {
    isLightMode,
    toggleTheme
  }
}
