<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { rolePermissionApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

// --- DỮ LIỆU CẤU TRÚC 18 TAB SIDEBAR ---
const sidebarGroups = ref([
  {
    id: 'operations',
    name: 'Tổng quan & Vận hành',
    icon: 'space_dashboard',
    tabs: [
      {
        id: 'dashboard_stats',
        name: 'Tổng quan (Dashboard)',
        icon: 'dashboard',
        desc: 'Xem báo cáo doanh thu & số liệu thống kê tổng hợp',
        actions: ['view'],
        labels: { view: 'Xem báo cáo' }
      },
      {
        id: 'pos_ticketing',
        name: 'Bán vé (POS)',
        icon: 'confirmation_number',
        desc: 'Bán vé tại quầy, chọn ghế, F&B, voucher & thanh toán',
        actions: ['view', 'add'],
        labels: { view: 'Vào quầy POS', add: 'Bán vé & Thu tiền' }
      },
      {
        id: 'ticket_checkin',
        name: 'Kiểm soát vé',
        icon: 'qr_code_scanner',
        desc: 'Quét mã QR vé / đơn đặt vé để soát khách vào phòng chiếu',
        actions: ['view'],
        labels: { view: 'Quét & Soát vé' }
      },
      {
        id: 'bookings',
        name: 'Hoá đơn',
        icon: 'receipt_long',
        desc: 'Tra cứu danh sách hoá đơn và xem chi tiết giao dịch',
        actions: ['view'],
        labels: { view: 'Xem hoá đơn' }
      }
    ]
  },
  {
    id: 'content',
    name: 'Phim & Nội dung',
    icon: 'movie_filter',
    tabs: [
      {
        id: 'movies',
        name: 'Quản lý phim',
        icon: 'movie',
        desc: 'Danh sách phim, thông tin chi tiết và điều phối lịch chiếu',
        actions: ['view', 'add', 'edit', 'delete'],
        labels: { view: 'Xem danh sách', add: 'Thêm phim', edit: 'Sửa phim & Suất', delete: 'Xoá phim' }
      },
      {
        id: 'movie_categories',
        name: 'Danh mục phim',
        icon: 'category',
        desc: 'Quản lý Thể loại phim, Định dạng chiếu (2D/3D) và Độ tuổi',
        actions: ['view', 'add', 'edit', 'delete'],
        labels: { view: 'Xem danh mục', add: 'Thêm danh mục', edit: 'Sửa danh mục', delete: 'Xoá danh mục' }
      },
      {
        id: 'banners',
        name: 'Quản lý Banner',
        icon: 'view_carousel',
        desc: 'Cấu hình banner slider trang chủ và thứ tự hiển thị',
        actions: ['view', 'add', 'edit', 'delete'],
        labels: { view: 'Xem banner', add: 'Thêm banner', edit: 'Sửa banner', delete: 'Xoá banner' }
      }
    ]
  },
  {
    id: 'infrastructure',
    name: 'Rạp & Hạ tầng',
    icon: 'domain',
    tabs: [
      {
        id: 'cinemas',
        name: 'Cụm rạp & Phòng chiếu',
        icon: 'theater_comedy',
        desc: 'Quản lý cụm rạp, phòng chiếu và thiết lập sơ đồ ghế',
        actions: ['view', 'add', 'edit', 'delete'],
        labels: { view: 'Xem rạp & ghế', add: 'Thêm rạp/phòng', edit: 'Sửa rạp & ghế', delete: 'Xoá rạp/phòng' }
      },
      {
        id: 'fnb_menu',
        name: 'Thực đơn F&B / Combo',
        icon: 'fastfood',
        desc: 'Quản lý thực đơn bắp rang, nước uống và các gói combo',
        actions: ['view', 'add', 'edit', 'delete'],
        labels: { view: 'Xem thực đơn', add: 'Thêm món', edit: 'Sửa món & Giá', delete: 'Xoá món' }
      }
    ]
  },
  {
    id: 'business',
    name: 'Kinh doanh & Khách hàng',
    icon: 'storefront',
    tabs: [
      {
        id: 'pricing',
        name: 'Quản lý giá',
        icon: 'payments',
        desc: 'Cấu hình bảng giá vé gốc theo đối tượng, ngày và phụ thu ghế/suất',
        actions: ['view', 'edit'],
        labels: { view: 'Xem bảng giá', edit: 'Chỉnh sửa giá' }
      },
      {
        id: 'promotions',
        name: 'Khuyến mãi',
        icon: 'loyalty',
        desc: 'Quản lý mã giảm giá voucher và bài viết tin tức ưu đãi',
        actions: ['view', 'add', 'edit', 'delete'],
        labels: { view: 'Xem khuyến mãi', add: 'Tạo voucher', edit: 'Sửa voucher', delete: 'Xoá voucher' }
      },
      {
        id: 'customers',
        name: 'Khách hàng',
        icon: 'groups',
        desc: 'Quản lý danh sách thành viên, khóa tài khoản và reset mật khẩu',
        actions: ['view', 'edit'],
        labels: { view: 'Xem danh sách', edit: 'Sửa & Khoá TK' }
      },
      {
        id: 'admin_reviews',
        name: 'Đánh giá phim',
        icon: 'reviews',
        desc: 'Kiểm duyệt và quản lý bình luận đánh giá của khán giả',
        adminOnly: true,
        actions: []
      },
      {
        id: 'admin_faqs',
        name: 'Câu hỏi (FAQ)',
        icon: 'quiz',
        desc: 'Quản lý bộ câu hỏi thường gặp trợ giúp khách hàng',
        adminOnly: true,
        actions: []
      }
    ]
  },
  {
    id: 'hr',
    name: 'Nhân sự',
    icon: 'badge',
    tabs: [
      {
        id: 'staff_management',
        name: 'Nhân viên',
        icon: 'group',
        desc: 'Quản lý danh sách nhân sự rạp, tài khoản và thông tin công tác',
        actions: ['view', 'add', 'edit'],
        labels: { view: 'Xem nhân viên', add: 'Thêm nhân viên', edit: 'Sửa & Đổi trạng thái' }
      }
    ]
  },
  {
    id: 'system',
    name: 'Hệ thống',
    icon: 'tune',
    tabs: [
      {
        id: 'roles',
        name: 'Phân quyền',
        icon: 'admin_panel_settings',
        desc: 'Thiết lập ma trận quyền hạn truy cập cho từng nhóm vai trò',
        actions: ['manage'],
        labels: { manage: 'Quản trị phân quyền' }
      },
      {
        id: 'audit_logs',
        name: 'Nhật ký',
        icon: 'manage_search',
        desc: 'Tra cứu lịch sử hoạt động và nhật ký thao tác toàn hệ thống',
        actions: ['view'],
        labels: { view: 'Xem nhật ký' }
      },
      {
        id: 'settings',
        name: 'Cài đặt',
        icon: 'settings',
        desc: 'Cấu hình tham số rạp, tài khoản VietQR và thời gian giữ chỗ',
        actions: ['view', 'edit'],
        labels: { view: 'Xem cài đặt', edit: 'Đổi cài đặt' }
      }
    ]
  }
])

// Danh sách phẳng tất cả các tab (dùng cho lookup và tính toán)
const allTabs = computed(() => {
  const list = []
  sidebarGroups.value.forEach(g => {
    g.tabs.forEach(t => list.push({ ...t, groupName: g.name }))
  })
  return list
})

// --- TRẠNG THÁI (STATE) ---
const roles = ref([])
const staffUsers = ref([])
const configMode = ref('role') // 'role' | 'user'
const activeRole = ref(null)
const activeUserId = ref(null)
const isLoading = ref(false)
const isSaving = ref(false)
const saveMessage = ref('')

// Ma trận quyền: roleId -> featureId -> array of actions
const permissions = ref({})

// Cấu hình quyền riêng cho nhân viên
const userPermissionConfig = ref({
  role: 'STAFF',
  basePermissions: {},
  allow: {},
  deny: {},
  effectivePermissions: {}
})

const activeRoleData = computed(() => roles.value.find(r => r.id === activeRole.value) || null)
const isAdminRole = computed(() => (activeRoleData.value?.name || '').toUpperCase() === 'ADMIN')
const isUserMode = computed(() => configMode.value === 'user')

const activeUserData = computed(() => staffUsers.value.find(u => u.id === activeUserId.value) || null)
const activeScopeName = computed(() => {
  if (isUserMode.value) return activeUserData.value?.fullName || 'Chưa chọn nhân viên'
  return activeRoleData.value?.name || 'Chưa chọn vai trò'
})

// --- API FETCHING ---
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
    if (roles.value.length && activeRole.value === null) {
      activeRole.value = roles.value[0].id
    }
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được danh sách vai trò.'))
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
    toast.error(friendlyError(err, 'Không tải được danh sách nhân viên.'))
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
    toast.error(friendlyError(err, 'Không tải được quyền riêng của nhân viên.'))
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

// --- MATRIX & PERMISSION HELPERS ---
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

// Kiểm tra quyền đang bật
const hasAction = (featureId, action) => {
  if (isUserMode.value) {
    return matrixHas(userPermissionConfig.value.effectivePermissions, featureId, action)
  }
  if (isAdminRole.value) return true
  const perms = getRolePerms()
  return perms[featureId]?.includes(action) || false
}

// Kiểm tra xem tab đó có quyền "Xem / Truy cập" hay không
const hasView = (tab) => {
  if (tab.adminOnly) return isAdminRole.value
  const viewAction = tab.actions.includes('view') ? 'view' : (tab.actions.includes('manage') ? 'manage' : tab.actions[0])
  if (!viewAction) return false
  return hasAction(tab.id, viewAction)
}

// Kiểm tra số lượng action đã chọn trên tab
const getSelectedCount = (tab) => {
  if (tab.adminOnly) return isAdminRole.value ? 1 : 0
  return tab.actions.filter(action => hasAction(tab.id, action)).length
}

const isTabAll = (tab) => {
  if (tab.adminOnly) return isAdminRole.value
  return tab.actions.length > 0 && getSelectedCount(tab) === tab.actions.length
}

const isTabPartial = (tab) => {
  if (tab.adminOnly) return false
  const count = getSelectedCount(tab)
  return count > 0 && count < tab.actions.length
}

// Trạng thái nhóm: All / Partial / None
const isGroupAll = (group) => {
  const nonAdminTabs = group.tabs.filter(t => !t.adminOnly)
  if (nonAdminTabs.length === 0) return true
  return nonAdminTabs.every(t => isTabAll(t))
}

const isGroupPartial = (group) => {
  const nonAdminTabs = group.tabs.filter(t => !t.adminOnly)
  const anyChecked = nonAdminTabs.some(t => getSelectedCount(t) > 0)
  return anyChecked && !isGroupAll(group)
}

// User Override Status Label
const getUserOverrideEffect = (featureId, action) => {
  if (matrixHas(userPermissionConfig.value.deny, featureId, action)) return 'DENY'
  if (matrixHas(userPermissionConfig.value.allow, featureId, action)) return 'ALLOW'
  return 'INHERIT'
}

// --- LOGIC TƯƠNG TÁC THÔNG MINH ---
// 1. Toggle 1 action lẻ (có tự động ràng buộc quyền View cha - con)
const toggleAction = (tab, action) => {
  if (isAdminRole.value && !isUserMode.value) return
  if (tab.adminOnly) return

  const viewAction = tab.actions.includes('view') ? 'view' : (tab.actions.includes('manage') ? 'manage' : null)
  const isCurrentlyActive = hasAction(tab.id, action)

  if (isUserMode.value) {
    const allow = userPermissionConfig.value.allow
    const deny = userPermissionConfig.value.deny

    if (isCurrentlyActive) {
      // Đang bật -> Tắt
      removeMatrixAction(allow, tab.id, action)
      addMatrixAction(deny, tab.id, action)

      // Nếu tắt quyền XEM -> Tự động tắt luôn toàn bộ các quyền khác của tab
      if (action === viewAction) {
        tab.actions.forEach(act => {
          removeMatrixAction(allow, tab.id, act)
          addMatrixAction(deny, tab.id, act)
        })
      }
    } else {
      // Đang tắt -> Bật
      removeMatrixAction(deny, tab.id, action)
      addMatrixAction(allow, tab.id, action)

      // Nếu bật bất kỳ quyền nào (Thêm/Sửa/Xóa) -> Tự động bật luôn quyền XEM
      if (viewAction && action !== viewAction) {
        removeMatrixAction(deny, tab.id, viewAction)
        addMatrixAction(allow, tab.id, viewAction)
      }
    }
    rebuildUserEffectivePermissions()
    return
  }

  // Chế độ Vai trò (Role Mode)
  const perms = getRolePerms()
  if (!perms[tab.id]) perms[tab.id] = []

  if (isCurrentlyActive) {
    // Tắt action
    perms[tab.id] = perms[tab.id].filter(a => a !== action)
    // Nếu tắt quyền XEM -> Tự động xóa toàn bộ quyền CRUD của tab đó
    if (action === viewAction) {
      perms[tab.id] = []
    }
  } else {
    // Bật action
    if (!perms[tab.id].includes(action)) perms[tab.id].push(action)
    // Nếu bật CRUD khác -> Tự động cấp quyền XEM
    if (viewAction && action !== viewAction && !perms[tab.id].includes(viewAction)) {
      perms[tab.id].push(viewAction)
    }
  }
}

// 2. Toggle Tất cả quyền của 1 Tab (Row Toggle)
const toggleTabAll = (tab) => {
  if (isAdminRole.value && !isUserMode.value) return
  if (tab.adminOnly) return

  const allActive = isTabAll(tab)

  if (isUserMode.value) {
    const allow = userPermissionConfig.value.allow
    const deny = userPermissionConfig.value.deny

    if (allActive) {
      tab.actions.forEach(action => {
        removeMatrixAction(allow, tab.id, action)
        addMatrixAction(deny, tab.id, action)
      })
    } else {
      tab.actions.forEach(action => {
        removeMatrixAction(deny, tab.id, action)
        addMatrixAction(allow, tab.id, action)
      })
    }
    rebuildUserEffectivePermissions()
    return
  }

  const perms = getRolePerms()
  if (allActive) {
    perms[tab.id] = []
  } else {
    perms[tab.id] = [...tab.actions]
  }
}

// 3. Toggle Tất cả quyền trong 1 Nhóm Sidebar (Group Toggle)
const toggleGroup = (group, enable) => {
  if (isAdminRole.value && !isUserMode.value) return

  group.tabs.forEach(tab => {
    if (tab.adminOnly) return
    if (isUserMode.value) {
      const allow = userPermissionConfig.value.allow
      const deny = userPermissionConfig.value.deny
      tab.actions.forEach(action => {
        if (enable) {
          removeMatrixAction(deny, tab.id, action)
          addMatrixAction(allow, tab.id, action)
        } else {
          removeMatrixAction(allow, tab.id, action)
          addMatrixAction(deny, tab.id, action)
        }
      })
    } else {
      const perms = getRolePerms()
      perms[tab.id] = enable ? [...tab.actions] : []
    }
  })

  if (isUserMode.value) {
    rebuildUserEffectivePermissions()
  }
}

// 4. Bỏ toàn bộ quyền hệ thống
const clearAllPermissions = () => {
  if (isAdminRole.value && !isUserMode.value) return
  sidebarGroups.value.forEach(group => toggleGroup(group, false))
}

// 5. Cấp toàn bộ quyền (Select All All)
const grantAllPermissions = () => {
  if (isAdminRole.value && !isUserMode.value) return
  sidebarGroups.value.forEach(group => toggleGroup(group, true))
}

const resetUserOverrides = () => {
  if (!isUserMode.value) return
  userPermissionConfig.value.allow = {}
  userPermissionConfig.value.deny = {}
  rebuildUserEffectivePermissions()
  toast.success('Đã đặt lại quyền theo vai trò mặc định.')
}

// --- TÍNH TOÁN TỔNG KẾT QUYỀN ĐÃ CẤP ---
const totalGrantedTabs = computed(() => {
  return allTabs.value.filter(tab => hasView(tab)).length
})

const selectedPermissionsSummary = computed(() => {
  const summary = []
  allTabs.value.forEach(tab => {
    if (tab.adminOnly) return
    const activeActions = tab.actions.filter(a => hasAction(tab.id, a))
    if (activeActions.length > 0) {
      summary.push({
        name: tab.name,
        count: `${activeActions.length}/${tab.actions.length}`
      })
    }
  })
  return summary
})

// --- LƯU THAY ĐỔI VÀO CSDL ---
const sanitizeMatrix = (matrix) => {
  const payload = {}
  allTabs.value.forEach(tab => {
    if (tab.adminOnly) return
    const actions = (matrix[tab.id] || []).filter(a => tab.actions.includes(a))
    if (actions.length > 0) {
      payload[tab.id] = actions
      // Đồng bộ ngầm schedules theo movies để backend ShowtimeController chạy hoàn hảo
      if (tab.id === 'movies') {
        payload['schedules'] = actions
      }
    }
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
        allow: sanitizeMatrix(userPermissionConfig.value.allow),
        deny: sanitizeMatrix(userPermissionConfig.value.deny)
      })
      await fetchUserPermissionConfig()
      const msg = `Đã lưu cấu hình phân quyền cho ${activeUserData.value?.fullName || 'nhân viên'}.`
      toast.success(msg)
      saveMessage.value = msg
      setTimeout(() => { saveMessage.value = '' }, 3000)
    } catch (err) {
      saveMessage.value = friendlyError(err, 'Lưu quyền riêng thất bại.')
      toast.error(saveMessage.value)
    } finally {
      isSaving.value = false
    }
    return
  }

  if (activeRole.value === null) return
  if (isAdminRole.value) {
    const msg = 'Tài khoản ADMIN luôn có toàn quyền hệ thống và không cần cấu hình.'
    toast.info(msg)
    saveMessage.value = msg
    setTimeout(() => { saveMessage.value = '' }, 3000)
    return
  }

  isSaving.value = true
  saveMessage.value = ''
  try {
    const matrix = permissions.value[activeRole.value] || {}
    const payload = sanitizeMatrix(matrix)
    await rolePermissionApi.updatePermissions(activeRole.value, payload)
    const roleName = roles.value.find(r => r.id === activeRole.value)?.name || 'vai trò'
    const msg = `Đã lưu phân quyền cho vai trò ${roleName} thành công!`
    toast.success(msg)
    saveMessage.value = msg
    setTimeout(() => { saveMessage.value = '' }, 3000)
  } catch (err) {
    saveMessage.value = friendlyError(err, 'Lưu phân quyền thất bại.')
    toast.error(saveMessage.value)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="flex flex-col h-full relative bg-surface-container-lowest text-on-surface">
    
    <!-- VÙNG NỘI DUNG CUỘN CHÍNH -->
    <div class="flex-1 overflow-y-auto p-6 lg:p-10 pb-36">
      
      <!-- HEADER CHÍNH -->
      <header class="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-outline-variant/10 pb-6">
        <div>
          <h1 class="text-2xl lg:text-3xl font-extrabold tracking-tight font-headline uppercase">Phân Quyền Hệ Thống</h1>
          <p class="text-on-surface-variant text-xs lg:text-sm mt-1">Thiết lập quyền truy cập và thao tác theo từng tab chức năng trên thanh điều hướng</p>
        </div>

        <!-- Chuyển đổi chế độ: Theo Vai Trò <-> Theo Nhân Viên -->
        <div class="inline-flex bg-surface-container-low p-1 rounded-sm border border-outline-variant/20 shrink-0">
          <button 
            @click="configMode = 'role'"
            class="px-4 py-2 text-xs font-bold uppercase tracking-wider rounded-sm transition-all flex items-center gap-2"
            :class="!isUserMode ? 'bg-primary text-on-primary shadow-sm' : 'text-on-surface-variant hover:text-on-surface'">
            <span class="material-symbols-outlined text-sm">groups</span>
            Theo Vai Trò
          </button>
          <button 
            @click="configMode = 'user'"
            class="px-4 py-2 text-xs font-bold uppercase tracking-wider rounded-sm transition-all flex items-center gap-2"
            :class="isUserMode ? 'bg-primary text-on-primary shadow-sm' : 'text-on-surface-variant hover:text-on-surface'">
            <span class="material-symbols-outlined text-sm">person</span>
            Nhân Viên Riêng
          </button>
        </div>
      </header>

      <!-- KHỐI CHỌN VAI TRÒ HOẶC NHÂN VIÊN -->
      <section class="mb-8 bg-surface-container-low border border-outline-variant/10 rounded-sm p-6 shadow-sm">
        
        <!-- Chế độ 1: Chọn Vai Trò -->
        <div v-if="!isUserMode">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
            <h2 class="text-xs font-extrabold uppercase tracking-widest text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-base">shield_person</span>
              1. Chọn Vai Trò Cần Phân Quyền
            </h2>
            <div class="flex items-center gap-3">
              <button 
                @click="grantAllPermissions" 
                :disabled="isAdminRole"
                class="px-3 py-1.5 text-[11px] font-bold text-primary bg-primary/10 hover:bg-primary/20 rounded-sm uppercase tracking-wider transition-colors disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1.5">
                <span class="material-symbols-outlined text-sm">select_all</span>
                Chọn Tất Cả
              </button>
              <button 
                @click="clearAllPermissions" 
                :disabled="isAdminRole"
                class="px-3 py-1.5 text-[11px] font-bold text-red-400 bg-red-400/10 hover:bg-red-400/20 rounded-sm uppercase tracking-wider transition-colors disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1.5">
                <span class="material-symbols-outlined text-sm">block</span>
                Bỏ Toàn Bộ
              </button>
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <button 
              v-for="role in roles" 
              :key="role.id"
              @click="activeRole = role.id"
              class="p-4 rounded-sm border text-left transition-all duration-200 flex items-center justify-between group"
              :class="activeRole === role.id 
                ? 'bg-primary/10 border-primary text-primary shadow-[0_0_15px_rgba(245,197,24,0.15)]' 
                : 'bg-surface-container-lowest border-outline-variant/20 hover:border-outline-variant/50 text-on-surface'">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-sm flex items-center justify-center font-black text-sm"
                     :class="activeRole === role.id ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'">
                  {{ role.name.charAt(0) }}
                </div>
                <div>
                  <h3 class="font-extrabold text-sm uppercase tracking-wider">{{ role.name }}</h3>
                  <p class="text-[10px] text-on-surface-variant">
                    {{ role.name === 'ADMIN' ? 'Toàn quyền hệ thống' : (role.name === 'MANAGER' ? 'Quản lý cơ sở' : 'Nhân viên trực quầy') }}
                  </p>
                </div>
              </div>
              <span class="material-symbols-outlined text-lg"
                    :class="activeRole === role.id ? 'text-primary' : 'text-on-surface-variant/30 group-hover:text-on-surface-variant'">
                {{ activeRole === role.id ? 'radio_button_checked' : 'radio_button_unchecked' }}
              </span>
            </button>
          </div>
        </div>

        <!-- Chế độ 2: Chọn Nhân Viên Riêng -->
        <div v-else class="grid md:grid-cols-[minmax(280px,400px)_1fr] gap-6 items-center">
          <div>
            <label class="text-[11px] font-extrabold uppercase tracking-widest text-primary mb-2 block flex items-center gap-2">
              <span class="material-symbols-outlined text-base">person_search</span>
              Chọn Nhân Viên (Staff) Cần Cấu Hình Riêng:
            </label>
            <select
              v-model="activeUserId"
              class="w-full bg-surface-container-lowest border border-outline-variant/30 rounded-sm px-4 py-3 text-sm font-bold text-on-surface focus:outline-none focus:border-primary">
              <option v-for="staff in staffUsers" :key="staff.id" :value="staff.id">
                {{ staff.fullName }} ({{ staff.username }})
              </option>
            </select>
          </div>

          <div class="bg-surface-container-lowest border border-outline-variant/10 rounded-sm p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Quy Tắc Kế Thừa Quyền</p>
              <p class="text-xs text-on-surface mt-1">
                Nhân viên <strong class="text-primary">{{ activeUserData?.fullName || 'STAFF' }}</strong> mặc định kế thừa vai trò <span class="bg-primary/20 text-primary px-1.5 py-0.5 rounded-sm font-bold">{{ userPermissionConfig.role || 'STAFF' }}</span>.
                Bạn có thể tick để cấp thêm hoặc chặn riêng quyền.
              </p>
            </div>
            <button 
              @click="resetUserOverrides" 
              class="px-4 py-2 text-[11px] font-bold text-primary bg-primary/10 hover:bg-primary/20 rounded-sm uppercase tracking-wider transition-colors shrink-0 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">restart_alt</span>
              Đặt Lại Mặc Định
            </button>
          </div>
        </div>

      </section>

      <!-- BẢNG MA TRẬN PHÂN QUYỀN 18 TAB SIDEBAR -->
      <section class="space-y-6">
        
        <div v-for="group in sidebarGroups" :key="group.id" class="bg-surface-container-low border border-outline-variant/10 rounded-sm overflow-hidden shadow-sm">
          
          <!-- GROUP HEADER -->
          <div class="bg-surface-container px-6 py-4 border-b border-outline-variant/10 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-primary text-xl">{{ group.icon }}</span>
              <h2 class="text-sm font-extrabold uppercase tracking-widest text-on-surface">{{ group.name }}</h2>
              <span class="text-[10px] font-bold text-on-surface-variant bg-surface-container-highest px-2.5 py-0.5 rounded-sm">
                {{ group.tabs.length }} Tab
              </span>
            </div>

            <!-- Nút chọn nhanh nhóm -->
            <div class="flex items-center gap-2">
              <button 
                @click="toggleGroup(group, true)" 
                :disabled="isAdminRole && !isUserMode"
                class="px-2.5 py-1 text-[10px] font-bold text-primary bg-primary/10 hover:bg-primary/20 rounded-sm uppercase tracking-wider transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                Chọn Cả Nhóm
              </button>
              <button 
                @click="toggleGroup(group, false)" 
                :disabled="isAdminRole && !isUserMode"
                class="px-2.5 py-1 text-[10px] font-bold text-on-surface-variant hover:text-red-400 bg-surface-container-highest hover:bg-red-400/10 rounded-sm uppercase tracking-wider transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                Bỏ Cả Nhóm
              </button>
            </div>
          </div>

          <!-- BẢNG CÁC TAB TRONG NHÓM -->
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="border-b border-outline-variant/10 bg-surface-container-lowest/50 text-[10px] font-extrabold uppercase tracking-[0.15em] text-on-surface-variant">
                  <th class="py-3 px-6 w-[34%]">Tên Mục / Tab Sidebar</th>
                  <th class="py-3 px-4 w-[14%] text-center">Xem / Truy Cập</th>
                  <th class="py-3 px-4 w-[14%] text-center">Thêm Mới</th>
                  <th class="py-3 px-4 w-[14%] text-center">Chỉnh Sửa</th>
                  <th class="py-3 px-4 w-[14%] text-center">Xoá</th>
                  <th class="py-3 px-4 w-[10%] text-center">Chọn Nhanh</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-outline-variant/5 text-sm">
                
                <tr 
                  v-for="tab in group.tabs" 
                  :key="tab.id"
                  class="hover:bg-surface-container-high/40 transition-colors"
                  :class="hasView(tab) ? 'bg-surface-container-lowest' : 'bg-surface-container-lowest/40 opacity-80'">
                  
                  <!-- CỘT 1: TÊN TAB & MÔ TẢ -->
                  <td class="py-4 px-6">
                    <div class="flex items-start gap-3.5">
                      <div class="w-8 h-8 rounded-sm flex items-center justify-center shrink-0 mt-0.5"
                           :class="hasView(tab) ? 'bg-primary/20 text-primary' : 'bg-surface-container-highest text-on-surface-variant/50'">
                        <span class="material-symbols-outlined text-lg">{{ tab.icon }}</span>
                      </div>
                      <div>
                        <div class="flex items-center gap-2">
                          <span class="font-extrabold text-xs lg:text-sm uppercase tracking-wide"
                                :class="hasView(tab) ? 'text-on-surface' : 'text-on-surface-variant'">
                            {{ tab.name }}
                          </span>
                          <span v-if="tab.adminOnly" class="text-[9px] font-black uppercase tracking-wider px-2 py-0.5 rounded-sm bg-amber-400/20 text-amber-400 border border-amber-400/30">
                            Chỉ Admin
                          </span>
                        </div>
                        <p class="text-[11px] text-on-surface-variant mt-0.5 leading-snug">{{ tab.desc }}</p>
                      </div>
                    </div>
                  </td>

                  <!-- CỘT 2: XEM / TRUY CẬP (VIEW) -->
                  <td class="py-4 px-4 text-center">
                    <div v-if="tab.adminOnly" class="text-on-surface-variant/40 text-xs italic">Cố định</div>
                    <div v-else-if="tab.actions.includes('view') || tab.actions.includes('manage')" class="flex flex-col items-center justify-center">
                      <button 
                        @click="toggleAction(tab, tab.actions.includes('view') ? 'view' : 'manage')"
                        :disabled="isAdminRole && !isUserMode"
                        class="p-1.5 rounded-sm transition-all flex items-center justify-center group"
                        :class="(isAdminRole && !isUserMode) ? 'cursor-not-allowed opacity-80' : 'cursor-pointer hover:bg-primary/10'">
                        <span class="material-symbols-outlined text-2xl transition-transform group-hover:scale-110"
                              :class="hasAction(tab.id, tab.actions.includes('view') ? 'view' : 'manage') ? 'text-primary' : 'text-on-surface-variant/30'">
                          {{ hasAction(tab.id, tab.actions.includes('view') ? 'view' : 'manage') ? 'check_box' : 'check_box_outline_blank' }}
                        </span>
                      </button>
                      <span class="text-[9px] font-bold uppercase tracking-wider mt-0.5"
                            :class="hasAction(tab.id, tab.actions.includes('view') ? 'view' : 'manage') ? 'text-on-surface' : 'text-on-surface-variant/40'">
                        {{ tab.labels?.view || tab.labels?.manage || 'Xem' }}
                      </span>
                    </div>
                    <div v-else class="text-on-surface-variant/20 text-xs">-</div>
                  </td>

                  <!-- CỘT 3: THÊM MỚI (ADD) -->
                  <td class="py-4 px-4 text-center">
                    <div v-if="tab.actions.includes('add')" class="flex flex-col items-center justify-center">
                      <button 
                        @click="toggleAction(tab, 'add')"
                        :disabled="(isAdminRole && !isUserMode) || (!isUserMode && !hasView(tab))"
                        class="p-1.5 rounded-sm transition-all flex items-center justify-center group"
                        :class="((isAdminRole && !isUserMode) || (!isUserMode && !hasView(tab))) ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-primary/10'">
                        <span class="material-symbols-outlined text-2xl transition-transform group-hover:scale-110"
                              :class="hasAction(tab.id, 'add') ? 'text-primary' : 'text-on-surface-variant/30'">
                          {{ hasAction(tab.id, 'add') ? 'check_box' : 'check_box_outline_blank' }}
                        </span>
                      </button>
                      <span class="text-[9px] font-bold uppercase tracking-wider mt-0.5"
                            :class="hasAction(tab.id, 'add') ? 'text-on-surface' : 'text-on-surface-variant/40'">
                        {{ tab.labels?.add || 'Thêm' }}
                      </span>
                    </div>
                    <div v-else class="text-on-surface-variant/20 text-xs">-</div>
                  </td>

                  <!-- CỘT 4: CHỈNH SỬA (EDIT) -->
                  <td class="py-4 px-4 text-center">
                    <div v-if="tab.actions.includes('edit')" class="flex flex-col items-center justify-center">
                      <button 
                        @click="toggleAction(tab, 'edit')"
                        :disabled="(isAdminRole && !isUserMode) || (!isUserMode && !hasView(tab))"
                        class="p-1.5 rounded-sm transition-all flex items-center justify-center group"
                        :class="((isAdminRole && !isUserMode) || (!isUserMode && !hasView(tab))) ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-primary/10'">
                        <span class="material-symbols-outlined text-2xl transition-transform group-hover:scale-110"
                              :class="hasAction(tab.id, 'edit') ? 'text-primary' : 'text-on-surface-variant/30'">
                          {{ hasAction(tab.id, 'edit') ? 'check_box' : 'check_box_outline_blank' }}
                        </span>
                      </button>
                      <span class="text-[9px] font-bold uppercase tracking-wider mt-0.5"
                            :class="hasAction(tab.id, 'edit') ? 'text-on-surface' : 'text-on-surface-variant/40'">
                        {{ tab.labels?.edit || 'Sửa' }}
                      </span>
                    </div>
                    <div v-else class="text-on-surface-variant/20 text-xs">-</div>
                  </td>

                  <!-- CỘT 5: XOÁ (DELETE) -->
                  <td class="py-4 px-4 text-center">
                    <div v-if="tab.actions.includes('delete')" class="flex flex-col items-center justify-center">
                      <button 
                        @click="toggleAction(tab, 'delete')"
                        :disabled="(isAdminRole && !isUserMode) || (!isUserMode && !hasView(tab))"
                        class="p-1.5 rounded-sm transition-all flex items-center justify-center group"
                        :class="((isAdminRole && !isUserMode) || (!isUserMode && !hasView(tab))) ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-primary/10'">
                        <span class="material-symbols-outlined text-2xl transition-transform group-hover:scale-110"
                              :class="hasAction(tab.id, 'delete') ? 'text-primary' : 'text-on-surface-variant/30'">
                          {{ hasAction(tab.id, 'delete') ? 'check_box' : 'check_box_outline_blank' }}
                        </span>
                      </button>
                      <span class="text-[9px] font-bold uppercase tracking-wider mt-0.5"
                            :class="hasAction(tab.id, 'delete') ? 'text-on-surface' : 'text-on-surface-variant/40'">
                        {{ tab.labels?.delete || 'Xoá' }}
                      </span>
                    </div>
                    <div v-else class="text-on-surface-variant/20 text-xs">-</div>
                  </td>

                  <!-- CỘT 6: CHỌN NHANH (ALL ROW TOGGLE) -->
                  <td class="py-4 px-4 text-center">
                    <div v-if="!tab.adminOnly" class="flex items-center justify-center">
                      <button 
                        @click="toggleTabAll(tab)"
                        :disabled="isAdminRole && !isUserMode"
                        class="px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded-sm border transition-all"
                        :class="isTabAll(tab) 
                          ? 'bg-primary/20 text-primary border-primary/40 hover:bg-primary/30' 
                          : (getSelectedCount(tab) > 0 ? 'bg-primary/10 text-primary border-primary/20' : 'bg-surface-container-high text-on-surface-variant border-transparent hover:text-on-surface')">
                        {{ isTabAll(tab) ? 'Tất cả' : (getSelectedCount(tab) > 0 ? `${getSelectedCount(tab)}/${tab.actions.length}` : 'Chọn') }}
                      </button>
                    </div>
                    <div v-else class="text-on-surface-variant/20 text-xs">-</div>
                  </td>

                </tr>

              </tbody>
            </table>
          </div>

        </div>

      </section>

    </div>

    <!-- FOOTER CỐ ĐỊNH Ở ĐÁY (SUMMARY & SAVE ACTION) -->
    <div class="absolute bottom-0 left-0 right-0 bg-surface/95 backdrop-blur-md border-t border-outline-variant/10 p-4 lg:p-6 px-6 lg:px-10 flex flex-col md:flex-row justify-between items-center gap-4 z-40 shadow-[0_-10px_40px_-10px_rgba(0,0,0,0.5)]">
      
      <!-- Thông tin tóm tắt -->
      <div class="flex items-center gap-6 flex-1 min-w-0 overflow-hidden w-full">
        <div class="flex items-center gap-3.5 text-on-surface border-r border-outline-variant/10 pr-6 shrink-0">
          <span class="material-symbols-outlined text-3xl text-primary opacity-90">admin_panel_settings</span>
          <div>
            <p class="text-[9px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">Đang thiết lập cho</p>
            <p class="text-sm font-extrabold uppercase tracking-widest text-primary">{{ activeScopeName }}</p>
          </div>
        </div>

        <div class="flex items-center gap-4 overflow-x-auto hide-scrollbar flex-1 min-w-0">
          <div class="flex items-center gap-2 bg-surface-container-high px-3.5 py-2 rounded-sm border border-outline-variant/10 shrink-0">
            <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Đã cấp quyền xem:</span>
            <span class="text-xs font-black text-primary">{{ totalGrantedTabs }} / 18 Tab</span>
          </div>

          <div v-if="selectedPermissionsSummary.length === 0" class="text-xs text-on-surface-variant italic">
            Chưa có quyền nào được cấp.
          </div>
          <div v-for="(item, index) in selectedPermissionsSummary" :key="index" class="flex items-center gap-1.5 bg-surface-container-high px-3 py-1.5 rounded-sm border border-outline-variant/5 shrink-0">
            <span class="text-[10px] font-bold uppercase tracking-wider text-on-surface">{{ item.name }}</span>
            <span class="text-[9px] font-extrabold text-primary bg-primary/20 px-1.5 py-0.2 rounded-sm">{{ item.count }}</span>
          </div>
        </div>
      </div>

      <!-- Nút Lưu Thay Đổi -->
      <div class="flex items-center gap-3 w-full md:w-auto shrink-0 justify-end">
        <span v-if="saveMessage" class="text-[11px] font-bold uppercase tracking-wider text-primary">{{ saveMessage }}</span>
        <button 
          @click="saveChanges" 
          :disabled="isSaving || (!isUserMode && (activeRole === null || isAdminRole)) || (isUserMode && !activeUserId)" 
          class="w-full md:w-auto px-8 py-3.5 bg-primary text-on-primary font-black text-xs uppercase tracking-[0.2em] rounded-sm shadow-[0_0_20px_rgba(245,197,24,0.3)] hover:brightness-110 hover:shadow-[0_0_30px_rgba(245,197,24,0.5)] transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed">
          {{ (!isUserMode && isAdminRole) ? 'ADMIN TOÀN QUYỀN' : (isSaving ? 'Đang lưu...' : 'LƯU THAY ĐỔI') }}
          <span class="material-symbols-outlined text-base">{{ (!isUserMode && isAdminRole) ? 'shield' : (isSaving ? 'autorenew' : 'save') }}</span>
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
