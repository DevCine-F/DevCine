<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/api/inventory'
const toast = useToastStore()

const items = ref([])
const transactions = ref([])
const isLoading = ref(false)

const isImportModalOpen = ref(false)
const isAuditModalOpen = ref(false)

const importForm = ref({
  itemId: '',
  quantity: 0,
  note: '',
  type: 'IMPORT'
})

const auditData = ref([]) // Temporary list for auditing

const fetchData = async () => {
  isLoading.value = true
  try {
    const [itemsRes, transRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/items`),
      axios.get(`${API_BASE_URL}/transactions`)
    ])
    items.value = itemsRes.data
    transactions.value = transRes.data
  } catch (error) {
    console.error('Error fetching inventory:', error)
  } finally {
    isLoading.value = false
  }
}

const handleImport = async () => {
  try {
    const selectedItem = items.value.find(i => i.id === importForm.value.itemId)
    await axios.post(`${API_BASE_URL}/transactions`, {
      item: { id: importForm.value.itemId },
      quantity: importForm.value.quantity,
      type: 'IMPORT',
      note: importForm.value.note,
      performer: 'Nguyen Admin'
    })
    await fetchData()
    isImportModalOpen.value = false
  } catch (error) {
    console.error('Error importing stock:', error)
  }
}

const startAudit = () => {
  auditData.value = items.value.map(item => ({
    ...item,
    actualQuantity: item.stockQuantity,
    discrepancy: 0
  }))
  isAuditModalOpen.value = true
}

const saveAudit = async () => {
  // Logic to save adjustments
  for (const row of auditData.value) {
    const diff = row.actualQuantity - row.stockQuantity
    if (diff !== 0) {
      await axios.post(`${API_BASE_URL}/transactions`, {
        item: { id: row.id },
        quantity: Math.abs(diff),
        type: diff > 0 ? 'IMPORT' : 'EXPORT',
        note: `Kiểm kê định kỳ - Điều chỉnh sai lệch: ${diff}`,
        performer: 'Nguyen Admin'
      })
    }
  }
  await fetchData()
  isAuditModalOpen.value = false
}

const seedInitialItems = async () => {
  try {
    await axios.post(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/system/seed-all`);
    toast.success('Dữ liệu mẫu kho bãi đã được khởi tạo!');
    await fetchData();
  } catch (error) {
    console.error('Seeding error:', error);
    toast.error(friendlyError(error, 'Lỗi khi khởi tạo dữ liệu!'));
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="p-10 space-y-10">
    <header class="flex justify-between items-center">
      <div>
        <h1 class="text-3xl font-black tracking-tighter text-on-surface uppercase italic">
          Quản lý kho <span class="text-primary">F&B</span>
        </h1>
        <p class="text-on-surface-variant text-xs mt-1 font-bold uppercase tracking-widest">
          Nhập kho, Kiểm kê & Đối soát tỷ lệ hao hụt
        </p>
      </div>
      <div class="flex gap-4">
        <AppButton variant="ghost" @click="seedInitialItems">Dữ liệu Mẫu</AppButton>
        <AppButton variant="outline" @click="startAudit">
          <span class="material-symbols-outlined mr-2">inventory</span> Kiểm kê định kỳ
        </AppButton>
        <AppButton @click="isImportModalOpen = true">
          <span class="material-symbols-outlined mr-2">add_shopping_cart</span> Nhập kho mới
        </AppButton>
      </div>
    </header>

    <!-- Stock Overview Cards -->
    <div class="grid grid-cols-4 gap-6">
      <div v-for="item in items" :key="item.id" 
           class="bg-surface-container-low p-6 rounded-3xl border border-outline-variant/10 relative overflow-hidden group">
        <div class="relative z-10">
          <p class="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-1">{{ item.category }}</p>
          <h3 class="text-lg font-bold text-on-surface">{{ item.name }}</h3>
          <div class="mt-4 flex items-end justify-between">
            <span class="text-3xl font-black italic tracking-tighter" :class="item.stockQuantity < item.minThreshold ? 'text-red-500' : 'text-primary'">
              {{ item.stockQuantity }}
            </span>
            <span class="text-xs font-bold text-on-surface-variant mb-1 uppercase">{{ item.unit }}</span>
          </div>
          <div v-if="item.stockQuantity < item.minThreshold" class="mt-2 flex items-center gap-1 text-red-500 text-[9px] font-black uppercase">
            <span class="material-symbols-outlined text-xs">warning</span> Cần nhập hàng gấp!
          </div>
        </div>
        <div class="absolute -right-4 -bottom-4 opacity-5 group-hover:scale-110 transition-transform">
          <span class="material-symbols-outlined text-8xl">box</span>
        </div>
      </div>
    </div>

    <!-- Main Content Tabs (Inventory List / History / Reconciliation) -->
    <div class="grid grid-cols-3 gap-8">
      <!-- Recent Transactions -->
      <div class="col-span-2 bg-surface-container-low rounded-3xl border border-outline-variant/10 overflow-hidden shadow-2xl">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center">
          <h2 class="text-sm font-black uppercase tracking-widest text-on-surface">Nhật ký kho gần đây</h2>
          <span class="text-[10px] font-bold text-primary uppercase">Hiển thị {{ transactions.length }} bản ghi</span>
        </div>
        <div class="max-h-[500px] overflow-y-auto custom-scrollbar">
          <table class="w-full text-left">
            <thead>
              <tr class="bg-surface-container-high/30 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">
                <th class="px-6 py-4">Thời gian</th>
                <th class="px-6 py-4">Sản phẩm</th>
                <th class="px-6 py-4 text-center">Loại</th>
                <th class="px-6 py-4 text-right">Số lượng</th>
                <th class="px-6 py-4">Người thực hiện</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="t in transactions.slice().reverse()" :key="t.id" class="hover:bg-white/5 transition-colors">
                <td class="px-6 py-4 text-[10px] font-bold text-on-surface-variant">{{ new Date(t.transactionDate).toLocaleString() }}</td>
                <td class="px-6 py-4">
                  <p class="text-xs font-bold">{{ t.item.name }}</p>
                  <p class="text-[9px] text-on-surface-variant italic truncate max-w-[150px]">{{ t.note }}</p>
                </td>
                <td class="px-6 py-4 text-center">
                  <span :class="t.type === 'IMPORT' ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'" 
                        class="px-3 py-1 rounded-full text-[9px] font-black border border-current/20">
                    {{ t.type === 'IMPORT' ? 'NHẬP' : 'XUẤT' }}
                  </span>
                </td>
                <td class="px-6 py-4 text-right text-sm font-black italic">{{ t.quantity }}</td>
                <td class="px-6 py-4 text-[10px] font-bold uppercase tracking-wider">{{ t.performer }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Loss Rate / Reconciliation Sidebar -->
      <div class="space-y-6">
        <div class="bg-primary/5 border border-primary/20 rounded-3xl p-8 space-y-6">
          <h2 class="text-sm font-black uppercase tracking-widest text-primary flex items-center gap-2">
            <span class="material-symbols-outlined">analytics</span> Chỉ số hao hụt (Loss Rate)
          </h2>
          <div class="space-y-4">
            <div class="flex justify-between items-end">
              <p class="text-[10px] font-bold text-on-surface-variant uppercase">Hao hụt tháng này</p>
              <p class="text-2xl font-black italic text-primary">2.4%</p>
            </div>
            <div class="w-full h-2 bg-primary/10 rounded-full overflow-hidden">
              <div class="w-[24%] h-full bg-primary animate-pulse"></div>
            </div>
            <p class="text-[9px] text-on-surface-variant leading-relaxed">
              * Tỷ lệ hao hụt được tính dựa trên sai lệch giữa số lượng xuất kho thực tế và doanh số bán lẻ từ POS.
            </p>
          </div>
          <AppButton variant="primary" class="w-full">Xuất báo cáo đối soát</AppButton>
        </div>

        <div class="bg-surface-container-low border border-outline-variant/10 rounded-3xl p-8">
          <h2 class="text-sm font-black uppercase tracking-widest text-on-surface mb-6">Mặt hàng sắp hết</h2>
          <div class="space-y-4">
            <div v-for="item in items.filter(i => i.stockQuantity < i.minThreshold)" :key="item.id" 
                 class="flex items-center gap-4 p-3 bg-red-500/5 rounded-xl border border-red-500/10">
              <span class="material-symbols-outlined text-red-500">error</span>
              <div class="flex-grow">
                <p class="text-xs font-bold">{{ item.name }}</p>
                <p class="text-[9px] font-black text-red-500/60 uppercase">Còn lại: {{ item.stockQuantity }} {{ item.unit }}</p>
              </div>
            </div>
            <p v-if="!items.some(i => i.stockQuantity < i.minThreshold)" class="text-[10px] text-on-surface-variant text-center py-4 font-bold italic">
              Tất cả mặt hàng đều ổn định
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Import Modal -->
    <AppModal v-model="isImportModalOpen" title="Nhập kho nguyên liệu">
      <div class="space-y-6 pt-4">
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Chọn mặt hàng</label>
          <select v-model="importForm.itemId" class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none">
            <option v-for="item in items" :key="item.id" :value="item.id">{{ item.name }} (Hiện có: {{ item.stockQuantity }} {{ item.unit }})</option>
          </select>
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Số lượng nhập</label>
          <input v-model.number="importForm.quantity" type="number" 
                 class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none font-bold" />
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ghi chú</label>
          <textarea v-model="importForm.note" rows="3" placeholder="Lý do nhập kho, số hóa đơn..." 
                    class="w-full bg-surface-container-high rounded-xl py-3 px-4 text-on-surface text-sm outline-none resize-none"></textarea>
        </div>
        <div class="flex gap-4 pt-4">
          <AppButton variant="ghost" class="flex-1" @click="isImportModalOpen = false">Hủy</AppButton>
          <AppButton class="flex-1" @click="handleImport">Xác nhận nhập kho</AppButton>
        </div>
      </div>
    </AppModal>

    <!-- Audit Modal -->
    <AppModal v-model="isAuditModalOpen" title="Kiểm kê định kỳ & Đối soát">
      <div class="space-y-6 pt-4 max-h-[70vh] overflow-y-auto custom-scrollbar px-1">
        <p class="text-[10px] text-on-surface-variant italic mb-4">
          Nhập số lượng thực tế kiểm đếm tại kho. Hệ thống sẽ tự động tính toán sai lệch và cập nhật kho.
        </p>
        <div v-for="row in auditData" :key="row.id" 
             class="p-4 bg-surface-container-high rounded-2xl flex items-center gap-6 border border-outline-variant/10">
          <div class="flex-grow">
            <p class="text-xs font-bold">{{ row.name }}</p>
            <p class="text-[9px] font-black text-on-surface-variant uppercase">Sổ sách: {{ row.stockQuantity }} {{ row.unit }}</p>
          </div>
          <div class="w-32">
            <input v-model.number="row.actualQuantity" type="number" 
                   class="w-full bg-surface-container-lowest rounded-lg py-2 px-3 text-center text-sm font-bold text-primary outline-none" />
          </div>
          <div class="w-20 text-right">
            <p :class="row.actualQuantity - row.stockQuantity < 0 ? 'text-red-500' : 'text-green-500'" class="text-xs font-black italic">
              {{ row.actualQuantity - row.stockQuantity > 0 ? '+' : '' }}{{ row.actualQuantity - row.stockQuantity }}
            </p>
          </div>
        </div>
        <div class="flex gap-4 pt-6 sticky bottom-0 bg-surface-container-low py-4 border-t border-outline-variant/10">
          <AppButton variant="ghost" class="flex-1" @click="isAuditModalOpen = false">Hủy</AppButton>
          <AppButton class="flex-1" @click="saveAudit">Lưu kết quả & Điều chỉnh</AppButton>
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
