<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import api from '@/api/axios'
import { fnbApi, fnbGroupApi } from '@/api/admin/index'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { can } = useAdminPerm()
const items = ref([])
const isLoading = ref(false)
const error = ref('')

const activeTab = ref('items')

const isDrawerOpen = ref(false)
const editingId = ref(null)
const isSaving = ref(false)
const isUploading = ref(false)
const form = ref({ name: '', type: 'COMBO', price: null, imageUrl: '', description: '', isActive: true, slots: [] })
const optionGroups = ref([])

const isGroupDrawerOpen = ref(false)
const editingGroupId = ref(null)
// Kho Tùy Chọn (Pool) nay thuần túy: chỉ tên + danh sách vị. Ràng buộc min/max/required
// đã chuyển xuống Slot của từng combo.
const groupForm = ref({ name: '', items: [] })
const groupItemNameRefs = ref([])
const groupTouched = ref({ name: false })
const groupItemTouched = ref([])
const groupSubmitAttempted = ref(false)

// ── Helpers chuẩn hóa text & masking tiền tệ ──
const normalizeInputText = (text) => {
  if (!text) return ''
  return text.trim().replace(/\s+/g, ' ')
}

const handleCurrencyInput = (event, item, field = 'surchargePrice', displayField = 'surchargeDisplay') => {
  const input = event.target
  const rawOldVal = input.value || ''
  const caretPos = input.selectionStart || 0

  // Đếm số lượng chữ số nằm phía trước con trỏ
  const digitsBefore = rawOldVal.slice(0, caretPos).replace(/\D/g, '').length

  // Chỉ giữ lại chữ số và loại bỏ số 0 thừa phía trước
  let cleanDigits = rawOldVal.replace(/\D/g, '').replace(/^0+(?=\d)/, '')
  if (!cleanDigits) cleanDigits = '0'

  let numVal = Number(cleanDigits) || 0
  if (numVal > 100000000) {
    numVal = 100000000
    cleanDigits = '100000000'
  }

  const formattedVal = numVal.toLocaleString('vi-VN')
  item[field] = numVal
  item[displayField] = formattedVal
  input.value = formattedVal

  // Tìm lại vị trí con trỏ chuột chính xác (tránh Cursor Jump)
  let newCaretPos = 0
  let digitsCount = 0
  for (let i = 0; i < formattedVal.length; i++) {
    if (/\d/.test(formattedVal[i])) {
      digitsCount++
    }
    if (digitsCount === digitsBefore) {
      newCaretPos = i + 1
      break
    }
  }
  if (digitsBefore === 0) {
    newCaretPos = formattedVal === '0' ? 1 : 0
  }
  input.setSelectionRange(newCaretPos, newCaretPos)
}

const fmtThousand = (n) => (n === null || n === undefined || n === '' ? '' : Number(n).toLocaleString('vi-VN'))

const handlePriceInput = (event) => {
  const input = event.target
  const rawOldVal = input.value || ''
  const caretPos = input.selectionStart || 0

  const digitsBefore = rawOldVal.slice(0, caretPos).replace(/\D/g, '').length
  let cleanDigits = rawOldVal.replace(/\D/g, '').replace(/^0+(?=\d)/, '')

  if (!cleanDigits) {
    form.value.price = null
    input.value = ''
    return
  }

  let numVal = Number(cleanDigits) || 0
  if (numVal > 100000000) {
    numVal = 100000000
    cleanDigits = '100000000'
  }

  const formattedVal = numVal.toLocaleString('vi-VN')
  form.value.price = numVal
  input.value = formattedVal

  let newCaretPos = 0
  let digitsCount = 0
  for (let i = 0; i < formattedVal.length; i++) {
    if (/\d/.test(formattedVal[i])) digitsCount++
    if (digitsCount === digitsBefore) {
      newCaretPos = i + 1
      break
    }
  }
  if (digitsBefore === 0) {
    newCaretPos = 0
  }
  input.setSelectionRange(newCaretPos, newCaretPos)
}

const handleCurrencyPaste = (event, item, field = 'surchargePrice', displayField = 'surchargeDisplay') => {
  event.preventDefault()
  const pastedText = (event.clipboardData || window.clipboardData)?.getData('text') || ''
  let cleanDigits = pastedText.replace(/\D/g, '').replace(/^0+(?=\d)/, '')
  if (!cleanDigits) cleanDigits = '0'

  const numVal = Math.min(Number(cleanDigits) || 0, 100000000)
  const formattedVal = numVal.toLocaleString('vi-VN')

  item[field] = numVal
  item[displayField] = formattedVal
  if (event.target) {
    event.target.value = formattedVal
  }
}

const typeOptions = [
  { value: 'COMBO', label: 'Combo' },
  { value: 'POPCORN', label: 'Bắp rang' },
  { value: 'DRINK', label: 'Nước uống' },
  { value: 'SNACK', label: 'Đồ ăn vặt' }
]
const typeLabel = (t) => (typeOptions.find(o => o.value === t)?.label) || t || '—'

// Toast
const toast = useToastStore()

const fetchItems = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await fnbApi.getAll()
    items.value = data.data ?? data
  } catch (err) {
    error.value = friendlyError(err, 'Không thể tải thực đơn F&B.')
    toast.error(error.value)
  } finally {
    isLoading.value = false
  }
}

const fetchOptionGroups = async () => {
  try {
    const { data } = await api.get('/fnbs/groups')
    optionGroups.value = data.data ?? data
  } catch (err) {
    console.error('Failed to fetch option groups', err)
  }
}

const openCreate = () => {
  editingId.value = null
  form.value = { name: '', type: 'COMBO', price: null, imageUrl: '', description: '', isActive: true, slots: [] }
  builderMode.value = true
  resetTouched()
  isDrawerOpen.value = true
}

const openEdit = (item) => {
  editingId.value = item.id
  form.value = {
    name: item.name || '',
    type: item.type || 'COMBO',
    price: item.price != null ? Number(item.price) : null,
    imageUrl: item.imageUrl || '',
    description: item.description || '',
    isActive: item.isActive !== false,
    slots: [...(item.slots || [])]
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
      .map(s => ({
        slotLabel: s.slotLabel || '',
        optionGroupId: s.optionGroup?.id ?? null,
        defaultOptionItemId: resolveDefault(s.optionGroup?.id ?? null, s.defaultOptionItem?.id ?? null),
        minChoices: s.minChoices ?? 1,
        maxChoices: s.maxChoices ?? 1,
        _autoLabel: false // đã có nhãn lưu sẵn → không tự ghi đè
      }))
  }
  // Chọn chế độ hiển thị: combo rỗng hoặc đồng đều (mỗi ô chọn đúng 1) → Builder số lượng;
  // combo có cấu hình lệch (mix vị / tùy chọn) → Nâng cao để không mất dữ liệu.
  builderMode.value = form.value.slots.length === 0 || slotsAreUniform(form.value.slots)
  resetTouched()
  isDrawerOpen.value = true
}

const handleUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  let prepared
  try {
    prepared = await prepareImageForUpload(file)
  } catch (err) {
    toast.error(friendlyError(err, 'Ảnh không hợp lệ.'))
    event.target.value = ''
    return
  }
  isUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', prepared)
    const { data } = await api.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.value.imageUrl = data.url
  } catch (err) {
    toast.error(friendlyError(err, 'Tải ảnh thất bại.'))
  } finally {
    isUploading.value = false
  }
}

