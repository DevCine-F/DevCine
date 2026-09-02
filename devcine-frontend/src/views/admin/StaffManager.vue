<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { staffApi, cinemaListApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { useAuthStore } from '@/stores/auth'
import { friendlyError } from '@/utils/friendlyError'
import AppModal from '@/components/common/AppModal.vue'

const toast = useToastStore()
const confirm = useConfirmStore()
const auth = useAuthStore()
const can = (feature, action = 'view') => auth.hasPermission(feature, action)

const staff = ref([])
const cinemas = ref([])
const isLoading = ref(false)
const loadError = ref(false)

// ===== Bộ lọc (client-side, dữ liệu nhỏ nên lọc tức thời, không gọi lại server) =====
const search = ref('')
const cinemaFilter = ref('') // '' = tất cả cơ sở
const statusFilter = ref('ALL') // ALL | ACTIVE | INACTIVE

// ===== Modal thêm/sửa =====
const isModalOpen = ref(false)
const isSaving = ref(false)
const editingId = ref(null)
const blankForm = () => ({
  fullName: '', username: '', email: '', phone: '',
  staffCode: '', cinemaId: '', isActive: true, role: 'STAFF',
})
const form = ref(blankForm())

// Đánh dấu ô đã tương tác (blur) + đã bấm lưu — để chỉ hiện lỗi khi cần
const touched = ref({})
const attempted = ref(false)
const touch = (field) => { touched.value[field] = true }

// ===== Validate realtime (mirror quy tắc backend) =====
const RE_USERNAME = /^[a-z0-9_]{3,15}$/
const RE_STAFFCODE = /^[A-Z0-9]{3,15}$/
const RE_EMAIL = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
const RE_PHONE = /^\d{10}$/
const RE_NAME = /^[\p{L}\p{M} ]+$/u

const toTitleCase = (str) => {
  if (!str) return ''
  return str.trim().split(/\s+/).map(word => {
    if (!word) return ''
    return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  }).join(' ')
}

const onInputFullName = (e) => {
  let val = e.target.value
  val = val.replace(/[^\p{L}\p{M}\s]/gu, '')
  if (val.length > 30) val = val.slice(0, 30)
  form.value.fullName = val
}

const onBlurFullName = () => {
  touch('fullName')
  if (form.value.fullName) {
    form.value.fullName = toTitleCase(form.value.fullName)
  }
}

const onInputUsername = (e) => {
  let val = e.target.value
  val = val.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  val = val.toLowerCase().replace(/[^a-z0-9_]/g, '')
  if (val.length > 15) val = val.slice(0, 15)
  form.value.username = val
}

const onInputPhone = (e) => {
  let val = e.target.value
  val = val.replace(/\D/g, '')
  if (val.length > 10) val = val.slice(0, 10)
  form.value.phone = val
}

const onInputEmail = (e) => {
  let val = e.target.value.replace(/\s+/g, '').toLowerCase()
  if (val.length > 100) val = val.slice(0, 100)
  form.value.email = val
}

const errFullName = computed(() => {
  const s = form.value.fullName.trim()
  if (!s) return 'Vui lòng nhập họ và tên.'
  if (s.length < 2 || s.length > 30) return 'Họ tên phải từ 2 đến 30 ký tự.'
  if (!RE_NAME.test(s)) return 'Họ tên chỉ gồm chữ cái và khoảng trắng.'
  return ''
})
const errUsername = computed(() => {
  if (editingId.value) return '' // username không sửa khi edit
  const s = form.value.username.trim()
  if (!s) return 'Vui lòng nhập tài khoản đăng nhập.'
  if (!RE_USERNAME.test(s)) return '3–15 ký tự; chỉ chữ thường, số, gạch dưới, không dấu, không khoảng trắng.'
  return ''
})
const errStaffCode = computed(() => {
  return ''
})
const errEmail = computed(() => {
  const s = form.value.email.trim()
  if (!s) return 'Vui lòng nhập email.'
  if (!RE_EMAIL.test(s)) return 'Email không đúng định dạng, vui lòng kiểm tra lại.'
  return ''
})
const errPhone = computed(() => {
  const s = form.value.phone.trim()
  if (!s) return 'Vui lòng nhập số điện thoại.'
  if (!RE_PHONE.test(s)) return 'Số điện thoại không hợp lệ (yêu cầu đúng 10 chữ số).'
  return ''
})
const errCinema = computed(() => form.value.cinemaId ? '' : 'Vui lòng chọn cơ sở làm việc.')

const showErr = (field, err) => ((touched.value[field] || attempted.value) && err) ? err : ''
const formValid = computed(() =>
  !errFullName.value && !errUsername.value && !errStaffCode.value &&
  !errEmail.value && !errPhone.value && !errCinema.value
)

// Kết quả tạo tài khoản (username + mật khẩu mặc định) để admin báo cho nhân viên
const credsResult = ref(null)

const roleLabel = (role) => {
  switch ((role || '').toUpperCase()) {
    case 'ADMIN': return 'Quản trị viên'
    case 'MANAGER': return 'Quản lý cơ sở'
    case 'STAFF': return 'Nhân viên'
    default: return role || 'Nhân viên'
  }
}

const formatDate = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d)) return '—'
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const cinemaNameById = (id) => cinemas.value.find(c => c.id === id)?.name || null

