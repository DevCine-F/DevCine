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

const slots = computed(() => {
  const list = props.fnbItem?.slots ? [...props.fnbItem.slots] : []
  return list.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
})

const selectedBySlot = ref({})
const activeSlotId = ref(null)

watch(() => props.isOpen, (open) => {
  if (!open || !props.fnbItem) {
    activeSlotId.value = null
    return
  }
  const state = {}
  for (const slot of slots.value) {
    let init = props.initialOptions
      .filter(o => o.slotId === slot.id)
      .map(o => o.optionItemId)
    if (init.length === 0 && slot.defaultOptionItemId) {
      init = [slot.defaultOptionItemId]
    }
    state[slot.id] = init
  }
  selectedBySlot.value = state
  activeSlotId.value = null // reset view
})

const isSelected = (slot, option) =>
  (selectedBySlot.value[slot.id] || []).includes(option.id)

const toggleOption = (slot, option) => {
  const current = selectedBySlot.value[slot.id] || []
  const idx = current.indexOf(option.id)
  const max = slot.maxChoices ?? 1

  if (idx !== -1) {
    current.splice(idx, 1)
  } else if (max === 1) {
    selectedBySlot.value[slot.id] = [option.id]
    // Tự động back về list nếu chọn 1
    activeSlotId.value = null
  } else if (current.length < max) {
    current.push(option.id)
  } else {
    current.shift()
    current.push(option.id)
  }
}

const slotHint = (slot) => {
  const min = slot.minChoices ?? 0
  const max = slot.maxChoices ?? 1
  return min === max ? `Chọn ${max}` : `Chọn ${min} - ${max}`
}

const isValid = computed(() => {
  for (const slot of slots.value) {
    const count = (selectedBySlot.value[slot.id] || []).length
    if (slot.isRequired && count === 0) return false
    if (count > 0 && count < (slot.minChoices ?? 0)) return false
    if (count > (slot.maxChoices ?? 1)) return false
  }
  return true
})

const selectedOptions = computed(() => {
  const out = []
  for (const slot of slots.value) {
    const ids = selectedBySlot.value[slot.id] || []
    for (const optId of ids) {
      const opt = (slot.optionGroup?.items || []).find(i => i.id === optId)
      if (!opt) continue
      out.push({
        slotId: slot.id,
        slotLabel: slot.slotLabel,
        optionGroupId: slot.optionGroup?.id ?? null,
        optionItemId: opt.id,
        optionName: opt.name,
        surchargePrice: Number(opt.surchargePrice) || 0,
        snapshotSurcharge: Number(opt.surchargePrice) || 0
      })
    }
  }
  return out
})

const totalSurcharge = computed(() =>
  selectedOptions.value.reduce((sum, o) => sum + o.surchargePrice, 0))

const totalPrice = computed(() =>
  (Number(props.fnbItem?.price) || 0) + totalSurcharge.value)

const confirm = () => {
  if (!isValid.value) return
  emit('confirm', { options: selectedOptions.value, totalSurcharge: totalSurcharge.value })
  emit('close')
}

const activeSlot = computed(() => slots.value.find(s => s.id === activeSlotId.value))

const getSelectedNames = (slot) => {
  const ids = selectedBySlot.value[slot.id] || []
  if (ids.length === 0) return 'Chưa chọn'
  const names = ids.map(id => slot.optionGroup?.items?.find(i => i.id === id)?.name).filter(Boolean)
  return names.join(', ')
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
    <div class="bg-surface-container rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
      <!-- Header -->
      <div class="p-4 sm:p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-highest">
        <div class="flex items-center gap-3">
          <button v-if="activeSlotId" @click="activeSlotId = null" class="text-on-surface-variant hover:text-on-surface">
            <span class="material-symbols-outlined">arrow_back</span>
          </button>
          <h3 class="font-headline font-bold text-lg sm:text-xl text-on-surface">
            {{ activeSlotId ? activeSlot?.slotLabel : fnbItem?.name }}
          </h3>
        </div>
        <button @click="emit('close')" class="text-on-surface-variant hover:text-on-surface">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Body -->
      <div class="p-4 sm:p-6 overflow-y-auto space-y-3 sm:space-y-4 flex-grow custom-scrollbar bg-surface">
        <div v-if="slots.length === 0" class="text-sm text-on-surface-variant text-center py-6">
          Món này không cần chọn tùy chọn.
        </div>

        <!-- Màn hình chính: Danh sách các Slot -->
        <template v-else-if="!activeSlotId">
          <div
            v-for="slot in slots" :key="slot.id"
            @click="activeSlotId = slot.id"
            class="flex items-center justify-between p-3.5 sm:p-4 bg-surface-container-highest rounded-xl cursor-pointer hover:bg-surface-container-highest/80 border border-outline-variant/20 transition-colors"
          >
            <div class="flex flex-col">
              <span class="font-bold text-xs sm:text-sm text-on-surface">{{ slot.slotLabel }} <span v-if="slot.isRequired" class="text-red-400">*</span></span>
              <span class="text-xs text-on-surface-variant">{{ getSelectedNames(slot) }}</span>
            </div>
            <span class="material-symbols-outlined text-on-surface-variant text-lg">chevron_right</span>
          </div>
        </template>

        <!-- Màn hình phụ: Danh sách các option của Slot đang chọn -->
        <template v-else>
          <div class="flex justify-between items-end mb-2">
            <h4 class="font-bold text-on-surface text-xs sm:text-sm text-on-surface-variant">{{ slotHint(activeSlot) }}</h4>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5 sm:gap-3">
            <button
              v-for="opt in (activeSlot.optionGroup?.items || [])" :key="opt.id"
              @click="toggleOption(activeSlot, opt)"
              :class="[
                isSelected(activeSlot, opt)
                  ? 'border-primary bg-primary/10 text-primary-container'
                  : 'border-outline-variant/20 hover:border-primary/50 text-on-surface bg-surface-container-highest',
                'border rounded-xl p-3 text-left transition-colors flex flex-col gap-1 items-start group-button'
              ]"
            >
              <span class="text-xs sm:text-sm font-semibold">{{ opt.name }}</span>
              <span v-if="opt.surchargePrice > 0" class="text-xs font-bold text-primary-container">+{{ Number(opt.surchargePrice).toLocaleString('vi-VN') }}đ</span>
              <span v-else class="text-xs text-on-surface-variant">+0đ</span>
            </button>
          </div>
        </template>
      </div>

      <!-- Footer -->
      <div v-if="!activeSlotId" class="p-4 sm:p-6 border-t border-outline-variant/10 bg-surface-container-highest flex justify-between items-center">
        <div class="flex flex-col">
          <span class="text-[11px] sm:text-xs text-on-surface-variant">Tạm tính</span>
          <span class="font-headline font-bold text-base sm:text-lg text-primary-container">{{ totalPrice.toLocaleString('vi-VN') }}đ</span>
        </div>
        <button
          @click="confirm"
          :disabled="!isValid"
          class="bg-primary text-on-primary px-6 sm:px-8 py-2.5 sm:py-3 rounded-xl text-xs sm:text-sm font-bold transition-all disabled:opacity-50 hover:brightness-110 active:scale-95"
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
