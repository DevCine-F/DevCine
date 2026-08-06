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
const RE_USERNAME = /^[a-z0-9_]{3,20}$/
const RE_STAFFCODE = /^[A-Z0-9]{3,15}$/
const RE_EMAIL = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
const RE_PHONE = /^(03|05|07|08|09)\d{8}$/
const RE_NAME = /^[\p{L}\p{M} ]+$/u

const errFullName = computed(() => {
  const s = form.value.fullName.trim()
  if (!s) return 'Vui lòng nhập họ và tên.'
  if (s.length < 2 || s.length > 50) return 'Họ tên phải từ 2 đến 50 ký tự.'
  if (!RE_NAME.test(s)) return 'Họ tên chỉ gồm chữ cái và khoảng trắng.'
  return ''
})
const errUsername = computed(() => {
  if (editingId.value) return '' // username không sửa khi edit
  const s = form.value.username.trim()
  if (!s) return 'Vui lòng nhập tài khoản đăng nhập.'
  if (!RE_USERNAME.test(s)) return '3–20 ký tự; chỉ chữ thường, số, gạch dưới, không dấu, không khoảng trắng.'
  return ''
})
const errStaffCode = computed(() => {
  return ''
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
  if (!RE_PHONE.test(s)) return 'Số điện thoại không hợp lệ (10 số, bắt đầu 03/05/07/08/09).'
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
    if (res.data?.success) {
      form.value.staffCode = res.data.data
    }
  } catch (error) {
    console.error('Lỗi khi lấy mã nhân viên:', error)
  }
}

const openAddModal = async () => {
  editingId.value = null
  form.value = blankForm()
  resetValidationState()
  isModalOpen.value = true
  await fetchNextCode()
}

