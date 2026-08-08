<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  isOpen: Boolean,
  fnbItem: Object,
  initialOptions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'confirm'])

// state lưu trữ các tùy chọn đang chọn: map groupId -> array of selected optionItemId
const selectedOptions = ref({})

// Reset state when modal opens
watch(() => props.isOpen, (newVal) => {
  if (newVal && props.fnbItem) {
    const newState = {}
    props.fnbItem.optionGroups?.forEach(group => {
      // Tìm xem trong initialOptions có món nào thuộc group này không
      const initForGroup = props.initialOptions.filter(o => o.optionGroupId === group.id).map(o => o.optionItemId)
      newState[group.id] = initForGroup.length ? initForGroup : []
    })
    selectedOptions.value = newState
  }
})

const toggleOption = (group, option) => {
  const current = selectedOptions.value[group.id] || []
  const idx = current.indexOf(option.id)
  if (idx !== -1) {
    // Bỏ chọn (nếu cho phép dưới minChoices thì sẽ validate sau khi confirm)
    current.splice(idx, 1)
  } else {
    // Chọn mới
    if (group.maxChoices === 1) {
      selectedOptions.value[group.id] = [option.id] // Replace
    } else {
      if (current.length < group.maxChoices) {
        current.push(option.id)
      }
    }
  }
}

const isValid = computed(() => {
  if (!props.fnbItem || !props.fnbItem.optionGroups) return true
  for (const group of props.fnbItem.optionGroups) {
    const current = selectedOptions.value[group.id] || []
    if (current.length < group.minChoices) return false
    if (current.length > group.maxChoices) return false
  }
  return true
})

const totalPrice = computed(() => {
  if (!props.fnbItem) return 0
  let total = props.fnbItem.price || 0
  
  if (props.fnbItem.optionGroups) {
    for (const group of props.fnbItem.optionGroups) {
      const current = selectedOptions.value[group.id] || []
      for (const optId of current) {
        const opt = group.items?.find(i => i.id === optId)
        if (opt) {
          total += opt.surchargePrice || 0
        }
      }
    }
  }
  return total
})

const confirm = () => {
  if (!isValid.value) return
  
  // Format lại thành mảng các options
  const finalOptions = []
  if (props.fnbItem.optionGroups) {
    for (const group of props.fnbItem.optionGroups) {
      const current = selectedOptions.value[group.id] || []
      for (const optId of current) {
        const opt = group.items?.find(i => i.id === optId)
        if (opt) {
          finalOptions.push({
            optionGroupId: group.id,
            optionItemId: opt.id,
            optionName: opt.name,
            surchargePrice: opt.surchargePrice || 0
          })
        }
      }
    }
  }
  
  emit('confirm', finalOptions)
  emit('close')
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
    <div class="bg-surface-container rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
      <!-- Header -->
      <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-highest">
        <h3 class="font-headline font-bold text-xl text-on-surface">Tùy chọn: {{ fnbItem?.name }}</h3>
        <button @click="emit('close')" class="text-on-surface-variant hover:text-on-surface">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      
      <!-- Body -->
      <div class="p-6 overflow-y-auto space-y-6 flex-grow custom-scrollbar">
        <div v-for="group in fnbItem?.optionGroups" :key="group.id" class="space-y-3">
          <div class="flex justify-between items-end">
            <h4 class="font-bold text-on-surface">{{ group.name }}</h4>
            <span class="text-xs text-on-surface-variant">
              Chọn {{ group.minChoices === group.maxChoices ? group.minChoices : `${group.minChoices} - ${group.maxChoices}` }}
            </span>
          </div>
          
          <div class="grid grid-cols-2 gap-3">
            <button 
              v-for="opt in group.items" :key="opt.id"
              @click="toggleOption(group, opt)"
              :class="[
                (selectedOptions[group.id] || []).includes(opt.id) 
                  ? 'border-primary bg-primary/10 text-primary-container' 
                  : 'border-outline-variant/20 hover:border-primary/50 text-on-surface bg-surface-container-highest',
                'border rounded-xl p-3 text-left transition-colors flex justify-between items-center group-button'
              ]"
            >
              <span class="text-sm font-semibold">{{ opt.name }}</span>
              <span v-if="opt.surchargePrice > 0" class="text-xs font-bold">+{{ (opt.surchargePrice).toLocaleString('vi-VN') }}đ</span>
            </button>
          </div>
        </div>
      </div>
      
      <!-- Footer -->
      <div class="p-6 border-t border-outline-variant/10 bg-surface-container-highest flex justify-between items-center">
        <div class="flex flex-col">
          <span class="text-xs text-on-surface-variant">Tạm tính</span>
          <span class="font-headline font-bold text-lg text-primary-container">{{ totalPrice.toLocaleString('vi-VN') }} VNĐ</span>
        </div>
        <button 
          @click="confirm"
          :disabled="!isValid"
          class="bg-primary text-on-primary px-8 py-3 rounded-xl font-bold transition-all disabled:opacity-50 hover:brightness-110 active:scale-95"
        >
          Xác nhận
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
</style>
