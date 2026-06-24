<script setup>
import { ref } from 'vue'
import axios from 'axios'
import SeatMapBuilder from '@/components/admin/SeatMapBuilder.vue'
import { useToastStore } from '@/stores/toast'

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/api'
const toast = useToastStore()

const layoutData = ref({
  rows: 10,
  cols: 12,
  seats: {}
})

const handleUpdateLayout = (data) => {
  layoutData.value = data
}

const saveLayout = async () => {
  try {
    // Lưu tạm vào localStorage
    localStorage.setItem('cinema_layout_preview', JSON.stringify(layoutData.value))
    
    // Gửi về Java Backend (Giả định đang chỉnh sửa Cụm rạp ID: 1)
    await axios.put(`${API_BASE_URL}/cinemas/1/layout`, JSON.stringify(layoutData.value), {
      headers: { 'Content-Type': 'application/json' }
    })
    
    toast.success('Đã đồng bộ sơ đồ ghế với Server Java thành công!')
  } catch (error) {
    console.error('Lỗi khi lưu sơ đồ ghế:', error)
    toast.warning('Đã lưu cục bộ, nhưng không thể kết nối tới Server Java để đồng bộ.')
  }
}
</script>

<template>
  <div class="p-10 flex flex-col h-screen overflow-hidden select-none">
    <header class="flex justify-between items-center mb-8 flex-shrink-0">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase italic">Phòng <span class="text-primary">Kiến trúc</span></h1>
        <p class="text-on-surface-variant text-[10px] font-black uppercase tracking-[0.3em]">Trình thiết kế sơ đồ ghế v2.1 • Smart Selection Enabled</p>
      </div>
      <div class="flex gap-4">
        <button class="px-8 py-3 bg-surface-container-high text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm border border-outline-variant/10 hover:bg-white/5 transition-all">Nhập JSON</button>
        <button @click="saveLayout" class="px-8 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all shadow-xl shadow-primary/20">Lưu Cấu Trúc</button>
      </div>
    </header>

    <div class="flex-grow overflow-hidden">
      <SeatMapBuilder 
        :initial-rows="layoutData.rows" 
        :initial-cols="layoutData.cols" 
        :initial-seat-map="layoutData.seats" 
        @update:layout="handleUpdateLayout" 
      />
    </div>
  </div>
</template>
