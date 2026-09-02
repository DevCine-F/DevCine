<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { customerApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { formatVND, formatDate } from '@/utils/format'

const toast = useToastStore()

// ===== State danh sách & Bộ lọc =====
const customers = ref([])
const isLoading = ref(false)
const error = ref('')
const searchQuery = ref('')
const tierFilter = ref('ALL')
const statusFilter = ref('ALL')
const typeFilter = ref('ALL')
const sortBy = ref('createdAt')
const sortOrder = ref('desc') // 'asc' | 'desc'
const pageSize = ref(10)
const currentPage = ref(1)
let searchTimer = null

// ===== State Custom Dropdown UI =====
const tierDropdownOpen = ref(false)
const statusDropdownOpen = ref(false)
const typeDropdownOpen = ref(false)
const pageSizeDropdownOpen = ref(false)

const TIER_OPTIONS = [
  { value: 'ALL', label: 'Mọi hạng thẻ', icon: 'apps', color: 'text-on-surface-variant' },
  { value: 'PLATINUM', label: 'Platinum (Bạch Kim)', icon: 'workspace_premium', color: 'text-sky-300' },
  { value: 'GOLD', label: 'Gold (Vàng)', icon: 'military_tech', color: 'text-primary' },
  { value: 'SILVER', label: 'Silver (Bạc)', icon: 'stars', color: 'text-slate-300' },
  { value: 'BRONZE', label: 'Bronze (Đồng)', icon: 'shield', color: 'text-amber-600' }
]

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Mọi trạng thái', icon: 'apps', color: 'text-on-surface-variant' },
  { value: 'ACTIVE', label: 'Đang hoạt động', dot: 'bg-emerald-400', color: 'text-emerald-400' },
  { value: 'LOCKED', label: 'Đã khóa', dot: 'bg-rose-400', color: 'text-rose-400' }
]

const TYPE_OPTIONS = [
  { value: 'ALL', label: 'Mọi loại khách', icon: 'apps', color: 'text-on-surface-variant' },
  { value: 'MEMBER', label: 'Thành viên', icon: 'person', color: 'text-sky-300' },
  { value: 'GUEST', label: 'Khách vãng lai', icon: 'storefront', color: 'text-amber-400' }
]

const PAGE_SIZE_OPTIONS = [10, 20, 50]

const LOCK_REASONS = [
  'Spam giữ chỗ / Không nhận vé',
  'Gian lận khuyến mãi / Điểm thưởng',
  'Yêu cầu từ chính chủ tài khoản',
  'Vi phạm chính sách và quy chế rạp',
  'Khác (nhập chi tiết bên dưới)'
]

const selectedTierOption = computed(() => TIER_OPTIONS.find(o => o.value === tierFilter.value) || TIER_OPTIONS[0])
const selectedStatusOption = computed(() => STATUS_OPTIONS.find(o => o.value === statusFilter.value) || STATUS_OPTIONS[0])
const selectedTypeOption = computed(() => TYPE_OPTIONS.find(o => o.value === typeFilter.value) || TYPE_OPTIONS[0])

const closeAllDropdowns = () => {
  tierDropdownOpen.value = false
  statusDropdownOpen.value = false
  typeDropdownOpen.value = false
  pageSizeDropdownOpen.value = false
  orderPageSizeDropdownOpen.value = false
}

// ===== State Modal Chi tiết =====
const showDetailModal = ref(false)
const detailLoading = ref(false)
const selectedCustomer = ref(null)
const detailTab = ref('general') // 'general' | 'orders' | 'points_vouchers'
const customerOrders = ref([])
const customerOrdersLoading = ref(false)
const customerVouchers = ref([])
const customerPoints = ref([])
const customerHistoryLoading = ref(false)

// ===== Phân trang Lịch sử đơn hàng của khách (trong Modal) =====
const orderPageSize = ref(5)
const orderCurrentPage = ref(1)
const orderPageSizeDropdownOpen = ref(false)
const ORDER_PAGE_SIZE_OPTIONS = [5, 10, 20]

const totalOrderPages = computed(() => {
  return Math.ceil(customerOrders.value.length / orderPageSize.value) || 1
})

const paginatedCustomerOrders = computed(() => {
  const start = (orderCurrentPage.value - 1) * orderPageSize.value
  return customerOrders.value.slice(start, start + orderPageSize.value)
})

watch(totalOrderPages, (newTotal) => {
  if (orderCurrentPage.value > newTotal) {
    orderCurrentPage.value = newTotal
  }
})

const changeOrderPageSize = (size) => {
  orderPageSize.value = size
  orderCurrentPage.value = 1
  orderPageSizeDropdownOpen.value = false
}

const goToOrderPage = (p) => {
  if (p < 1 || p > totalOrderPages.value || p === orderCurrentPage.value) return
  orderCurrentPage.value = p
}

// ===== State Modal Chỉnh sửa =====
const showEditModal = ref(false)
const editSaving = ref(false)
const editSendingReset = ref(false)
const editForm = ref({
  userId: null,
  fullName: '',
  dob: '',
  email: '',
  phone: '',
  membershipTier: '',
  loyaltyPoints: 0,
  createdAt: '',
  isActive: true,
  isGuest: false
})
const editErrors = ref({})

// ===== State Modal Khóa / Mở khóa =====
const showLockModal = ref(false)
const lockSaving = ref(false)
const lockTarget = ref(null)
const lockReasonOption = ref('Spam giữ chỗ / Không nhận vé')
const lockReasonCustom = ref('')

// ===== Helper Functions =====
const tierStyle = (tier) => {
  const t = (tier || 'BRONZE').toUpperCase()
  return {
    PLATINUM: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
    GOLD: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
    SILVER: 'bg-slate-400/15 text-slate-300 border-slate-400/30',
    BRONZE: 'bg-amber-800/20 text-amber-600 border-amber-800/40'
  }[t] || 'bg-white/10 text-on-surface-variant border-white/10'
}

const formatDateTime = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d)) return '—'
  return d.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

// Tính thông tin Thẻ hội viên VIP (khớp chuẩn ProfileInfoView)
const computeVipCard = (c) => {
  if (!c) return null
  const tier = (c.membershipTier || 'BRONZE').toUpperCase()
  const lifetime = c.lifetimePoints ?? c.loyaltyPoints ?? 0

  if (tier === 'PLATINUM' || lifetime >= 10000) {
    return {
      name: 'Platinum',
      nextTier: null,
      pointsNeeded: 0,
      percent: 100,
      colorClass: 'text-sky-200',
      bgGradient: 'from-[#0c1420] via-[#16243a] to-[#0a0e16] border-sky-400/30',
      badgeBg: 'bg-sky-400/15 text-sky-200 border border-sky-400/40',
      glow: 'shadow-[0_0_38px_rgba(56,189,248,0.28)]',
      cardIcon: 'workspace_premium',
      note: 'Đã đạt hạng thành viên cao nhất Platinum với đặc quyền tối đa.'
    }
  } else if (tier === 'GOLD' || lifetime >= 5000) {
    const needed = Math.max(0, 10000 - lifetime)
    const percent = Math.min(100, Math.max(0, ((lifetime - 5000) / 5000) * 100))
    return {
      name: 'Gold',
      nextTier: 'Platinum',
      pointsNeeded: needed,
      percent,
      colorClass: 'text-primary',
      bgGradient: 'from-[#241c06] via-[#3a2e08] to-[#16110a] border-primary/40',
      badgeBg: 'bg-primary/15 text-primary border border-primary/40',
      glow: 'shadow-[0_0_38px_rgba(245,197,24,0.3)]',
      cardIcon: 'military_tech',
      note: `Tích lũy thêm ${needed.toLocaleString('vi-VN')} điểm để nâng hạng Platinum.`
    }
  } else if (tier === 'SILVER' || lifetime >= 2000) {
    const needed = Math.max(0, 5000 - lifetime)
    const percent = Math.min(100, Math.max(0, ((lifetime - 2000) / 3000) * 100))
    return {
      name: 'Silver',
      nextTier: 'Gold',
      pointsNeeded: needed,
      percent,
      colorClass: 'text-slate-200',
      bgGradient: 'from-[#161b22] via-[#242c38] to-[#12151a] border-slate-400/30',
      badgeBg: 'bg-slate-400/15 text-slate-200 border border-slate-400/40',
      glow: 'shadow-[0_0_38px_rgba(148,163,184,0.2)]',
      cardIcon: 'stars',
      note: `Tích lũy thêm ${needed.toLocaleString('vi-VN')} điểm để nâng hạng Gold.`
    }
  } else {
    const needed = Math.max(0, 2000 - lifetime)
    const percent = Math.min(100, Math.max(0, (lifetime / 2000) * 100))
    return {
      name: 'Bronze',
      nextTier: 'Silver',
      pointsNeeded: needed,
      percent,
      colorClass: 'text-amber-500',
      bgGradient: 'from-[#20120a] via-[#351e12] to-[#170c06] border-amber-800/40',
      badgeBg: 'bg-amber-800/15 text-amber-500 border border-amber-800/40',
      glow: 'shadow-[0_0_38px_rgba(180,83,9,0.2)]',
      cardIcon: 'shield',
      note: `Tích lũy thêm ${needed.toLocaleString('vi-VN')} điểm để nâng hạng Silver.`
    }
  }
}