const isSelf = (person) => Number(person.userId) === Number(auth.user?.id)
const isEditingSelf = computed(() => editingId.value && Number(editingId.value) === Number(auth.user?.id))

const getRoleLevel = (role) => {
  switch ((role || '').toUpperCase()) {
    case 'ADMIN': return 3
    case 'MANAGER': return 2
    case 'STAFF': return 1
    default: return 0
  }
}

const canEditPerson = (person) => {
  if (!can('staff_management', 'edit')) return false
  if (isSelf(person)) return true // Cho phép tự sửa thông tin cá nhân

  const callerRole = auth.user?.role || auth.role || ''
  const callerLevel = getRoleLevel(callerRole)
  const targetLevel = getRoleLevel(person.role)

  // Vai trò cùng cấp hoặc cấp cao hơn -> không được sửa thông tin của nhau
  if (targetLevel >= callerLevel) return false

  // Quản lý chỉ được sửa nhân viên thuộc cơ sở của mình
  if (!auth.isAdmin) {
    if (!auth.user?.cinemaId || person.cinemaId !== auth.user?.cinemaId) return false
  }

  return true
}

const editPersonTitle = (person) => {
  if (isSelf(person)) return 'Sửa thông tin của bạn'

  const callerRole = auth.user?.role || auth.role || ''
  const callerLevel = getRoleLevel(callerRole)
  const targetLevel = getRoleLevel(person.role)

  if (targetLevel > callerLevel) return 'Không thể sửa tài khoản cấp cao hơn'
  if (targetLevel === callerLevel) return `Không thể sửa tài khoản của ${roleLabel(person.role)} cùng cấp`
  if (!auth.isAdmin && person.cinemaId !== auth.user?.cinemaId) return 'Không thể sửa nhân viên khác cơ sở'
  return 'Sửa'
}

const canTogglePerson = (person) => {
  if (!can('staff_management', 'edit')) return false
  if (isSelf(person)) return false // Không được chuyển trạng thái của bản thân

  const callerRole = auth.user?.role || auth.role || ''
  const callerLevel = getRoleLevel(callerRole)
  const targetLevel = getRoleLevel(person.role)

  // Vai trò cùng cấp hoặc cấp cao hơn -> không được đổi trạng thái
  if (targetLevel >= callerLevel) return false

  // Quản lý chỉ được đổi trạng thái nhân viên thuộc cơ sở của mình
  if (!auth.isAdmin) {
    if (!auth.user?.cinemaId || person.cinemaId !== auth.user?.cinemaId) return false
  }

  return true
}

const togglePersonTitle = (person) => {
  if (isSelf(person)) return 'Không thể tự đổi trạng thái của chính mình'

  const callerRole = auth.user?.role || auth.role || ''
  const callerLevel = getRoleLevel(callerRole)
  const targetLevel = getRoleLevel(person.role)

  if (targetLevel > callerLevel) return 'Không thể đổi trạng thái tài khoản cấp cao hơn'
  if (targetLevel === callerLevel) return `Không thể đổi trạng thái của ${roleLabel(person.role)} cùng cấp`
  if (!auth.isAdmin && person.cinemaId !== auth.user?.cinemaId) return 'Không thể đổi trạng thái nhân viên khác cơ sở'
  return person.isActive ? 'Tạm ngưng' : 'Kích hoạt'
}

// ===== Tải dữ liệu =====
const fetchStaff = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await staffApi.list()
    staff.value = Array.isArray(data) ? data : (data.data ?? [])
  } catch (e) {
    loadError.value = true
    toast.error(friendlyError(e, 'Không tải được danh sách nhân viên.'))
  } finally {
    isLoading.value = false
  }
}

const fetchCinemas = async () => {
  try {
    const { data } = await cinemaListApi.getAll()
    cinemas.value = (Array.isArray(data) ? data : (data.data ?? [])).map(c => ({ id: c.id, name: c.name }))
  } catch (e) {
    toast.error(friendlyError(e, 'Không tải được danh sách cơ sở.'))
  }
}

