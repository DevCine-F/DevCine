<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api/staff'
const staff = ref([])
const isLoading = ref(false)

const fetchStaff = async () => {
  isLoading.value = true
  try {
    const response = await axios.get(API_BASE_URL)
    staff.value = response.data.map(s => ({
      ...s,
      name: s.fullName, // Map backend fullName to frontend name
      joinDate: '12/05/2023', // Demo date
      status: s.isActive ? 'active' : 'inactive'
    }))
  } catch (error) {
    console.error('Error fetching staff:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchStaff)
</script>

<template>
  <div class="p-10">
    <header class="flex justify-between items-center mb-12 text-on-surface">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Quản lý Nhân viên</h1>
        <p class="text-on-surface-variant text-sm mt-1">Quản lý đội ngũ vận hành và phân quyền hệ thống</p>
      </div>
      <button class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-6 py-3 rounded-sm hover:brightness-110 transition-all flex items-center gap-2">
        <span class="material-symbols-outlined text-sm">person_add</span>
        Thêm Nhân Viên
      </button>
    </header>

    <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg overflow-hidden">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant border-b border-outline-variant/10">
            <th class="px-8 py-5">Nhân viên</th>
            <th class="px-8 py-5">Vai trò</th>
            <th class="px-8 py-5">Ngày gia nhập</th>
            <th class="px-8 py-5">Trạng thái</th>
            <th class="px-8 py-5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-outline-variant/10 text-on-surface">
          <tr v-for="person in staff" :key="person.id" class="group hover:bg-white/5 transition-all">
            <td class="px-8 py-4">
              <div class="flex items-center gap-4">
                <div class="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold">
                  {{ person.name.charAt(0) }}
                </div>
                <div>
                  <p class="font-bold text-sm uppercase tracking-tight group-hover:text-primary transition-colors">{{ person.name }}</p>
                  <p class="text-[10px] text-on-surface-variant mt-0.5">{{ person.email }}</p>
                </div>
              </div>
            </td>
            <td class="px-8 py-4">
              <span class="text-xs font-semibold">{{ person.role }}</span>
            </td>
            <td class="px-8 py-4">
              <span class="text-xs text-on-surface-variant">{{ person.joinDate }}</span>
            </td>
            <td class="px-8 py-4">
              <span :class="person.status === 'active' ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'" class="px-2 py-1 text-[10px] font-bold uppercase tracking-tighter">
                {{ person.status === 'active' ? 'Đang làm việc' : 'Đã nghỉ' }}
              </span>
            </td>
            <td class="px-8 py-4 text-right">
              <div class="flex justify-end gap-2">
                <button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant">
                  <span class="material-symbols-outlined text-sm">shield_person</span>
                </button>
                <button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant">
                  <span class="material-symbols-outlined text-sm">edit</span>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>