// ===== Lấy dữ liệu từ Server =====
const fetchCustomers = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await customerApi.list(searchQuery.value)
    customers.value = data.data ?? data ?? []
  } catch (err) {
    error.value = friendlyError(err, 'Không thể tải danh sách khách hàng.')
    customers.value = []
    toast.error(error.value)
  } finally {
    isLoading.value = false
  }
}

const handleSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchCustomers()
  }, 350)
}

// ===== Lọc và sắp xếp Client-side =====
const filteredCustomers = computed(() => {
  return customers.value.filter(c => {
    // 1. Hạng
    if (tierFilter.value !== 'ALL') {
      if ((c.membershipTier || 'BRONZE').toUpperCase() !== tierFilter.value) return false
    }
    // 2. Trạng thái
    if (statusFilter.value !== 'ALL') {
      if (statusFilter.value === 'ACTIVE' && !c.isActive) return false
      if (statusFilter.value === 'LOCKED' && c.isActive) return false
    }
    // 3. Loại khách
    if (typeFilter.value !== 'ALL') {
      if (typeFilter.value === 'MEMBER' && c.isGuest) return false
      if (typeFilter.value === 'GUEST' && !c.isGuest) return false
    }
    return true
  }).sort((a, b) => {
    let va = a[sortBy.value]
    let vb = b[sortBy.value]

    if (sortBy.value === 'createdAt') {
      va = va ? new Date(va).getTime() : 0
      vb = vb ? new Date(vb).getTime() : 0
    } else if (sortBy.value === 'totalSpent' || sortBy.value === 'loyaltyPoints' || sortBy.value === 'userId') {
      va = Number(va) || 0
      vb = Number(vb) || 0
    } else {
      va = String(va || '').toLowerCase()
      vb = String(vb || '').toLowerCase()
    }

    if (sortOrder.value === 'asc') return va > vb ? 1 : (va < vb ? -1 : 0)
    return va < vb ? 1 : (va > vb ? -1 : 0)
  })
})

const totalPages = computed(() => {
  return Math.ceil(filteredCustomers.value.length / pageSize.value) || 1
})

const paginatedCustomers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredCustomers.value.slice(start, start + pageSize.value)
})

const toggleSort = (field) => {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortBy.value = field
    sortOrder.value = 'desc'
  }
}

const resetFilters = () => {
  closeAllDropdowns()
  searchQuery.value = ''
  tierFilter.value = 'ALL'
  statusFilter.value = 'ALL'
  typeFilter.value = 'ALL'
  sortBy.value = 'createdAt'
  sortOrder.value = 'desc'
  currentPage.value = 1
  fetchCustomers()
}

