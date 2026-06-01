<script setup>
import { ref, computed } from 'vue'

// --- DỮ LIỆU CẤU TRÚC ---
const roles = ref([
  { id: 'admin', name: 'Quản trị viên' },
  { id: 'manager', name: 'Quản lý rạp' },
  { id: 'staff', name: 'Nhân viên' }
])

const modules = ref([
  { id: 'dashboard', name: 'Tổng quan' },
  { id: 'pos', name: 'Nghiệp vụ' },
  { id: 'content', name: 'Nội dung' },
  { id: 'system', name: 'Hệ thống' }
])

const features = ref([
  { id: 'dashboard_stats', moduleId: 'dashboard', name: 'Báo cáo doanh thu', actions: ['view', 'export'] },
  { id: 'dashboard_users', moduleId: 'dashboard', name: 'Thống kê khách hàng', actions: ['view', 'export'] },
  
  { id: 'pos_ticketing', moduleId: 'pos', name: 'Bán vé tại quầy (POS)', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'pos_inventory', moduleId: 'pos', name: 'Kiểm kê F&B', actions: ['view', 'add', 'edit'] },
  
  { id: 'movies', moduleId: 'content', name: 'Quản lý danh sách phim', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'schedules', moduleId: 'content', name: 'Điều phối lịch chiếu', actions: ['view', 'add', 'edit', 'delete', 'export'] },
  { id: 'banners', moduleId: 'content', name: 'Quản lý Banner quảng cáo', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'promotions', moduleId: 'content', name: 'Chương trình khuyến mãi', actions: ['view', 'add', 'edit', 'delete'] },

  { id: 'cinemas', moduleId: 'system', name: 'Hệ thống cụm rạp', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'staff_management', moduleId: 'system', name: 'Nhân sự & Ca trực', actions: ['view', 'add', 'edit', 'delete', 'export'] },
  { id: 'settings', moduleId: 'system', name: 'Cài đặt hệ thống', actions: ['view', 'edit'] },
])

const actionLabels = {
  view: 'Xem (View)',
  add: 'Thêm mới (Add)',
  edit: 'Chỉnh sửa (Edit)',
  delete: 'Xoá (Delete)',
  export: 'Xuất File (Export)'
}

const actionLabelsShort = {
  view: 'Xem',
  add: 'Thêm',
  edit: 'Sửa',
  delete: 'Xoá',
  export: 'Xuất'
}

// --- TRẠNG THÁI (STATE) ---
const activeRole = ref('admin')
const activeModule = ref('content')

// permissions Matrix: roleId -> featureId -> array of actions
const permissions = ref({
  admin: {
    dashboard_stats: ['view', 'export'],
    dashboard_users: ['view', 'export'],
    pos_ticketing: ['view', 'add', 'edit', 'delete'],
    pos_inventory: ['view', 'add', 'edit'],
    movies: ['view', 'add', 'edit', 'delete'],
    schedules: ['view', 'add', 'edit', 'delete', 'export'],
    banners: ['view', 'add', 'edit', 'delete'],
    promotions: ['view', 'add', 'edit', 'delete'],
    cinemas: ['view', 'add', 'edit', 'delete'],
    staff_management: ['view', 'add', 'edit', 'delete', 'export'],
    settings: ['view', 'edit']
  },
  manager: {
    dashboard_stats: ['view'],
    dashboard_users: ['view'],
    pos_ticketing: ['view', 'add', 'edit'],
    movies: ['view'],
    schedules: ['view', 'add', 'edit'],
    cinemas: ['view'],
    staff_management: ['view', 'add', 'edit'],
  },
  staff: {
    pos_ticketing: ['view', 'add'],
    movies: ['view'],
    schedules: ['view']
  }
})

// --- LOGIC HELPER ---
const currentModuleFeatures = computed(() => {
  return features.value.filter(f => f.moduleId === activeModule.value)
})

const getRolePerms = () => {
  if (!permissions.value[activeRole.value]) {
    permissions.value[activeRole.value] = {}
  }
  return permissions.value[activeRole.value]
}

const hasAction = (featureId, action) => {
  const perms = getRolePerms()
  return perms[featureId]?.includes(action) || false
}

const getSelectedCount = (feature) => {
  const perms = getRolePerms()
  return perms[feature.id]?.length || 0
}

const isFeatureAll = (feature) => {
  return getSelectedCount(feature) === feature.actions.length && feature.actions.length > 0
}

const isFeaturePartial = (feature) => {
  const count = getSelectedCount(feature)
  return count > 0 && count < feature.actions.length
}

