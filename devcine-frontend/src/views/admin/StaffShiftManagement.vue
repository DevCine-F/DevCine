<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/api/staff'

const staffList = ref([])
const shifts = ref([])
const isLoading = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0])

const isAddModalOpen = ref(false)
const newShift = ref({
  staffId: '',
  shiftDate: selectedDate.value,
  startTime: '08:00',
  endTime: '16:00',
  location: '',
  note: ''
})

const fetchData = async () => {
  isLoading.value = true
  try {
    const [staffRes, shiftsRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/list`),
      axios.get(`${API_BASE_URL}/shifts?date=${selectedDate.value}`)
    ])
    staffList.value = staffRes.data
    shifts.value = shiftsRes.data
  } catch (error) {
    console.error('Error fetching staff data:', error)
  } finally {
    isLoading.value = false
  }
}

const handleAddShift = async () => {
  try {
    await axios.post(`${API_BASE_URL}/shifts`, {
      staff: { id: newShift.value.staffId },
      shiftDate: newShift.value.shiftDate,
      startTime: newShift.value.startTime,
      endTime: newShift.value.endTime,
      location: newShift.value.location,
      note: newShift.value.note
    })
    await fetchData()
    isAddModalOpen.value = false
  } catch (error) {
    console.error('Error adding shift:', error)
  }
}

const approveShift = async (id) => {
  try {
    await axios.put(`${API_BASE_URL}/shifts/${id}/approve`)
    await fetchData()
  } catch (error) {
    console.error('Error approving shift:', error)
  }
}

const rejectShift = async (id) => {
  try {
    await axios.put(`${API_BASE_URL}/shifts/${id}/reject`)
    await fetchData()
  } catch (error) {
    console.error('Error rejecting shift:', error)
  }
}

const seedDemoData = async () => {
  try {
    await axios.post(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/system/seed-all`);
    alert('Dữ liệu mẫu nhân sự đã được khởi tạo!');
    await fetchData();
  } catch (error) {
    console.error('Seeding error:', error);
    alert('Lỗi khi khởi tạo dữ liệu!');
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="p-10 space-y-10">
    <header class="flex justify-between items-center">
      <div>
        <h1 class="text-3xl font-black tracking-tighter text-on-surface uppercase italic">
          Điều phối <span class="text-primary">Nhân sự & Ca trực</span>
        </h1>
        <p class="text-on-surface-variant text-xs mt-1 font-bold uppercase tracking-widest">
          Quản lý lịch làm việc, Phê duyệt ca trực Ticket Seller & Projectionist
        </p>
      </div>
      <div class="flex gap-4">
        <AppButton variant="ghost" @click="seedDemoData">Dữ liệu Mẫu</AppButton>
        <div class="bg-surface-container-high rounded-xl px-4 py-2 flex items-center gap-3 border border-outline-variant/10">
          <span class="material-symbols-outlined text-primary text-sm">calendar_today</span>
          <input type="date" v-model="selectedDate" @change="fetchData" class="bg-transparent border-none text-xs font-black uppercase text-on-surface outline-none" />
        </div>
        <AppButton @click="isAddModalOpen = true">
          <span class="material-symbols-outlined mr-2">add_task</span> Xếp ca mới
        </AppButton>
      </div>
    </header>

    <div class="grid grid-cols-12 gap-8">
      <!-- Main Shift Table -->
      <div class="col-span-8 bg-surface-container-low rounded-3xl border border-outline-variant/10 overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/20">
          <h2 class="text-sm font-black uppercase tracking-widest text-on-surface">Lịch trực ngày {{ selectedDate }}</h2>
          <div class="flex gap-2">
             <span class="px-3 py-1 bg-blue-500/10 text-blue-500 text-[8px] font-black rounded-full border border-blue-500/20 uppercase">Bán vé: {{ shifts.filter(s => s.staff.role === 'TICKET_SELLER').length }}</span>
             <span class="px-3 py-1 bg-purple-500/10 text-purple-500 text-[8px] font-black rounded-full border border-purple-500/20 uppercase">Phòng chiếu: {{ shifts.filter(s => s.staff.role === 'PROJECTIONIST').length }}</span>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-left">
            <thead>
              <tr class="bg-surface-container-high/30 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">
                <th class="px-8 py-5">Nhân viên</th>
                <th class="px-8 py-5">Vị trí trực</th>
                <th class="px-8 py-5">Thời gian</th>
                <th class="px-8 py-5 text-center">Trạng thái</th>
                <th class="px-8 py-5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="s in shifts" :key="s.id" class="group hover:bg-white/5 transition-colors">
                <td class="px-8 py-5">
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-surface-container-highest flex items-center justify-center border border-outline-variant/10">
                      <span class="material-symbols-outlined text-xs text-on-surface-variant">person</span>
                    </div>
                    <div>
                      <p class="text-sm font-bold">{{ s.staff.fullName }}</p>
                      <p class="text-[9px] font-black text-primary/60 uppercase">{{ s.staff.role === 'TICKET_SELLER' ? 'Nhân viên Bán vé' : 'Nhân viên Phòng chiếu' }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-8 py-5 text-xs font-bold text-on-surface-variant uppercase tracking-tight">{{ s.location || 'Chưa gán' }}</td>
                <td class="px-8 py-5">
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-black italic">{{ s.startTime }}</span>
                    <span class="text-[10px] text-on-surface-variant">→</span>
                    <span class="text-xs font-black italic">{{ s.endTime }}</span>
                  </div>
                </td>
                <td class="px-8 py-5 text-center">
                  <span :class="{
                    'px-3 py-1 rounded-full text-[8px] font-black border uppercase': true,
                    'bg-yellow-500/10 text-yellow-500 border-yellow-500/20': s.status === 'PENDING',
                    'bg-green-500/10 text-green-500 border-green-500/20': s.status === 'APPROVED',
                    'bg-red-500/10 text-red-500 border-red-500/20': s.status === 'REJECTED'
                  }">
                    {{ s.status === 'PENDING' ? 'Chờ duyệt' : s.status === 'APPROVED' ? 'Đã duyệt' : 'Từ chối' }}
                  </span>
                </td>
                <td class="px-8 py-5 text-right">
                  <div v-if="s.status === 'PENDING'" class="flex justify-end gap-2">
                    <button @click="approveShift(s.id)" class="p-2 hover:bg-green-500/10 hover:text-green-500 rounded-lg transition-all" title="Duyệt">
                      <span class="material-symbols-outlined text-lg">check_circle</span>
                    </button>
                    <button @click="rejectShift(s.id)" class="p-2 hover:bg-red-500/10 hover:text-red-500 rounded-lg transition-all" title="Từ chối">
                      <span class="material-symbols-outlined text-lg">cancel</span>
                    </button>
                  </div>
                  <div v-else class="text-[9px] font-bold text-on-surface-variant italic uppercase tracking-widest">
                    Đã xử lý
                  </div>
                </td>
              </tr>
              <tr v-if="shifts.length === 0">
                <td colspan="5" class="px-8 py-20 text-center text-on-surface-variant/40 italic font-bold uppercase tracking-widest text-xs">
                  Không có ca trực nào được xếp trong ngày này
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Quick Stats & Staff List -->
      <div class="col-span-4 space-y-8">
        <div class="bg-primary/5 border border-primary/20 rounded-3xl p-8 space-y-6">
          <h2 class="text-sm font-black uppercase tracking-widest text-primary">Phê duyệt nhanh</h2>
          <div v-if="shifts.filter(s => s.status === 'PENDING').length > 0" class="space-y-4">
             <p class="text-[10px] text-on-surface-variant font-bold uppercase">Có {{ shifts.filter(s => s.status === 'PENDING').length }} yêu cầu đang chờ</p>
             <AppButton class="w-full">Duyệt tất cả ca trực</AppButton>
          </div>
          <p v-else class="text-[10px] text-on-surface-variant italic font-bold text-center py-4">Hết yêu cầu chờ phê duyệt</p>
        </div>

        <div class="bg-surface-container-low border border-outline-variant/10 rounded-3xl p-8 overflow-hidden">
          <h2 class="text-sm font-black uppercase tracking-widest text-on-surface mb-6">Danh sách nhân sự</h2>
          <div class="space-y-4 max-h-[400px] overflow-y-auto custom-scrollbar pr-2">
             <div v-for="staff in staffList" :key="staff.id" class="flex items-center justify-between p-3 bg-surface-container-high rounded-xl border border-outline-variant/10">
                <div class="flex items-center gap-3">
                   <div class="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary font-black text-[10px] border border-primary/20">
                      {{ staff.employeeCode }}
                   </div>
                   <div>
                      <p class="text-xs font-bold">{{ staff.fullName }}</p>
                      <p class="text-[8px] font-black text-on-surface-variant uppercase">{{ staff.role.replace('_', ' ') }}</p>
                   </div>
                </div>
                <div class="w-2 h-2 rounded-full" :class="staff.isActive ? 'bg-green-500' : 'bg-red-500'"></div>
             </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add Shift Modal -->
    <AppModal v-model="isAddModalOpen" title="Xếp lịch ca trực">
       <div class="space-y-6 pt-4">
          <div class="grid grid-cols-2 gap-4">
             <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Chọn nhân viên</label>
                <select v-model="newShift.staffId" class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none">
                   <option v-for="s in staffList" :key="s.id" :value="s.id">{{ s.fullName }} ({{ s.role }})</option>
                </select>
             </div>
             <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ngày trực</label>
                <input type="date" v-model="newShift.shiftDate" class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none font-bold" />
             </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
             <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Bắt đầu</label>
                <input type="time" v-model="newShift.startTime" class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none font-bold" />
             </div>
             <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Kết thúc</label>
                <input type="time" v-model="newShift.endTime" class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none font-bold" />
             </div>
          </div>
          <div class="space-y-2">
             <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Vị trí làm việc</label>
             <input v-model="newShift.location" placeholder="Quầy vé 01, Phòng chiếu 03..." class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none font-bold" />
          </div>
          <div class="space-y-2">
             <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ghi chú công việc</label>
             <textarea v-model="newShift.note" rows="2" class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none resize-none"></textarea>
          </div>
          <div class="flex gap-4 pt-4">
             <AppButton variant="ghost" class="flex-1" @click="isAddModalOpen = false">Hủy bỏ</AppButton>
             <AppButton class="flex-1" @click="handleAddShift">Xếp ca & Chờ duyệt</AppButton>
          </div>
       </div>
    </AppModal>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(245, 197, 24, 0.2);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 197, 24, 0.4);
}
</style>
