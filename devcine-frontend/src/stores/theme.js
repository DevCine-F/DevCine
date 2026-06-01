import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const ambientColor = ref(null)
  const isWarping = ref(false)

  const setAmbientColor = (color) => {
    ambientColor.value = color
  }

  const resetAmbientColor = () => {
    ambientColor.value = null
  }

  const triggerWarp = () => {
    isWarping.value = true
    setTimeout(() => {
      isWarping.value = false
    }, 800) // Thời gian hiệu ứng Warp
  }

  return {
    ambientColor,
    isWarping,
    setAmbientColor,
    resetAmbientColor,
    triggerWarp
  }
})