const handleSave = async () => {
  submitAttempted.value = true // từ giờ mọi lỗi được phép hiện đỏ
  // Chặn lưu khi còn bất kỳ lỗi nào — KHÔNG âm thầm bỏ qua/sửa dữ liệu.
  if (!canSave.value) {
    toast.warning(nameError.value || priceError.value || 'Vui lòng sửa các mục đang báo lỗi (viền đỏ) trước khi lưu.')
    return
  }
  isSaving.value = true
  try {
    const isCombo = form.value.type === 'COMBO'
    const payload = {
      name: form.value.name.trim(),
      type: form.value.type,
      price: Number(form.value.price),
      imageUrl: form.value.imageUrl || null,
      description: form.value.description || null,
      isActive: form.value.isActive,
      // Món đơn lẻ (không phải COMBO) không có Ô chọn.
      slots: isCombo
        ? form.value.slots.map((s, i) => ({
            slotLabel: (s.slotLabel || '').trim(),
            optionGroupId: s.optionGroupId,
            defaultOptionItemId: s.defaultOptionItemId || null,
            displayOrder: i,
            minChoices: Number(s.minChoices) || 0,
            maxChoices: Number(s.maxChoices) || 1,
            isRequired: (Number(s.minChoices) || 0) > 0 // suy ra từ min, không còn checkbox riêng
          }))
        : []
    }
    if (editingId.value) {
      await fnbApi.update(editingId.value, payload)
      toast.success('Cập nhật món thành công.')
    } else {
      await fnbApi.create(payload)
      toast.success('Thêm món thành công.')
    }
    isDrawerOpen.value = false
    await fetchItems()
  } catch (err) {
    toast.error(friendlyError(err, 'Lưu thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const toggleActive = async (item) => {
  try {
    await fnbApi.update(item.id, { isActive: !(item.isActive !== false) })
    await fetchItems()
  } catch (err) {
    toast.error(friendlyError(err, 'Đổi trạng thái thất bại.'))
  }
}

const deleteTarget = ref(null)
const isDeleting = ref(false)
const confirmDelete = async () => {
  if (!deleteTarget.value) return
  isDeleting.value = true
  try {
    await fnbApi.delete(deleteTarget.value.id)
    toast.success('Đã xoá món.')
    deleteTarget.value = null
    await fetchItems()
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể xoá (món có thể đang nằm trong đơn). Hãy ẩn món thay vì xoá.'))
    deleteTarget.value = null
  } finally {
    isDeleting.value = false
  }
}

// ── Validation Kho Tùy Chọn ──
const groupNameError = computed(() => {
  const n = (groupForm.value.name || '').trim()
  if (!n) return 'Vui lòng nhập tên kho tùy chọn'
  if (n.length < 2 || n.length > 100) return 'Tên kho phải từ 2 đến 100 ký tự'
  if (/<|>|javascript:/i.test(groupForm.value.name || '')) return 'Tên kho chứa ký tự không hợp lệ'
  const dup = optionGroups.value.some(g => 
    (editingGroupId.value == null || g.id !== editingGroupId.value) && 
    (g.name || '').trim().toLowerCase() === n.toLowerCase()
  )
  if (dup) return 'Tên kho tùy chọn đã tồn tại'
  return ''
})

const groupItemErrors = computed(() => {
  const items = groupForm.value.items
  return items.map((item) => {
    const e = {}
    const n = (item.name || '').trim()
    if (!n) {
      e.name = 'Vui lòng nhập tên vị'
    } else if (n.length > 50) {
      e.name = 'Tên vị không được quá 50 ký tự'
    } else {
      const count = items.filter(it => (it.name || '').trim().toLowerCase() === n.toLowerCase()).length
      if (count > 1) e.name = 'Tên vị này đã tồn tại trong danh sách'
    }

    const p = item.surchargePrice
    if (p == null || p === '') {
      e.price = 'Giá không hợp lệ'
    } else {
      const num = Number(p)
      if (!Number.isInteger(num) || num < 0) e.price = 'Giá phụ thu không được là số âm'
      else if (num > 100000000) e.price = 'Giá tối đa không vượt quá 100.000.000đ'
    }
    return e
  })
})

const canSaveGroup = computed(() => {
  if (groupNameError.value) return false
  if (groupForm.value.items.length < 1 || groupForm.value.items.length > 50) return false
  return groupItemErrors.value.every(e => Object.keys(e).length === 0)
})

const showGroupError = (field, msg) => (groupTouched.value[field] || groupSubmitAttempted.value ? msg : '')
const showGroupItemError = (idx, field, msg) => {
  const isTouched = groupItemTouched.value[idx]?.[field]
  return (isTouched || groupSubmitAttempted.value) ? msg : ''
}

const openGroupCreate = () => {
  editingGroupId.value = null
  groupForm.value = {
    name: '',
    items: [{ id: null, name: '', surchargePrice: 0, surchargeDisplay: '0' }]
  }
  groupItemNameRefs.value = []
  groupTouched.value = { name: false }
  groupItemTouched.value = [{ name: false, price: false }]
  groupSubmitAttempted.value = false
  isGroupDrawerOpen.value = true
}

const openGroupEdit = (group) => {
  editingGroupId.value = group.id
  const rawItems = group.items && group.items.length > 0 ? group.items : [{ id: null, name: '', surchargePrice: 0 }]
  groupForm.value = {
    name: group.name || '',
    items: rawItems.map(i => ({
      id: i.id || null,
      name: i.name || '',
      surchargePrice: Number(i.surchargePrice) || 0,
      surchargeDisplay: (Number(i.surchargePrice) || 0).toLocaleString('vi-VN')
    }))
  }
  groupItemNameRefs.value = []
  groupTouched.value = { name: false }
  groupItemTouched.value = groupForm.value.items.map(() => ({ name: false, price: false }))
  groupSubmitAttempted.value = false
  isGroupDrawerOpen.value = true
}

const addGroupItem = async () => {
  if (groupForm.value.items.length >= 50) {
    toast.warning('Đã đạt giới hạn tối đa số lượng vị (tối đa 50).')
    return
  }
  groupForm.value.items.push({ id: null, name: '', surchargePrice: 0, surchargeDisplay: '0' })
  groupItemTouched.value.push({ name: false, price: false })
  await nextTick()
  const lastIndex = groupForm.value.items.length - 1
  if (groupItemNameRefs.value[lastIndex]) {
    groupItemNameRefs.value[lastIndex].focus()
  }
}

const removeGroupItem = (index) => {
  if (groupForm.value.items.length <= 1) {
    groupForm.value.items[0] = { id: null, name: '', surchargePrice: 0, surchargeDisplay: '0' }
    if (groupItemTouched.value[0]) {
      groupItemTouched.value[0] = { name: false, price: false }
    }
    return
  }
  groupForm.value.items.splice(index, 1)
  groupItemTouched.value.splice(index, 1)
  groupItemNameRefs.value.splice(index, 1)
}

const onGroupNameBlur = () => {
  groupTouched.value.name = true
  groupForm.value.name = normalizeInputText(groupForm.value.name)
}

const onGroupItemNameBlur = (idx) => {
  if (!groupItemTouched.value[idx]) groupItemTouched.value[idx] = { name: false, price: false }
  groupItemTouched.value[idx].name = true
  if (groupForm.value.items[idx]) {
    groupForm.value.items[idx].name = normalizeInputText(groupForm.value.items[idx].name)
  }
}

const onGroupItemPriceBlur = (idx) => {
  if (!groupItemTouched.value[idx]) groupItemTouched.value[idx] = { name: false, price: false }
  groupItemTouched.value[idx].price = true
}

const handleGroupSave = async () => {
  groupSubmitAttempted.value = true
  groupForm.value.name = normalizeInputText(groupForm.value.name)
  groupForm.value.items.forEach(it => {
    it.name = normalizeInputText(it.name)
  })

  // Nếu có nhiều hơn 1 dòng và có dòng hoàn toàn trống, tự động lọc bỏ
  if (groupForm.value.items.length > 1) {
    const activeItems = groupForm.value.items.filter(it => it.name || (it.surchargePrice && it.surchargePrice > 0))
    if (activeItems.length > 0) {
      groupForm.value.items = activeItems
      groupItemTouched.value = activeItems.map(() => ({ name: true, price: true }))
    }
  }

  if (!canSaveGroup.value) {
    if (groupNameError.value) {
      toast.warning(groupNameError.value)
    } else {
      const firstErr = groupItemErrors.value.find(e => Object.keys(e).length > 0)
      if (firstErr) {
        toast.warning(firstErr.name || firstErr.price || 'Vui lòng kiểm tra lại danh sách vị con.')
      } else if (groupForm.value.items.length === 0) {
        toast.warning('Cần ít nhất 1 lựa chọn vị con.')
      }
    }
    return
  }

  isSaving.value = true
  try {
    const payload = {
      name: groupForm.value.name,
      items: groupForm.value.items.map(i => ({
        id: i.id || null,
        name: i.name,
        surchargePrice: Number(i.surchargePrice) || 0
      }))
    }
    if (editingGroupId.value) {
      await fnbGroupApi.update(editingGroupId.value, payload)
      toast.success('Cập nhật kho tùy chọn thành công.')
    } else {
      await fnbGroupApi.create(payload)
      toast.success('Thêm kho tùy chọn thành công.')
    }
    isGroupDrawerOpen.value = false
    await fetchOptionGroups()
  } catch (err) {
    toast.error(friendlyError(err, 'Lưu thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const deleteGroupTarget = ref(null)
const confirmDeleteGroup = async () => {
  if (!deleteGroupTarget.value) return
  isDeleting.value = true
  try {
    await fnbGroupApi.delete(deleteGroupTarget.value.id)
    toast.success('Đã xoá kho tùy chọn.')
    deleteGroupTarget.value = null
    await fetchOptionGroups()
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể xoá kho tùy chọn.'))
    deleteGroupTarget.value = null
  } finally {
    isDeleting.value = false
  }
}

// ── Cấu hình Ô chọn (Slot) khi tạo/sửa Combo ──
const poolById = (id) => optionGroups.value.find(g => g.id === id)
const poolItems = (id) => poolById(id)?.items ?? []
const poolIsEmpty = (id) => (poolById(id)?.items?.length ?? 0) === 0

// Edge Case 3 — 'Ghost Default': vị mặc định từ API không còn trong kho (đã bị xoá)
// → trả null thay vì để id treo gây lỗi. Chỉ reset khi CHẮC CHẮN kho đã nạp items.
const resolveDefault = (optionGroupId, defaultId) => {
  if (defaultId == null) return null
  const pool = poolById(optionGroupId)
  if (pool && Array.isArray(pool.items) && !pool.items.some(i => i.id === defaultId)) return null
  return defaultId
}

const addSlot = () => {
  const firstPool = optionGroups.value[0]
  form.value.slots.push({
    slotLabel: firstPool?.name ?? '',
    optionGroupId: firstPool?.id ?? null,
    defaultOptionItemId: null,
    minChoices: 1,
    maxChoices: 1,
    _autoLabel: true // nhãn đang được tự sinh theo kho → sẽ đồng bộ khi đổi kho
  })
}
const removeSlot = (index) => {
  form.value.slots.splice(index, 1)
}
// Đổi thứ tự Ô chọn bằng mũi tên (không dùng thư viện kéo-thả để giữ code nhẹ).
const moveSlot = (index, dir) => {
  const target = index + dir
  if (target < 0 || target >= form.value.slots.length) return
  const list = form.value.slots
  ;[list[index], list[target]] = [list[target], list[index]]
}
// Khi đổi Kho: reset vị mặc định + tự điền lại nhãn nếu nhãn đang ở chế độ auto.
const onSlotPoolChange = (slot) => {
  slot.defaultOptionItemId = null
  if (slot._autoLabel) slot.slotLabel = poolById(slot.optionGroupId)?.name ?? ''
}
// Người dùng gõ tay vào nhãn → ngừng auto-sync để tôn trọng nội dung họ nhập.
const onSlotLabelInput = (slot) => { slot._autoLabel = false }

// ── Builder theo SỐ LƯỢNG (mặc định) ↔ Nâng cao (editor slot chi tiết) ──
// Nguồn sự thật khi lưu vẫn là form.slots; Builder chỉ là lớp UI thao tác trên slot.
const builderMode = ref(true)

// Slot "đồng đều" = mỗi ô chọn đúng 1 (min=max=1) → biểu diễn được bằng Builder.
const slotsAreUniform = (slots) =>
  slots.length > 0 && slots.every(s => Number(s.minChoices) === 1 && Number(s.maxChoices) === 1)

// Số ô hiện có của một kho (= số lần kho đó xuất hiện trong form.slots).
const builderPoolCount = (poolId) =>
  form.value.slots.filter(s => s.optionGroupId === poolId).length

// Đặt số lượng ô cho một kho: thêm/bớt ô NHƯNG GIỮ vị mặc định đã đặt ở các ô cũ.
// Sau đó dựng lại form.slots theo thứ tự kho trong optionGroups + đánh nhãn lại.
const setBuilderPoolCount = (pool, newCount) => {
  newCount = Math.min(10, Math.max(0, Number(newCount) || 0))
  const byPool = new Map()
  for (const s of form.value.slots) {
    if (!byPool.has(s.optionGroupId)) byPool.set(s.optionGroupId, [])
    byPool.get(s.optionGroupId).push(s)
  }
  const cur = byPool.get(pool.id) || []
  if (newCount <= cur.length) {
    byPool.set(pool.id, cur.slice(0, newCount)) // bớt: cắt các ô cuối
  } else {
    const extra = Array.from({ length: newCount - cur.length }, () => ({
      optionGroupId: pool.id, defaultOptionItemId: null,
      minChoices: 1, maxChoices: 1, _autoLabel: true, slotLabel: pool.name
    }))
    byPool.set(pool.id, [...cur, ...extra]) // thêm: nối ô mới vào cuối
  }
  const rebuilt = []
  for (const g of optionGroups.value) {
    const arr = byPool.get(g.id) || []
    arr.forEach((s, i) => {
      s.slotLabel = arr.length > 1 ? `${g.name} ${i + 1}` : g.name // nhãn tự sinh
      rebuilt.push(s)
    })
  }
  form.value.slots = rebuilt
}
const stepBuilderPool = (pool, delta) =>
  setBuilderPoolCount(pool, builderPoolCount(pool.id) + delta)

// Đổi giữa 2 chế độ. Chỉ rút gọn về Builder khi slot đang đồng đều (không mất cấu hình).
const canUseBuilder = computed(() =>
  form.value.slots.length === 0 || slotsAreUniform(form.value.slots))
const setBuilderMode = (useBuilder) => {
  if (useBuilder && !canUseBuilder.value) return // nút bị khóa; giữ chế độ nâng cao
  builderMode.value = useBuilder
}

// ── Validation real-time (chuẩn devcine-validation-principle) ──
// Trả về mảng lỗi theo từng slot; không âm thầm sửa/bỏ dữ liệu, chỉ chặn nút Lưu.
const slotErrors = computed(() => {
  if (form.value.type !== 'COMBO') return []
  return form.value.slots.map((s) => {
    const e = {}
    const pool = poolById(s.optionGroupId)
    const poolSize = pool?.items?.length ?? 0
    const min = Number(s.minChoices)
    const max = Number(s.maxChoices)
    if (!(s.slotLabel || '').trim()) e.label = 'Chưa nhập nhãn hiển thị'
    if (!s.optionGroupId) e.group = 'Chưa chọn kho tùy chọn'
    if (!Number.isFinite(min) || min < 0) e.min = 'Tối thiểu không hợp lệ'
    if (!Number.isFinite(max) || max < 1) e.max = 'Tối đa phải ≥ 1'
    if (Number.isFinite(min) && Number.isFinite(max) && min > max) e.min = 'Tối thiểu vượt tối đa'
    if (s.optionGroupId && Number.isFinite(max) && max > poolSize)
      e.max = `Vượt số vị trong kho (${poolSize})`
    // I.3 — Vị mặc định (nếu có) BẮT BUỘC thuộc kho slot đang tham chiếu.
    if (s.defaultOptionItemId != null && pool && Array.isArray(pool.items) &&
        !pool.items.some(i => i.id === s.defaultOptionItemId))
      e.default = 'Vị mặc định không còn trong kho'
    return e
  })
})
const hasSlotErrors = computed(() => slotErrors.value.some(e => Object.keys(e).length > 0))

// ── Validate chung (tên + giá gốc) ──
// UX: chỉ hiện lỗi đỏ khi ô đã "touched" (focus rồi blur) HOẶC đã bấm Lưu.
// Không báo đỏ lúc form vừa mở (pristine). Bản thân canSave vẫn chặn nút Lưu như thường.
const touched = ref({ name: false, price: false })
const submitAttempted = ref(false)
const showError = (field, msg) => ((touched.value[field] || submitAttempted.value) ? msg : '')
const resetTouched = () => { touched.value = { name: false, price: false }; submitAttempted.value = false }
const NAME_MAX = 50
const nameError = computed(() => {
  const n = (form.value.name || '')
  if (!n.trim()) return 'Chưa nhập tên món'
  if (n.trim().length > NAME_MAX) return `Tối đa ${NAME_MAX} ký tự (đang ${n.trim().length})`
  return ''
})
const priceError = computed(() => {
  const p = form.value.price
  if (p == null || p === '') return 'Chưa nhập giá gốc'
  const num = Number(p)
  if (!Number.isFinite(num) || num < 0) return 'Giá phải là số ≥ 0'
  return ''
})
const canSave = computed(() => {
  if (nameError.value || priceError.value) return false
  if (form.value.type === 'COMBO' && hasSlotErrors.value) return false
  return true
})

// ── Preview giá: base + phụ thu tối thiểu / tối đa khách có thể chọn ──
const pricePreview = computed(() => {
  const base = Number(form.value.price) || 0
  if (form.value.type !== 'COMBO' || form.value.slots.length === 0)
    return { min: base, max: base, hasRange: false }
  let minAdd = 0, maxAdd = 0
  for (const s of form.value.slots) {
    const surcharges = poolItems(s.optionGroupId)
      .map(i => Number(i.surchargePrice) || 0)
      .sort((a, b) => a - b)
    const n = surcharges.length
    const min = Math.max(0, Math.min(Number(s.minChoices) || 0, n))
    const max = Math.max(0, Math.min(Number(s.maxChoices) || 0, n))
    for (let k = 0; k < min; k++) minAdd += surcharges[k]          // rẻ nhất
    for (let k = 0; k < max; k++) maxAdd += surcharges[n - 1 - k]  // đắt nhất
  }
  const min = base + minAdd
  const max = base + maxAdd
  return { min, max, hasRange: min !== max }
})

// ── Tạo Kho tùy chọn ngay trong form (inline) ──
// ── Tạo Kho tùy chọn ngay trong form (inline) ──
const isPoolCreateOpen = ref(false)
const isPoolCreating = ref(false)
const poolCreateTargetSlot = ref(null)
const poolCreateForm = ref({ name: '', items: [] })
const inlinePoolItemNameRefs = ref([])
const poolCreateTouched = ref({ name: false })
const poolCreateItemTouched = ref([])
const poolCreateSubmitAttempted = ref(false)

const poolCreateNameError = computed(() => {
  const n = (poolCreateForm.value.name || '').trim()
  if (!n) return 'Vui lòng nhập tên kho tùy chọn'
  if (n.length < 2 || n.length > 100) return 'Tên kho phải từ 2 đến 100 ký tự'
  if (/<|>|javascript:/i.test(poolCreateForm.value.name || '')) return 'Tên kho chứa ký tự không hợp lệ'
  const dup = optionGroups.value.some(g => (g.name || '').trim().toLowerCase() === n.toLowerCase())
  if (dup) return 'Tên kho tùy chọn đã tồn tại'
  return ''
})

const poolCreateItemErrors = computed(() => {
  const items = poolCreateForm.value.items
  return items.map(item => {
    const e = {}
    const n = (item.name || '').trim()
    if (!n) {
      e.name = 'Vui lòng nhập tên vị'
    } else if (n.length > 50) {
      e.name = 'Tên vị không được quá 50 ký tự'
    } else {
      const count = items.filter(it => (it.name || '').trim().toLowerCase() === n.toLowerCase()).length
      if (count > 1) e.name = 'Tên vị này đã tồn tại trong danh sách'
    }

    const p = item.surchargePrice
    if (p == null || p === '') {
      e.price = 'Giá không hợp lệ'
    } else {
      const num = Number(p)
      if (!Number.isInteger(num) || num < 0) e.price = 'Giá phụ thu không được là số âm'
      else if (num > 100000000) e.price = 'Giá tối đa không vượt quá 100.000.000đ'
    }
    return e
  })
})

const canSaveInlinePool = computed(() => {
  if (poolCreateNameError.value) return false
  if (poolCreateForm.value.items.length < 1 || poolCreateForm.value.items.length > 50) return false
  return poolCreateItemErrors.value.every(e => Object.keys(e).length === 0)
})

const showPoolCreateError = (field, msg) => (poolCreateTouched.value[field] || poolCreateSubmitAttempted.value ? msg : '')
const showPoolCreateItemError = (idx, field, msg) => {
  const isTouched = poolCreateItemTouched.value[idx]?.[field]
  return (isTouched || poolCreateSubmitAttempted.value) ? msg : ''
}

const openInlinePoolCreate = (slot) => {
  poolCreateTargetSlot.value = slot
  poolCreateForm.value = {
    name: '',
    items: [{ name: '', surchargePrice: 0, surchargeDisplay: '0' }]
  }
  inlinePoolItemNameRefs.value = []
  poolCreateTouched.value = { name: false }
  poolCreateItemTouched.value = [{ name: false, price: false }]
  poolCreateSubmitAttempted.value = false
  isPoolCreateOpen.value = true
}

const addInlinePoolItem = async () => {
  if (poolCreateForm.value.items.length >= 50) {
    toast.warning('Đã đạt giới hạn tối đa số lượng vị (tối đa 50).')
    return
  }
  poolCreateForm.value.items.push({ name: '', surchargePrice: 0, surchargeDisplay: '0' })
  poolCreateItemTouched.value.push({ name: false, price: false })
  await nextTick()
  const lastIndex = poolCreateForm.value.items.length - 1
  if (inlinePoolItemNameRefs.value[lastIndex]) {
    inlinePoolItemNameRefs.value[lastIndex].focus()
  }
}

const removeInlinePoolItem = (i) => {
  if (poolCreateForm.value.items.length <= 1) {
    poolCreateForm.value.items[0] = { name: '', surchargePrice: 0, surchargeDisplay: '0' }
    if (poolCreateItemTouched.value[0]) {
      poolCreateItemTouched.value[0] = { name: false, price: false }
    }
    return
  }
  poolCreateForm.value.items.splice(i, 1)
  poolCreateItemTouched.value.splice(i, 1)
  inlinePoolItemNameRefs.value.splice(i, 1)
}

const onPoolCreateNameBlur = () => {
  poolCreateTouched.value.name = true
  poolCreateForm.value.name = normalizeInputText(poolCreateForm.value.name)
}

const onPoolCreateItemNameBlur = (idx) => {
  if (!poolCreateItemTouched.value[idx]) poolCreateItemTouched.value[idx] = { name: false, price: false }
  poolCreateItemTouched.value[idx].name = true
  if (poolCreateForm.value.items[idx]) {
    poolCreateForm.value.items[idx].name = normalizeInputText(poolCreateForm.value.items[idx].name)
  }
}

const onPoolCreateItemPriceBlur = (idx) => {
  if (!poolCreateItemTouched.value[idx]) poolCreateItemTouched.value[idx] = { name: false, price: false }
  poolCreateItemTouched.value[idx].price = true
}

const saveInlinePool = async () => {
  poolCreateSubmitAttempted.value = true
  poolCreateForm.value.name = normalizeInputText(poolCreateForm.value.name)
  poolCreateForm.value.items.forEach(it => {
    it.name = normalizeInputText(it.name)
  })

  if (poolCreateForm.value.items.length > 1) {
    const activeItems = poolCreateForm.value.items.filter(it => it.name || (it.surchargePrice && it.surchargePrice > 0))
    if (activeItems.length > 0) {
      poolCreateForm.value.items = activeItems
      poolCreateItemTouched.value = activeItems.map(() => ({ name: true, price: true }))
    }
  }

  if (!canSaveInlinePool.value) {
    if (poolCreateNameError.value) {
      toast.warning(poolCreateNameError.value)
    } else {
      const firstItemErr = poolCreateItemErrors.value.find(e => Object.keys(e).length > 0)
      if (firstItemErr) {
        toast.warning(firstItemErr.name || firstItemErr.price || 'Vui lòng kiểm tra lại danh sách vị con.')
      } else if (poolCreateForm.value.items.length === 0) {
        toast.warning('Cần ít nhất 1 lựa chọn vị con.')
      }
    }
    return
  }

  isPoolCreating.value = true
  try {
    const payload = {
      name: poolCreateForm.value.name,
      items: poolCreateForm.value.items.map(i => ({
        name: i.name,
        surchargePrice: Number(i.surchargePrice) || 0
      }))
    }
    const { data } = await fnbGroupApi.create(payload)
    const created = data.data ?? data
    await fetchOptionGroups()
    // Tự gán kho vừa tạo vào đúng slot đang mở + đồng bộ nhãn.
    const slot = poolCreateTargetSlot.value
    if (slot && created?.id) {
      slot.optionGroupId = created.id
      slot.defaultOptionItemId = null
      if (slot._autoLabel) slot.slotLabel = created.name
    }
    toast.success('Đã tạo kho tùy chọn.')
    isPoolCreateOpen.value = false
  } catch (err) {
    toast.error(friendlyError(err, 'Tạo kho thất bại.'))
  } finally {
    isPoolCreating.value = false
  }
}

const formatPrice = (n) => (n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : '')

onMounted(() => {
  fetchItems()
  fetchOptionGroups()
})

</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end flex-wrap gap-4">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Thực đơn F&B / Combo</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Combo bắp nước & đồ ăn khách chọn khi đặt vé</p>
      </div>
      <button v-if="can('fnb_menu', 'add')" @click="activeTab === 'items' ? openCreate() : openGroupCreate()" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
        <span class="material-symbols-outlined text-sm">add</span> {{ activeTab === 'items' ? 'Thêm món / combo' : 'Thêm kho tùy chọn' }}
      </button>
    </header>

    <!-- Sub-tabs -->
    <div class="flex bg-surface-container-high p-1 rounded-xl w-fit">
      <button 
        @click="activeTab = 'items'" 
        :class="activeTab === 'items' ? 'bg-primary text-on-primary shadow-md' : 'text-on-surface-variant hover:text-on-surface hover:bg-white/5'"
        class="px-6 py-2 rounded-lg font-bold text-sm uppercase tracking-widest transition-all"
      >
        Thực đơn & Combo
      </button>
      <button 
        @click="activeTab = 'groups'" 
        :class="activeTab === 'groups' ? 'bg-primary text-on-primary shadow-md' : 'text-on-surface-variant hover:text-on-surface hover:bg-white/5'"
        class="px-6 py-2 rounded-lg font-bold text-sm uppercase tracking-widest transition-all"
      >
        Kho Tùy Chọn (Pools)
      </button>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="i in 6" :key="i" class="h-40 bg-surface-container-low rounded-2xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
      <span class="material-symbols-outlined">error</span> {{ error }}
    </div>

    <template v-else-if="activeTab === 'items'">
      <!-- Empty -->
      <div v-if="items.length === 0" class="py-24 text-center border border-dashed border-white/10 rounded-2xl">
        <span class="material-symbols-outlined text-5xl text-neutral-600 mb-4">fastfood</span>
        <p class="text-neutral-400 font-semibold">Chưa có món nào. Bấm "Thêm món / combo".</p>
      </div>

      <!-- Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="item in items" :key="item.id" class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl flex flex-col">
          <div class="h-36 bg-surface-container-high relative overflow-hidden">
            <img v-if="item.imageUrl" :src="item.imageUrl" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-on-surface-variant/40">
              <span class="material-symbols-outlined text-5xl">fastfood</span>
            </div>
            <span :class="item.isActive !== false ? 'bg-green-500/80 text-white' : 'bg-neutral-600/80 text-white'" class="absolute top-3 left-3 text-[9px] font-bold uppercase tracking-widest px-2 py-1 rounded">
              {{ item.isActive !== false ? 'Đang bán' : 'Đang ẩn' }}
            </span>
            <span class="absolute top-3 right-3 text-[9px] font-bold uppercase tracking-widest px-2 py-1 rounded bg-black/50 text-primary">{{ typeLabel(item.type) }}</span>
          </div>
          <div class="p-5 flex flex-col flex-grow">
            <div class="flex justify-between items-start gap-2 mb-1">
              <h3 class="text-base font-black text-on-surface uppercase italic">{{ item.name }}</h3>
              <span class="text-sm font-black text-primary whitespace-nowrap">{{ formatPrice(item.price) }}</span>
            </div>
            <p class="text-xs text-on-surface-variant line-clamp-2 flex-grow">{{ item.description || 'Chưa có mô tả' }}</p>
            <div class="flex justify-end items-center gap-2 pt-4 mt-3 border-t border-outline-variant/5">
              <button v-if="can('fnb_menu', 'edit')" @click="toggleActive(item)" class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary px-2 py-1 transition-colors">
                {{ item.isActive !== false ? 'Ẩn' : 'Hiện' }}
              </button>
              <button v-if="can('fnb_menu', 'edit')" @click="openEdit(item)" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button v-if="can('fnb_menu', 'delete')" @click="deleteTarget = item" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>
    
    <template v-else-if="activeTab === 'groups'">
      <div v-if="optionGroups.length === 0" class="py-24 text-center border border-dashed border-white/10 rounded-2xl">
        <span class="material-symbols-outlined text-5xl text-neutral-600 mb-4">list_alt</span>
        <p class="text-neutral-400 font-semibold">Chưa có kho tùy chọn nào.</p>
      </div>
      
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="group in optionGroups" :key="group.id" class="bg-surface-container-low border border-outline-variant/10 rounded-2xl p-5 shadow-xl flex flex-col space-y-4">
          <div class="flex justify-between items-start">
            <div>
              <h3 class="text-lg font-black text-primary uppercase">{{ group.name }}</h3>
              <p class="text-xs text-on-surface-variant mt-1 font-bold">
                {{ (group.items || []).length }} lựa chọn · Kho dùng chung
              </p>
            </div>
            <div class="flex gap-2">
              <button v-if="can('fnb_menu', 'edit')" @click="openGroupEdit(group)" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button v-if="can('fnb_menu', 'delete')" @click="deleteGroupTarget = group" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
          
          <div class="flex-grow space-y-2">
            <div v-for="item in group.items" :key="item.id" class="flex justify-between items-center text-sm p-2 bg-surface-container-highest rounded-lg border border-outline-variant/5">
              <span class="text-on-surface font-semibold">{{ item.name }}</span>
              <span v-if="item.surchargePrice > 0" class="text-primary font-bold">+{{ formatPrice(item.surchargePrice) }}</span>
              <span v-else class="text-on-surface-variant text-xs">Miễn phí</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Drawer Form -->
    <div v-if="isDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isDrawerOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h3 class="font-headline font-black uppercase italic text-primary text-xl">{{ editingId ? 'Sửa món / combo' : 'Thêm món / combo' }}</h3>
          <button @click="isDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-5">
          <!-- Ảnh -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Hình ảnh</label>
            <div class="w-full h-36 bg-surface-container-highest border-2 border-dashed border-outline-variant/20 rounded-xl flex items-center justify-center overflow-hidden relative">
              <img v-if="form.imageUrl" :src="form.imageUrl" class="w-full h-full object-cover" />
              <div v-else class="text-on-surface-variant/50 flex flex-col items-center">
                <span class="material-symbols-outlined text-3xl mb-1">cloud_upload</span>
                <span class="text-[10px] font-bold uppercase tracking-widest">{{ isUploading ? 'Đang tải...' : 'Tải ảnh lên' }}</span>
              </div>
              <input type="file" accept="image/*" @change="handleUpload" class="absolute inset-0 opacity-0 cursor-pointer" :disabled="isUploading" />
            </div>
          </div>

          <div class="space-y-1.5">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên món / combo</label>
              <span class="text-[10px] font-semibold tabular-nums" :class="(form.name || '').length >= 50 ? 'text-amber-400' : 'text-on-surface-variant/60'">{{ (form.name || '').length }}/50</span>
            </div>
            <input
              v-model="form.name" maxlength="50" @blur="touched.name = true"
              class="w-full bg-surface-container-highest border p-3 rounded-lg text-sm font-bold text-on-surface focus:border-primary outline-none"
              :class="showError('name', nameError) ? 'border-red-500/60' : 'border-outline-variant/20'"
              placeholder="VD: Combo Couple"
            />
            <p v-if="showError('name', nameError)" class="text-[10px] text-red-400 font-semibold">{{ nameError }}</p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Loại</label>
              <select v-model="form.type" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-lg text-sm font-bold text-on-surface focus:border-primary outline-none">
                <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div class="space-y-1.5">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá gốc (VNĐ)</label>
              <div class="relative">
                <input
                  :value="fmtThousand(form.price)"
                  @input="handlePriceInput"
                  @blur="touched.price = true"
                  type="text"
                  inputmode="numeric"
                  class="w-full bg-surface-container-highest border p-3 pr-10 rounded-lg text-sm font-bold text-on-surface focus:border-primary outline-none tabular-nums"
                  :class="showError('price', priceError) ? 'border-red-500/60' : 'border-outline-variant/20'"
                  placeholder="VD: 89.000"
                />
                <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant/60 pointer-events-none select-none">đ</span>
              </div>
            </div>
          </div>
          <p v-if="showError('price', priceError)" class="text-[10px] text-red-400 font-semibold -mt-2">{{ priceError }}</p>

          <div class="space-y-1.5">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả</label>
              <span class="text-[10px] font-semibold tabular-nums text-on-surface-variant/60">{{ (form.description || '').length }}/255</span>
            </div>
            <textarea v-model="form.description" rows="3" maxlength="255" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-lg text-sm text-on-surface focus:border-primary outline-none resize-none" placeholder="VD: 1 bắp lớn + 2 nước ngọt"></textarea>
          </div>

          <!-- Thành phần / Ô chọn — CHỈ hiện cho COMBO -->
          <div v-if="form.type === 'COMBO'" class="space-y-3 pt-5 border-t border-outline-variant/10">
            <div class="flex justify-between items-center gap-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-primary">Thành phần combo</label>
              <!-- Toggle chế độ: Số lượng (Builder) ↔ Nâng cao -->
              <div class="flex bg-surface-container-high p-0.5 rounded-lg text-[10px] font-bold uppercase tracking-wider">
                <button
                  @click="setBuilderMode(true)" :disabled="!canUseBuilder"
                  :class="builderMode ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-on-surface'"
                  class="px-2.5 py-1 rounded-md transition-all disabled:opacity-30 disabled:cursor-not-allowed"
                  :title="canUseBuilder ? '' : 'Combo có cấu hình nâng cao (mix vị / tùy chọn), không thể rút gọn về số lượng'"
                >Số lượng</button>
                <button
                  @click="setBuilderMode(false)"
                  :class="!builderMode ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-on-surface'"
                  class="px-2.5 py-1 rounded-md transition-all"
                >Nâng cao</button>
              </div>
            </div>

            <!-- ── CHẾ ĐỘ BUILDER (số lượng) ── -->
            <template v-if="builderMode">
              <p v-if="optionGroups.length === 0" class="text-xs text-on-surface-variant italic">
                Chưa có Kho tùy chọn nào. Tạo kho ở tab "Kho Tùy Chọn (Pools)" trước.
              </p>

              <template v-else>
                <!-- Bước 1: khai báo số lượng mỗi kho (0 = không có trong combo) -->
                <div class="space-y-2">
                  <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số lượng mỗi loại</p>
                  <div
                    v-for="g in optionGroups" :key="g.id"
                    class="flex items-center justify-between gap-3 bg-surface-container-highest p-3 rounded-xl border border-outline-variant/20"
                  >
                    <div class="min-w-0">
                      <span class="text-sm font-bold text-on-surface truncate block">{{ g.name }} <span class="text-on-surface-variant font-medium">({{ (g.items || []).length }} vị)</span></span>
                      <span v-if="poolIsEmpty(g.id)" class="text-[10px] text-amber-400 font-semibold flex items-center gap-0.5">
                        <span class="material-symbols-outlined text-xs">warning</span> Kho này chưa có món
                      </span>
                    </div>
                    <div class="flex items-center gap-1 shrink-0">
                      <button @click="stepBuilderPool(g, -1)" :disabled="builderPoolCount(g.id) <= 0" class="w-8 h-8 rounded-lg bg-surface-container hover:bg-white/10 text-on-surface flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                        <span class="material-symbols-outlined text-sm">remove</span>
                      </button>
                      <span class="w-7 text-center text-sm font-black text-on-surface tabular-nums">{{ builderPoolCount(g.id) }}</span>
                      <button @click="stepBuilderPool(g, 1)" :disabled="builderPoolCount(g.id) >= 10 || poolIsEmpty(g.id)" :title="poolIsEmpty(g.id) ? 'Kho chưa có món — thêm vị trước' : ''" class="w-8 h-8 rounded-lg bg-surface-container hover:bg-white/10 text-on-surface flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                        <span class="material-symbols-outlined text-sm">add</span>
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Bước 2: các ô chọn sinh ra tương ứng — đặt vị mặc định cho mỗi ô -->
                <div v-if="form.slots.length" class="space-y-2 pt-2">
                  <p class="text-[10px] font-bold uppercase tracking-widest text-primary">Các ô khách sẽ chọn ({{ form.slots.length }})</p>
                  <div
                    v-for="(slot, idx) in form.slots" :key="idx"
                    class="bg-surface-container-highest p-3 rounded-xl border border-outline-variant/20 space-y-2"
                  >
                    <div class="flex items-center gap-2">
                      <span class="material-symbols-outlined text-sm text-primary">tune</span>
                      <span class="text-sm font-bold text-on-surface">{{ slot.slotLabel }}</span>
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Vị mặc định</label>
                      <select v-model="slot.defaultOptionItemId" class="w-full bg-surface-container border border-outline-variant/20 p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none">
                        <option :value="null">-- Không chọn sẵn (khách tự chọn) --</option>
                        <option v-for="i in poolItems(slot.optionGroupId)" :key="i.id" :value="i.id">{{ i.name }}{{ Number(i.surchargePrice) > 0 ? ` (+${formatPrice(i.surchargePrice)})` : '' }}</option>
                      </select>
                    </div>
                  </div>
                </div>
                <p v-else class="text-xs text-on-surface-variant italic">
                  Chưa đặt số lượng nào — combo sẽ là món cố định (khách không chọn vị).
                </p>
              </template>
            </template>

            <!-- ── CHẾ ĐỘ NÂNG CAO (editor slot chi tiết) ── -->
            <template v-else>
            <div class="flex justify-end">
              <button @click="addSlot" class="text-xs font-bold text-on-surface-variant hover:text-primary transition-colors flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">add_circle</span> Thêm ô chọn
              </button>
            </div>

            <p v-if="form.slots.length === 0" class="text-xs text-on-surface-variant italic">
              Combo chưa có ô chọn nào (khách nhận đúng combo cố định, không chọn vị).
            </p>

            <div
              v-for="(slot, idx) in form.slots"
              :key="idx"
              class="bg-surface-container-highest p-4 rounded-xl border space-y-3"
              :class="Object.keys(slotErrors[idx] || {}).length ? 'border-red-500/50' : 'border-outline-variant/20'"
            >
              <div class="flex justify-between items-center">
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ô chọn #{{ idx + 1 }}</span>
                <div class="flex items-center gap-1">
                  <button @click="moveSlot(idx, -1)" :disabled="idx === 0" class="w-7 h-7 rounded-lg hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed" title="Lên">
                    <span class="material-symbols-outlined text-sm">arrow_upward</span>
                  </button>
                  <button @click="moveSlot(idx, 1)" :disabled="idx === form.slots.length - 1" class="w-7 h-7 rounded-lg hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed" title="Xuống">
                    <span class="material-symbols-outlined text-sm">arrow_downward</span>
                  </button>
                  <button @click="removeSlot(idx)" class="w-7 h-7 rounded-lg hover:bg-red-500/20 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors" title="Xoá">
                    <span class="material-symbols-outlined text-sm">close</span>
                  </button>
                </div>
              </div>

              <div class="space-y-1.5">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Nhãn hiển thị</label>
                <input
                  v-model="slot.slotLabel" @input="onSlotLabelInput(slot)"
                  class="w-full bg-surface-container border p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                  :class="slotErrors[idx]?.label ? 'border-red-500/60' : 'border-outline-variant/20'"
                  placeholder="VD: Chọn vị bắp / Nước 1"
                />
                <p v-if="slotErrors[idx]?.label" class="text-[10px] text-red-400 font-semibold">{{ slotErrors[idx].label }}</p>
              </div>

              <div class="space-y-1.5">
                <div class="flex items-center justify-between">
                  <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Kho tùy chọn (Pool)</label>
                  <button v-if="can('fnb_menu', 'add')" @click="openInlinePoolCreate(slot)" class="text-[10px] font-bold text-primary hover:brightness-110 transition-all flex items-center gap-0.5">
                    <span class="material-symbols-outlined text-xs">add</span> Tạo kho mới
                  </button>
                </div>
                <select
                  v-model="slot.optionGroupId" @change="onSlotPoolChange(slot)"
                  class="w-full bg-surface-container border p-2.5 rounded-lg text-sm font-bold text-on-surface focus:border-primary outline-none"
                  :class="slotErrors[idx]?.group ? 'border-red-500/60' : 'border-outline-variant/20'"
                >
                  <option :value="null" disabled>-- Chọn kho --</option>
                  <option v-for="g in optionGroups" :key="g.id" :value="g.id">{{ g.name }} ({{ (g.items || []).length }} vị)</option>
                </select>
                <p v-if="slotErrors[idx]?.group" class="text-[10px] text-red-400 font-semibold">{{ slotErrors[idx].group }}</p>
              </div>

              <div class="space-y-1.5" v-if="slot.optionGroupId">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Vị mặc định</label>
                <select
                  v-model="slot.defaultOptionItemId"
                  class="w-full bg-surface-container border p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                  :class="slotErrors[idx]?.default ? 'border-red-500/60' : 'border-outline-variant/20'"
                >
                  <option :value="null">-- Không chọn sẵn --</option>
                  <option v-for="i in poolItems(slot.optionGroupId)" :key="i.id" :value="i.id">{{ i.name }}{{ Number(i.surchargePrice) > 0 ? ` (+${formatPrice(i.surchargePrice)})` : '' }}</option>
                </select>
                <p v-if="slotErrors[idx]?.default" class="text-[10px] text-red-400 font-semibold">{{ slotErrors[idx].default }}</p>
              </div>

              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-1.5">
                  <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chọn tối thiểu</label>
                  <input
                    v-model.number="slot.minChoices" type="number" min="0"
                    class="w-full bg-surface-container border p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                    :class="slotErrors[idx]?.min ? 'border-red-500/60' : 'border-outline-variant/20'"
                  />
                  <p v-if="slotErrors[idx]?.min" class="text-[10px] text-red-400 font-semibold">{{ slotErrors[idx].min }}</p>
                </div>
                <div class="space-y-1.5">
                  <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chọn tối đa</label>
                  <input
                    v-model.number="slot.maxChoices" type="number" min="1"
                    class="w-full bg-surface-container border p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                    :class="slotErrors[idx]?.max ? 'border-red-500/60' : 'border-outline-variant/20'"
                  />
                  <p v-if="slotErrors[idx]?.max" class="text-[10px] text-red-400 font-semibold">{{ slotErrors[idx].max }}</p>
                </div>
              </div>

              <p class="text-[10px] text-on-surface-variant italic">
                {{ Number(slot.minChoices) > 0 ? 'Bắt buộc chọn (tối thiểu ≥ 1).' : 'Không bắt buộc — khách có thể bỏ qua ô này.' }}
              </p>
            </div>
            </template>
          </div>

          <!-- Preview giá: base + phụ thu min/max -->
          <div class="p-4 rounded-xl border border-primary/20 bg-primary/5 space-y-1">
            <p class="text-[10px] font-bold uppercase tracking-widest text-primary">Giá khách trả (tạm tính)</p>
            <p class="text-sm font-black text-on-surface">
              <template v-if="pricePreview.hasRange">
                Từ {{ formatPrice(pricePreview.min) }} <span class="text-on-surface-variant font-semibold">đến</span> {{ formatPrice(pricePreview.max) }}
              </template>
              <template v-else>Giá: {{ formatPrice(pricePreview.min) }}</template>
            </p>
            <p v-if="pricePreview.hasRange" class="text-[10px] text-on-surface-variant">Đã gồm phụ thu các vị khách có thể chọn.</p>
          </div>

          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Đang bán (hiện cho khách)</p>
            <button @click="form.isActive = !form.isActive" :class="form.isActive ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors shrink-0">
              <span :class="form.isActive ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform shadow-md absolute top-0.5 left-0.5"></span>
            </button>
          </div>
        </div>

        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest space-y-3">
          <p v-if="!canSave && form.type === 'COMBO' && hasSlotErrors" class="text-[10px] text-red-400 font-semibold text-center">
            Còn Ô chọn báo lỗi — hãy sửa trước khi lưu.
          </p>
          <div class="flex gap-3">
            <button @click="isDrawerOpen = false" class="flex-1 px-6 py-3 rounded-lg border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy</button>
            <button @click="handleSave" :disabled="isSaving || !canSave" class="flex-1 px-6 py-3 rounded-lg bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100">{{ isSaving ? 'Đang lưu...' : 'Lưu' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Inline: Tạo Kho tùy chọn ngay trong form Combo (đè lên drawer) -->
    <div v-if="isPoolCreateOpen" class="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="isPoolCreateOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low border border-outline-variant/20 rounded-xl shadow-2xl flex flex-col max-h-[85vh]">
        <div class="p-5 border-b border-outline-variant/10 flex justify-between items-center">
          <h3 class="font-headline font-black uppercase italic text-primary text-lg">Tạo kho tùy chọn</h3>
          <button @click="isPoolCreateOpen = false" class="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-5 space-y-4">
          <div class="space-y-1.5">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên kho tùy chọn</label>
              <span class="text-[10px] font-semibold tabular-nums" :class="(poolCreateForm.name || '').length >= 100 ? 'text-amber-400' : 'text-on-surface-variant/60'">{{ (poolCreateForm.name || '').length }}/100</span>
            </div>
            <input
              v-model="poolCreateForm.name"
              maxlength="100"
              @blur="onPoolCreateNameBlur"
              class="w-full bg-surface-container-highest border p-3 rounded-lg text-sm font-bold text-on-surface focus:border-primary outline-none"
              :class="showPoolCreateError('name', poolCreateNameError) ? 'border-red-500/60' : 'border-outline-variant/20'"
              placeholder="VD: Tùy Chọn Nước"
            />
            <p v-if="showPoolCreateError('name', poolCreateNameError)" class="text-[10px] text-red-400 font-semibold">{{ poolCreateNameError }}</p>
          </div>

          <div class="space-y-3 pt-3 border-t border-outline-variant/10">
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-primary">Danh sách vị</label>
                <span class="text-[10px] text-on-surface-variant font-medium">({{ poolCreateForm.items.length }}/50)</span>
              </div>
              <button
                @click="addInlinePoolItem"
                :disabled="poolCreateForm.items.length >= 50"
                class="text-xs font-bold text-on-surface-variant hover:text-primary transition-colors flex items-center gap-1 disabled:opacity-40 disabled:cursor-not-allowed"
                :title="poolCreateForm.items.length >= 50 ? 'Đã đạt tối đa 50 vị' : ''"
              >
                <span class="material-symbols-outlined text-sm">add_circle</span> Thêm vị
              </button>
            </div>
            <div
              v-for="(it, i) in poolCreateForm.items"
              :key="i"
              class="flex gap-2 items-center bg-surface-container-highest p-3 rounded-lg border"
              :class="(showPoolCreateItemError(i, 'name', poolCreateItemErrors[i]?.name) || showPoolCreateItemError(i, 'price', poolCreateItemErrors[i]?.price)) ? 'border-red-500/50' : 'border-outline-variant/20'"
            >
              <div class="flex-grow space-y-2">
                <div class="space-y-1">
                  <input
                    :ref="el => { if (el) inlinePoolItemNameRefs[i] = el }"
                    v-model="it.name"
                    maxlength="50"
                    @blur="onPoolCreateItemNameBlur(i)"
                    class="w-full bg-surface-container border p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                    :class="showPoolCreateItemError(i, 'name', poolCreateItemErrors[i]?.name) ? 'border-red-500/60' : 'border-outline-variant/20'"
                    placeholder="Tên vị (VD: Pepsi 32oz)"
                  />
                  <p v-if="showPoolCreateItemError(i, 'name', poolCreateItemErrors[i]?.name)" class="text-[10px] text-red-400 font-semibold">{{ poolCreateItemErrors[i].name }}</p>
                </div>

                <div class="space-y-1">
                  <input
                    :value="it.surchargeDisplay ?? (it.surchargePrice != null ? Number(it.surchargePrice).toLocaleString('vi-VN') : '0')"
                    @input="handleCurrencyInput($event, it)"
                    @paste="handleCurrencyPaste($event, it)"
                    @blur="onPoolCreateItemPriceBlur(i)"
                    class="w-full bg-surface-container border p-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                    :class="showPoolCreateItemError(i, 'price', poolCreateItemErrors[i]?.price) ? 'border-red-500/60' : 'border-outline-variant/20'"
                    placeholder="Phụ thu (VD: 10.000)"
                  />
                  <p v-if="showPoolCreateItemError(i, 'price', poolCreateItemErrors[i]?.price)" class="text-[10px] text-red-400 font-semibold">{{ poolCreateItemErrors[i].price }}</p>
                </div>
              </div>
              <button
                @click="removeInlinePoolItem(i)"
                class="w-8 h-8 rounded-lg hover:bg-red-500/20 text-on-surface-variant hover:text-red-400 flex flex-shrink-0 items-center justify-center transition-colors"
                :title="poolCreateForm.items.length <= 1 ? 'Xóa nội dung' : 'Xóa vị này'"
              >
                <span class="material-symbols-outlined text-sm">close</span>
              </button>
            </div>
          </div>
        </div>

        <div class="p-5 border-t border-outline-variant/10 flex gap-3">
          <button @click="isPoolCreateOpen = false" class="flex-1 px-6 py-3 rounded-lg border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy</button>
          <button @click="saveInlinePool" :disabled="isPoolCreating || (poolCreateSubmitAttempted && !canSaveInlinePool)" class="flex-1 px-6 py-3 rounded-lg bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform disabled:opacity-60">{{ isPoolCreating ? 'Đang tạo...' : 'Tạo & chọn' }}</button>
        </div>
      </div>
    </div>

    <!-- Delete confirm -->
    <div v-if="deleteTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="deleteTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-red-400 mb-3">warning</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">Xoá món?</h3>
        <p class="text-sm text-on-surface-variant">Xoá "<span class="font-bold text-primary">{{ deleteTarget.name }}</span>"? Nếu món đã có trong đơn cũ, hãy dùng "Ẩn" thay vì xoá.</p>
        <div class="flex gap-3 mt-6">
          <button @click="deleteTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmDelete" :disabled="isDeleting" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isDeleting ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>

    <!-- Delete Group confirm -->
    <div v-if="deleteGroupTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="deleteGroupTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-red-400 mb-3">warning</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">Xoá Kho Tùy Chọn?</h3>
        <p class="text-sm text-on-surface-variant">Xoá "<span class="font-bold text-primary">{{ deleteGroupTarget.name }}</span>"? Nếu kho đang được dùng trong Ô chọn của combo, hệ thống sẽ chặn xoá — hãy gỡ khỏi combo trước.</p>
        <div class="flex gap-3 mt-6">
          <button @click="deleteGroupTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmDeleteGroup" :disabled="isDeleting" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isDeleting ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>

    <!-- Group Drawer Form -->
    <div v-if="isGroupDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isGroupDrawerOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h3 class="font-headline font-black uppercase italic text-primary text-xl">{{ editingGroupId ? 'Sửa Kho Tùy Chọn' : 'Thêm Kho Tùy Chọn' }}</h3>
          <button @click="isGroupDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-5">
          <div class="space-y-1.5">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên kho tùy chọn</label>
              <span class="text-[10px] font-semibold tabular-nums" :class="(groupForm.name || '').length >= 100 ? 'text-amber-400' : 'text-on-surface-variant/60'">{{ (groupForm.name || '').length }}/100</span>
            </div>
            <input
              v-model="groupForm.name"
              maxlength="100"
              @blur="onGroupNameBlur"
              class="w-full bg-surface-container-highest border p-3 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none"
              :class="showGroupError('name', groupNameError) ? 'border-red-500/60' : 'border-outline-variant/20'"
              placeholder="VD: Tùy Chọn Bắp"
            />
            <p v-if="showGroupError('name', groupNameError)" class="text-[10px] text-red-400 font-semibold">{{ groupNameError }}</p>
            <p class="text-[10px] text-on-surface-variant italic">Kho dùng chung – số lượng chọn (min/max) đặt riêng ở từng Ô chọn của combo.</p>
          </div>

          <div class="space-y-4 mt-2 pt-6 border-t border-outline-variant/10">
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-primary">Danh sách Vị con</label>
                <span class="text-[10px] text-on-surface-variant font-medium">({{ groupForm.items.length }}/50)</span>
              </div>
              <button
                @click="addGroupItem"
                :disabled="groupForm.items.length >= 50"
                class="text-xs font-bold text-on-surface-variant hover:text-primary transition-colors flex items-center gap-1 disabled:opacity-40 disabled:cursor-not-allowed"
                :title="groupForm.items.length >= 50 ? 'Đã đạt tối đa 50 vị' : ''"
              >
                <span class="material-symbols-outlined text-sm">add_circle</span> Thêm vị
              </button>
            </div>
            
            <div
              v-for="(item, idx) in groupForm.items"
              :key="idx"
              class="flex gap-2 items-center bg-surface-container-highest p-3 rounded-xl border"
              :class="(showGroupItemError(idx, 'name', groupItemErrors[idx]?.name) || showGroupItemError(idx, 'price', groupItemErrors[idx]?.price)) ? 'border-red-500/50' : 'border-outline-variant/20'"
            >
              <div class="flex-grow space-y-2">
                <div class="space-y-1">
                  <input
                    :ref="el => { if (el) groupItemNameRefs[idx] = el }"
                    v-model="item.name"
                    maxlength="50"
                    @blur="onGroupItemNameBlur(idx)"
                    class="w-full bg-surface-container border p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                    :class="showGroupItemError(idx, 'name', groupItemErrors[idx]?.name) ? 'border-red-500/60' : 'border-outline-variant/20'"
                    placeholder="Tên vị (VD: Caramel)"
                  />
                  <p v-if="showGroupItemError(idx, 'name', groupItemErrors[idx]?.name)" class="text-[10px] text-red-400 font-semibold">{{ groupItemErrors[idx].name }}</p>
                </div>

                <div class="space-y-1">
                  <input
                    :value="item.surchargeDisplay ?? (item.surchargePrice != null ? Number(item.surchargePrice).toLocaleString('vi-VN') : '0')"
                    @input="handleCurrencyInput($event, item)"
                    @paste="handleCurrencyPaste($event, item)"
                    @blur="onGroupItemPriceBlur(idx)"
                    class="w-full bg-surface-container border p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none"
                    :class="showGroupItemError(idx, 'price', groupItemErrors[idx]?.price) ? 'border-red-500/60' : 'border-outline-variant/20'"
                    placeholder="Phụ thu (VD: 10.000)"
                  />
                  <p v-if="showGroupItemError(idx, 'price', groupItemErrors[idx]?.price)" class="text-[10px] text-red-400 font-semibold">{{ groupItemErrors[idx].price }}</p>
                </div>
              </div>
              <button
                @click="removeGroupItem(idx)"
                class="w-8 h-8 rounded-full hover:bg-red-500/20 text-on-surface-variant hover:text-red-400 flex flex-shrink-0 items-center justify-center transition-colors"
                :title="groupForm.items.length <= 1 ? 'Xóa nội dung' : 'Xóa vị này'"
              >
                <span class="material-symbols-outlined text-sm">close</span>
              </button>
            </div>
          </div>
        </div>

        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isGroupDrawerOpen = false" class="flex-1 px-6 py-3 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy</button>
          <button @click="handleGroupSave" :disabled="isSaving || (groupSubmitAttempted && !canSaveGroup)" class="flex-1 px-6 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform disabled:opacity-50 disabled:cursor-not-allowed">{{ isSaving ? 'Đang lưu...' : 'Lưu' }}</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
