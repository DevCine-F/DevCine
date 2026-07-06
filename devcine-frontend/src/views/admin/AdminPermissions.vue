<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { rolePermissionApi } from '@/api/admin/index'

// --- DỮ LIỆU CẤU TRÚC ---
// Vai trò được nạp động từ backend (id là số, kèm ma trận quyền hiện tại)
const roles = ref([])
const staffUsers = ref([])
const configMode = ref('role')
const activeUserId = ref(null)
const userPermissionConfig = ref({
  role: 'STAFF',
  basePermissions: {},
  allow: {},
  deny: {},
  effectivePermissions: {}
})

const modules = ref([
  { id: 'dashboard', name: 'Tổng quan' },
  { id: 'pos', name: 'Nghiệp vụ' },
  { id: 'content', name: 'Nội dung' },
  { id: 'system', name: 'Hệ thống' }
])

// Các feature/action khớp với khoá enforce ở backend (@perm.can('<feature>','<action>'))
const features = ref([
  { id: 'dashboard_stats', moduleId: 'dashboard', name: 'Báo cáo doanh thu', actions: ['view', 'export'] },

  { id: 'pos_ticketing', moduleId: 'pos', name: 'Bán vé tại quầy (POS)', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'pos_inventory', moduleId: 'pos', name: 'Kiểm kê & Kho F&B', actions: ['view', 'add', 'edit', 'delete'] },

  { id: 'movies', moduleId: 'content', name: 'Quản lý danh sách phim', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'schedules', moduleId: 'content', name: 'Điều phối lịch chiếu', actions: ['view', 'add', 'edit', 'delete', 'export'] },
  { id: 'banners', moduleId: 'content', name: 'Quản lý Banner quảng cáo', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'promotions', moduleId: 'content', name: 'Chương trình khuyến mãi', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'pricing', moduleId: 'content', name: 'Cấu hình giá vé', actions: ['view', 'add', 'edit', 'delete'] },

  { id: 'cinemas', moduleId: 'system', name: 'Hệ thống cụm rạp', actions: ['view', 'add', 'edit', 'delete'] },
  { id: 'staff_management', moduleId: 'system', name: 'Nhân sự & Ca trực', actions: ['view', 'add', 'edit', 'delete', 'export'] },
  { id: 'support', moduleId: 'system', name: 'Chăm sóc khách hàng', actions: ['view', 'edit', 'delete'] },
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
const activeRole = ref(null)   // id (số) của vai trò đang chọn
const activeModule = ref('content')
const isLoading = ref(false)
const isSaving = ref(false)
const saveMessage = ref('')

// permissions Matrix: roleId -> featureId -> array of actions
const permissions = ref({})

const activeRoleData = computed(() => roles.value.find(r => r.id === activeRole.value) || null)
const isAdminRole = computed(() => (activeRoleData.value?.name || '').toUpperCase() === 'ADMIN')
const isUserMode = computed(() => configMode.value === 'user')
const activeUserData = computed(() => staffUsers.value.find(u => u.id === activeUserId.value) || null)
const activeScopeName = computed(() => {
  if (isUserMode.value) return activeUserData.value?.fullName || 'Chưa chọn nhân viên'
  return activeRoleData.value?.name || 'Chưa chọn vai trò'
})

const fetchRoles = async () => {
  isLoading.value = true
  try {
    const { data } = await rolePermissionApi.getRoles()
    const list = data.data ?? data
    // Bỏ vai trò CUSTOMER + sắp xếp theo thứ tự cấp bậc: ADMIN → MANAGER → STAFF
    const ROLE_ORDER = { ADMIN: 0, MANAGER: 1, STAFF: 2 }
    roles.value = list
      .filter(r => (r.name || '').toUpperCase() !== 'CUSTOMER')
      .sort((a, b) => (ROLE_ORDER[(a.name || '').toUpperCase()] ?? 99) - (ROLE_ORDER[(b.name || '').toUpperCase()] ?? 99))
    const matrix = {}
    roles.value.forEach(r => {
      matrix[r.id] = r.permissions || {}
    })
    permissions.value = matrix
    if (roles.value.length && activeRole.value === null) {
      activeRole.value = roles.value[0].id
    }
  } catch (err) {
    console.error('Không tải được danh sách vai trò', err)
  } finally {
    isLoading.value = false
  }
}

const fetchStaffUsers = async () => {
  try {
    const { data } = await rolePermissionApi.getStaffUsers()
    const list = data.data ?? data
    staffUsers.value = Array.isArray(list) ? list : []
    if (staffUsers.value.length && activeUserId.value === null) {
      activeUserId.value = staffUsers.value[0].id
    }
  } catch (err) {
    console.error('Không tải được danh sách nhân viên', err)
  }
}

const fetchUserPermissionConfig = async () => {
  if (!activeUserId.value) return
  isLoading.value = true
  try {
    const { data } = await rolePermissionApi.getUserOverrides(activeUserId.value)
    const payload = data.data ?? data
    userPermissionConfig.value = {
      role: payload.role || 'STAFF',
      basePermissions: payload.basePermissions || {},
      allow: payload.allow || {},
      deny: payload.deny || {},
      effectivePermissions: payload.effectivePermissions || {}
    }
  } catch (err) {
    console.error('Không tải được quyền riêng của nhân viên', err)
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await fetchRoles()
  await fetchStaffUsers()
})

watch(activeUserId, () => {
  if (isUserMode.value) fetchUserPermissionConfig()
})

watch(configMode, (mode) => {
  if (mode === 'user') fetchUserPermissionConfig()
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

const matrixHas = (matrix, featureId, action) => matrix?.[featureId]?.includes(action) || false

const addMatrixAction = (matrix, featureId, action) => {
  if (!matrix[featureId]) matrix[featureId] = []
  if (!matrix[featureId].includes(action)) matrix[featureId].push(action)
}

const removeMatrixAction = (matrix, featureId, action) => {
  if (!matrix[featureId]) return
  matrix[featureId] = matrix[featureId].filter(a => a !== action)
}

const rebuildUserEffectivePermissions = () => {
  const base = userPermissionConfig.value.basePermissions || {}
  const allow = userPermissionConfig.value.allow || {}
  const deny = userPermissionConfig.value.deny || {}
  const effective = {}

  Object.keys(base).forEach(featureId => {
    effective[featureId] = [...base[featureId]]
  })
  Object.keys(allow).forEach(featureId => {
    allow[featureId].forEach(action => addMatrixAction(effective, featureId, action))
  })
  Object.keys(deny).forEach(featureId => {
    deny[featureId].forEach(action => removeMatrixAction(effective, featureId, action))
    if (effective[featureId]?.length === 0) delete effective[featureId]
  })

  userPermissionConfig.value.effectivePermissions = effective
}

const hasAction = (featureId, action) => {
  if (isUserMode.value) {
    return matrixHas(userPermissionConfig.value.effectivePermissions, featureId, action)
  }
  if (isAdminRole.value) return true
  const perms = getRolePerms()
  return perms[featureId]?.includes(action) || false
}

const getSelectedCount = (feature) => {
  return feature.actions.filter(action => hasAction(feature.id, action)).length
}

const isFeatureAll = (feature) => {
  return getSelectedCount(feature) === feature.actions.length && feature.actions.length > 0
}

const isFeaturePartial = (feature) => {
  const count = getSelectedCount(feature)
  return count > 0 && count < feature.actions.length
}

const getUserOverrideEffect = (featureId, action) => {
  if (matrixHas(userPermissionConfig.value.deny, featureId, action)) return 'DENY'
  if (matrixHas(userPermissionConfig.value.allow, featureId, action)) return 'ALLOW'
  return 'INHERIT'
}

const effectLabel = (featureId, action) => {
  const effect = getUserOverrideEffect(featureId, action)
  if (effect === 'ALLOW') return 'Riêng'
  if (effect === 'DENY') return 'Chặn'
  return matrixHas(userPermissionConfig.value.basePermissions, featureId, action) ? 'Kế thừa' : 'Không cấp'
}

const selectedPermissionsSummary = computed(() => {
  const summary = []
  
  features.value.forEach(feature => {
    const selectedActions = feature.actions.filter(action => hasAction(feature.id, action))
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
  if (isAdminRole.value && !isUserMode.value) return
  if (isUserMode.value) {
    const allow = userPermissionConfig.value.allow
    const deny = userPermissionConfig.value.deny
    if (hasAction(feature.id, action)) {
      removeMatrixAction(allow, feature.id, action)
      addMatrixAction(deny, feature.id, action)
    } else {
      removeMatrixAction(deny, feature.id, action)
      addMatrixAction(allow, feature.id, action)
    }
    rebuildUserEffectivePermissions()
    return
  }
  const perms = getRolePerms()
  if (!perms[feature.id]) perms[feature.id] = []
  
  if (perms[feature.id].includes(action)) {
    perms[feature.id] = perms[feature.id].filter(a => a !== action)
  } else {
    perms[feature.id].push(action)
  }
}

const toggleFeatureAll = (feature) => {
  if (isAdminRole.value && !isUserMode.value) return
  if (isUserMode.value) {
    const allow = userPermissionConfig.value.allow
    const deny = userPermissionConfig.value.deny
    if (isFeatureAll(feature)) {
      feature.actions.forEach(action => {
        removeMatrixAction(allow, feature.id, action)
        addMatrixAction(deny, feature.id, action)
      })
    } else {
      feature.actions.forEach(action => {
        removeMatrixAction(deny, feature.id, action)
        addMatrixAction(allow, feature.id, action)
      })
    }
    rebuildUserEffectivePermissions()
    return
  }
  const perms = getRolePerms()
  if (isFeatureAll(feature)) {
    perms[feature.id] = []
  } else {
    perms[feature.id] = [...feature.actions]
  }
}

const clearCurrentModule = () => {
  if (isAdminRole.value && !isUserMode.value) return
  if (isUserMode.value) {
    currentModuleFeatures.value.forEach(feature => {
      feature.actions.forEach(action => {
        removeMatrixAction(userPermissionConfig.value.allow, feature.id, action)
        addMatrixAction(userPermissionConfig.value.deny, feature.id, action)
      })
    })
    rebuildUserEffectivePermissions()
    return
  }
  const perms = getRolePerms()
  currentModuleFeatures.value.forEach(f => {
    perms[f.id] = []
  })
}

const clearAllPermissions = () => {
  if (isAdminRole.value && !isUserMode.value) return
  if (isUserMode.value) {
    features.value.forEach(feature => {
      feature.actions.forEach(action => {
        removeMatrixAction(userPermissionConfig.value.allow, feature.id, action)
        addMatrixAction(userPermissionConfig.value.deny, feature.id, action)
      })
    })
    rebuildUserEffectivePermissions()
    return
  }
  const perms = getRolePerms()
  features.value.forEach(f => {
    perms[f.id] = []
  })
}

const resetUserOverrides = () => {
  if (!isUserMode.value) return
  userPermissionConfig.value.allow = {}
  userPermissionConfig.value.deny = {}
  rebuildUserEffectivePermissions()
}

const compactMatrix = (matrix) => {
  const payload = {}
  Object.keys(matrix || {}).forEach(k => {
    if (Array.isArray(matrix[k]) && matrix[k].length > 0) payload[k] = matrix[k]
  })
  return payload
}

const saveChanges = async () => {
  if (isUserMode.value) {
    if (!activeUserId.value) return
    isSaving.value = true
    saveMessage.value = ''
    try {
      await rolePermissionApi.updateUserOverrides(activeUserId.value, {
        allow: compactMatrix(userPermissionConfig.value.allow),
        deny: compactMatrix(userPermissionConfig.value.deny)
      })
      await fetchUserPermissionConfig()
      saveMessage.value = `Đã lưu quyền riêng cho ${activeUserData.value?.fullName || 'nhân viên'}`
      setTimeout(() => { saveMessage.value = '' }, 3000)
    } catch (err) {
      saveMessage.value = err.response?.data?.message || 'Lưu quyền riêng thất bại.'
    } finally {
      isSaving.value = false
    }
    return
  }

  if (activeRole.value === null) return
  if (isAdminRole.value) {
    saveMessage.value = 'ADMIN luon co toan quyen va khong can luu ma tran.'
    setTimeout(() => { saveMessage.value = '' }, 3000)
    return
  }
  isSaving.value = true
  saveMessage.value = ''
  try {
    const matrix = permissions.value[activeRole.value] || {}
    // Chỉ gửi các feature có ít nhất 1 action để ma trận gọn gàng
    const payload = compactMatrix(matrix)
    await rolePermissionApi.updatePermissions(activeRole.value, payload)
    const roleName = roles.value.find(r => r.id === activeRole.value)?.name
    saveMessage.value = `Đã lưu phân quyền cho vai trò ${roleName}`
    setTimeout(() => { saveMessage.value = '' }, 3000)
  } catch (err) {
    saveMessage.value = err.response?.data?.message || 'Lưu phân quyền thất bại.'
  } finally {
    isSaving.value = false
  }
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

      <section v-if="!isUserMode" class="mb-12">
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
         <div v-if="isUserMode" class="grid md:grid-cols-[minmax(260px,420px)_1fr] gap-5 items-stretch mb-8">
          <select
            v-model="activeUserId"
            class="bg-surface-container-low border border-outline-variant/20 rounded-xl px-5 py-4 text-sm font-bold text-on-surface focus:outline-none focus:border-primary">
            <option v-for="staff in staffUsers" :key="staff.id" :value="staff.id">
              {{ staff.fullName }} - {{ staff.username }}
            </option>
          </select>
          <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl px-5 py-4">
            <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Quyền riêng từng người</p>
            <p class="text-sm text-on-surface mt-1">
              {{ activeUserData?.fullName || 'Chưa chọn nhân viên' }} kế thừa role {{ userPermissionConfig.role || 'STAFF' }}.
              Tick để cấp/chặn riêng, hoặc đặt lại để dùng đúng quyền theo vai trò.
            </p>
          </div>
         </div>

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
           <div class="flex flex-wrap justify-end gap-3">
             <button @click="clearCurrentModule" :disabled="isAdminRole && !isUserMode" class="text-[10px] text-red-400 hover:text-red-300 bg-red-400/10 hover:bg-red-400/20 px-4 py-2 rounded-full uppercase tracking-widest font-bold flex items-center gap-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
               <span class="material-symbols-outlined text-sm">clear_all</span>
               Bỏ tất cả trong Tab này
             </button>
             <button @click="clearAllPermissions" :disabled="isAdminRole && !isUserMode" class="text-[10px] text-red-300 hover:text-red-200 bg-red-500/20 hover:bg-red-500/30 px-4 py-2 rounded-full uppercase tracking-widest font-bold flex items-center gap-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
               <span class="material-symbols-outlined text-sm">block</span>
               Bỏ toàn bộ quyền
             </button>
             <button v-if="isUserMode" @click="resetUserOverrides" class="text-[10px] text-primary hover:text-yellow-200 bg-primary/10 hover:bg-primary/20 px-4 py-2 rounded-full uppercase tracking-widest font-bold flex items-center gap-2 transition-colors">
               <span class="material-symbols-outlined text-sm">restart_alt</span>
               Đặt lại theo vai trò
             </button>
           </div>
        </div>

        <!-- Khối Features (Cards) -->
        <div v-if="currentModuleFeatures.length === 0" class="p-10 text-center text-on-surface-variant bg-surface-container-low rounded-xl border border-outline-variant/10">
           Chưa có tính năng nào trong phân hệ này.
        </div>
        
        <div v-for="feature in currentModuleFeatures" :key="feature.id" class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 shadow-sm hover:shadow-md transition-shadow">
           <!-- Parent Header -->
           <div class="flex justify-between items-center mb-6 pb-4 border-b border-outline-variant/10">
              <div class="flex items-center gap-4">
                 <button @click="toggleFeatureAll(feature)" :disabled="isAdminRole && !isUserMode" class="flex items-center justify-center transition-transform hover:scale-110 disabled:cursor-not-allowed disabled:hover:scale-100"
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
                   class="flex items-center gap-3 group"
                   :class="(isAdminRole && !isUserMode) ? 'cursor-not-allowed opacity-80' : 'cursor-pointer'"
                   @click="toggleAction(feature, action)">
                 
                 <span class="material-symbols-outlined text-[20px] transition-all group-hover:text-primary group-hover:scale-110"
                       :class="hasAction(feature.id, action) ? 'text-primary' : 'text-on-surface-variant/40'">
                   {{ hasAction(feature.id, action) ? 'check_box' : 'check_box_outline_blank' }}
                 </span>
                 <span class="text-[10px] font-semibold uppercase tracking-wider transition-colors group-hover:text-on-surface"
                       :class="hasAction(feature.id, action) ? 'text-on-surface' : 'text-on-surface-variant'">
                   {{ actionLabels[action] || action }}
                 </span>
                 <span v-if="isUserMode" class="text-[9px] font-black uppercase tracking-widest"
                       :class="getUserOverrideEffect(feature.id, action) === 'DENY' ? 'text-red-300' : (getUserOverrideEffect(feature.id, action) === 'ALLOW' ? 'text-primary' : 'text-on-surface-variant/50')">
                   {{ effectLabel(feature.id, action) }}
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
               <p class="text-sm font-extrabold uppercase tracking-widest text-primary">{{ activeScopeName }}</p>
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
      <div class="flex flex-col items-end gap-2 w-full md:w-auto shrink-0">
        <span v-if="saveMessage" class="text-[10px] font-bold uppercase tracking-widest text-primary">{{ saveMessage }}</span>
        <button @click="saveChanges" :disabled="isSaving || (!isUserMode && (activeRole === null || isAdminRole)) || (isUserMode && !activeUserId)" class="w-full md:w-auto px-10 py-4 bg-primary text-on-primary font-black text-xs uppercase tracking-[0.2em] rounded-sm shadow-[0_0_20px_rgba(245,197,24,0.3)] hover:brightness-110 hover:shadow-[0_0_30px_rgba(245,197,24,0.5)] transition-all flex items-center justify-center gap-3 disabled:opacity-60 disabled:cursor-not-allowed">
           {{ (!isUserMode && isAdminRole) ? 'ADMIN toàn quyền' : (isSaving ? 'Đang lưu...' : 'Lưu Thay Đổi') }}
           <span class="material-symbols-outlined text-sm">{{ (!isUserMode && isAdminRole) ? 'admin_panel_settings' : (isSaving ? 'autorenew' : 'save') }}</span>
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
