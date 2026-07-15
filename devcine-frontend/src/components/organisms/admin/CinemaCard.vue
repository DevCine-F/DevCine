<script setup>
import { computed } from 'vue'

const props = defineProps({
  cinema: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

// Trạng thái hoạt động -> nhãn + màu (mặc định ACTIVE khi chưa set).
const statusMeta = computed(() => {
  const s = (props.cinema.status || 'ACTIVE').toString().toUpperCase()
  if (s === 'MAINTENANCE') return { label: 'Bảo trì', dot: 'bg-amber-400', text: 'text-amber-400' }
  if (s === 'CLOSED') return { label: 'Ngừng hoạt động', dot: 'bg-red-400', text: 'text-red-400' }
  return { label: 'Đang hoạt động', dot: 'bg-emerald-400', text: 'text-emerald-400' }
})

const location = computed(() =>
  [props.cinema.district, props.cinema.city].filter(Boolean).join(', ')
)
const roomCount = computed(() => props.cinema.halls?.length ?? props.cinema.rooms ?? 0)
</script>

<template>
  <div
    @click="$emit('click')"
    class="group relative flex flex-col rounded-2xl bg-surface-container-low border border-outline-variant/10 overflow-hidden cursor-pointer transition-all duration-500 hover:border-primary/40 hover:-translate-y-1 shadow-sm hover:shadow-[0_20px_40px_-12px_rgba(0,0,0,0.5)]"
  >
    <!-- Vệt nhấn vàng mảnh ở đỉnh card (premium accent) -->
    <div class="h-[3px] w-full bg-gradient-to-r from-primary/70 via-primary/30 to-transparent"></div>
    <!-- Hào quang vàng mờ khi hover -->
    <div class="pointer-events-none absolute -top-16 -right-16 w-40 h-40 rounded-full bg-primary/10 blur-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>

    <div class="relative p-6 flex flex-col flex-1">
      <!-- Header: icon + tên + trạng thái -->
      <div class="flex items-start gap-4">
        <div class="shrink-0 w-12 h-12 rounded-xl bg-gradient-to-br from-primary/25 to-primary/5 border border-primary/25 flex items-center justify-center text-primary group-hover:scale-105 transition-transform duration-500">
          <span class="material-symbols-outlined text-2xl">theaters</span>
        </div>

        <div class="min-w-0 flex-1">
          <h3 class="font-headline font-bold text-lg leading-tight text-on-surface truncate group-hover:text-primary transition-colors">
            {{ cinema.name }}
          </h3>
          <div v-if="location" class="flex items-center gap-1 mt-1 text-xs text-on-surface-variant truncate">
            <span class="material-symbols-outlined text-sm text-primary/70">location_on</span>
            <span class="truncate">{{ location }}</span>
          </div>
        </div>

        <!-- Trạng thái: chấm + nhãn -->
        <div class="shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-on-surface/5">
          <span class="w-1.5 h-1.5 rounded-full" :class="statusMeta.dot"></span>
          <span class="text-[9px] font-black uppercase tracking-wider" :class="statusMeta.text">{{ statusMeta.label }}</span>
        </div>
      </div>

      <!-- Địa chỉ chi tiết -->
      <p class="mt-4 text-xs leading-relaxed text-on-surface-variant/70 italic line-clamp-2 min-h-[2.5rem]">
        {{ cinema.address }}
      </p>

      <!-- Footer: số phòng + loại rạp + mũi tên -->
      <div class="mt-5 pt-5 border-t border-outline-variant/10 flex items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <div class="p-2 rounded-lg bg-on-surface/5 text-on-surface-variant">
            <span class="material-symbols-outlined text-lg">meeting_room</span>
          </div>
          <div class="leading-none">
            <p class="text-sm font-black text-on-surface tabular-nums">{{ roomCount }}</p>
            <p class="text-[8px] uppercase tracking-widest opacity-50 font-bold mt-0.5">Phòng chiếu</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <span
            v-if="cinema.type"
            class="text-[9px] font-black px-2.5 py-1 rounded-full uppercase tracking-widest text-primary/90 border border-primary/20 bg-primary/5"
            >{{ cinema.type }}</span
          >
          <div class="w-9 h-9 flex items-center justify-center rounded-xl bg-surface-container-high border border-outline-variant/10 text-on-surface-variant group-hover:bg-primary group-hover:text-on-primary group-hover:border-primary transition-all duration-500">
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