// ===== Xuất CSV (Excel UTF-8 BOM) =====
const exportCsv = () => {
  if (filteredCustomers.value.length === 0) {
    toast.info('Không có dữ liệu phù hợp để xuất.')
    return
  }

  const header = [
    'Mã khách hàng',
    'Họ và tên',
    'Phân loại',
    'Email',
    'Số điện thoại',
    'Ngày sinh',
    'Hạng thành viên',
    'Điểm khả dụng',
    'Điểm trọn đời',
    'Tổng chi tiêu (VNĐ)',
    'Tổng số đơn',
    'Trạng thái',
    'Ngày tham gia'
  ]

  const lines = filteredCustomers.value.map(c => [
    `#DC-${c.userId}`,
    c.fullName || 'Khách hàng',
    c.isGuest ? 'Khách vãng lai' : 'Thành viên chính thức',
    c.isGuest ? '' : (c.email || ''),
    c.phone || '',
    c.dob ? formatDate(c.dob) : '',
    c.membershipTier || 'BRONZE',
    c.loyaltyPoints || 0,
    c.lifetimePoints || 0,
    c.totalSpent || 0,
    c.orderCount || 0,
    c.isActive ? 'Đang hoạt động' : 'Đã khóa',
    c.createdAt ? formatDate(c.createdAt) : ''
  ].map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(','))

  const csv = '\uFEFF' + [header.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `danh-sach-khach-hang-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('Xuất danh sách khách hàng thành công!')
}

// ===== Modal Chi tiết =====
const openDetailModal = async (customer) => {
  closeAllDropdowns()
  selectedCustomer.value = customer
  detailTab.value = 'general'
  showDetailModal.value = true
  customerOrders.value = []
  customerVouchers.value = []
  customerPoints.value = []
  orderCurrentPage.value = 1
  orderPageSizeDropdownOpen.value = false

  detailLoading.value = true
  try {
    const { data } = await customerApi.getById(customer.userId)
    selectedCustomer.value = data.data ?? data ?? customer
  } catch (err) {
    // Keep cached customer info
  } finally {
    detailLoading.value = false
  }
}

// Load Tab Orders / Points on tab change
watch(detailTab, async (newTab) => {
  if (!selectedCustomer.value) return

  if (newTab === 'orders') {
    orderCurrentPage.value = 1
    orderPageSizeDropdownOpen.value = false
    customerOrdersLoading.value = true
    try {
      const { data } = await customerApi.getOrders(selectedCustomer.value.userId)
      customerOrders.value = data.data ?? data ?? []
    } catch (err) {
      toast.error(friendlyError(err, 'Không thể tải lịch sử đặt vé của khách hàng.'))
    } finally {
      customerOrdersLoading.value = false
    }
  } else if (newTab === 'points_vouchers') {
    customerHistoryLoading.value = true
    try {
      const [vRes, pRes] = await Promise.all([
        customerApi.getVouchers(selectedCustomer.value.userId),
        customerApi.getPointHistory(selectedCustomer.value.userId)
      ])
      customerVouchers.value = vRes.data?.data ?? vRes.data ?? []
      customerPoints.value = pRes.data?.data ?? pRes.data ?? []
    } catch (err) {
      toast.error(friendlyError(err, 'Không thể tải lịch sử điểm và voucher.'))
    } finally {
      customerHistoryLoading.value = false
    }
  }
})

// ===== Modal Chỉnh sửa =====
const openEditModal = (customer) => {
  closeAllDropdowns()
  editForm.value = {
    userId: customer.userId,
    fullName: customer.fullName || '',
    dob: customer.dob || '',
    email: customer.email || '',
    phone: customer.phone || '',
    membershipTier: customer.membershipTier || 'BRONZE',
    loyaltyPoints: customer.loyaltyPoints || 0,
    createdAt: customer.createdAt || '',
    isActive: customer.isActive,
    isGuest: customer.isGuest
  }
  editErrors.value = {}
  showEditModal.value = true
}

const saveEditForm = async () => {
  editErrors.value = {}
  const name = editForm.value.fullName.trim()
  if (!name) {
    editErrors.value.fullName = 'Vui lòng nhập họ và tên khách hàng.'
    return
  }
  if (name.length < 2 || name.length > 50) {
    editErrors.value.fullName = 'Họ và tên phải từ 2 đến 50 ký tự.'
    return
  }

  editSaving.value = true
  try {
    await customerApi.update(editForm.value.userId, {
      fullName: name,
      dob: editForm.value.dob || null
    })
    toast.success('Cập nhật thông tin khách hàng thành công!')
    showEditModal.value = false
    fetchCustomers()
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể cập nhật thông tin khách hàng.'))
  } finally {
    editSaving.value = false
  }
}

const sendResetPasswordFromModal = async () => {
  if (editForm.value.isGuest) {
    toast.error('Không thể gửi liên kết đặt lại mật khẩu cho tài khoản vãng lai.')
    return
  }
  if (!editForm.value.isActive) {
    toast.error('Tài khoản đang bị khóa. Vui lòng mở khóa tài khoản trước khi gửi yêu cầu.')
    return
  }

  editSendingReset.value = true
  try {
    const { data } = await customerApi.sendResetPassword(editForm.value.userId)
    toast.success(data?.data?.message || data?.message || 'Đã gửi mã xác minh đặt lại mật khẩu thành công!')
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể gửi yêu cầu đặt lại mật khẩu.'))
  } finally {
    editSendingReset.value = false
  }
}

// ===== Modal Khóa / Mở khóa =====
const openLockModal = (customer) => {
  closeAllDropdowns()
  lockTarget.value = customer
  lockReasonOption.value = 'Spam giữ chỗ / Không nhận vé'
  lockReasonCustom.value = ''
  showLockModal.value = true
}

const submitToggleLock = async () => {
  if (!lockTarget.value) return

  lockSaving.value = true
  const willLock = lockTarget.value.isActive
  const reason = lockReasonOption.value.startsWith('Khác')
    ? (lockReasonCustom.value.trim() || 'Vi phạm chính sách rạp')
    : lockReasonOption.value

  try {
    await customerApi.toggleStatus(lockTarget.value.userId, {
      isActive: !willLock,
      reason: willLock ? reason : null
    })
    toast.success(willLock ? 'Đã khóa tài khoản khách hàng thành công!' : 'Đã mở khóa tài khoản thành công!')
    showLockModal.value = false
    fetchCustomers()
  } catch (err) {
    toast.error(friendlyError(err, 'Thao tác thay đổi trạng thái tài khoản thất bại.'))
  } finally {
    lockSaving.value = false
  }
}

onMounted(fetchCustomers)
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <div class="h-full flex flex-col space-y-6 p-10">
    <!-- Header -->
    <div class="flex justify-between items-end flex-shrink-0">
      <div>
        <h1 class="text-3xl font-black text-on-surface tracking-tighter uppercase italic">
          Quản lý <span class="text-primary">Khách hàng</span>
        </h1>
        <p class="text-sm font-bold text-on-surface-variant uppercase tracking-widest mt-1">
          Danh sách thành viên · Hạng thẻ · Điểm tích lũy &amp; Doanh thu · {{ filteredCustomers.length }} khách hàng
        </p>
      </div>

      <button
        @click="exportCsv"
        class="px-6 py-3 bg-surface-container-high hover:bg-white/10 text-on-surface font-bold text-xs uppercase tracking-widest rounded-xl transition-colors flex items-center gap-2 border border-outline-variant/20 shadow-sm"
      >
        <span class="material-symbols-outlined text-sm text-primary">download</span>
        Xuất CSV
      </button>
    </div>

    <!-- Toolbar lọc cao cấp với Custom Dropdowns -->
    <div class="bg-surface-container-low p-3 rounded-2xl border border-outline-variant/10 flex flex-wrap items-center gap-3 shadow-xl flex-shrink-0">
      <!-- Search Input -->
      <div class="relative flex-grow min-w-[240px] group">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-colors">search</span>
        <input
          v-model="searchQuery"
          @input="handleSearchInput"
          type="text"
          placeholder="Tìm theo tên, email, SĐT, mã #DC-xx..."
          class="w-full h-11 bg-surface-container-highest border border-outline-variant/10 rounded-xl pl-12 pr-4 text-sm text-on-surface placeholder:text-on-surface-variant/50 outline-none hover:border-outline-variant/30 focus:border-primary/60 focus:ring-2 focus:ring-primary/15 transition-all"
        />
      </div>

      <!-- Custom Dropdown: Hạng thẻ -->
      <div class="relative min-w-[170px]">
        <button
          type="button"
          @click="tierDropdownOpen = !tierDropdownOpen; statusDropdownOpen = false; typeDropdownOpen = false"
          class="w-full h-11 bg-surface-container-highest border rounded-xl px-3.5 text-xs font-semibold text-on-surface outline-none cursor-pointer transition-all flex items-center justify-between gap-2 shadow-sm"
          :class="tierDropdownOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'"
        >
          <div class="flex items-center gap-2 truncate">
            <span class="material-symbols-outlined text-base shrink-0" :class="selectedTierOption.color">{{ selectedTierOption.icon }}</span>
            <span class="truncate">{{ selectedTierOption.label }}</span>
          </div>
          <span class="material-symbols-outlined text-base text-on-surface-variant transition-transform duration-200 shrink-0" :class="{ 'rotate-180': tierDropdownOpen }">expand_more</span>
        </button>

        <div v-if="tierDropdownOpen" class="fixed inset-0 z-[55]" @click="tierDropdownOpen = false"></div>
        
        <transition name="fade">
          <div v-if="tierDropdownOpen" class="absolute left-0 top-full mt-1.5 w-full min-w-[200px] bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_12px_40px_-8px_rgba(0,0,0,0.7)] z-[60] overflow-hidden py-1 backdrop-blur-xl">
            <button
              v-for="opt in TIER_OPTIONS"
              :key="opt.value"
              type="button"
              @click="tierFilter = opt.value; currentPage = 1; tierDropdownOpen = false"
              class="w-full flex items-center justify-between px-3.5 py-2.5 text-xs text-left transition-colors"
              :class="tierFilter === opt.value ? 'text-primary bg-primary/10 font-bold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'"
            >
              <div class="flex items-center gap-2.5">
                <span class="material-symbols-outlined text-base" :class="opt.color">{{ opt.icon }}</span>
                <span :class="opt.color">{{ opt.label }}</span>
              </div>
              <span v-if="tierFilter === opt.value" class="material-symbols-outlined text-sm text-primary">check</span>
            </button>
          </div>
        </transition>
      </div>

      <!-- Custom Dropdown: Trạng thái -->
      <div class="relative min-w-[160px]">
        <button
          type="button"
          @click="statusDropdownOpen = !statusDropdownOpen; tierDropdownOpen = false; typeDropdownOpen = false"
          class="w-full h-11 bg-surface-container-highest border rounded-xl px-3.5 text-xs font-semibold text-on-surface outline-none cursor-pointer transition-all flex items-center justify-between gap-2 shadow-sm"
          :class="statusDropdownOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'"
        >
          <div class="flex items-center gap-2 truncate">
            <span v-if="selectedStatusOption.dot" class="w-2 h-2 rounded-full shrink-0" :class="selectedStatusOption.dot"></span>
            <span v-else class="material-symbols-outlined text-base shrink-0" :class="selectedStatusOption.color">{{ selectedStatusOption.icon }}</span>
            <span class="truncate">{{ selectedStatusOption.label }}</span>
          </div>
          <span class="material-symbols-outlined text-base text-on-surface-variant transition-transform duration-200 shrink-0" :class="{ 'rotate-180': statusDropdownOpen }">expand_more</span>
        </button>

        <div v-if="statusDropdownOpen" class="fixed inset-0 z-[55]" @click="statusDropdownOpen = false"></div>
        
        <transition name="fade">
          <div v-if="statusDropdownOpen" class="absolute left-0 top-full mt-1.5 w-full min-w-[180px] bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_12px_40px_-8px_rgba(0,0,0,0.7)] z-[60] overflow-hidden py-1 backdrop-blur-xl">
            <button
              v-for="opt in STATUS_OPTIONS"
              :key="opt.value"
              type="button"
              @click="statusFilter = opt.value; currentPage = 1; statusDropdownOpen = false"
              class="w-full flex items-center justify-between px-3.5 py-2.5 text-xs text-left transition-colors"
              :class="statusFilter === opt.value ? 'text-primary bg-primary/10 font-bold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'"
            >
              <div class="flex items-center gap-2.5">
                <span v-if="opt.dot" class="w-2 h-2 rounded-full" :class="opt.dot"></span>
                <span v-else class="material-symbols-outlined text-base" :class="opt.color">{{ opt.icon }}</span>
                <span :class="opt.color">{{ opt.label }}</span>
              </div>
              <span v-if="statusFilter === opt.value" class="material-symbols-outlined text-sm text-primary">check</span>
            </button>
          </div>
        </transition>
      </div>

      <!-- Custom Dropdown: Loại khách -->
      <div class="relative min-w-[160px]">
        <button
          type="button"
          @click="typeDropdownOpen = !typeDropdownOpen; tierDropdownOpen = false; statusDropdownOpen = false"
          class="w-full h-11 bg-surface-container-highest border rounded-xl px-3.5 text-xs font-semibold text-on-surface outline-none cursor-pointer transition-all flex items-center justify-between gap-2 shadow-sm"
          :class="typeDropdownOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'"
        >
          <div class="flex items-center gap-2 truncate">
            <span class="material-symbols-outlined text-base shrink-0" :class="selectedTypeOption.color">{{ selectedTypeOption.icon }}</span>
            <span class="truncate">{{ selectedTypeOption.label }}</span>
          </div>
          <span class="material-symbols-outlined text-base text-on-surface-variant transition-transform duration-200 shrink-0" :class="{ 'rotate-180': typeDropdownOpen }">expand_more</span>
        </button>

        <div v-if="typeDropdownOpen" class="fixed inset-0 z-[55]" @click="typeDropdownOpen = false"></div>
        
        <transition name="fade">
          <div v-if="typeDropdownOpen" class="absolute left-0 top-full mt-1.5 w-full min-w-[180px] bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_12px_40px_-8px_rgba(0,0,0,0.7)] z-[60] overflow-hidden py-1 backdrop-blur-xl">
            <button
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              type="button"
              @click="typeFilter = opt.value; currentPage = 1; typeDropdownOpen = false"
              class="w-full flex items-center justify-between px-3.5 py-2.5 text-xs text-left transition-colors"
              :class="typeFilter === opt.value ? 'text-primary bg-primary/10 font-bold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'"
            >
              <div class="flex items-center gap-2.5">
                <span class="material-symbols-outlined text-base" :class="opt.color">{{ opt.icon }}</span>
                <span :class="opt.color">{{ opt.label }}</span>
              </div>
              <span v-if="typeFilter === opt.value" class="material-symbols-outlined text-sm text-primary">check</span>
            </button>
          </div>
        </transition>
      </div>

      <!-- Reset Filter Button -->
      <button
        v-if="searchQuery || tierFilter !== 'ALL' || statusFilter !== 'ALL' || typeFilter !== 'ALL'"
        @click="resetFilters"
        class="text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors px-3 h-11 flex items-center gap-1"
      >
        <span class="material-symbols-outlined text-sm">filter_alt_off</span> Bỏ lọc
      </button>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="isLoading" class="space-y-3">
      <div v-for="i in 6" :key="i" class="h-16 bg-surface-container-low rounded-xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Error Alert -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-2xl text-red-400 text-sm flex items-center gap-3">
      <span class="material-symbols-outlined text-2xl">error</span>
      <span>{{ error }}</span>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="filteredCustomers.length === 0"
      class="py-20 text-center bg-surface-container-low border border-dashed border-outline-variant/20 rounded-2xl space-y-4"
    >
      <div class="w-16 h-16 rounded-full bg-surface-container-highest mx-auto flex items-center justify-center text-neutral-500">
        <span class="material-symbols-outlined text-4xl">search_off</span>
      </div>
      <div>
        <h3 class="text-base font-bold text-on-surface">Không tìm thấy khách hàng phù hợp</h3>
        <p class="text-xs text-on-surface-variant mt-1">Không có kết quả nào khớp với từ khóa tìm kiếm hoặc bộ lọc hiện tại.</p>
      </div>
      <button
        @click="resetFilters"
        class="px-5 py-2.5 bg-primary/15 hover:bg-primary/25 text-primary border border-primary/30 rounded-xl text-xs font-bold uppercase tracking-wider transition-colors inline-flex items-center gap-2"
      >
        <span class="material-symbols-outlined text-base">filter_alt_off</span>
        Xóa bộ lọc
      </button>
    </div>

    <!-- Main Data Table -->
    <div v-else class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl flex flex-col">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse min-w-[950px]">
          <thead>
            <tr class="bg-surface-container-highest/50 border-b border-outline-variant/10 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant select-none">
              <th class="px-6 py-4">Khách hàng</th>
              <th class="px-6 py-4">Liên hệ</th>
              <th class="px-6 py-4 text-center">Hạng</th>
              <th
                @click="toggleSort('loyaltyPoints')"
                class="px-6 py-4 text-right cursor-pointer hover:text-primary transition-colors"
              >
                <div class="inline-flex items-center gap-1 justify-end">
                  <span>Điểm</span>
                  <span class="material-symbols-outlined text-sm" v-if="sortBy === 'loyaltyPoints'">
                    {{ sortOrder === 'desc' ? 'arrow_downward' : 'arrow_upward' }}
                  </span>
                  <span class="material-symbols-outlined text-sm opacity-40" v-else>swap_vert</span>
                </div>
              </th>
              <th
                @click="toggleSort('totalSpent')"
                class="px-6 py-4 text-right cursor-pointer hover:text-primary transition-colors"
              >
                <div class="inline-flex items-center gap-1 justify-end">
                  <span>Tổng chi tiêu</span>
                  <span class="material-symbols-outlined text-sm" v-if="sortBy === 'totalSpent'">
                    {{ sortOrder === 'desc' ? 'arrow_downward' : 'arrow_upward' }}
                  </span>
                  <span class="material-symbols-outlined text-sm opacity-40" v-else>swap_vert</span>
                </div>
              </th>
              <th class="px-6 py-4 text-center">Trạng thái</th>
              <th
                @click="toggleSort('createdAt')"
                class="px-6 py-4 text-right cursor-pointer hover:text-primary transition-colors"
              >
                <div class="inline-flex items-center gap-1 justify-end">
                  <span>Tham gia</span>
                  <span class="material-symbols-outlined text-sm" v-if="sortBy === 'createdAt'">
                    {{ sortOrder === 'desc' ? 'arrow_downward' : 'arrow_upward' }}
                  </span>
                  <span class="material-symbols-outlined text-sm opacity-40" v-else>swap_vert</span>
                </div>
              </th>
              <th class="px-6 py-4 text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/5 text-xs">
            <tr
              v-for="c in paginatedCustomers"
              :key="c.userId"
              class="border-b border-outline-variant/5 last:border-0 hover:bg-white/5 transition-colors"
            >
              <!-- Khách hàng + Tag -->
              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-primary/15 border border-primary/30 flex items-center justify-center shrink-0 overflow-hidden shadow-inner">
                    <img v-if="c.avatarUrl" :src="c.avatarUrl" alt="Avatar" class="w-full h-full object-cover" />
                    <span v-else class="material-symbols-outlined text-primary text-lg">person</span>
                  </div>
                  <div>
                    <button
                      @click="openDetailModal(c)"
                      class="text-sm font-bold text-on-surface hover:text-primary transition-colors text-left flex items-center gap-1.5"
                    >
                      {{ c.fullName || 'Khách hàng' }}
                    </button>
                    <div class="flex items-center gap-2 mt-0.5">
                      <span class="text-[10px] text-on-surface-variant font-mono">#DC-{{ c.userId }}</span>
                      <span
                        v-if="c.isGuest"
                        class="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-amber-500/15 text-amber-400 border border-amber-500/30"
                      >
                        Vãng lai
                      </span>
                      <span
                        v-else
                        class="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-sky-500/15 text-sky-300 border border-sky-500/30"
                      >
                        Thành viên
                      </span>
                    </div>
                  </div>
                </div>
              </td>

              <!-- Liên hệ -->
              <td class="px-6 py-4">
                <p v-if="c.isGuest" class="text-on-surface-variant/70 italic text-[11px]">—</p>
                <p v-else class="text-on-surface font-medium text-xs">{{ c.email || '—' }}</p>
                <p class="text-[11px] text-on-surface-variant font-mono mt-0.5">{{ c.phone || 'Chưa cập nhật' }}</p>
              </td>

              <!-- Hạng (Center) -->
              <td class="px-6 py-4 text-center">
                <span :class="tierStyle(c.membershipTier)" class="text-[9px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-md border inline-block shadow-sm">
                  {{ c.membershipTier || 'BRONZE' }}
                </span>
              </td>

              <!-- Điểm (Right) -->
              <td class="px-6 py-4 text-right">
                <span class="font-mono font-bold text-sm text-primary-container">
                  {{ (c.loyaltyPoints || 0).toLocaleString('vi-VN') }}
                </span>
                <p class="text-[10px] text-on-surface-variant font-mono whitespace-nowrap">
                  Trọn đời: {{ (c.lifetimePoints || 0).toLocaleString('vi-VN') }}
                </p>
              </td>

              <!-- Tổng chi tiêu (Right) -->
              <td class="px-6 py-4 text-right">
                <span class="font-mono font-bold text-sm text-primary">
                  {{ formatVND(c.totalSpent || 0) }}
                </span>
                <p class="text-[10px] text-on-surface-variant font-mono whitespace-nowrap">
                  {{ c.orderCount || 0 }} đơn hàng
                </p>
              </td>

              <!-- Trạng thái (Center) -->
              <td class="px-6 py-4 text-center">
                <span
                  v-if="c.isActive"
                  class="text-[9px] font-bold uppercase tracking-widest px-2.5 py-1 rounded bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 inline-flex items-center gap-1.5"
                >
                  <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                  Đang hoạt động
                </span>
                <span
                  v-else
                  class="text-[9px] font-bold uppercase tracking-widest px-2.5 py-1 rounded bg-rose-500/15 text-rose-400 border border-rose-500/30 inline-flex items-center gap-1.5"
                  :title="c.lockReason || 'Tài khoản bị khóa'"
                >
                  <span class="w-1.5 h-1.5 rounded-full bg-rose-400"></span>
                  Đã khóa
                </span>
              </td>

              <!-- Tham gia (Right) -->
              <td class="px-6 py-4 text-right text-xs text-on-surface-variant font-mono">
                {{ formatDate(c.createdAt) }}
              </td>

              <!-- Thao tác (Action Buttons) -->
              <td class="px-6 py-4 text-center">
                <div class="flex items-center justify-center gap-1">
                  <!-- Xem chi tiết -->
                  <button
                    @click="openDetailModal(c)"
                    title="Xem chi tiết hồ sơ"
                    class="p-2 rounded-lg text-on-surface-variant hover:text-primary hover:bg-primary/10 transition-colors"
                  >
                    <span class="material-symbols-outlined text-lg">visibility</span>
                  </button>

                  <!-- Khóa / Mở khóa -->
                  <button
                    @click="openLockModal(c)"
                    :title="c.isActive ? 'Khóa tài khoản' : 'Mở khóa tài khoản'"
                    class="p-2 rounded-lg transition-colors"
                    :class="c.isActive ? 'text-on-surface-variant hover:text-rose-400 hover:bg-rose-500/10' : 'text-rose-400 hover:text-emerald-400 hover:bg-emerald-500/10'"
                  >
                    <span class="material-symbols-outlined text-lg">{{ c.isActive ? 'lock' : 'lock_open' }}</span>
                  </button>

                  <!-- Chỉnh sửa -->
                  <button
                    @click="openEditModal(c)"
                    title="Chỉnh sửa thông tin"
                    class="p-2 rounded-lg text-on-surface-variant hover:text-white hover:bg-white/10 transition-colors"
                  >
                    <span class="material-symbols-outlined text-lg">edit</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination & Footer với Custom Page Size Dropdown -->
      <div class="p-4 bg-surface-container-highest/30 text-[11px] font-bold uppercase tracking-widest text-on-surface-variant flex flex-col sm:flex-row justify-between items-center gap-4">
        <!-- Page size selector & Summary text -->
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <span>Hiển thị:</span>
            
            <!-- Custom Page Size Dropdown -->
            <div class="relative">
              <button
                type="button"
                @click="pageSizeDropdownOpen = !pageSizeDropdownOpen"
                class="h-8 bg-surface-container-highest border rounded-lg px-2.5 text-xs font-bold font-mono text-on-surface outline-none cursor-pointer flex items-center gap-1.5 transition-all shadow-sm"
                :class="pageSizeDropdownOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'"
              >
                <span>{{ pageSize }}</span>
                <span class="material-symbols-outlined text-sm text-on-surface-variant transition-transform duration-200" :class="{ 'rotate-180': pageSizeDropdownOpen }">expand_more</span>
              </button>

              <div v-if="pageSizeDropdownOpen" class="fixed inset-0 z-[55]" @click="pageSizeDropdownOpen = false"></div>

              <transition name="fade">
                <div v-if="pageSizeDropdownOpen" class="absolute bottom-full left-0 mb-1.5 w-24 bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_12px_40px_-8px_rgba(0,0,0,0.7)] z-[60] overflow-hidden py-1 backdrop-blur-xl">
                  <button
                    v-for="size in PAGE_SIZE_OPTIONS"
                    :key="size"
                    type="button"
                    @click="pageSize = size; currentPage = 1; pageSizeDropdownOpen = false"
                    class="w-full flex items-center justify-between px-3 py-2 text-xs font-mono transition-colors"
                    :class="pageSize === size ? 'text-primary bg-primary/10 font-bold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'"
                  >
                    <span>{{ size }}</span>
                    <span v-if="pageSize === size" class="material-symbols-outlined text-sm text-primary">check</span>
                  </button>
                </div>
              </transition>
            </div>

            <span>dòng/trang</span>
          </div>
          <span class="hidden md:inline text-on-surface-variant/40">|</span>
          <span>
            Tổng: <strong class="text-primary">{{ filteredCustomers.length }}</strong> khách hàng
          </span>
        </div>

        <!-- Navigation Buttons -->
        <div class="flex items-center gap-1">
          <button
            @click="currentPage = 1"
            :disabled="currentPage === 1"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang đầu"
          >
            <span class="material-symbols-outlined text-base">first_page</span>
          </button>
          <button
            @click="currentPage--"
            :disabled="currentPage === 1"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang trước"
          >
            <span class="material-symbols-outlined text-base">chevron_left</span>
          </button>

          <span class="px-3 py-1 bg-surface-container-highest rounded-lg font-mono font-bold text-primary text-xs">
            {{ currentPage }} / {{ totalPages }}
          </span>

          <button
            @click="currentPage++"
            :disabled="currentPage === totalPages"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang sau"
          >
            <span class="material-symbols-outlined text-base">chevron_right</span>
          </button>
          <button
            @click="currentPage = totalPages"
            :disabled="currentPage === totalPages"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang cuối"
          >
            <span class="material-symbols-outlined text-base">last_page</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- MODAL A: CHI TIẾT KHÁCH HÀNG (CUSTOMER DETAIL MODAL - 3 TABS)             -->
    <!-- ========================================================================= -->
    <Teleport to="body">
      <div v-if="showDetailModal && selectedCustomer" class="fixed inset-0 z-[999] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md" @click="showDetailModal = false"></div>
        
        <div class="relative w-full max-w-5xl bg-surface border border-outline-variant/20 shadow-2xl rounded-3xl overflow-hidden max-h-[92vh] flex flex-col animate-in fade-in zoom-in duration-200">
          <!-- Modal Header -->
          <div class="p-6 bg-surface-container-high/60 border-b border-outline-variant/10 flex justify-between items-center">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-primary text-2xl">account_circle</span>
              <div>
                <h3 class="text-lg font-black uppercase tracking-wider text-on-surface">Chi tiết Khách hàng</h3>
                <p class="text-xs text-on-surface-variant font-mono">ID #DC-{{ selectedCustomer.userId }} · {{ selectedCustomer.fullName }}</p>
              </div>
            </div>
            <button @click="showDetailModal = false" class="p-1.5 rounded-xl hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Tab Bar Navigation -->
          <div class="flex border-b border-outline-variant/10 bg-surface-container-low px-6">
            <button
              @click="detailTab = 'general'"
              :class="detailTab === 'general' ? 'border-primary text-primary font-bold' : 'border-transparent text-on-surface-variant hover:text-white'"
              class="py-3.5 px-4 border-b-2 text-xs uppercase tracking-wider transition-colors flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-base">badge</span>
              Thông tin chung
            </button>
            <button
              @click="detailTab = 'orders'"
              :class="detailTab === 'orders' ? 'border-primary text-primary font-bold' : 'border-transparent text-on-surface-variant hover:text-white'"
              class="py-3.5 px-4 border-b-2 text-xs uppercase tracking-wider transition-colors flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-base">receipt_long</span>
              Lịch sử đặt vé &amp; Bắp nước
            </button>
            <button
              @click="detailTab = 'points_vouchers'"
              :class="detailTab === 'points_vouchers' ? 'border-primary text-primary font-bold' : 'border-transparent text-on-surface-variant hover:text-white'"
              class="py-3.5 px-4 border-b-2 text-xs uppercase tracking-wider transition-colors flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-base">loyalty</span>
              Lịch sử Điểm &amp; Voucher
            </button>
          </div>

          <!-- Modal Body Content -->
          <div class="p-6 overflow-y-auto flex-1 space-y-6">
            <!-- TAB 1: THÔNG TIN CHUNG -->
            <div v-if="detailTab === 'general'" class="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <!-- Left Column: Personal Profile -->
              <div class="lg:col-span-7 space-y-5">
                <!-- Profile Summary Card -->
                <div class="p-5 bg-surface-container-low rounded-2xl border border-outline-variant/10 flex items-center gap-4">
                  <div class="w-16 h-16 rounded-full bg-primary/10 border-2 border-primary/30 flex items-center justify-center shrink-0 overflow-hidden shadow-lg">
                    <img v-if="selectedCustomer.avatarUrl" :src="selectedCustomer.avatarUrl" alt="Avatar" class="w-full h-full object-cover" />
                    <span v-else class="material-symbols-outlined text-primary text-3xl">person</span>
                  </div>
                  <div class="space-y-1">
                    <div class="flex items-center gap-2.5 flex-wrap">
                      <h4 class="text-lg font-bold text-on-surface">{{ selectedCustomer.fullName || 'Khách hàng' }}</h4>
                      <span
                        v-if="selectedCustomer.isGuest"
                        class="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-400 border border-amber-500/30"
                      >
                        Khách vãng lai
                      </span>
                      <span
                        v-else
                        class="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-sky-500/15 text-sky-300 border border-sky-500/30"
                      >
                        Thành viên
                      </span>
                    </div>
                    <p class="text-xs text-on-surface-variant font-mono">Mã tài khoản: #DC-{{ selectedCustomer.userId }}</p>
                    <div class="pt-1">
                      <span
                        v-if="selectedCustomer.isActive"
                        class="text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 inline-flex items-center gap-1"
                      >
                        <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span> Đang hoạt động
                      </span>
                      <span
                        v-else
                        class="text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-rose-500/15 text-rose-400 border border-rose-500/30 inline-flex items-center gap-1"
                      >
                        <span class="w-1.5 h-1.5 rounded-full bg-rose-400"></span> Đã khóa
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Account Lock Alert Box (if locked) -->
                <div v-if="!selectedCustomer.isActive" class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-2xl text-rose-300 text-xs space-y-1">
                  <div class="flex items-center gap-2 font-bold text-rose-400">
                    <span class="material-symbols-outlined text-base">warning</span>
                    <span>Tài khoản này đang bị tạm khóa truy cập</span>
                  </div>
                  <p class="text-[11px] text-rose-200/80">Lý do: <strong>{{ selectedCustomer.lockReason || 'Theo yêu cầu của Quản trị viên' }}</strong></p>
                  <p v-if="selectedCustomer.lockedAt" class="text-[10px] text-rose-200/60 font-mono">Thời điểm khóa: {{ formatDateTime(selectedCustomer.lockedAt) }}</p>
                </div>

                <!-- 4 Core Fields Grid -->
                <div class="p-5 bg-surface-container-low rounded-2xl border border-outline-variant/10 space-y-4">
                  <h5 class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">Thông tin liên hệ &amp; Cá nhân</h5>
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                    <div>
                      <span class="text-on-surface-variant block text-[11px]">Email</span>
                      <p class="font-medium text-on-surface mt-0.5">{{ selectedCustomer.isGuest ? '— (Khách vãng lai)' : (selectedCustomer.email || '—') }}</p>
                    </div>
                    <div>
                      <span class="text-on-surface-variant block text-[11px]">Số điện thoại</span>
                      <p class="font-mono font-medium text-on-surface mt-0.5">{{ selectedCustomer.phone || 'Chưa cập nhật' }}</p>
                    </div>
                    <div>
                      <span class="text-on-surface-variant block text-[11px]">Ngày sinh</span>
                      <p class="font-medium text-on-surface mt-0.5">{{ selectedCustomer.dob ? formatDate(selectedCustomer.dob) : 'Chưa cập nhật' }}</p>
                    </div>
                    <div>
                      <span class="text-on-surface-variant block text-[11px]">Ngày tham gia</span>
                      <p class="font-mono font-medium text-on-surface mt-0.5">{{ formatDateTime(selectedCustomer.createdAt) }}</p>
                    </div>
                  </div>
                </div>

                <!-- Financial & Orders Summary -->
                <div class="grid grid-cols-2 gap-4">
                  <div class="p-4 bg-surface-container-low rounded-2xl border border-outline-variant/10">
                    <div class="flex items-center gap-2 text-on-surface-variant text-xs">
                      <span class="material-symbols-outlined text-primary text-base">account_balance_wallet</span>
                      <span>Tổng chi tiêu</span>
                    </div>
                    <p class="text-lg font-black font-mono text-primary mt-1">{{ formatVND(selectedCustomer.totalSpent || 0) }}</p>
                  </div>
                  <div class="p-4 bg-surface-container-low rounded-2xl border border-outline-variant/10">
                    <div class="flex items-center gap-2 text-on-surface-variant text-xs">
                      <span class="material-symbols-outlined text-sky-400 text-base">shopping_bag</span>
                      <span>Tổng đơn hàng</span>
                    </div>
                    <p class="text-lg font-black font-mono text-on-surface mt-1">{{ selectedCustomer.orderCount || 0 }} <span class="text-xs font-normal text-on-surface-variant">đơn</span></p>
                  </div>
                </div>
              </div>

              <!-- Right Column: VIP Membership Card & Progress -->
              <div class="lg:col-span-5 flex flex-col">
                <div v-if="computeVipCard(selectedCustomer)" class="space-y-4">
                  <h5 class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">Thẻ thành viên &amp; Tiến trình</h5>
                  
                  <!-- Luxury Membership Card -->
                  <div
                    :class="[computeVipCard(selectedCustomer).bgGradient, computeVipCard(selectedCustomer).glow]"
                    class="p-6 rounded-3xl border relative overflow-hidden bg-gradient-to-br transition-all shadow-xl"
                  >
                    <div class="absolute -right-6 -bottom-6 w-36 h-36 bg-white/5 rounded-full blur-xl pointer-events-none"></div>

                    <div class="flex justify-between items-start">
                      <div>
                        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/80">DevCine Luxury Club</p>
                        <h4 :class="computeVipCard(selectedCustomer).colorClass" class="text-2xl font-black uppercase tracking-wider mt-1">
                          {{ computeVipCard(selectedCustomer).name }}
                        </h4>
                      </div>
                      <span :class="computeVipCard(selectedCustomer).colorClass" class="material-symbols-outlined text-4xl opacity-90">
                        {{ computeVipCard(selectedCustomer).cardIcon }}
                      </span>
                    </div>

                    <!-- Points Summary -->
                    <div class="mt-6 pt-4 border-t border-white/10 grid grid-cols-2 gap-4">
                      <div>
                        <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/80 block">Điểm khả dụng (Ví)</span>
                        <span class="text-xl font-black font-mono text-primary-container">
                          {{ (selectedCustomer.loyaltyPoints || 0).toLocaleString('vi-VN') }}
                        </span>
                        <span class="text-[10px] text-on-surface-variant ml-1">pts</span>
                      </div>
                      <div>
                        <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/80 block">Điểm trọn đời (Hạng)</span>
                        <span class="text-xl font-black font-mono text-white">
                          {{ (selectedCustomer.lifetimePoints || 0).toLocaleString('vi-VN') }}
                        </span>
                        <span class="text-[10px] text-on-surface-variant ml-1">pts</span>
                      </div>
                    </div>

                    <!-- Progress Bar -->
                    <div class="mt-5 space-y-2">
                      <div class="flex justify-between text-[11px] font-bold">
                        <span class="text-on-surface-variant">Tiến trình xét hạng</span>
                        <span :class="computeVipCard(selectedCustomer).colorClass">{{ Math.round(computeVipCard(selectedCustomer).percent) }}%</span>
                      </div>
                      <div class="w-full h-2 bg-black/40 rounded-full overflow-hidden border border-white/5">
                        <div
                          class="h-full bg-gradient-to-r from-primary to-amber-300 transition-all duration-500 rounded-full"
                          :style="{ width: `${computeVipCard(selectedCustomer).percent}%` }"
                        ></div>
                      </div>
                      <p class="text-[10px] text-on-surface-variant/90 italic pt-1">
                        {{ computeVipCard(selectedCustomer).note }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- TAB 2: LỊCH SỬ ĐẶT VÉ & BẮP NƯỚC -->
            <div v-else-if="detailTab === 'orders'" class="space-y-4">
              <div v-if="customerOrdersLoading" class="py-12 text-center">
                <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
                <p class="text-xs text-on-surface-variant mt-2">Đang tải lịch sử đơn hàng...</p>
              </div>
              <div v-else-if="customerOrders.length === 0" class="py-16 text-center border border-dashed border-outline-variant/10 rounded-2xl">
                <span class="material-symbols-outlined text-4xl text-neutral-600 mb-2">receipt_long</span>
                <p class="text-xs text-on-surface-variant font-medium">Khách hàng chưa có lịch sử đặt vé hoặc mua bắp nước nào.</p>
              </div>
              <div v-else class="border border-outline-variant/10 rounded-2xl bg-surface-container-low overflow-hidden shadow-md flex flex-col">
                <div class="overflow-x-auto">
                  <table class="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr class="bg-surface-container-highest/60 border-b border-outline-variant/10 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant select-none">
                        <th class="p-3.5 pl-4">Mã đơn</th>
                        <th class="p-3.5">Phim / Dịch vụ</th>
                        <th class="p-3.5">Suất chiếu &amp; Rạp</th>
                        <th class="p-3.5">Ghế / Món</th>
                        <th class="p-3.5 text-right">Tổng tiền</th>
                        <th class="p-3.5 text-center">Trạng thái</th>
                        <th class="p-3.5 pr-4 text-right">Thời gian</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-outline-variant/5 text-on-surface">
                      <tr v-for="o in paginatedCustomerOrders" :key="o.orderCode" class="hover:bg-white/[0.02] transition-colors">
                        <td class="p-3.5 pl-4">
                          <span class="font-mono font-bold text-primary">{{ o.orderCode }}</span>
                          <div>
                            <span
                              v-if="o.orderType === 'TICKET'"
                              class="text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-sky-500/15 text-sky-300 border border-sky-500/30"
                            >
                              Vé xem phim
                            </span>
                            <span
                              v-else
                              class="text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-amber-500/15 text-amber-400 border border-amber-500/30"
                            >
                              Bắp nước
                            </span>
                          </div>
                        </td>
                        <td class="p-3.5 font-bold text-on-surface">{{ o.title }}</td>
                        <td class="p-3.5 text-[11px] text-on-surface-variant">
                          <p v-if="o.showtimeStart" class="text-on-surface font-medium">{{ formatDateTime(o.showtimeStart) }}</p>
                          <p>{{ o.cinemaName }} · {{ o.roomName }}</p>
                        </td>
                        <td class="p-3.5 font-mono text-[11px] text-on-surface-variant">
                          {{ o.seats || '—' }}
                        </td>
                        <td class="p-3.5 text-right font-mono font-bold text-primary">
                          {{ formatVND(o.finalPrice) }}
                        </td>
                        <td class="p-3.5 text-center">
                          <span
                            class="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md border"
                            :class="o.status === 'CONFIRMED' || o.status === 'COMPLETED' ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30' : 'bg-white/10 text-on-surface-variant border-white/10'"
                          >
                            {{ o.status }}
                          </span>
                        </td>
                        <td class="p-3.5 pr-4 text-right font-mono text-[11px] text-on-surface-variant">
                          {{ formatDateTime(o.createdAt) }}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Pagination Footer trong Modal -->
                <div class="p-3.5 bg-surface-container-highest/30 text-[11px] font-bold uppercase tracking-widest text-on-surface-variant flex flex-col sm:flex-row justify-between items-center gap-3 border-t border-outline-variant/10 flex-shrink-0">
                  <!-- Page size selector & Summary text -->
                  <div class="flex items-center gap-4">
                    <div class="flex items-center gap-2">
                      <span>Hiển thị:</span>

                      <!-- Custom Page Size Dropdown -->
                      <div class="relative">
                        <button
                          type="button"
                          @click="orderPageSizeDropdownOpen = !orderPageSizeDropdownOpen"
                          class="h-7 bg-surface-container-highest border rounded-lg px-2 text-xs font-bold font-mono text-on-surface outline-none cursor-pointer flex items-center gap-1 transition-all shadow-sm"
                          :class="orderPageSizeDropdownOpen ? 'border-primary/60 ring-2 ring-primary/15' : 'border-outline-variant/10 hover:border-outline-variant/30'"
                        >
                          <span>{{ orderPageSize }}</span>
                          <span class="material-symbols-outlined text-sm text-on-surface-variant transition-transform duration-200" :class="{ 'rotate-180': orderPageSizeDropdownOpen }">expand_more</span>
                        </button>

                        <div v-if="orderPageSizeDropdownOpen" class="fixed inset-0 z-[1055]" @click="orderPageSizeDropdownOpen = false"></div>

                        <transition name="fade">
                          <div v-if="orderPageSizeDropdownOpen" class="absolute bottom-full left-0 mb-1.5 w-24 bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_12px_40px_-8px_rgba(0,0,0,0.7)] z-[1060] overflow-hidden py-1 backdrop-blur-xl">
                            <button
                              v-for="size in ORDER_PAGE_SIZE_OPTIONS"
                              :key="size"
                              type="button"
                              @click="changeOrderPageSize(size)"
                              class="w-full flex items-center justify-between px-3 py-1.5 text-xs font-mono transition-colors"
                              :class="orderPageSize === size ? 'text-primary bg-primary/10 font-bold' : 'text-on-surface-variant hover:bg-white/5 hover:text-on-surface'"
                            >
                              <span>{{ size }}</span>
                              <span v-if="orderPageSize === size" class="material-symbols-outlined text-sm text-primary">check</span>
                            </button>
                          </div>
                        </transition>
                      </div>

                      <span>dòng/trang</span>
                    </div>
                    <span class="hidden md:inline text-on-surface-variant/40">|</span>
                    <span>
                      Tổng: <strong class="text-primary">{{ customerOrders.length.toLocaleString('vi-VN') }}</strong> đơn hàng
                    </span>
                  </div>

                  <!-- Navigation Buttons -->
                  <div class="flex items-center gap-1">
                    <button
                      @click="goToOrderPage(1)"
                      :disabled="orderCurrentPage === 1 || customerOrdersLoading"
                      class="p-1 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
                      title="Trang đầu"
                    >
                      <span class="material-symbols-outlined text-base">first_page</span>
                    </button>
                    <button
                      @click="goToOrderPage(orderCurrentPage - 1)"
                      :disabled="orderCurrentPage === 1 || customerOrdersLoading"
                      class="p-1 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
                      title="Trang trước"
                    >
                      <span class="material-symbols-outlined text-base">chevron_left</span>
                    </button>

                    <span class="px-2.5 py-0.5 bg-surface-container-highest rounded-lg font-mono font-bold text-primary text-xs">
                      {{ orderCurrentPage }} / {{ totalOrderPages }}
                    </span>

                    <button
                      @click="goToOrderPage(orderCurrentPage + 1)"
                      :disabled="orderCurrentPage === totalOrderPages || customerOrdersLoading"
                      class="p-1 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
                      title="Trang sau"
                    >
                      <span class="material-symbols-outlined text-base">chevron_right</span>
                    </button>
                    <button
                      @click="goToOrderPage(totalOrderPages)"
                      :disabled="orderCurrentPage === totalOrderPages || customerOrdersLoading"
                      class="p-1 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
                      title="Trang cuối"
                    >
                      <span class="material-symbols-outlined text-base">last_page</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- TAB 3: LỊCH SỬ ĐIỂM & VOUCHER -->
            <div v-else-if="detailTab === 'points_vouchers'" class="space-y-6">
              <div v-if="customerHistoryLoading" class="py-12 text-center">
                <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
                <p class="text-xs text-on-surface-variant mt-2">Đang tải lịch sử điểm và voucher...</p>
              </div>
              <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <!-- Points History -->
                <div class="space-y-3">
                  <h5 class="text-xs font-bold uppercase tracking-wider text-on-surface-variant flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary text-base">stars</span>
                    Biến động điểm thưởng
                  </h5>
                  <div v-if="customerPoints.length === 0" class="p-8 text-center border border-dashed border-outline-variant/10 rounded-2xl text-on-surface-variant text-xs">
                    Chưa có giao dịch biến động điểm nào.
                  </div>
                  <div v-else class="border border-outline-variant/10 rounded-2xl overflow-hidden bg-surface-container-low max-h-96 overflow-y-auto">
                    <table class="w-full text-left border-collapse text-xs">
                      <thead>
                        <tr class="bg-surface-container-highest/60 border-b border-outline-variant/10 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                          <th class="p-3 pl-4">Thời gian</th>
                          <th class="p-3 text-right">Biến động</th>
                          <th class="p-3 text-right">Số dư</th>
                          <th class="p-3 pr-4">Nguồn &amp; Ghi chú</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-outline-variant/5">
                        <tr v-for="p in customerPoints" :key="p.id">
                          <td class="p-3 pl-4 font-mono text-[11px] text-on-surface-variant">
                            {{ formatDateTime(p.createdAt) }}
                          </td>
                          <td class="p-3 text-right font-mono font-bold">
                            <span :class="p.points > 0 ? 'text-emerald-400' : 'text-rose-400'">
                              {{ p.points > 0 ? `+${p.points}` : p.points }}
                            </span>
                          </td>
                          <td class="p-3 text-right font-mono font-medium text-on-surface">
                            {{ (p.balanceAfter || 0).toLocaleString('vi-VN') }}
                          </td>
                          <td class="p-3 pr-4 text-[11px] text-on-surface-variant">
                            <p class="font-medium text-on-surface">{{ p.source || p.type }}</p>
                            <p v-if="p.refCode" class="font-mono text-[10px] text-primary">{{ p.refCode }}</p>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <!-- Vouchers List -->
                <div class="space-y-3">
                  <h5 class="text-xs font-bold uppercase tracking-wider text-on-surface-variant flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary text-base">confirmation_number</span>
                    Danh sách Voucher sở hữu
                  </h5>
                  <div v-if="customerVouchers.length === 0" class="p-8 text-center border border-dashed border-outline-variant/10 rounded-2xl text-on-surface-variant text-xs">
                    Khách hàng hiện không sở hữu voucher nào.
                  </div>
                  <div v-else class="space-y-3 max-h-96 overflow-y-auto pr-1">
                    <div
                      v-for="v in customerVouchers"
                      :key="v.id"
                      class="p-4 rounded-2xl border border-outline-variant/10 bg-surface-container-low flex justify-between items-center gap-4"
                    >
                      <div class="space-y-1">
                        <div class="flex items-center gap-2">
                          <span class="font-mono font-bold text-xs text-primary px-2 py-0.5 bg-primary/10 rounded border border-primary/20">
                            {{ v.code }}
                          </span>
                          <span
                            class="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full border"
                            :class="{
                              'bg-emerald-500/15 text-emerald-400 border-emerald-500/30': v.status === 'ACTIVE',
                              'bg-slate-500/15 text-slate-400 border-slate-500/30': v.status === 'USED',
                              'bg-rose-500/15 text-rose-400 border-rose-500/30': v.status === 'EXPIRED'
                            }"
                          >
                            {{ v.status === 'ACTIVE' ? 'Khả dụng' : (v.status === 'USED' ? 'Đã sử dụng' : 'Hết hạn') }}
                          </span>
                        </div>
                        <p class="text-xs font-bold text-on-surface">{{ v.title }}</p>
                        <p class="text-[10px] text-on-surface-variant font-mono">Hạn dùng: {{ formatDateTime(v.validUntil) }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========================================================================= -->
    <!-- MODAL B: CHỈNH SỬA THÔNG TIN KHÁCH HÀNG (CUSTOMER EDIT MODAL)             -->
    <!-- ========================================================================= -->
    <Teleport to="body">
      <div v-if="showEditModal" class="fixed inset-0 z-[999] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md" @click="showEditModal = false"></div>
        
        <div class="relative w-full max-w-xl bg-surface border border-outline-variant/20 shadow-2xl rounded-3xl overflow-hidden animate-in fade-in zoom-in duration-200">
          <div class="p-6 bg-surface-container-high border-b border-outline-variant/10 flex justify-between items-center">
            <div class="flex items-center gap-2.5">
              <span class="material-symbols-outlined text-primary text-2xl">edit_note</span>
              <h3 class="text-base font-black uppercase tracking-wider text-on-surface">Chỉnh sửa thông tin</h3>
            </div>
            <button @click="showEditModal = false" class="p-1 rounded-lg hover:bg-white/10 text-on-surface-variant hover:text-white">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <form @submit.prevent="saveEditForm" class="p-6 space-y-5">
            <!-- Editable Field: Họ và tên -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-on-surface-variant mb-1.5">
                Họ và tên <span class="text-red-400">*</span>
              </label>
              <input
                v-model="editForm.fullName"
                type="text"
                class="w-full bg-surface-container-highest border border-outline-variant/10 rounded-xl px-4 py-2.5 text-xs text-on-surface focus:ring-1 focus:ring-primary outline-none"
                placeholder="Nhập họ và tên khách hàng"
              />
              <p v-if="editErrors.fullName" class="text-red-400 text-[11px] mt-1">{{ editErrors.fullName }}</p>
            </div>

            <!-- Editable Field: Ngày sinh -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-on-surface-variant mb-1.5">
                Ngày sinh
              </label>
              <input
                v-model="editForm.dob"
                type="date"
                class="w-full bg-surface-container-highest border border-outline-variant/10 rounded-xl px-4 py-2.5 text-xs text-on-surface focus:ring-1 focus:ring-primary outline-none"
              />
            </div>

            <!-- Read-only Security Box -->
            <div class="p-4 bg-surface-container-low rounded-2xl border border-outline-variant/10 space-y-2.5 text-xs">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/80 block">Thông tin bảo mật (Chỉ đọc)</span>
              <div class="grid grid-cols-2 gap-3 text-[11px]">
                <div>
                  <span class="text-on-surface-variant block">Mã ID</span>
                  <span class="font-mono font-bold text-on-surface">#DC-{{ editForm.userId }}</span>
                </div>
                <div>
                  <span class="text-on-surface-variant block">Email</span>
                  <span class="font-medium text-on-surface truncate block">{{ editForm.email || '—' }}</span>
                </div>
                <div>
                  <span class="text-on-surface-variant block">Số điện thoại</span>
                  <span class="font-mono font-medium text-on-surface">{{ editForm.phone || 'Chưa cập nhật' }}</span>
                </div>
                <div>
                  <span class="text-on-surface-variant block">Hạng &amp; Điểm</span>
                  <span class="font-bold text-primary">{{ editForm.membershipTier }} · {{ editForm.loyaltyPoints }} pts</span>
                </div>
              </div>
            </div>

            <!-- Security Action: Send Reset Password Email -->
            <div class="pt-2">
              <button
                type="button"
                @click="sendResetPasswordFromModal"
                :disabled="editSendingReset || editForm.isGuest || !editForm.isActive"
                class="w-full py-2.5 bg-surface-container-high hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed text-on-surface text-xs font-bold uppercase tracking-wider rounded-xl transition-colors border border-outline-variant/20 flex items-center justify-center gap-2"
              >
                <span class="material-symbols-outlined text-base text-primary">mail_lock</span>
                <span>{{ editSendingReset ? 'Đang gửi yêu cầu...' : 'Gửi mã đặt lại mật khẩu về email' }}</span>
              </button>
            </div>

            <!-- Form Actions -->
            <div class="flex justify-end gap-3 pt-4 border-t border-outline-variant/10">
              <button
                type="button"
                @click="showEditModal = false"
                class="px-5 py-2.5 rounded-xl border border-outline-variant/20 text-xs font-bold uppercase tracking-wider text-on-surface-variant hover:text-white transition-colors"
              >
                Hủy
              </button>
              <button
                type="submit"
                :disabled="editSaving"
                class="px-6 py-2.5 rounded-xl bg-primary hover:bg-primary/90 text-on-primary text-xs font-bold uppercase tracking-wider transition-colors flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-base">save</span>
                <span>{{ editSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- ========================================================================= -->
    <!-- MODAL C: KHÓA / MỞ KHÓA TÀI KHOẢN (LOCK / UNLOCK MODAL)                   -->
    <!-- ========================================================================= -->
    <Teleport to="body">
      <div v-if="showLockModal && lockTarget" class="fixed inset-0 z-[999] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-md" @click="showLockModal = false"></div>
        
        <div class="relative w-full max-w-lg bg-surface border border-outline-variant/20 shadow-2xl rounded-3xl overflow-hidden animate-in fade-in zoom-in duration-200">
          <div class="p-6 bg-surface-container-high border-b border-outline-variant/10 flex justify-between items-center">
            <div class="flex items-center gap-2.5">
              <span class="material-symbols-outlined text-2xl" :class="lockTarget.isActive ? 'text-rose-400' : 'text-emerald-400'">
                {{ lockTarget.isActive ? 'lock' : 'lock_open' }}
              </span>
              <h3 class="text-base font-black uppercase tracking-wider text-on-surface">
                {{ lockTarget.isActive ? 'Khóa tài khoản khách hàng' : 'Mở khóa tài khoản khách hàng' }}
              </h3>
            </div>
            <button @click="showLockModal = false" class="p-1 rounded-lg hover:bg-white/10 text-on-surface-variant hover:text-white">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="p-6 space-y-5">
            <!-- Case 1: Khóa tài khoản -->
            <div v-if="lockTarget.isActive" class="space-y-4">
              <div class="p-4 bg-rose-500/10 border border-rose-500/30 rounded-2xl text-rose-300 text-xs flex items-start gap-3">
                <span class="material-symbols-outlined text-rose-400 text-xl shrink-0 mt-0.5">warning</span>
                <div>
                  <p class="font-bold text-rose-300">Cảnh báo an ninh</p>
                  <p class="mt-1 text-rose-200/80 leading-relaxed">
                    Tài khoản bị khóa sẽ không thể đăng nhập trên Web/App và không thể sử dụng điểm tích lũy tại quầy.
                  </p>
                </div>
              </div>

              <!-- Dropdown: Lý do khóa -->
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-on-surface-variant mb-1.5">
                  Lý do khóa tài khoản <span class="text-red-400">*</span>
                </label>
                
                <div class="relative">
                  <select
                    v-model="lockReasonOption"
                    class="w-full bg-surface-container-highest border border-outline-variant/10 hover:border-outline-variant/30 rounded-xl pl-4 pr-10 py-2.5 text-xs text-on-surface outline-none cursor-pointer transition-all appearance-none font-medium focus:border-primary/60 focus:ring-2 focus:ring-primary/15"
                  >
                    <option
                      v-for="r in LOCK_REASONS"
                      :key="r"
                      :value="r"
                      class="bg-surface-container-high text-on-surface py-2"
                    >
                      {{ r }}
                    </option>
                  </select>
                  <span class="material-symbols-outlined absolute right-3.5 top-1/2 -translate-y-1/2 text-base text-on-surface-variant pointer-events-none">
                    expand_more
                  </span>
                </div>
              </div>

              <div v-if="lockReasonOption.startsWith('Khác')">
                <label class="block text-xs font-bold uppercase tracking-wider text-on-surface-variant mb-1.5">
                  Chi tiết lý do khóa <span class="text-red-400">*</span>
                </label>
                <textarea
                  v-model="lockReasonCustom"
                  rows="3"
                  placeholder="Ghi rõ lý do tạm khóa để đối soát..."
                  class="w-full bg-surface-container-highest border border-outline-variant/10 rounded-xl p-3 text-xs text-on-surface focus:ring-1 focus:ring-primary outline-none"
                ></textarea>
              </div>
            </div>

            <!-- Case 2: Mở khóa tài khoản -->
            <div v-else class="space-y-3">
              <div class="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-2xl text-emerald-300 text-xs flex items-start gap-3">
                <span class="material-symbols-outlined text-emerald-400 text-xl shrink-0 mt-0.5">check_circle</span>
                <div>
                  <p class="font-bold text-emerald-300">Xác nhận mở lại quyền truy cập</p>
                  <p class="mt-1 text-emerald-200/80 leading-relaxed">
                    Mở lại quyền đăng nhập và sử dụng điểm tích lũy cho tài khoản <strong>{{ lockTarget.fullName }}</strong> (#DC-{{ lockTarget.userId }}).
                  </p>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex justify-end gap-3 pt-4 border-t border-outline-variant/10">
              <button
                type="button"
                @click="showLockModal = false"
                class="px-5 py-2.5 rounded-xl border border-outline-variant/20 text-xs font-bold uppercase tracking-wider text-on-surface-variant hover:text-white transition-colors"
              >
                Hủy
              </button>
              <button
                type="button"
                @click="submitToggleLock"
                :disabled="lockSaving"
                :class="lockTarget.isActive ? 'bg-rose-600 hover:bg-rose-500 text-white' : 'bg-emerald-600 hover:bg-emerald-500 text-white'"
                class="px-6 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider transition-colors flex items-center gap-2 shadow-lg"
              >
                <span class="material-symbols-outlined text-base">{{ lockTarget.isActive ? 'lock' : 'lock_open' }}</span>
                <span>{{ lockSaving ? 'Đang xử lý...' : (lockTarget.isActive ? 'Xác nhận khóa tài khoản' : 'Xác nhận mở khóa') }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
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
