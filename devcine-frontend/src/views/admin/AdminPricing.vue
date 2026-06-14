<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const API_BASE_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080") + "/api/pricing";
const pricingRules = ref([]);

const fetchRules = async () => {
  try {
    const response = await axios.get(API_BASE_URL);
    pricingRules.value = response.data;
  } catch (error) {
    console.error("Error fetching pricing rules:", error);
  }
};

onMounted(fetchRules);

const selectedRule = ref(null);
const isModalOpen = ref(false);

const openEditModal = (rule) => {
  selectedRule.value = { ...rule };
  isModalOpen.value = true;
};

const closeModal = () => {
  isModalOpen.value = false;
  selectedRule.value = null;
};

const saveRule = () => {
  const index = pricingRules.value.findIndex(r => r.id === selectedRule.value.id);
  if (index !== -1) {
    pricingRules.value[index] = { ...selectedRule.value };
  }
  closeModal();
};

const getModifierText = (rule) => {
  if (rule.modifierType === 'PERCENTAGE') {
    return `${rule.modifierValue > 0 ? '+' : ''}${rule.modifierValue}%`;
  } else if (rule.modifierType === 'FIXED_ADD') {
    return `${rule.modifierValue > 0 ? '+' : ''}${rule.modifierValue.toLocaleString()}đ`;
  }
  return `${rule.modifierValue.toLocaleString()}đ`;
};