const selectedPermissionsSummary = computed(() => {
  const perms = getRolePerms()
  const summary = []
  
  features.value.forEach(feature => {
    const selectedActions = perms[feature.id]
    if (selectedActions && selectedActions.length > 0) {
      const actionNames = selectedActions.map(a => actionLabelsShort[a] || a)
      summary.push({
        name: feature.name,
        actions: actionNames.join(', ')
      })
    }
  })
  
  return summary
})

// --- HÀNH ĐỘNG (ACTIONS) ---
const toggleAction = (feature, action) => {
  const perms = getRolePerms()
  if (!perms[feature.id]) perms[feature.id] = []
  
  if (perms[feature.id].includes(action)) {
    perms[feature.id] = perms[feature.id].filter(a => a !== action)
  } else {
    perms[feature.id].push(action)
  }
}

const toggleFeatureAll = (feature) => {
  const perms = getRolePerms()
  if (isFeatureAll(feature)) {
    perms[feature.id] = []
  } else {
    perms[feature.id] = [...feature.actions]
  }
}

const clearCurrentModule = () => {
  const perms = getRolePerms()
  currentModuleFeatures.value.forEach(f => {
    perms[f.id] = []
  })
}

const saveChanges = () => {
  alert(`Đã lưu phân quyền thành công cho vai trò: ${roles.value.find(r => r.id === activeRole.value)?.name}`)
}
</script>