const openEditModal = (person) => {
  editingId.value = person.userId
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

const closeModal = () => { isModalOpen.value = false }

const buildPayload = () => {
  const base = {
    fullName: form.value.fullName.trim(),
    email: form.value.email.trim(),
    phone: form.value.phone.trim() || null,
    staffCode: form.value.staffCode.trim().toUpperCase() || null,
    cinemaId: form.value.cinemaId || null,
  }
  // Chỉ ADMIN mới được gán vai trò (STAFF/MANAGER); manager tạo NV luôn là STAFF
  if (auth.isAdmin) base.role = form.value.role
  if (editingId.value) {
    return { ...base, isActive: form.value.isActive }
  }
  // Tạo mới: KHÔNG gửi mật khẩu — backend dùng mật khẩu mặc định + buộc đổi lần đầu
  return { ...base, username: form.value.username.trim() }
}

const validate = () => {
  attempted.value = true
  if (!formValid.value) { toast.warning('Vui lòng kiểm tra lại các trường còn thiếu hoặc chưa hợp lệ.'); return false }
  return true
}

const saveStaff = async () => {
  if (!validate()) return
  isSaving.value = true
  try {
    if (editingId.value) {
      await staffApi.update(editingId.value, buildPayload())
      toast.success('Cập nhật nhân viên thành công.')
      await fetchStaff()
      closeModal()
    } else {
      const res = await staffApi.create(buildPayload())
      const d = res?.data ?? {}
      toast.success('Thêm nhân viên thành công.')
      await fetchStaff()
      closeModal()
      // Hiện thông tin đăng nhập cho admin (email best-effort đã gửi song song)
      credsResult.value = { username: d.username, password: d.defaultPassword, emailSent: d.emailSent }
    }
  } catch (e) {
    toast.error(friendlyError(e, 'Lưu nhân viên thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const copyCreds = async () => {
  if (!credsResult.value) return
  const text = `Tài khoản: ${credsResult.value.username}\nMật khẩu mặc định: ${credsResult.value.password}`
  try { await navigator.clipboard.writeText(text); toast.success('Đã sao chép thông tin đăng nhập.') }
  catch { toast.warning('Không sao chép được, vui lòng ghi lại thủ công.') }
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

const resetFilters = () => { search.value = ''; cinemaFilter.value = ''; statusFilter.value = 'ALL' }

onMounted(() => { fetchStaff(); fetchCinemas() })
</script>

<template>
  <div class="p-10">
    <!-- Header -->
    <header class="flex justify-between items-start mb-8 text-on-surface gap-4 flex-wrap">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Quản lý Nhân viên</h1>
        <p class="text-on-surface-variant text-sm mt-1">Quản lý đội ngũ vận hành trên toàn bộ cơ sở &amp; phân quyền hệ thống</p>
      </div>
      <button v-if="can('staff_management', 'add')" @click="openAddModal" class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-6 py-3 rounded-sm hover:brightness-110 transition-all flex items-center gap-2">
        <span class="material-symbols-outlined text-sm">person_add</span>
        Thêm Nhân Viên
      </button>
    </header>

    <!-- Toolbar lọc -->
    <div class="flex flex-wrap items-center gap-3 mb-6">
      <div class="relative flex-1 min-w-[220px]">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">search</span>
        <input v-model="search" type="text" placeholder="Tìm theo tên, email, mã NV, SĐT..."
               class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 pl-10 pr-4 text-on-surface" />
      </div>

      <select v-model="cinemaFilter" class="bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface min-w-[200px]">
        <option value="">Tất cả cơ sở</option>
        <option v-for="c in cinemas" :key="c.id" :value="c.id">{{ c.name }} ({{ countByCinema[c.id] || 0 }})</option>
      </select>

      <select v-model="statusFilter" class="bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
        <option value="ALL">Mọi trạng thái</option>
        <option value="ACTIVE">Đang làm việc</option>
        <option value="INACTIVE">Đã tạm ngưng</option>
      </select>

      <button v-if="search || cinemaFilter || statusFilter !== 'ALL'" @click="resetFilters"
              class="text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors px-3 py-2.5 flex items-center gap-1">
        <span class="material-symbols-outlined text-sm">filter_alt_off</span> Bỏ lọc
      </button>
    </div>

    <!-- Bảng -->
    <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg overflow-hidden">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant border-b border-outline-variant/10">
            <th class="px-8 py-5">Nhân viên</th>
            <th class="px-8 py-5">Mã NV</th>
            <th class="px-8 py-5">Cơ sở</th>
            <th class="px-8 py-5">Vai trò</th>
            <th class="px-8 py-5">Ngày gia nhập</th>
            <th class="px-8 py-5">Trạng thái</th>
            <th class="px-8 py-5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-outline-variant/10 text-on-surface">
          <!-- Loading skeleton -->
          <template v-if="isLoading">
            <tr v-for="i in 4" :key="`sk-${i}`">
              <td colspan="7" class="px-8 py-4">
                <div class="h-10 bg-surface-container-highest rounded animate-pulse"></div>
              </td>
            </tr>
          </template>

          <!-- Dữ liệu -->
          <template v-else-if="filteredStaff.length">
            <tr v-for="person in filteredStaff" :key="person.userId" class="group hover:bg-white/5 transition-all">
            <td class="px-8 py-4">
              <div class="flex items-center gap-4">
                <img v-if="person.avatarUrl" :src="person.avatarUrl" class="w-10 h-10 rounded-full object-cover" alt="" />
                <div v-else class="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold uppercase">
                  {{ (person.fullName || '?').charAt(0) }}
                </div>
                <div>
                  <p class="font-bold text-sm uppercase tracking-tight group-hover:text-primary transition-colors">{{ person.fullName }}</p>
                  <p class="text-[10px] text-on-surface-variant mt-0.5">{{ person.email || '—' }}</p>
                </div>
              </div>
            </td>
            <td class="px-8 py-4">
              <span class="text-xs font-mono text-on-surface-variant">{{ person.staffCode || '—' }}</span>
            </td>
            <td class="px-8 py-4">
              <span v-if="person.cinemaName" class="inline-flex items-center gap-1.5 text-xs font-semibold">
                <span class="material-symbols-outlined text-sm text-primary/70">location_on</span>{{ person.cinemaName }}
              </span>
              <span v-else class="text-xs text-on-surface-variant/60 italic">Chưa gán cơ sở</span>
            </td>
            <td class="px-8 py-4">
              <span class="text-xs font-semibold">{{ roleLabel(person.role) }}</span>
            </td>
            <td class="px-8 py-4">
              <span class="text-xs text-on-surface-variant">{{ formatDate(person.joinDate) }}</span>
            </td>
            <td class="px-8 py-4">
              <span :class="person.isActive ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'" class="px-2 py-1 rounded text-[10px] font-bold uppercase tracking-tighter">
                {{ person.isActive ? 'Đang làm việc' : 'Đã tạm ngưng' }}
              </span>
            </td>
            <td class="px-8 py-4 text-right">
              <div class="flex justify-end gap-2">
                <button v-if="can('staff_management', 'edit')" @click="openEditModal(person)" :disabled="person.userId === auth.user?.id" :title="person.userId === auth.user?.id ? 'Không thể tự sửa tài khoản của chính mình' : 'Sửa'" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant disabled:opacity-30 disabled:cursor-not-allowed">
                  <span class="material-symbols-outlined text-sm">edit</span>
                </button>
                <button v-if="can('staff_management', 'edit')" @click="toggleActive(person)" :disabled="person.userId === auth.user?.id" :title="person.userId === auth.user?.id ? 'Không thể tự đổi trạng thái của chính mình' : (person.isActive ? 'Tạm ngưng' : 'Kích hoạt')" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant disabled:opacity-30 disabled:cursor-not-allowed">
                  <span class="material-symbols-outlined text-sm">{{ person.isActive ? 'toggle_on' : 'toggle_off' }}</span>
                </button>
                <span v-if="!can('staff_management','edit')" class="text-on-surface-variant/40 text-xs">—</span>
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
    </section>

    <p v-if="!isLoading && filteredStaff.length" class="text-[11px] text-on-surface-variant mt-3">
      Hiển thị {{ filteredStaff.length }}/{{ staff.length }} nhân viên
    </p>

    <!-- Modal thêm/sửa -->
    <AppModal :show="isModalOpen" :title="editingId ? 'Sửa nhân viên' : 'Thêm nhân viên mới'" @close="closeModal">
      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Họ và tên <span class="text-red-500">*</span></label>
            <input v-model="form.fullName" @blur="touch('fullName')" type="text" placeholder="VD: Trần Quang Huy"
                   class="w-full bg-surface-container-high border text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                   :class="showErr('fullName', errFullName) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('fullName', errFullName)" class="text-[11px] text-red-400 font-medium">{{ errFullName }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
              Tài khoản đăng nhập <span v-if="!editingId" class="text-red-500">*</span>
            </label>
            <input v-model="form.username" @blur="touch('username')" type="text" :disabled="!!editingId" placeholder="vd: nv_huy"
                   class="w-full bg-surface-container-high border text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                   :class="showErr('username', errUsername) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('username', errUsername)" class="text-[11px] text-red-400 font-medium">{{ errUsername }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mã nhân viên</label>
            <input v-model.trim="form.staffCode" type="text" placeholder="Đang tạo mã..." disabled
                   class="w-full bg-surface-container-high border text-sm rounded-lg py-2.5 px-4 text-on-surface uppercase border-transparent disabled:opacity-50 disabled:cursor-not-allowed font-bold" />
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Email <span class="text-red-500">*</span></label>
            <input v-model="form.email" @blur="touch('email')" type="email" placeholder="email@devcine.com"
                   class="w-full bg-surface-container-high border text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                   :class="showErr('email', errEmail) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('email', errEmail)" class="text-[11px] text-red-400 font-medium">{{ errEmail }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số điện thoại <span class="text-red-500">*</span></label>
            <input v-model="form.phone" @blur="touch('phone')" type="text" placeholder="09xxxxxxxx"
                   class="w-full bg-surface-container-high border text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                   :class="showErr('phone', errPhone) ? 'border-red-500' : 'border-transparent'" />
            <p v-if="showErr('phone', errPhone)" class="text-[11px] text-red-400 font-medium">{{ errPhone }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Cơ sở làm việc <span class="text-red-500">*</span></label>
            <select v-model="form.cinemaId" @blur="touch('cinemaId')"
                    class="w-full bg-surface-container-high border text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface"
                    :class="showErr('cinemaId', errCinema) ? 'border-red-500' : 'border-transparent'">
              <option value="">— Chưa gán cơ sở —</option>
              <option v-for="c in cinemas" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
            <p v-if="showErr('cinemaId', errCinema)" class="text-[11px] text-red-400 font-medium">{{ errCinema }}</p>
          </div>

          <div v-if="auth.isAdmin" class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Vai trò <span class="text-red-500">*</span></label>
            <select v-model="form.role" class="w-full bg-surface-container-high border border-transparent text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
              <option value="STAFF">Nhân viên</option>
              <option value="MANAGER">Quản lý cơ sở</option>
            </select>
          </div>

          <div v-if="editingId" class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Trạng thái</label>
            <button type="button" @click="form.isActive = !form.isActive"
                    class="w-full h-[42px] px-4 rounded-lg bg-surface-container-high border border-outline-variant/10 text-left flex items-center justify-between transition-all hover:border-primary/30">
              <span class="text-sm font-semibold text-on-surface">{{ form.isActive ? 'Đang làm việc' : 'Tạm ngưng' }}</span>
              <span class="relative inline-flex h-5 w-9 rounded-full transition-colors" :class="form.isActive ? 'bg-green-500' : 'bg-outline-variant'">
                <span class="absolute top-0.5 h-4 w-4 rounded-full bg-white transition-transform" :class="form.isActive ? 'translate-x-4' : 'translate-x-0.5'"></span>
              </span>
            </button>
          </div>
        </div>

        <p v-if="!editingId" class="text-[11px] text-on-surface-variant/70">
          Hệ thống sẽ tạo tài khoản với <b>mật khẩu mặc định</b> và gửi email cho nhân viên; họ phải <b>đổi mật khẩu ở lần đăng nhập đầu</b> để kích hoạt. Bỏ trống mã nhân viên thì hệ thống tự tạo.
        </p>

        <div class="flex justify-end gap-3 pt-2 border-t border-outline-variant/10">
          <button @click="closeModal" class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">Huỷ</button>
          <button @click="saveStaff" :disabled="isSaving || !formValid" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            {{ isSaving ? 'Đang lưu...' : (editingId ? 'Lưu thay đổi' : 'Thêm nhân viên') }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Kết quả tạo tài khoản: hiển thị thông tin đăng nhập cho admin -->
    <AppModal :show="!!credsResult" title="Tài khoản đã được tạo" @close="credsResult = null">
      <div v-if="credsResult" class="space-y-4">
        <p class="text-sm text-on-surface-variant">
          Nhân viên đăng nhập bằng thông tin dưới đây và sẽ được yêu cầu <b>đổi mật khẩu ở lần đầu</b>.
        </p>
        <div class="bg-surface-container-high rounded-xl p-4 space-y-2">
          <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Tài khoản</span><span class="font-black text-on-surface">{{ credsResult.username }}</span></div>
          <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Mật khẩu mặc định</span><span class="font-black text-primary font-mono">{{ credsResult.password }}</span></div>
        </div>
        <div class="flex items-center gap-2 text-xs" :class="credsResult.emailSent ? 'text-green-400' : 'text-amber-400'">
          <span class="material-symbols-outlined text-base">{{ credsResult.emailSent ? 'mark_email_read' : 'unsubscribe' }}</span>
          {{ credsResult.emailSent ? 'Đã gửi email thông tin đăng nhập cho nhân viên.' : 'Chưa gửi được email — vui lòng báo trực tiếp thông tin trên cho nhân viên.' }}
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button @click="copyCreds" class="px-4 py-2 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">Sao chép</button>
          <button @click="credsResult = null" class="px-5 py-2 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all">Đã hiểu</button>
        </div>
      </div>
    </AppModal>
  </div>
</template>
