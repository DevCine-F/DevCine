<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '-- Chọn --'
  }
});

const emit = defineEmits(['update:modelValue']);

const isOpen = ref(false);
const selectRef = ref(null);

const selectedOption = computed(() => {
  return props.options.find(opt => opt.value === props.modelValue);
});

const toggleOpen = () => {
  isOpen.value = !isOpen.value;
};

const selectOption = (option) => {
  emit('update:modelValue', option.value);
  isOpen.value = false;
};

const handleClickOutside = (event) => {
  if (selectRef.value && !selectRef.value.contains(event.target)) {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
  <div class="relative w-full text-sm font-medium" ref="selectRef">
    <!-- Selected Value Display -->
    <div 
      @click="toggleOpen"
      class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 flex items-center justify-between cursor-pointer transition-colors"
      :class="isOpen ? 'border-primary/50 text-white' : 'hover:border-white/20 text-white/80'"
    >
      <span class="truncate pr-4" :class="{ 'text-white/40': !selectedOption }">
        {{ selectedOption ? selectedOption.label : placeholder }}
      </span>
      <span 
        class="material-symbols-outlined text-lg transition-transform duration-300"
        :class="{ 'rotate-180 text-primary': isOpen, 'text-white/40': !isOpen }"
      >
        expand_more
      </span>
    </div>

    <!-- Dropdown Options -->
    <Transition name="fade-slide">
      <div 
        v-if="isOpen" 
        class="absolute z-[100] mt-2 w-full bg-[#1A1C1E] border border-white/10 rounded-xl shadow-2xl max-h-60 overflow-y-auto overflow-x-hidden scrollbar-thin custom-scrollbar"
      >
        <div class="py-1">
          <div 
            v-if="options.length === 0" 
            class="px-4 py-3 text-white/40 italic text-center"
          >
            Không có dữ liệu
          </div>
          <div
            v-for="option in options"
            :key="option.value"
            @click="selectOption(option)"
            class="px-4 py-3 cursor-pointer transition-colors truncate"
            :class="option.value === modelValue 
              ? 'bg-primary/10 text-primary font-bold border-l-2 border-primary' 
              : 'text-white/80 hover:bg-white/5 hover:text-white border-l-2 border-transparent'"
          >
            {{ option.label }}
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease-out;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.2);
}
</style>
