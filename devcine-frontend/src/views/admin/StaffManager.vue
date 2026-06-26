<script setup>
import { ref, computed, onMounted } from 'vue'
import { staffApi, cinemaListApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'
import AppModal from '@/components/common/AppModal.vue'

const toast = useToastStore()
const confirm = useConfirmStore()

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
  password: '', staffCode: '', cinemaId: '', isActive: true,
})
const form = ref(blankForm())

const roleLabel = (role) => {
  switch ((role || '').toUpperCase()) {
    case 'ADMIN': return 'Quản trị viên'
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
    console.error('Failed to load staff', e)
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
    console.error('Failed to load cinemas', e)
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
const openAddModal = () => {
  editingId.value = null
  form.value = blankForm()
  isModalOpen.value = true
}

const openEditModal = (person) => {
  editingId.value = person.userId
  form.value = {
    fullName: person.fullName || '',
    username: person.username || '',
    email: person.email || '',
    phone: person.phone || '',
    password: '',
    staffCode: person.staffCode || '',
    cinemaId: person.cinemaId ?? '',
    isActive: !!person.isActive,
  }
  isModalOpen.value = true
}

const closeModal = () => { isModalOpen.value = false }

const buildPayload = () => {
  const base = {
    fullName: form.value.fullName.trim(),
    email: form.value.email.trim(),
    phone: form.value.phone.trim() || null,
    staffCode: form.value.staffCode.trim() || null,
    cinemaId: form.value.cinemaId || null,
  }
  if (editingId.value) {
    return { ...base, isActive: form.value.isActive }
  }
  return { ...base, username: form.value.username.trim(), password: form.value.password }
}

const validate = () => {
  if (!form.value.fullName.trim()) { toast.warning('Vui lòng nhập họ tên nhân viên.'); return false }
  if (!form.value.email.trim()) { toast.warning('Vui lòng nhập email.'); return false }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email.trim())) { toast.warning('Email không hợp lệ.'); return false }
  if (!editingId.value) {
    if (!form.value.username.trim()) { toast.warning('Vui lòng nhập tài khoản đăng nhập.'); return false }
    if (!form.value.password || form.value.password.length < 6) { toast.warning('Mật khẩu cần tối thiểu 6 ký tự.'); return false }
  }
  return true
}

const saveStaff = async () => {
  if (!validate()) return
  isSaving.value = true
  try {
    if (editingId.value) {
      await staffApi.update(editingId.value, buildPayload())
      toast.success('Cập nhật nhân viên thành công.')
    } else {
      await staffApi.create(buildPayload())
      toast.success('Thêm nhân viên thành công.')
    }
    await fetchStaff()
    closeModal()
  } catch (e) {
    console.error('Failed to save staff', e)
    toast.error(friendlyError(e, 'Lưu nhân viên thất bại.'))
  } finally {
    isSaving.value = false
  }
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
    console.error('Failed to toggle staff', e)
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
      <button @click="openAddModal" class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-6 py-3 rounded-sm hover:brightness-110 transition-all flex items-center gap-2">
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
                <button @click="openEditModal(person)" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant" title="Sửa">
                  <span class="material-symbols-outlined text-sm">edit</span>
                </button>
                <button @click="toggleActive(person)" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant" :title="person.isActive ? 'Tạm ngưng' : 'Kích hoạt'">
                  <span class="material-symbols-outlined text-sm">{{ person.isActive ? 'toggle_on' : 'toggle_off' }}</span>
                </button>
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
              <button v-else @click="openAddModal" class="mt-3 text-xs font-bold uppercase tracking-widest text-primary hover:underline">Thêm nhân viên đầu tiên</button>
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
            <input v-model="form.fullName" type="text" placeholder="VD: Trần Quang Huy" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface" />
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
              Tài khoản đăng nhập <span v-if="!editingId" class="text-red-500">*</span>
            </label>
            <input v-model="form.username" type="text" :disabled="!!editingId" placeholder="vd: nv_huy"
                   class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface disabled:opacity-50 disabled:cursor-not-allowed" />
          </div>
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mã nhân viên</label>
            <input v-model="form.staffCode" type="text" placeholder="vd: NV001" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface" />
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Email <span class="text-red-500">*</span></label>
            <input v-model="form.email" type="email" placeholder="email@devcine.com" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface" />
          </div>
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số điện thoại</label>
            <input v-model="form.phone" type="text" placeholder="09xxxxxxxx" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface" />
          </div>

          <div v-if="!editingId" class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mật khẩu <span class="text-red-500">*</span></label>
            <input v-model="form.password" type="password" placeholder="Tối thiểu 6 ký tự" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface" />
          </div>

          <div class="space-y-1.5" :class="editingId ? '' : 'col-span-2'">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Cơ sở làm việc</label>
            <select v-model="form.cinemaId" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
              <option value="">— Chưa gán cơ sở —</option>
              <option v-for="c in cinemas" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>

          <div v-if="editingId" class="space-y-1.5">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Trạng thái</label>
            <button type="button" @click="form.isActive = !form.isActive"
                    :class="form.isActive ? 'bg-green-500/15 text-green-500 border-green-500/30' : 'bg-red-500/15 text-red-500 border-red-500/30'"
                    class="w-full py-2.5 rounded-lg border text-xs font-bold uppercase tracking-widest transition-all">
              {{ form.isActive ? 'Đang làm việc' : 'Tạm ngưng' }}
            </button>
          </div>
        </div>

        <p v-if="!editingId" class="text-[11px] text-on-surface-variant/70">
          Nhân viên sẽ đăng nhập trang quản trị bằng tài khoản &amp; mật khẩu trên, với vai trò <b>Nhân viên (STAFF)</b>.
        </p>

        <div class="flex justify-end gap-3 pt-2 border-t border-outline-variant/10">
          <button @click="closeModal" class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">Huỷ</button>
          <button @click="saveStaff" :disabled="isSaving" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all disabled:opacity-60">
            {{ isSaving ? 'Đang lưu...' : (editingId ? 'Lưu thay đổi' : 'Thêm nhân viên') }}
          </button>
        </div>
      </div>
    </AppModal>
  </div>
</template>