<template>
  <div class="flex flex-col h-full relative bg-surface-container-lowest">
    
    <!-- Vùng Nội dung chính (Scrollable) -->
    <div class="flex-1 overflow-y-auto p-10 pb-32">
      
      <!-- HEADER -->
      <header class="mb-10 text-on-surface">
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Phân quyền Hệ thống</h1>
        <p class="text-on-surface-variant text-sm mt-1">Quản lý và thiết lập quyền truy cập cho từng nhóm vai trò hệ thống</p>
      </header>

      <!-- CẤP 1: CHỌN VAI TRÒ (PILL BUTTONS) -->
      <section class="mb-12">
        <h2 class="text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] mb-4">1. Chọn Vai trò</h2>
        <div class="flex flex-wrap gap-4">
          <button 
            v-for="role in roles" 
            :key="role.id"
            @click="activeRole = role.id"
            class="px-8 py-4 rounded-full font-bold text-sm uppercase tracking-widest transition-all duration-300 shadow-sm border border-outline-variant/10"
            :class="activeRole === role.id 
              ? 'bg-primary text-on-primary shadow-[0_4px_20px_-5px_rgba(245,197,24,0.4)] scale-105 border-transparent' 
              : 'bg-surface-container-low text-on-surface hover:bg-surface-container-high hover:border-outline-variant/30'">
            {{ role.name }}
          </button>
        </div>
      </section>

      <!-- CẤP 2: TABS PHÂN HỆ -->
      <section class="mb-8">
         <div class="flex border-b border-outline-variant/20 overflow-x-auto hide-scrollbar">
            <button 
              v-for="mod in modules" 
              :key="mod.id"
              @click="activeModule = mod.id"
              class="px-8 py-5 font-bold text-xs uppercase tracking-widest relative whitespace-nowrap transition-colors"
              :class="activeModule === mod.id ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'">
              {{ mod.name }}
              
              <!-- Indicator line -->
              <div v-if="activeModule === mod.id" 
                   class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-t-full shadow-[0_0_10px_rgba(245,197,24,0.5)] animate-in slide-in-from-bottom-2 duration-300">
              </div>
            </button>
         </div>
      </section>

      <!-- CẤP 3: CHI TIẾT HÀNH ĐỘNG (CHECKBOXES) -->
      <section class="space-y-6">
        
        <!-- Action Bar cho Tab hiện tại -->
        <div class="flex justify-between items-center mb-2">
           <h2 class="text-xs font-bold text-on-surface uppercase tracking-widest">
             Chi tiết phân quyền: <span class="text-primary">{{ modules.find(m => m.id === activeModule)?.name }}</span>
           </h2>
           <button @click="clearCurrentModule" class="text-[10px] text-red-400 hover:text-red-300 bg-red-400/10 hover:bg-red-400/20 px-4 py-2 rounded-full uppercase tracking-widest font-bold flex items-center gap-2 transition-colors">
             <span class="material-symbols-outlined text-sm">clear_all</span>
             Bỏ tất cả trong Tab này
           </button>
        </div>

        <!-- Khối Features (Cards) -->
        <div v-if="currentModuleFeatures.length === 0" class="p-10 text-center text-on-surface-variant bg-surface-container-low rounded-xl border border-outline-variant/10">
           Chưa có tính năng nào trong phân hệ này.
        </div>
        
        <div v-for="feature in currentModuleFeatures" :key="feature.id" class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 shadow-sm hover:shadow-md transition-shadow">
           <!-- Parent Header -->
           <div class="flex justify-between items-center mb-6 pb-4 border-b border-outline-variant/10">
              <div class="flex items-center gap-4">
                 <button @click="toggleFeatureAll(feature)" class="flex items-center justify-center transition-transform hover:scale-110"
                         :class="isFeatureAll(feature) ? 'text-primary' : 'text-on-surface-variant'">
                    <span class="material-symbols-outlined text-3xl">
                      {{ isFeatureAll(feature) ? 'check_box' : (isFeaturePartial(feature) ? 'indeterminate_check_box' : 'check_box_outline_blank') }}
                    </span>
                 </button>
                 <div>
                    <h3 class="font-bold text-sm text-on-surface uppercase tracking-wide">{{ feature.name }}</h3>
                 </div>
              </div>
              <div class="text-[10px] font-black uppercase tracking-widest px-3 py-1.5 rounded-full"
                   :class="getSelectedCount(feature) > 0 ? 'bg-primary/20 text-primary' : 'bg-surface-container-highest text-on-surface-variant'">
                Đã chọn: {{ getSelectedCount(feature) }} / {{ feature.actions.length }}
              </div>
           </div>

           <!-- Children Actions -->
           <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-y-6 gap-x-4">
              <div v-for="action in feature.actions" :key="action" 
                   class="flex items-center gap-3 cursor-pointer group" 
                   @click="toggleAction(feature, action)">
                 
                 <span class="material-symbols-outlined text-[20px] transition-all group-hover:text-primary group-hover:scale-110"
                       :class="hasAction(feature.id, action) ? 'text-primary' : 'text-on-surface-variant/40'">
                   {{ hasAction(feature.id, action) ? 'check_box' : 'check_box_outline_blank' }}
                 </span>
                 <span class="text-[10px] font-semibold uppercase tracking-wider transition-colors group-hover:text-on-surface"
                       :class="hasAction(feature.id, action) ? 'text-on-surface' : 'text-on-surface-variant'">
                   {{ actionLabels[action] || action }}
                 </span>
              </div>
           </div>
        </div>

      </section>
    </div>

    <!-- CẤP 4: TÓM TẮT QUYỀN (SUMMARY FOOTER) -->
    <div class="absolute bottom-0 left-0 right-0 bg-surface/95 backdrop-blur-md border-t border-outline-variant/10 p-6 px-10 flex flex-col md:flex-row justify-between items-center gap-6 z-40 shadow-[0_-10px_40px_-10px_rgba(0,0,0,0.5)]">
      
      <!-- Summary Stats -->
      <div class="flex items-center gap-6 flex-1 overflow-hidden w-full">
         <div class="flex items-center gap-4 text-on-surface border-r border-outline-variant/10 pr-6 shrink-0">
            <span class="material-symbols-outlined text-4xl text-primary opacity-80">admin_panel_settings</span>
            <div>
               <p class="text-[9px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">Vai trò đang sửa</p>
               <p class="text-sm font-extrabold uppercase tracking-widest text-primary">{{ roles.find(r => r.id === activeRole)?.name }}</p>
            </div>
         </div>
         
         <div class="flex gap-3 overflow-x-auto hide-scrollbar shrink-0 w-full max-w-[50vw]">
            <div v-if="selectedPermissionsSummary.length === 0" class="text-xs text-on-surface-variant italic py-2">
              Chưa có quyền nào được cấp.
            </div>
            <div v-for="(item, index) in selectedPermissionsSummary" :key="index" class="flex flex-col bg-surface-container-high px-4 py-2.5 rounded-lg border border-outline-variant/5 shrink-0">
               <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface">{{ item.name }}</span>
               <span class="text-[9px] font-bold uppercase tracking-widest text-primary mt-1 opacity-90">{{ item.actions }}</span>
            </div>
         </div>
      </div>

      <!-- Action Button -->
      <button @click="saveChanges" class="w-full md:w-auto shrink-0 px-10 py-4 bg-primary text-on-primary font-black text-xs uppercase tracking-[0.2em] rounded-sm shadow-[0_0_20px_rgba(245,197,24,0.3)] hover:brightness-110 hover:shadow-[0_0_30px_rgba(245,197,24,0.5)] transition-all flex items-center justify-center gap-3">
         Lưu Thay Đổi
         <span class="material-symbols-outlined text-sm">save</span>
      </button>
    </div>

  </div>
</template>

<style scoped>
/* Ẩn scrollbar mặc định nhưng vẫn scroll được */
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