// ===== Danh sách sau lọc =====
const filteredStaff = computed(() => {
  const kw = search.value.trim().toLowerCase()
  return staff.value.filter(s => {
    if (cinemaFilter.value && s.cinemaId !== Number(cinemaFilter.value)) return false
    if (statusFilter.value === 'ACTIVE' && !s.isActive) return false
    if (statusFilter.value === 'INACTIVE' && s.isActive) return false
    if (kw) {
      const hay = `${s.fullName || ''} ${s.email || ''} ${s.staffCode || ''} ${s.phone || ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
})

// ===== Phân trang (client-side) =====
const pageSize = ref(10)
const currentPage = ref(1)
const pageSizeDropdownOpen = ref(false)
const PAGE_SIZE_OPTIONS = [10, 20, 50]

const totalPages = computed(() => {
  return Math.ceil(filteredStaff.value.length / pageSize.value) || 1
})

const paginatedStaff = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredStaff.value.slice(start, start + pageSize.value)
})

watch([search, cinemaFilter, statusFilter], () => {
  currentPage.value = 1
})

watch(totalPages, (newTotal) => {
  if (currentPage.value > newTotal) {
    currentPage.value = newTotal
  }
})

const changePageSize = (size) => {
  pageSize.value = size
  currentPage.value = 1
  pageSizeDropdownOpen.value = false
}

const goToPage = (p) => {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return
  currentPage.value = p
}

// Thống kê số nhân viên theo cơ sở (hiển thị nhanh)
const countByCinema = computed(() => {
  const map = {}
  for (const s of staff.value) {
    const key = s.cinemaId ?? 'none'
    map[key] = (map[key] || 0) + 1
  }
  return map
})

// ===== Mở modal =====
const resetValidationState = () => { touched.value = {}; attempted.value = false }

const fetchNextCode = async () => {
  if (editingId.value) return
  try {
    const res = await staffApi.getNextCode()
    const code = typeof res.data === 'string' ? res.data : (res.data?.data || res.data)
    if (code) {
      form.value.staffCode = code
    }
  } catch (error) {
    console.error('Lỗi khi lấy mã nhân viên:', error)
  }
}

const originalPerson = ref(null)

const openAddModal = async () => {
  editingId.value = null
  originalPerson.value = null
  form.value = blankForm()
  resetValidationState()
  isModalOpen.value = true
  await fetchNextCode()
}

const openEditModal = (person) => {
  editingId.value = person.userId
  originalPerson.value = { ...person }
  form.value = {
    fullName: person.fullName || '',
    username: person.username || '',
    email: person.email || '',
    phone: person.phone || '',
    staffCode: person.staffCode || '',
    cinemaId: person.cinemaId ?? '',
    isActive: !!person.isActive,
    role: (person.role || 'STAFF').toUpperCase(),
  }
  resetValidationState()
  isModalOpen.value = true
}

const hasUnsavedChanges = computed(() => {
  if (!isModalOpen.value) return false
  if (!editingId.value) {
    return !!(form.value.fullName || form.value.username || form.value.email || form.value.phone || form.value.cinemaId)
  }
  if (!originalPerson.value) return false
  const p = originalPerson.value
  return form.value.fullName.trim() !== (p.fullName || '').trim() ||
         form.value.email.trim() !== (p.email || '').trim() ||
         form.value.phone.trim() !== (p.phone || '').trim() ||
         Number(form.value.cinemaId || 0) !== Number(p.cinemaId || 0) ||
         form.value.role !== (p.role || 'STAFF').toUpperCase() ||
         form.value.isActive !== !!p.isActive
})

const closeModal = async (force = false) => {
  if (!force && hasUnsavedChanges.value) {
    const ok = await confirm.show({
      title: 'Đóng biểu mẫu',
      message: 'Bạn có những thay đổi chưa được lưu. Bạn có chắc chắn muốn thoát không?',
      confirmText: 'Đóng biểu mẫu',
      cancelText: 'Tiếp tục chỉnh sửa',
      tone: 'neutral',
    })
    if (!ok) return
  }
  isModalOpen.value = false
  originalPerson.value = null
}

const checkSensitiveChanges = async () => {
  if (!editingId.value || !originalPerson.value) return true
  const p = originalPerson.value
  const targetName = p.fullName || form.value.fullName.trim() || 'nhân viên'

  // 1. Chuyển trạng thái sang TẠM NGƯNG
  const isDeactivating = p.isActive && !form.value.isActive
  if (isDeactivating) {
    const ok = await confirm.show({
      title: 'Xác nhận tạm ngưng nhân viên',
      message: `Bạn có chắc chắn muốn tạm ngưng hoạt động của "${targetName}"? Nhân viên sẽ bị ngắt quyền đăng nhập vào hệ thống quầy vé POS và các trang nội bộ.`,
      confirmText: 'Tạm ngưng',
      cancelText: 'Giữ nguyên',
      tone: 'danger',
    })
    if (!ok) return false
  }

  // 2. Thay đổi Vai trò (STAFF <-> MANAGER)
  const oldRole = (p.role || 'STAFF').toUpperCase()
  const newRole = form.value.role.toUpperCase()
  if (oldRole !== newRole) {
    const isDemotion = oldRole === 'MANAGER' && newRole === 'STAFF'
    const ok = await confirm.show({
      title: isDemotion ? 'Xác nhận hạ vai trò' : 'Xác nhận nâng vai trò',
      message: isDemotion
        ? `Hạ vai trò của "${targetName}" từ Quản lý cơ sở xuống Nhân viên? Tài khoản sẽ bị thu hồi quyền xem báo cáo doanh thu và duyệt nghiệp vụ cụm rạp.`
        : `Nâng vai trò của "${targetName}" lên Quản lý cơ sở? Tài khoản sẽ được cấp quyền quản trị nhân sự và duyệt các thao tác tại cụm rạp.`,
      confirmText: isDemotion ? 'Hạ vai trò' : 'Nâng vai trò',
      cancelText: 'Giữ nguyên',
      tone: isDemotion ? 'danger' : 'warning',
    })
    if (!ok) return false
  }

  // 3. Chuyển Cơ sở làm việc (Đổi cụm rạp)
  const oldCinemaId = p.cinemaId ? Number(p.cinemaId) : null
  const newCinemaId = form.value.cinemaId ? Number(form.value.cinemaId) : null
  if (oldCinemaId && newCinemaId && oldCinemaId !== newCinemaId) {
    const oldCinemaName = cinemaNameById(oldCinemaId) || 'Cơ sở cũ'
    const newCinemaName = cinemaNameById(newCinemaId) || 'Cơ sở mới'
    const ok = await confirm.show({
      title: 'Xác nhận chuyển cơ sở làm việc',
      message: `Chuyển nhân viên "${targetName}" từ [${oldCinemaName}] sang [${newCinemaName}]? Quyền mở ca POS và dữ liệu phân công sẽ được chuyển sang cụm rạp mới.`,
      confirmText: 'Chuyển cơ sở',
      cancelText: 'Giữ nguyên',
      tone: 'warning',
    })
    if (!ok) return false
  }

  return true
}

const buildPayload = () => {
  const base = {
    fullName: form.value.fullName.trim(),
    email: form.value.email.trim(),
    phone: form.value.phone.trim() || null,
    staffCode: form.value.staffCode.trim().toUpperCase() || null,
  }
  if (isEditingSelf.value) {
    // Sửa thông tin bản thân: không thay đổi trạng thái, vai trò, cơ sở
    return base
  }
  base.cinemaId = form.value.cinemaId || null
  if (auth.isAdmin) base.role = form.value.role
  if (editingId.value) {
    return { ...base, isActive: form.value.isActive }
  }
  // Tạo mới: KHÔNG gửi mật khẩu — backend dùng mật khẩu mặc định + buộc đổi lần đầu
  return { ...base, username: form.value.username.trim() }
}

const validate = () => {
  attempted.value = true
  touched.value = {
    fullName: true,
    username: true,
    email: true,
    phone: true,
    cinemaId: true,
  }
  if (!formValid.value) { toast.warning('Vui lòng kiểm tra lại các trường còn thiếu hoặc chưa hợp lệ.'); return false }
  return true
}

const saveStaff = async () => {
  if (!validate()) return

  if (editingId.value) {
    const confirmed = await checkSensitiveChanges()
    if (!confirmed) return
  }

  isSaving.value = true
  try {
    if (editingId.value) {
      await staffApi.update(editingId.value, buildPayload())
      toast.success(isEditingSelf.value ? 'Cập nhật thông tin cá nhân thành công.' : 'Cập nhật nhân viên thành công.')
      if (isEditingSelf.value && auth.user) {
        auth.user.fullName = form.value.fullName.trim()
        auth.user.email = form.value.email.trim()
        auth.user.phone = form.value.phone.trim()
        localStorage.setItem('user', JSON.stringify(auth.user))
      }
      await fetchStaff()
      closeModal(true)
    } else {
      const res = await staffApi.create(buildPayload())
      const d = res?.data ?? {}
      toast.success('Thêm nhân viên thành công.')
      await fetchStaff()
      closeModal(true)
      // Hiện thông tin đăng nhập cho admin (email best-effort đã gửi song song)
      credsResult.value = {
        username: d.username || form.value.username.trim(),
        password: d.defaultPassword || 'DevCine@2026',
        fullName: d.fullName || form.value.fullName.trim(),
        staffCode: d.staffCode || form.value.staffCode || 'DC---',
        cinemaName: d.cinemaName || cinemaNameById(Number(form.value.cinemaId)) || 'Cơ sở đã chọn',
        role: d.role || form.value.role,
        email: d.email || form.value.email.trim(),
        emailSent: d.emailSent,
      }
    }
  } catch (e) {
    toast.error(friendlyError(e, 'Lưu nhân viên thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const copiedField = ref(null)
const copyText = async (text, fieldName = 'all') => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    copiedField.value = fieldName
    toast.success(fieldName === 'all' ? 'Đã sao chép toàn bộ thông tin tài khoản.' : `Đã sao chép ${fieldName}.`)
    setTimeout(() => {
      if (copiedField.value === fieldName) copiedField.value = null
    }, 2000)
  } catch {
    toast.warning('Không sao chép được, vui lòng ghi lại thủ công.')
  }
}

const copyCreds = () => {
  if (!credsResult.value) return
  const r = credsResult.value
  const text = [
    `HỌ VÀ TÊN: ${r.fullName}`,
    `MÃ NHÂN VIÊN: ${r.staffCode}`,
    `VAI TRÒ: ${roleLabel(r.role)}`,
    `CƠ SỞ LÀM VIỆC: ${r.cinemaName}`,
    `TÀI KHOẢN ĐĂNG NHẬP: ${r.username}`,
    `MẬT KHẨU TẠM: ${r.password}`,
    `EMAIL: ${r.email}`,
  ].join('\n')
  copyText(text, 'all')
}

const toggleActive = async (person) => {
  if (person.isActive) {
    const ok = await confirm.show({
      title: 'Tạm ngưng nhân viên',
      message: `Tạm ngưng làm việc của "${person.fullName}"? Nhân viên sẽ không đăng nhập được vào hệ thống.`,
      confirmText: 'Tạm ngưng',
      cancelText: 'Giữ nguyên',
      tone: 'danger',
    })
    if (!ok) return
  }
  try {
    const { data } = await staffApi.toggle(person.userId)
    person.isActive = data.isActive
    toast.success(person.isActive ? 'Đã kích hoạt lại nhân viên.' : 'Đã tạm ngưng nhân viên.')
  } catch (e) {
    toast.error(friendlyError(e, 'Không đổi được trạng thái nhân viên.'))
  }
}

const resetFilters = () => { search.value = ''; cinemaFilter.value = ''; statusFilter.value = 'ALL'; currentPage.value = 1 }

onMounted(() => { fetchStaff(); fetchCinemas() })
</script>

<template>
  <div class="h-full flex flex-col space-y-6 p-10 overflow-hidden">
    <!-- Header -->
    <div class="flex justify-between items-end flex-shrink-0">
      <div>
        <h1 class="text-3xl font-black text-on-surface tracking-tighter uppercase italic">
          Quản lý <span class="text-primary">Nhân viên</span>
        </h1>
        <p class="text-sm font-bold text-on-surface-variant uppercase tracking-widest mt-1">
          Quản lý đội ngũ vận hành trên toàn bộ cơ sở &amp; phân quyền hệ thống · {{ filteredStaff.length }} nhân viên
        </p>
      </div>

      <button
        v-if="can('staff_management', 'add')"
        @click="openAddModal"
        class="px-6 py-3 bg-primary hover:brightness-110 text-on-primary font-bold text-xs uppercase tracking-widest rounded-xl transition-all flex items-center gap-2 shadow-sm"
      >
        <span class="material-symbols-outlined text-sm">person_add</span>
        Thêm nhân viên
      </button>
    </div>

    <!-- Toolbar lọc cao cấp với Bo góc chuẩn Admin Customers -->
    <div class="bg-surface-container-low p-3 rounded-2xl border border-outline-variant/10 flex flex-wrap items-center gap-3 shadow-xl flex-shrink-0">
      <!-- Search Input -->
      <div class="relative flex-grow min-w-[240px] group">
        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-colors">search</span>
        <input
          v-model="search"
          type="text"
          placeholder="Tìm theo tên, email, mã NV, SĐT..."
          class="w-full h-11 bg-surface-container-highest border border-outline-variant/10 rounded-xl pl-12 pr-4 text-sm text-on-surface placeholder:text-on-surface-variant/50 outline-none hover:border-outline-variant/30 focus:border-primary/60 focus:ring-2 focus:ring-primary/15 transition-all"
        />
      </div>

      <!-- Select Cơ sở -->
      <div class="relative min-w-[200px]">
        <select
          v-model="cinemaFilter"
          class="w-full h-11 bg-surface-container-highest border border-outline-variant/10 rounded-xl px-4 text-xs font-semibold text-on-surface outline-none cursor-pointer hover:border-outline-variant/30 focus:border-primary/60 focus:ring-2 focus:ring-primary/15 transition-all shadow-sm"
        >
          <option value="">Tất cả cơ sở</option>
          <option v-for="c in cinemas" :key="c.id" :value="c.id">{{ c.name }} ({{ countByCinema[c.id] || 0 }})</option>
        </select>
      </div>

      <!-- Select Trạng thái -->
      <div class="relative min-w-[170px]">
        <select
          v-model="statusFilter"
          class="w-full h-11 bg-surface-container-highest border border-outline-variant/10 rounded-xl px-4 text-xs font-semibold text-on-surface outline-none cursor-pointer hover:border-outline-variant/30 focus:border-primary/60 focus:ring-2 focus:ring-primary/15 transition-all shadow-sm"
        >
          <option value="ALL">Mọi trạng thái</option>
          <option value="ACTIVE">Đang làm việc</option>
          <option value="INACTIVE">Đã tạm ngưng</option>
        </select>
      </div>

      <!-- Reset Filter Button -->
      <button
        v-if="search || cinemaFilter || statusFilter !== 'ALL'"
        @click="resetFilters"
        class="text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors px-3 h-11 flex items-center gap-1 rounded-xl"
      >
        <span class="material-symbols-outlined text-sm">filter_alt_off</span> Bỏ lọc
      </button>
    </div>

    <!-- Bảng dữ liệu: Bo góc rounded-2xl, flex-1 min-h-0 giới hạn chiều cao nội dung cuộn bên trong -->
    <section class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl flex flex-col flex-1 min-h-0">
      <div class="overflow-x-auto flex-1 min-h-0 overflow-y-auto">
        <table class="w-full text-left border-collapse min-w-[950px]">
          <thead class="sticky top-0 z-10 bg-surface-container-highest/80 backdrop-blur-md">
            <tr class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant border-b border-outline-variant/10 select-none">
              <th class="px-6 py-3.5">Nhân viên</th>
              <th class="px-6 py-3.5">Mã NV</th>
              <th class="px-6 py-3.5">Cơ sở</th>
              <th class="px-6 py-3.5">Vai trò</th>
              <th class="px-6 py-3.5">Ngày gia nhập</th>
              <th class="px-6 py-3.5">Trạng thái</th>
              <th class="px-6 py-3.5 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10 text-on-surface">
            <!-- Loading skeleton -->
            <template v-if="isLoading">
              <tr v-for="i in 5" :key="`sk-${i}`">
                <td colspan="7" class="px-6 py-3.5">
                  <div class="h-9 bg-surface-container-highest rounded animate-pulse"></div>
                </td>
              </tr>
            </template>

            <!-- Dữ liệu -->
            <template v-else-if="filteredStaff.length">
              <tr v-for="person in paginatedStaff" :key="person.userId" class="group hover:bg-white/5 transition-all">
                <td class="px-6 py-3">
                  <div class="flex items-center gap-3">
                    <img v-if="person.avatarUrl" :src="person.avatarUrl" class="w-9 h-9 rounded-full object-cover" alt="" />
                    <div v-else class="w-9 h-9 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold uppercase text-xs">
                      {{ (person.fullName || '?').charAt(0) }}
                    </div>
                    <div>
                      <p class="font-bold text-sm uppercase tracking-tight group-hover:text-primary transition-colors flex items-center gap-2">
                        {{ person.fullName }}
                        <span v-if="isSelf(person)" class="text-[9px] px-1.5 py-0.5 rounded-sm bg-primary/20 text-primary font-bold tracking-normal">BẠN</span>
                      </p>
                      <p class="text-[10px] text-on-surface-variant mt-0.5">{{ person.email || '—' }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-3">
                  <span class="text-xs font-mono text-on-surface-variant">{{ person.staffCode || '—' }}</span>
                </td>
                <td class="px-6 py-3">
                  <span v-if="person.cinemaName" class="inline-flex items-center gap-1.5 text-xs font-semibold">
                    <span class="material-symbols-outlined text-sm text-primary/70">location_on</span>{{ person.cinemaName }}
                  </span>
                  <span v-else class="text-xs text-on-surface-variant/60 italic">Chưa gán cơ sở</span>
                </td>
                <td class="px-6 py-3">
                  <span class="text-xs font-semibold">{{ roleLabel(person.role) }}</span>
                </td>
                <td class="px-6 py-3">
                  <span class="text-xs text-on-surface-variant">{{ formatDate(person.joinDate) }}</span>
                </td>
                <td class="px-6 py-3">
                  <span :class="person.isActive ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'" class="px-2 py-1 rounded-sm text-[10px] font-bold uppercase tracking-tighter">
                    {{ person.isActive ? 'Đang làm việc' : 'Đã tạm ngưng' }}
                  </span>
                </td>
                <td class="px-6 py-3 text-right">
                  <div class="flex justify-end gap-1 items-center">
                    <!-- Nút Sửa: Chỉ hiện khi có quyền sửa (bản thân hoặc cấp dưới) -->
                    <button v-if="can('staff_management', 'edit') && canEditPerson(person)"
                            @click="openEditModal(person)"
                            :title="editPersonTitle(person)"
                            class="p-2 rounded-lg text-on-surface-variant hover:text-white hover:bg-white/10 transition-colors">
                      <span class="material-symbols-outlined text-base">edit</span>
                    </button>

                    <!-- Nút Toggle: Chỉ hiện khi có quyền đổi trạng thái (chỉ cấp dưới, không hiện cho bản thân hoặc cùng cấp) -->
                    <button v-if="can('staff_management', 'edit') && canTogglePerson(person)"
                            @click="toggleActive(person)"
                            :title="togglePersonTitle(person)"
                            class="p-2 rounded-lg transition-colors"
                            :class="person.isActive ? 'text-on-surface-variant hover:text-rose-400 hover:bg-rose-500/10' : 'text-rose-400 hover:text-emerald-400 hover:bg-emerald-500/10'">
                      <span class="material-symbols-outlined text-base">{{ person.isActive ? 'toggle_on' : 'toggle_off' }}</span>
                    </button>

                    <!-- Dấu gạch ngang khi không có quyền thao tác nào (nhân sự cùng cấp hoặc cấp cao hơn) -->
                    <span v-if="!canEditPerson(person) && !canTogglePerson(person)"
                          class="text-on-surface-variant/40 text-xs px-2 select-none"
                          title="Không có quyền thao tác trên tài khoản này">—</span>
                  </div>
                </td>
              </tr>
            </template>

            <!-- Error -->
            <tr v-else-if="loadError">
              <td colspan="7" class="px-8 py-16 text-center">
                <span class="material-symbols-outlined text-4xl text-red-500/60 mb-2">error</span>
                <p class="text-on-surface-variant font-semibold">Không tải được danh sách nhân viên.</p>
                <button @click="fetchStaff" class="mt-3 text-xs font-bold uppercase tracking-widest text-primary hover:underline">Thử lại</button>
              </td>
            </tr>

            <!-- Empty -->
            <tr v-else>
              <td colspan="7" class="px-8 py-16 text-center">
                <span class="material-symbols-outlined text-4xl text-outline-variant mb-2">group_off</span>
                <p class="text-on-surface-variant font-semibold">
                  {{ staff.length ? 'Không có nhân viên khớp bộ lọc.' : 'Chưa có nhân viên nào.' }}
                </p>
                <button v-if="staff.length" @click="resetFilters" class="mt-3 text-xs font-bold uppercase tracking-widest text-primary hover:underline">Bỏ lọc</button>
                <button v-else-if="can('staff_management', 'add')" @click="openAddModal" class="mt-3 text-xs font-bold uppercase tracking-widest text-primary hover:underline">Thêm nhân viên đầu tiên</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination & Footer với Custom Page Size Dropdown -->
      <div class="p-4 bg-surface-container-highest/30 text-[11px] font-bold uppercase tracking-widest text-on-surface-variant flex flex-col sm:flex-row justify-between items-center gap-4 border-t border-outline-variant/10 flex-shrink-0">
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
                    @click="changePageSize(size)"
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
            Tổng: <strong class="text-primary">{{ filteredStaff.length.toLocaleString('vi-VN') }}</strong> nhân viên
          </span>
        </div>

        <!-- Navigation Buttons -->
        <div class="flex items-center gap-1">
          <button
            @click="goToPage(1)"
            :disabled="currentPage === 1 || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang đầu"
          >
            <span class="material-symbols-outlined text-base">first_page</span>
          </button>
          <button
            @click="goToPage(currentPage - 1)"
            :disabled="currentPage === 1 || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang trước"
          >
            <span class="material-symbols-outlined text-base">chevron_left</span>
          </button>

          <span class="px-3 py-1 bg-surface-container-highest rounded-lg font-mono font-bold text-primary text-xs">
            {{ currentPage }} / {{ totalPages }}
          </span>

          <button
            @click="goToPage(currentPage + 1)"
            :disabled="currentPage === totalPages || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang sau"
          >
            <span class="material-symbols-outlined text-base">chevron_right</span>
          </button>
          <button
            @click="goToPage(totalPages)"
            :disabled="currentPage === totalPages || isLoading"
            class="p-1.5 rounded-lg border border-outline-variant/10 hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed text-on-surface-variant hover:text-white transition-colors"
            title="Trang cuối"
          >
            <span class="material-symbols-outlined text-base">last_page</span>
          </button>
        </div>
      </div>
    </section>

    <!-- Modal thêm/sửa -->
    <AppModal :show="isModalOpen" :title="editingId ? (isEditingSelf ? 'Sửa thông tin cá nhân' : 'Sửa nhân viên') : 'Thêm nhân viên mới'" @close="closeModal">
      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Họ và tên <span class="text-red-500">*</span></label>
            <input :value="form.fullName" @input="onInputFullName" @blur="onBlurFullName" type="text" maxlength="30" placeholder="VD: Trần Quang Huy"
                   class="w-full bg-surface-container-high border text-sm rounded-sm focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                   :class="showErr('fullName', errFullName) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('fullName', errFullName)" class="text-[11px] text-red-400 font-medium">{{ errFullName }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
              Tài khoản đăng nhập <span v-if="!editingId" class="text-red-500">*</span>
            </label>
            <input :value="form.username" @input="onInputUsername" @blur="touch('username')" type="text" :disabled="!!editingId" maxlength="15" placeholder="vd: nv_huy"
                   class="w-full bg-surface-container-high border text-sm rounded-sm focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                   :class="showErr('username', errUsername) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('username', errUsername)" class="text-[11px] text-red-400 font-medium">{{ errUsername }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mã nhân viên</label>
            <input v-model.trim="form.staffCode" type="text" placeholder="Đang tạo mã..." disabled
                   class="w-full bg-surface-container-high border text-sm rounded-sm py-2.5 px-4 text-on-surface uppercase border-transparent disabled:opacity-50 disabled:cursor-not-allowed font-bold" />
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Email <span class="text-red-500">*</span></label>
            <input :value="form.email" @input="onInputEmail" @blur="touch('email')" type="email" maxlength="100" placeholder="email@devcine.com"
                   class="w-full bg-surface-container-high border text-sm rounded-sm focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                   :class="showErr('email', errEmail) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('email', errEmail)" class="text-[11px] text-red-400 font-medium">{{ errEmail }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số điện thoại <span class="text-red-500">*</span></label>
            <input :value="form.phone" @input="onInputPhone" @blur="touch('phone')" type="text" maxlength="10" placeholder="09xxxxxxxx"
                   class="w-full bg-surface-container-high border text-sm rounded-sm focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                   :class="showErr('phone', errPhone) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('phone', errPhone)" class="text-[11px] text-red-400 font-medium">{{ errPhone }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Cơ sở làm việc <span class="text-red-500">*</span></label>
            <select v-model="form.cinemaId" @blur="touch('cinemaId')" :disabled="isEditingSelf"
                    class="w-full bg-surface-container-high border text-sm rounded-sm focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                    :class="showErr('cinemaId', errCinema) ? 'border-red-500' : 'border-transparent'">
              <option value="">— Chưa gán cơ sở —</option>
              <option v-for="c in cinemas" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
            <p v-if="showErr('cinemaId', errCinema)" class="text-[11px] text-red-400 font-medium">{{ errCinema }}</p>
          </div>

          <div v-if="auth.isAdmin && !isEditingSelf" class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Vai trò <span class="text-red-500">*</span></label>
            <select v-model="form.role" class="w-full bg-surface-container-high border border-transparent text-sm rounded-sm focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
              <option value="STAFF">Nhân viên</option>
              <option value="MANAGER">Quản lý cơ sở</option>
            </select>
          </div>

          <div v-if="editingId" class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Trạng thái</label>
            <button v-if="!isEditingSelf" type="button" @click="form.isActive = !form.isActive"
                    class="w-full h-[42px] px-4 rounded-sm bg-surface-container-high border border-outline-variant/10 text-left flex items-center justify-between transition-all hover:border-primary/30">
              <span class="text-sm font-semibold text-on-surface">{{ form.isActive ? 'Đang làm việc' : 'Tạm ngưng' }}</span>
              <span class="relative inline-flex h-5 w-9 rounded-full transition-colors" :class="form.isActive ? 'bg-green-500' : 'bg-outline-variant'">
                <span class="absolute top-0.5 h-4 w-4 rounded-full bg-white transition-transform" :class="form.isActive ? 'translate-x-4' : 'translate-x-0.5'"></span>
              </span>
            </button>
            <input v-else type="text" :value="form.isActive ? 'Đang làm việc' : 'Đã tạm ngưng'" disabled
                   class="w-full bg-surface-container-high border text-sm rounded-sm py-2.5 px-4 text-on-surface border-transparent disabled:opacity-50 disabled:cursor-not-allowed font-medium" />
          </div>
        </div>

        <p v-if="!editingId" class="text-[11px] text-on-surface-variant/70">
          Hệ thống sẽ tạo tài khoản với <b>mật khẩu mặc định</b> và gửi email cho nhân viên; họ phải <b>đổi mật khẩu ở lần đăng nhập đầu</b> để kích hoạt. Bỏ trống mã nhân viên thì hệ thống tự tạo.
        </p>

        <div class="flex justify-end gap-3 pt-2 border-t border-outline-variant/10">
          <button @click="closeModal" class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all">Huỷ</button>
          <button @click="saveStaff" :disabled="isSaving || !formValid" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            {{ isSaving ? 'Đang lưu...' : (editingId ? 'Lưu thay đổi' : 'Thêm nhân viên') }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Kết quả tạo tài khoản: hiển thị thông tin đăng nhập cho admin -->
    <AppModal :show="!!credsResult" title="Tài khoản đã được tạo" @close="credsResult = null">
      <div v-if="credsResult" class="space-y-4">
        <p class="text-xs text-on-surface-variant">
          Nhân viên sử dụng thông tin dưới đây để đăng nhập và bắt buộc <b class="text-on-surface">đổi mật khẩu ở lần đầu</b>.
        </p>

        <div class="bg-surface-container-high rounded-sm p-4 space-y-2.5 border border-outline-variant/10 text-xs">
          <!-- Thông tin nhân sự -->
          <div class="flex items-center justify-between">
            <span class="text-on-surface-variant text-[11px] uppercase tracking-wider font-semibold">Họ và tên</span>
            <span class="font-bold text-on-surface text-sm">{{ credsResult.fullName }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-on-surface-variant text-[11px] uppercase tracking-wider font-semibold">Mã nhân viên</span>
            <div class="flex items-center gap-2">
              <span class="font-mono font-bold text-primary">{{ credsResult.staffCode }}</span>
              <span class="px-1.5 py-0.5 rounded-sm bg-primary/10 text-primary text-[10px] font-bold uppercase tracking-wider">
                {{ roleLabel(credsResult.role) }}
              </span>
            </div>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-on-surface-variant text-[11px] uppercase tracking-wider font-semibold">Cơ sở làm việc</span>
            <span class="font-medium text-on-surface">{{ credsResult.cinemaName }}</span>
          </div>

          <div class="h-px bg-outline-variant/10 my-1"></div>

          <!-- Thông tin tài khoản & Mật khẩu -->
          <div class="flex items-center justify-between group">
            <span class="text-on-surface-variant text-[11px] uppercase tracking-wider font-semibold">Tài khoản</span>
            <div class="flex items-center gap-1.5">
              <span class="font-mono font-bold text-on-surface bg-surface-container-highest px-2 py-0.5 rounded-sm select-all">{{ credsResult.username }}</span>
              <button @click="copyText(credsResult.username, 'tài khoản')" class="p-1 text-on-surface-variant hover:text-primary transition-colors rounded-sm" title="Sao chép tài khoản">
                <span class="material-symbols-outlined text-[15px]">{{ copiedField === 'tài khoản' ? 'check' : 'content_copy' }}</span>
              </button>
            </div>
          </div>

          <div class="flex items-center justify-between group">
            <span class="text-on-surface-variant text-[11px] uppercase tracking-wider font-semibold">Mật khẩu tạm</span>
            <div class="flex items-center gap-1.5">
              <span class="font-mono font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-sm select-all">{{ credsResult.password }}</span>
              <button @click="copyText(credsResult.password, 'mật khẩu')" class="p-1 text-on-surface-variant hover:text-primary transition-colors rounded-sm" title="Sao chép mật khẩu">
                <span class="material-symbols-outlined text-[15px]">{{ copiedField === 'mật khẩu' ? 'check' : 'content_copy' }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Trạng thái email -->
        <div class="flex items-center gap-2 text-xs p-2.5 rounded-sm border"
             :class="credsResult.emailSent ? 'bg-green-500/5 text-green-400 border-green-500/20' : 'bg-amber-500/5 text-amber-400 border-amber-500/20'">
          <span class="material-symbols-outlined text-base shrink-0">{{ credsResult.emailSent ? 'mark_email_read' : 'unsubscribe' }}</span>
          <span class="truncate">
            {{ credsResult.emailSent ? `Đã gửi email thông tin đăng nhập tới: ${credsResult.email}` : 'Chưa gửi được email — vui lòng sao chép và gửi trực tiếp cho nhân viên.' }}
          </span>
        </div>

        <!-- Nút hành động -->
        <div class="flex justify-end gap-2.5 pt-2 border-t border-outline-variant/10">
          <button @click="copyCreds" class="px-4 py-2 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded-sm hover:bg-white/10 transition-all flex items-center gap-1.5">
            <span class="material-symbols-outlined text-sm">{{ copiedField === 'all' ? 'check' : 'content_copy' }}</span>
            {{ copiedField === 'all' ? 'Đã sao chép' : 'Sao chép tất cả' }}
          </button>
          <button @click="credsResult = null" class="px-5 py-2 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all">
            Hoàn tất
          </button>
        </div>
      </div>
    </AppModal>
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
</style>