const getTypeIcon = (type) => {
  switch (type) {
    case 'TIME': return 'schedule';
    case 'DATE': return 'event';
    case 'SEAT_TYPE': return 'chair';
    case 'MEMBERSHIP': return 'card_membership';
    default: return 'settings';
  }
};
</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Pricing Matrix</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Thiết lập ma trận giá thông minh & đa tầng</p>
      </div>
      <button class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
        <span class="material-symbols-outlined text-sm">add</span>
        Thêm quy tắc mới
      </button>
    </header>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Matrix Overview Card -->
      <div class="lg:col-span-2 space-y-6">
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl overflow-hidden shadow-2xl">
          <div class="p-6 border-b border-outline-variant/10 bg-white/5 flex justify-between items-center">
            <h3 class="font-headline font-bold uppercase tracking-widest text-xs text-on-surface">Danh sách quy tắc áp dụng</h3>
            <div class="flex gap-2">
                <span class="px-2 py-1 bg-green-500/10 text-green-500 text-[10px] font-black rounded uppercase">8 Active</span>
                <span class="px-2 py-1 bg-on-surface-variant/10 text-on-surface-variant text-[10px] font-black rounded uppercase">2 Disabled</span>
            </div>
          </div>
          
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-surface-container-high/50 text-[10px] font-black uppercase tracking-[0.2em] text-on-surface-variant">
                  <th class="p-4 border-b border-outline-variant/10">Loại</th>
                  <th class="p-4 border-b border-outline-variant/10">Tên quy tắc</th>
                  <th class="p-4 border-b border-outline-variant/10">Điều chỉnh</th>
                  <th class="p-4 border-b border-outline-variant/10">Ưu tiên</th>
                  <th class="p-4 border-b border-outline-variant/10">Trạng thái</th>
                  <th class="p-4 border-b border-outline-variant/10 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody class="text-sm font-medium">
                <tr v-for="rule in pricingRules" :key="rule.id" class="border-b border-outline-variant/5 hover:bg-white/5 transition-colors group">
                  <td class="p-4">
                    <div class="w-8 h-8 rounded-lg bg-surface-container-highest flex items-center justify-center text-primary">
                      <span class="material-symbols-outlined text-lg">{{ getTypeIcon(rule.type) }}</span>
                    </div>
                  </td>
                  <td class="p-4">
                    <div class="font-bold text-on-surface">{{ rule.name }}</div>
                    <div class="text-[10px] text-on-surface-variant uppercase tracking-tighter">{{ rule.type }}</div>
                  </td>
                  <td class="p-4">
                    <span :class="rule.modifierValue > 0 ? 'text-red-400 bg-red-400/10' : 'text-green-400 bg-green-400/10'" class="px-3 py-1 rounded-full text-xs font-black">
                      {{ getModifierText(rule) }}
                    </span>
                  </td>
                  <td class="p-4">
                    <div class="flex items-center gap-2">
                        <div class="w-12 h-1.5 bg-surface-container-highest rounded-full overflow-hidden">
                            <div class="h-full bg-primary" :style="{ width: rule.priority * 2 + '%' }"></div>
                        </div>
                        <span class="text-[10px] font-bold">{{ rule.priority }}</span>
                    </div>
                  </td>
                  <td class="p-4">
                    <div class="flex items-center gap-1.5">
                      <span class="w-2 h-2 rounded-full" :class="rule.isActive ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]' : 'bg-on-surface-variant/30'"></span>
                      <span class="text-[10px] font-bold uppercase tracking-widest" :class="rule.isActive ? 'text-on-surface' : 'text-on-surface-variant'">{{ rule.isActive ? 'Active' : 'Draft' }}</span>
                    </div>
                  </td>
                  <td class="p-4 text-right">
                    <button @click="openEditModal(rule)" class="p-2 hover:text-primary transition-colors">
                      <span class="material-symbols-outlined text-lg">edit_note</span>
                    </button>
                    <button class="p-2 hover:text-red-500 transition-colors">
                      <span class="material-symbols-outlined text-lg">delete</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Quick Controls / Summary -->
      <div class="space-y-6">
        <!-- Simulation Tool -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 shadow-xl relative overflow-hidden">
            <div class="absolute -right-4 -top-4 opacity-5">
                <span class="material-symbols-outlined text-8xl text-primary">calculate</span>
            </div>
            <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-6 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">rocket_launch</span>
                Price Simulator
            </h3>
            
            <div class="space-y-4 relative z-10">
                <div class="space-y-1">
                    <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giá vé gốc</label>
                    <input type="number" value="100000" class="w-full bg-surface-container-high border border-outline-variant/20 p-3 rounded text-sm font-bold text-on-surface focus:border-primary outline-none transition-all" />
                </div>
                <div class="grid grid-cols-2 gap-4">
                    <div class="space-y-1">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ngày chiếu</label>
                        <select class="w-full bg-surface-container-high border border-outline-variant/20 p-3 rounded text-sm font-bold text-on-surface outline-none">
                            <option>Thứ 7 (Cuối tuần)</option>
                            <option>Thứ 2 (Ngày thường)</option>
                            <option>Ngày lễ</option>
                        </select>
                    </div>
                    <div class="space-y-1">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giờ chiếu</label>
                        <select class="w-full bg-surface-container-high border border-outline-variant/20 p-3 rounded text-sm font-bold text-on-surface outline-none">
                            <option>19:00 (Cao điểm)</option>
                            <option>10:00 (Thấp điểm)</option>
                        </select>
                    </div>
                </div>
                <div class="space-y-1">
                    <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Loại ghế</label>
                    <select class="w-full bg-surface-container-high border border-outline-variant/20 p-3 rounded text-sm font-bold text-on-surface outline-none">
                        <option>Normal Seat</option>
                        <option selected>VIP Seat (+20k)</option>
                        <option>Sweetbox (+50k)</option>
                    </select>
                </div>

                <div class="mt-8 p-6 bg-primary/10 border border-primary/20 rounded-xl">
                    <div class="flex justify-between items-center mb-2">
                        <span class="text-[10px] font-bold uppercase tracking-widest text-primary">Giá cuối cùng</span>
                        <span class="text-[10px] font-bold text-green-500 uppercase tracking-widest bg-green-500/10 px-2 py-0.5 rounded">Optimized</span>
                    </div>
                    <div class="text-4xl font-black font-headline text-primary">145.000đ</div>
                    <p class="text-[9px] text-on-surface-variant mt-2 italic font-medium leading-relaxed">
                        Đã áp dụng: <span class="text-on-surface font-bold">Cuối tuần (+25%)</span>, <span class="text-on-surface font-bold">Ghế VIP (+20k)</span>, <span class="text-on-surface font-bold">Thành viên Silver (-5%)</span>
                    </p>
                </div>
            </div>
        </div>

        <!-- System Settings -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 shadow-xl">
            <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-6 flex items-center gap-2">
                <span class="material-symbols-outlined text-on-surface-variant">tune</span>
                Global Config
            </h3>
            <div class="space-y-4">
                <div class="flex items-center justify-between p-3 bg-white/5 rounded-lg border border-white/5">
                    <div>
                        <p class="text-xs font-bold text-on-surface uppercase">Stacking Mode</p>
                        <p class="text-[9px] text-on-surface-variant uppercase tracking-widest">Cho phép cộng dồn quy tắc</p>
                    </div>
                    <div class="w-10 h-5 bg-primary rounded-full relative cursor-pointer">
                        <div class="absolute right-0.5 top-0.5 w-4 h-4 bg-white rounded-full"></div>
                    </div>
                </div>
                <div class="flex items-center justify-between p-3 bg-white/5 rounded-lg border border-white/5">
                    <div>
                        <p class="text-xs font-bold text-on-surface uppercase">Rounding</p>
                        <p class="text-[9px] text-on-surface-variant uppercase tracking-widest">Làm tròn lên 1.000đ</p>
                    </div>
                    <div class="w-10 h-5 bg-primary rounded-full relative cursor-pointer">
                        <div class="absolute right-0.5 top-0.5 w-4 h-4 bg-white rounded-full"></div>
                    </div>
                </div>
            </div>
        </div>
      </div>
    </div>

    <!-- Modal Edit -->
    <div v-if="isModalOpen" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="closeModal"></div>
        <div class="relative bg-surface-container-high border border-outline-variant/20 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-300">
            <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-white/5">
                <h3 class="font-headline font-black uppercase italic text-primary">Hiệu chỉnh quy tắc giá</h3>
                <button @click="closeModal" class="material-symbols-outlined hover:text-red-400 transition-colors">close</button>
            </div>
            
            <div class="p-8 space-y-6">
                <div class="space-y-2">
                    <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tên quy tắc</label>
                    <input v-model="selectedRule.name" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
                </div>

                <div class="grid grid-cols-2 gap-6">
                    <div class="space-y-2">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Loại thay đổi</label>
                        <select v-model="selectedRule.modifierType" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none">
                            <option value="PERCENTAGE">Phần trăm (%)</option>
                            <option value="FIXED_ADD">Cộng thêm (VNĐ)</option>
                            <option value="FIXED_SET">Gán giá cố định</option>
                        </select>
                    </div>
                    <div class="space-y-2">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giá trị</label>
                        <div class="relative">
                            <input type="number" v-model="selectedRule.modifierValue" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
                            <span class="absolute right-4 top-1/2 -translate-y-1/2 font-black text-xs text-primary">{{ selectedRule.modifierType === 'PERCENTAGE' ? '%' : 'đ' }}</span>
                        </div>
                    </div>
                </div>

                <div class="space-y-2">
                    <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Độ ưu tiên (Priority)</label>
                    <input type="range" min="1" max="100" v-model="selectedRule.priority" class="w-full accent-primary" />
                    <div class="flex justify-between text-[10px] font-bold text-on-surface-variant">
                        <span>Low (1)</span>
                        <span class="text-primary">{{ selectedRule.priority }}</span>
                        <span>Critical (100)</span>
                    </div>
                </div>

                <div class="flex gap-4 pt-4">
                    <button @click="closeModal" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
                    <button @click="saveRule" class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:scale-[1.02] transition-transform">Lưu cấu hình</button>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>

<style scoped>

input[type="range"] {
  height: 6px;
  -webkit-appearance: none;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(245, 197, 24, 0.2);
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 197, 24, 0.5);
}
</style>
