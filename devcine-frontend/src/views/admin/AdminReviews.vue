<script setup>
import { ref, computed, onMounted } from 'vue'
import { reviewAdminApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'
import { useAdminPerm } from '@/composables/useAdminPerm'

const { can } = useAdminPerm()
const toast = useToastStore()
const confirm = useConfirmStore()

const reviews = ref([])
const isLoading = ref(false)
const loadError = ref(false)

const search = ref('')
const ratingFilter = ref('') // '' | '1'..'5'
const statusFilter = ref('ALL') // ALL | VISIBLE | HIDDEN

const fetchReviews = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await reviewAdminApi.list()
    reviews.value = Array.isArray(data) ? data : (data.data ?? [])
  } catch (e) {
    console.error('Failed to load reviews', e)
    loadError.value = true
    toast.error(friendlyError(e, 'Không tải được danh sách đánh giá.'))
  } finally {
    isLoading.value = false
  }
}

const formatDate = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  return isNaN(d) ? '—' : d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const filtered = computed(() => {
  const kw = search.value.trim().toLowerCase()
  return reviews.value.filter(r => {
    if (ratingFilter.value && r.rating !== Number(ratingFilter.value)) return false
    if (statusFilter.value === 'VISIBLE' && r.hidden) return false
    if (statusFilter.value === 'HIDDEN' && !r.hidden) return false
    if (kw) {
      const hay = `${r.movieTitle || ''} ${r.customerName || ''} ${r.comment || ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
})

const stats = computed(() => {
  const total = reviews.value.length
  const hidden = reviews.value.filter(r => r.hidden).length
  const avg = total ? (reviews.value.reduce((s, r) => s + (r.rating || 0), 0) / total) : 0
  return { total, hidden, visible: total - hidden, avg: Math.round(avg * 10) / 10 }
})

const toggleHidden = async (rv) => {
  if (!rv.hidden) {
    const ok = await confirm.show({
      title: 'Ẩn đánh giá',
      message: `Ẩn đánh giá của "${rv.customerName}" cho phim "${rv.movieTitle}"? Đánh giá sẽ không hiển thị ở trang phim.`,
      confirmText: 'Ẩn',
      tone: 'danger',
    })
    if (!ok) return
  }
  try {
    const { data } = await reviewAdminApi.toggle(rv.id)
    rv.hidden = data.hidden
    toast.success(rv.hidden ? 'Đã ẩn đánh giá.' : 'Đã hiển thị lại đánh giá.')
  } catch (e) {
    console.error('Failed to toggle review', e)
    toast.error(friendlyError(e, 'Không đổi được trạng thái đánh giá.'))
  }
}

const removeReview = async (rv) => {
  const ok = await confirm.show({
    title: 'Xoá đánh giá',
    message: `Xoá vĩnh viễn đánh giá của "${rv.customerName}" cho phim "${rv.movieTitle}"? Hành động không thể hoàn tác.`,
    confirmText: 'Xoá',
    tone: 'danger',
  })
  if (!ok) return
  try {
    await reviewAdminApi.delete(rv.id)
    reviews.value = reviews.value.filter(r => r.id !== rv.id)
    toast.success('Đã xoá đánh giá.')
  } catch (e) {
    console.error('Failed to delete review', e)
    toast.error(friendlyError(e, 'Xoá đánh giá thất bại.'))
  }
}

const resetFilters = () => { search.value = ''; ratingFilter.value = ''; statusFilter.value = 'ALL' }

onMounted(fetchReviews)
</script>

<template>
  <div class="p-10">
    <!-- Header -->
    <header class="flex justify-between items-start mb-8 text-on-surface gap-4 flex-wrap">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Quản lý Đánh giá</h1>
        <p class="text-on-surface-variant text-sm mt-1">Kiểm duyệt đánh giá &amp; bình luận phim của khách hàng</p>
      </div>
    </header>

    <!-- Thống kê nhanh -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tổng đánh giá</p>
        <p class="text-2xl font-extrabold text-on-surface mt-1">{{ stats.total }}</p>
      </div>
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Đang hiển thị</p>
        <p class="text-2xl font-extrabold text-green-500 mt-1">{{ stats.visible }}</p>
      </div>
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Đã ẩn</p>
        <p class="text-2xl font-extrabold text-red-500 mt-1">{{ stats.hidden }}</p>
      </div>
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Điểm trung bình</p>
        <p class="text-2xl font-extrabold text-[#f5c518] mt-1">{{ stats.avg }} ★</p>
      </div>
    </div>

    <!-- Toolbar lọc -->
    <div class="flex flex-wrap items-center gap-3 mb-6">
      <div class="relative flex-1 min-w-[220px]">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">search</span>
        <input v-model="search" type="text" placeholder="Tìm theo phim, khách, nội dung..."
               class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 pl-10 pr-4 text-on-surface" />
      </div>
      <select v-model="ratingFilter" class="bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
        <option value="">Mọi số sao</option>
        <option v-for="s in [5,4,3,2,1]" :key="s" :value="String(s)">{{ s }} sao</option>
      </select>
      <select v-model="statusFilter" class="bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
        <option value="ALL">Mọi trạng thái</option>
        <option value="VISIBLE">Đang hiển thị</option>
        <option value="HIDDEN">Đã ẩn</option>
      </select>
      <button v-if="search || ratingFilter || statusFilter !== 'ALL'" @click="resetFilters"
              class="text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors px-3 py-2.5 flex items-center gap-1">
        <span class="material-symbols-outlined text-sm">filter_alt_off</span> Bỏ lọc
      </button>
    </div>

    <!-- Bảng -->
    <section class="bg-surface-container-low border border-outline-variant/10 rounded-lg overflow-hidden">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant border-b border-outline-variant/10">
            <th class="px-6 py-5">Phim</th>
            <th class="px-6 py-5">Khách hàng</th>
            <th class="px-6 py-5">Điểm</th>
            <th class="px-6 py-5">Nội dung</th>
            <th class="px-6 py-5">Ngày</th>
            <th class="px-6 py-5">Trạng thái</th>
            <th class="px-6 py-5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-outline-variant/10 text-on-surface">
          <template v-if="isLoading">
            <tr v-for="i in 4" :key="`sk-${i}`">
              <td colspan="7" class="px-6 py-4"><div class="h-9 bg-surface-container-highest rounded animate-pulse"></div></td>
            </tr>
          </template>

          <template v-else-if="filtered.length">
            <tr v-for="rv in filtered" :key="rv.id" class="group hover:bg-white/5 transition-all" :class="rv.hidden ? 'opacity-60' : ''">
              <td class="px-6 py-4">
                <p class="font-bold text-sm">{{ rv.movieTitle }}</p>
              </td>
              <td class="px-6 py-4">
                <span class="text-xs">{{ rv.customerName }}</span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="text-[#f5c518] font-bold text-sm">{{ rv.rating }}</span>
                <span class="text-[#f5c518] text-sm">★</span>
              </td>
              <td class="px-6 py-4 max-w-[320px]">
                <p class="text-xs text-on-surface-variant line-clamp-2">{{ rv.comment || '—' }}</p>
              </td>
              <td class="px-6 py-4">
                <span class="text-xs text-on-surface-variant whitespace-nowrap">{{ formatDate(rv.createdAt) }}</span>
              </td>
              <td class="px-6 py-4">
                <span :class="rv.hidden ? 'bg-red-500/10 text-red-500' : 'bg-green-500/10 text-green-500'" class="px-2 py-1 rounded text-[10px] font-bold uppercase tracking-tighter whitespace-nowrap">
                  {{ rv.hidden ? 'Đã ẩn' : 'Hiển thị' }}
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex justify-end gap-2">
                  <button v-if="can('support', 'edit')" @click="toggleHidden(rv)" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-primary/10 hover:text-primary transition-all text-on-surface-variant" :title="rv.hidden ? 'Hiển thị lại' : 'Ẩn'">
                    <span class="material-symbols-outlined text-sm">{{ rv.hidden ? 'visibility' : 'visibility_off' }}</span>
                  </button>
                  <button v-if="can('support', 'delete')" @click="removeReview(rv)" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-red-500/20 hover:text-red-400 transition-all text-on-surface-variant" title="Xoá">
                    <span class="material-symbols-outlined text-sm">delete</span>
                  </button>
                  <span v-if="!can('support','edit') && !can('support','delete')" class="text-on-surface-variant/40 text-xs">—</span>
                </div>
              </td>
            </tr>
          </template>

          <tr v-else-if="loadError">
            <td colspan="7" class="px-6 py-16 text-center">
              <span class="material-symbols-outlined text-4xl text-red-500/60 mb-2">error</span>
              <p class="text-on-surface-variant font-semibold">Không tải được danh sách đánh giá.</p>
              <button @click="fetchReviews" class="mt-3 text-xs font-bold uppercase tracking-widest text-primary hover:underline">Thử lại</button>
            </td>
          </tr>

          <tr v-else>
            <td colspan="7" class="px-6 py-16 text-center">
              <span class="material-symbols-outlined text-4xl text-outline-variant mb-2">reviews</span>
              <p class="text-on-surface-variant font-semibold">
                {{ reviews.length ? 'Không có đánh giá khớp bộ lọc.' : 'Chưa có đánh giá nào.' }}
              </p>
              <button v-if="reviews.length" @click="resetFilters" class="mt-3 text-xs font-bold uppercase tracking-widest text-primary hover:underline">Bỏ lọc</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <p v-if="!isLoading && filtered.length" class="text-[11px] text-on-surface-variant mt-3">
      Hiển thị {{ filtered.length }}/{{ reviews.length }} đánh giá
    </p>
  </div>
</template>
