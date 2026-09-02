<script setup>
import { ref, computed, onMounted } from 'vue'
import { rolePermissionApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

// --- DỮ LIỆU CẤU TRÚC 6 PHÂN HỆ KHỚP SIDEBAR ---
const roles = ref([])

const modules = ref([
  { id: 'operations', name: 'Vận hành & Quầy vé' },
  { id: 'content', name: 'Phim & Nội dung' },
  { id: 'infrastructure', name: 'Rạp & Hạ tầng' },
  { id: 'business', name: 'Kinh doanh & Khách hàng' },
  { id: 'hr', name: 'Nhân sự' },
  { id: 'system', name: 'Hệ thống' }
])

// 18 Tab chuẩn hóa chính xác theo Menu Sidebar thực tế
const features = ref([
  // ===== 1. VẬN HÀNH & QUẦY VÉ =====
  {
    id: 'dashboard_stats',
    moduleId: 'operations',
    name: 'Tổng quan (Dashboard)',
    actions: ['view'],
    labels: { view: 'Xem báo cáo doanh thu & thống kê' }
  },
  {
    id: 'pos_ticketing',
    moduleId: 'operations',
    name: 'Bán vé (POS)',
    actions: ['view', 'add'],
    labels: { view: 'Vào quầy POS & chọn ghế', add: 'Bán vé, bắp nước combo & thanh toán' }
  },
  {
    id: 'ticket_checkin',
    moduleId: 'operations',
    name: 'Kiểm soát vé',
    actions: ['view'],
    labels: { view: 'Quét mã QR & kiểm soát vé vào phòng' }
  },
  {
    id: 'bookings',
    moduleId: 'operations',
    name: 'Quản lý & Tra cứu hóa đơn',
    actions: ['view'],
    labels: { view: 'Xem danh sách / chi tiết hóa đơn' }
  },

  // ===== 2. PHIM & NỘI DUNG =====
  {
    id: 'movies',
    moduleId: 'content',
    name: 'Quản lý phim',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem danh sách phim', add: 'Thêm phim mới', edit: 'Sửa thông tin phim (Tên, Poster, Trailer...)', delete: 'Xoá / ẩn phim' }
  },
  {
    id: 'schedules',
    moduleId: 'content',
    name: 'Lịch chiếu & Suất chiếu',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem lịch chiếu', add: 'Xếp suất chiếu mới (Đơn & Lô)', edit: 'Sửa giờ chiếu & phòng chiếu', delete: 'Xoá / huỷ suất chiếu' }
  },
  {
    id: 'movie_categories',
    moduleId: 'content',
    name: 'Danh mục phim (Thể loại, Định dạng, Độ tuổi)',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem danh mục phân loại', add: 'Thêm danh mục mới', edit: 'Sửa thông tin danh mục', delete: 'Xoá danh mục' }
  },
  {
    id: 'banners',
    moduleId: 'content',
    name: 'Quản lý Banner quảng cáo',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem danh sách banner', add: 'Thêm banner mới', edit: 'Sửa banner & thứ tự hiển thị', delete: 'Xoá banner' }
  },

  // ===== 3. RẠP & HẠ TẦNG =====
  {
    id: 'cinemas',
    moduleId: 'infrastructure',
    name: 'Hệ thống Cụm rạp & Sơ đồ ghế',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem cụm rạp & sơ đồ ghế', add: 'Thêm cụm rạp mới', edit: 'Sửa thông tin cụm rạp & phòng chiếu', delete: 'Xoá / đóng cụm rạp' }
  },
  {
    id: 'fnb_menu',
    moduleId: 'infrastructure',
    name: 'Thực đơn F&B (Món & Combo)',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem toàn bộ thực đơn', add: 'Thêm món / combo mới', edit: 'Sửa món — tên, GIÁ, ảnh, tùy chọn', delete: 'Xoá món khỏi thực đơn' }
  },

  // ===== 4. KINH DOANH & KHÁCH HÀNG =====
  {
    id: 'pricing',
    moduleId: 'business',
    name: 'Cấu hình giá vé & Bảng giá',
    actions: ['view', 'edit'],
    labels: { view: 'Xem bảng giá vé', edit: 'Chỉnh cấu hình giá & phụ thu' }
  },
  {
    id: 'promotions',
    moduleId: 'business',
    name: 'Chương trình khuyến mãi & Voucher',
    actions: ['view', 'add', 'edit', 'delete'],
    labels: { view: 'Xem khuyến mãi & bài viết', add: 'Tạo khuyến mãi / voucher', edit: 'Sửa khuyến mãi & bài viết', delete: 'Xoá khuyến mãi' }
  },
  {
    id: 'customers',
    moduleId: 'business',
    name: 'Quản lý hồ sơ khách hàng',
    actions: ['view', 'edit'],
    labels: { view: 'Xem danh sách, đơn hàng, điểm & voucher', edit: 'Sửa họ tên, ngày sinh, khóa tài khoản & gửi reset mật khẩu' }
  },
  {
    id: 'admin_reviews',
    moduleId: 'business',
    name: 'Đánh giá phim',
    adminOnly: true,
    desc: 'Kiểm duyệt và quản lý bình luận đánh giá của khán giả (Mặc định bảo mật chỉ dành cho Quản trị viên ADMIN).'
  },
  {
    id: 'admin_faqs',
    moduleId: 'business',
    name: 'Câu hỏi trợ giúp (FAQ)',
    adminOnly: true,
    desc: 'Quản lý danh sách câu hỏi thường gặp của khách hàng (Mặc định bảo mật chỉ dành cho Quản trị viên ADMIN).'
  },

  // ===== 5. NHÂN SỰ =====
  {
    id: 'staff_management',
    moduleId: 'hr',
    name: 'Quản lý nhân sự',
    actions: ['view', 'add', 'edit'],
    labels: { view: 'Xem danh sách nhân sự', add: 'Thêm nhân viên mới', edit: 'Sửa thông tin & đổi trạng thái nhân viên' }
  },

  // ===== 6. HỆ THỐNG =====
  {
    id: 'roles',
    moduleId: 'system',
    name: 'Phân quyền hệ thống',
    actions: ['manage'],
    labels: { manage: 'Truy cập & quản lý phân quyền vai trò' }
  },
  {
    id: 'audit_logs',
    moduleId: 'system',
    name: 'Nhật ký hệ thống (Audit Logs)',
    actions: ['view'],
    labels: { view: 'Xem nhật ký thao tác hệ thống' }
  },
  {
    id: 'settings',
    moduleId: 'system',
    name: 'Cài đặt hệ thống',
    actions: ['view', 'edit'],
    labels: { view: 'Xem cài đặt hệ thống', edit: 'Đổi cài đặt hệ thống (VietQR, Timer giữ chỗ)' }
  }
])

const actionLabel = (feature, action) => feature?.labels?.[action] || actionLabels[action] || action

const actionLabels = {
  view: 'Xem (View)',
  add: 'Thêm mới (Add)',
  edit: 'Chỉnh sửa (Edit)',
  delete: 'Xoá (Delete)',
  manage: 'Quản lý (Manage)'
}

const actionLabelsShort = {
  view: 'Xem',
  add: 'Thêm',
  edit: 'Sửa',
  delete: 'Xoá',
  manage: 'Quản lý'
}

// --- TRẠNG THÁI (STATE) ---
const activeRole = ref(null)
const activeModule = ref('operations')
const isLoading = ref(false)
const isSaving = ref(false)
const saveMessage = ref('')

// permissions Matrix: roleId -> featureId -> array of actions
const permissions = ref({})

const activeRoleData = computed(() => roles.value.find(r => r.id === activeRole.value) || null)
const isAdminRole = computed(() => (activeRoleData.value?.name || '').toUpperCase() === 'ADMIN')
const activeScopeName = computed(() => activeRoleData.value?.name || 'Chưa chọn vai trò')

const fetchRoles = async () => {
  isLoading.value = true
  try {
    const { data } = await rolePermissionApi.getRoles()
    const list = data.data ?? data
    const ROLE_ORDER = { ADMIN: 0, MANAGER: 1, STAFF: 2 }
    roles.value = list
      .filter(r => (r.name || '').toUpperCase() !== 'CUSTOMER')
      .sort((a, b) => (ROLE_ORDER[(a.name || '').toUpperCase()] ?? 99) - (ROLE_ORDER[(b.name || '').toUpperCase()] ?? 99))
    const matrix = {}
    roles.value.forEach(r => {
      matrix[r.id] = r.permissions || {}
    })
    permissions.value = matrix
    if (roles.value.length && activeRole.value === null) activeRole.value = roles.value[0].id
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được danh sách vai trò.'))
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await fetchRoles()
})

// --- LOGIC HELPER ---
const currentModuleFeatures = computed(() => {
  return features.value.filter(f => f.moduleId === activeModule.value)
})

const getRolePerms = () => {
  if (!permissions.value[activeRole.value]) permissions.value[activeRole.value] = {}
  return permissions.value[activeRole.value]
}

const hasAction = (featureId, action) => {
  if (isAdminRole.value) return true
  return getRolePerms()[featureId]?.includes(action) || false
}

const getSelectedCount = (feature) => {
  if (feature.adminOnly) return isAdminRole.value ? 1 : 0
  return (feature.actions || []).filter(action => hasAction(feature.id, action)).length
}

const isFeatureAll = (feature) => {
  if (feature.adminOnly) return isAdminRole.value
  return getSelectedCount(feature) === feature.actions?.length && (feature.actions?.length || 0) > 0
}

const isFeaturePartial = (feature) => {
  if (feature.adminOnly) return false
  const count = getSelectedCount(feature)
  return count > 0 && count < (feature.actions?.length || 0)
}

const selectedPermissionsSummary = computed(() => {
  const summary = []
  features.value.forEach(feature => {
    if (feature.adminOnly) return
    const selectedActions = (feature.actions || []).filter(action => hasAction(feature.id, action))
    if (selectedActions && selectedActions.length > 0) {
      const actionNames = selectedActions.map(a => actionLabelsShort[a] || a)
      summary.push({ name: feature.name, actions: actionNames.join(', ') })
    }
  })
  return summary
})

// --- HÀNH ĐỘNG (ACTIONS) ---
const toggleAction = (feature, action) => {
  if (isAdminRole.value) return
  if (feature.adminOnly) return

  const perms = getRolePerms()
  if (!perms[feature.id]) perms[feature.id] = []
  if (perms[feature.id].includes(action)) perms[feature.id] = perms[feature.id].filter(a => a !== action)
  else perms[feature.id].push(action)
}

const toggleFeatureAll = (feature) => {
  if (isAdminRole.value) return
  if (feature.adminOnly) return

  const perms = getRolePerms()
  perms[feature.id] = isFeatureAll(feature) ? [] : [...feature.actions]
}

const clearCurrentModule = () => {
  if (isAdminRole.value) return
  const perms = getRolePerms()
  currentModuleFeatures.value.forEach(f => { if (!f.adminOnly) perms[f.id] = [] })
}

const clearAllPermissions = () => {
  if (isAdminRole.value) return
  const perms = getRolePerms()
  features.value.forEach(f => { if (!f.adminOnly) perms[f.id] = [] })
}

const sanitizeRoleMatrix = (matrix) => {
  const allowed = {}
  features.value.forEach(f => { if (!f.adminOnly) allowed[f.id] = new Set(f.actions) })
  const payload = {}
  Object.keys(matrix || {}).forEach(fid => {
    const allow = allowed[fid]
    if (!allow) return
    const acts = (matrix[fid] || []).filter(a => allow.has(a))
    if (acts.length) {
      payload[fid] = acts
    }
  })
  return payload
}

const saveChanges = async () => {
  if (!activeRole.value) return
  if (isAdminRole.value) {
    const msg = 'Tài khoản ADMIN luôn có toàn quyền hệ thống.'
    toast.info(msg)
    saveMessage.value = msg
    setTimeout(() => saveMessage.value = '', 3000)
    return
  }
  isSaving.value = true
  saveMessage.value = ''
  try {
    const matrix = permissions.value[activeRole.value] || {}
    const payload = sanitizeRoleMatrix(matrix)
    await rolePermissionApi.updatePermissions(activeRole.value, payload)
    const roleName = roles.value.find(r => r.id === activeRole.value)?.name || 'vai trò'
    const msg = `Đã lưu phân quyền cho vai trò ${roleName} thành công!`
    toast.success(msg)
    saveMessage.value = msg
    setTimeout(() => saveMessage.value = '', 3000)
  } catch (err) {
    saveMessage.value = friendlyError(err, 'Lưu phân quyền thất bại.')
    toast.error(saveMessage.value)
  } finally { isSaving.value = false }
}
</script>

<template>
  <div class="flex flex-col h-full relative bg-surface-container-lowest">
    
    <div class="flex-1 overflow-y-auto p-10 pb-36">
      <header class="mb-10 text-on-surface flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Phân quyền Hệ thống</h1>
          <p class="text-on-surface-variant text-sm mt-1">Quản lý và thiết lập quyền truy cập cho từng nhóm vai trò hệ thống (RBAC)</p>
        </div>
      </header>

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

      <section class="mb-8">
         <div class="flex border-b border-outline-variant/20 overflow-x-auto hide-scrollbar">
            <button 
              v-for="mod in modules" 
              :key="mod.id"
              @click="activeModule = mod.id"
              class="px-8 py-5 font-bold text-xs uppercase tracking-widest relative whitespace-nowrap transition-colors"
              :class="activeModule === mod.id ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'">
              {{ mod.name }}
              <div v-if="activeModule === mod.id" 
                   class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-t-full shadow-[0_0_10px_rgba(245,197,24,0.5)] animate-in slide-in-from-bottom-2 duration-300">
              </div>
            </button>
         </div>
      </section>

      <section class="space-y-6">
        <div class="flex justify-between items-center mb-2">
           <h2 class="text-xs font-bold text-on-surface uppercase tracking-widest">
             Chi tiết phân quyền: <span class="text-primary">{{ modules.find(m => m.id === activeModule)?.name }}</span>
           </h2>
           <div class="flex flex-wrap justify-end gap-3">
             <button @click="clearCurrentModule" :disabled="isAdminRole" class="text-[10px] text-red-400 hover:text-red-300 bg-red-400/10 hover:bg-red-400/20 px-4 py-2 rounded-full uppercase tracking-widest font-bold flex items-center gap-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
               <span class="material-symbols-outlined text-sm">clear_all</span> Bỏ tất cả Tab này
             </button>
             <button @click="clearAllPermissions" :disabled="isAdminRole" class="text-[10px] text-red-300 hover:text-red-200 bg-red-500/20 hover:bg-red-500/30 px-4 py-2 rounded-full uppercase tracking-widest font-bold flex items-center gap-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
               <span class="material-symbols-outlined text-sm">block</span> Bỏ toàn bộ quyền
             </button>
           </div>
        </div>

        <div v-if="currentModuleFeatures.length === 0" class="p-10 text-center text-on-surface-variant bg-surface-container-low rounded-xl border border-outline-variant/10">
           Chưa có tính năng nào trong phân hệ này.
        </div>
        
        <div v-for="feature in currentModuleFeatures" :key="feature.id" class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 shadow-sm hover:shadow-md transition-shadow">
           <div class="flex justify-between items-center mb-6 pb-4 border-b border-outline-variant/10">
              <div class="flex items-center gap-4">
                 <button 
                   v-if="!feature.adminOnly"
                   @click="toggleFeatureAll(feature)" 
                   :disabled="isAdminRole" 
                   class="flex items-center justify-center transition-transform hover:scale-110 disabled:cursor-not-allowed disabled:hover:scale-100"
                   :class="isFeatureAll(feature) ? 'text-primary' : 'text-on-surface-variant'">
                    <span class="material-symbols-outlined text-3xl">
                      {{ isFeatureAll(feature) ? 'check_box' : (isFeaturePartial(feature) ? 'indeterminate_check_box' : 'check_box_outline_blank') }}
                    </span>
                 </button>
                 <span v-else class="material-symbols-outlined text-3xl text-amber-400 opacity-90">shield</span>
                 <div><h3 class="font-bold text-sm text-on-surface uppercase tracking-wide">{{ feature.name }}</h3></div>
              </div>
              <div v-if="!feature.adminOnly" class="text-[10px] font-black uppercase tracking-widest px-3 py-1.5 rounded-full"
                   :class="getSelectedCount(feature) > 0 ? 'bg-primary/20 text-primary' : 'bg-surface-container-highest text-on-surface-variant'">
                Đã chọn: {{ getSelectedCount(feature) }} / {{ feature.actions.length }}
              </div>
              <div v-else class="text-[10px] font-black uppercase tracking-widest px-3 py-1.5 rounded-full bg-amber-400/20 text-amber-400 border border-amber-400/30">
                Chỉ ADMIN
              </div>
           </div>

           <div v-if="!feature.adminOnly" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-y-6 gap-x-4">
              <div v-for="action in feature.actions" :key="action" 
                   class="flex items-center gap-3 group cursor-pointer"
                   @click="toggleAction(feature, action)">
                 <span class="material-symbols-outlined text-[20px] transition-all group-hover:text-primary group-hover:scale-110"
                       :class="hasAction(feature.id, action) ? 'text-primary' : 'text-on-surface-variant/40'">
                   {{ hasAction(feature.id, action) ? 'check_box' : 'check_box_outline_blank' }}
                 </span>
                 <span class="text-[10px] font-semibold uppercase tracking-wider transition-colors group-hover:text-on-surface"
                       :class="hasAction(feature.id, action) ? 'text-on-surface' : 'text-on-surface-variant'">
                   {{ actionLabel(feature, action) }}
                 </span>
              </div>
           </div>
           <div v-else class="text-xs text-on-surface-variant italic py-1 flex items-center gap-2">
              <span class="material-symbols-outlined text-base text-amber-400">lock</span> {{ feature.desc }}
           </div>
        </div>
      </section>
    </div>

    <div class="absolute bottom-0 left-0 right-0 bg-surface/95 backdrop-blur-md border-t border-outline-variant/10 p-6 px-10 flex flex-col md:flex-row justify-between items-center gap-6 z-40 shadow-[0_-10px_40px_-10px_rgba(0,0,0,0.5)]">
      <div class="flex items-center gap-6 flex-1 min-w-0 overflow-hidden w-full">
         <div class="flex items-center gap-4 text-on-surface border-r border-outline-variant/10 pr-6 shrink-0">
            <span class="material-symbols-outlined text-4xl text-primary opacity-80">admin_panel_settings</span>
            <div>
               <p class="text-[9px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">Vai trò đang sửa</p>
               <p class="text-sm font-extrabold uppercase tracking-widest text-primary">{{ activeScopeName }}</p>
            </div>
         </div>
         <div class="flex gap-3 overflow-x-auto hide-scrollbar flex-1 min-w-0">
            <div v-for="(item, index) in selectedPermissionsSummary" :key="index" class="flex flex-col bg-surface-container-high px-4 py-2.5 rounded-lg border border-outline-variant/5 shrink-0">
               <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface">{{ item.name }}</span>
               <span class="text-[9px] font-bold uppercase tracking-widest text-primary mt-1">{{ item.actions }}</span>
            </div>
         </div>
      </div>

      <div class="flex flex-col items-end gap-2 w-full md:w-auto shrink-0">
        <span v-if="saveMessage" class="text-[11px] font-bold uppercase tracking-wider text-primary">{{ saveMessage }}</span>
        <button @click="saveChanges" :disabled="isSaving || activeRole === null || isAdminRole" class="w-full md:w-auto px-8 py-3.5 bg-primary text-on-primary font-black text-xs uppercase tracking-[0.2em] rounded-sm shadow-[0_0_20px_rgba(245,197,24,0.3)] hover:brightness-110 hover:shadow-[0_0_30px_rgba(245,197,24,0.5)] transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed">
           {{ isAdminRole ? 'ADMIN TOÀN QUYỀN' : (isSaving ? 'Đang lưu...' : 'LƯU THAY ĐỔI') }}
           <span class="material-symbols-outlined text-base">{{ isAdminRole ? 'shield' : (isSaving ? 'autorenew' : 'save') }}</span>
        </button>
      </div>

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

