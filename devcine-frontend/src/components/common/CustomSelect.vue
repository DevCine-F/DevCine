<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    required: true
  },
  options: {
    type: Array,
    required: true
  },
  placeholder: {
    type: String,
    default: 'Chọn một mục'
  },
  customClass: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const selectRef = ref(null)

const selectedOption = computed(() => {
  return props.options.find(opt => opt.value === props.modelValue)
})

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
}

const selectOption = (option) => {
  emit('update:modelValue', option.value)
  isOpen.value = false
}

const handleClickOutside = (event) => {
  if (selectRef.value && !selectRef.value.contains(event.target)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="relative" ref="selectRef">
    <!-- Trigger Button -->
    <button 
      type="button" 
      @click="toggleDropdown"
      :class="customClass || 'w-full p-4 rounded-xl text-sm border-outline-variant/20'"
      class="flex items-center justify-between bg-surface-container-highest border font-bold text-on-surface focus:border-primary outline-none transition-colors"
    >
      <span :class="{'text-on-surface-variant': !selectedOption}">
        {{ selectedOption ? selectedOption.label : placeholder }}
      </span>
      <span class="material-symbols-outlined text-on-surface-variant transition-transform duration-300" :class="{ 'rotate-180': isOpen }">
        expand_more
      </span>
    </button>

    <!-- Dropdown Menu -->
    <transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="transform scale-95 opacity-0"
      enter-to-class="transform scale-100 opacity-100"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="transform scale-100 opacity-100"
      leave-to-class="transform scale-95 opacity-0"
    >
      <div 
        v-if="isOpen" 
        class="absolute z-50 w-full mt-2 rounded-xl border border-outline-variant/20 bg-surface-container-high/80 backdrop-blur-md shadow-2xl overflow-hidden"
      >
        <button
          v-for="option in options"
          :key="option.value"
          type="button"
          @click="selectOption(option)"
          class="w-full text-left px-4 py-3 text-sm font-bold text-on-surface hover:bg-white/10 transition-colors flex items-center justify-between group"
          :class="{'bg-primary/20 text-primary': modelValue === option.value}"
        >
          {{ option.label }}
          <span v-if="modelValue === option.value" class="material-symbols-outlined text-sm">check</span>
        </button>
      </div>
    </transition>
  </div>
</template>
