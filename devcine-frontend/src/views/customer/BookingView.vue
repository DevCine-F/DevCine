<script setup>
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { paymentApi, voucherApi } from '@/api/customer'
import { settingsApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { formatComboTitle } from '@/utils/format'
import { useSeatRealtime } from '@/composables/useSeatRealtime'
import { useSeatGridRender } from '@/composables/useSeatGridRender'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import FnbOptionModal from '@/components/FnbOptionModal.vue'
import SeatGridRenderer from '@/components/common/SeatGridRenderer.vue'

const store = useBookingStore()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToastStore()

// ===== Khóa ghế real-time (WebSocket/STOMP) — đồng bộ với quầy POS & khách online khác =====
const seatRealtime = useSeatRealtime({
  by: 'Khách online',
  onDenied: (seatId) => {
    const seat = store.selectedSeats.find(s => s.seatId === seatId)
    if (seat) store.toggleSeat(seat) // gỡ ghế đã giành hụt khỏi lựa chọn
    const label = seatLabel(seat)
    toast.error(`Ghế ${label} vừa được chọn hoặc đã được bán ở nơi khác. Vui lòng chọn vị trí ghế khác!`)
  },
  onSold: (seatIds) => {
    const lost = store.selectedSeats.filter(s => seatIds.includes(s.seatId))
    lost.forEach(seat => store.toggleSeat(seat))
    if (lost.length) {
      toast.error(`Ghế ${lost.map(s => seatLabel(s)).join(', ')} vừa được bán ở nơi khác — đã gỡ khỏi lựa chọn.`)
    }
  },
})
const isSeatLockedByOthers = (seat) => !!seat && seatRealtime.isLockedByOthers(seat.seatId)

// Nhãn ghế: ưu tiên label lưu ở DB (có thể được Admin sửa tay), fallback rowChar+colNum
const seatLabel = (seat) => seat ? (seat.label || (seat.rowChar + seat.colNum)) : 'này'
// Ghế khóa vật lý (bảo trì/khóa) → không cho khách chọn
const isSeatMaintenance = (seat) => !!seat && (seat.status === 'MAINTENANCE' || seat.status === 'LOCKED' || seat.seatStatus === 'MAINTENANCE' || seat.seatStatus === 'LOCKED')

const paymentMethod = ref('VNPAY')

// ===== Điều hướng wizard từng bước =====
const currentStep = ref(1)
const held = ref(false)      // đã giữ ghế (tạo đơn) cho lựa chọn hiện tại chưa
const holding = ref(false)   // đang giữ ghế ở nền
const isPaying = ref(false)  // đang xử lý thanh toán
const steps = [
  { id: 1, label: 'Chọn ghế', icon: 'event_seat' },
  { id: 2, label: 'Combo', icon: 'fastfood' },
  { id: 3, label: 'Ưu đãi', icon: 'local_activity' },
  { id: 4, label: 'Thanh toán', icon: 'payments' },
]

// Tiêu đề + mô tả của từng bước (render ở header chung phía trên)
const stepMeta = computed(() => ({
  1: { title: '01. Chọn Chỗ Ngồi', desc: '' },
  2: { title: '02. Combo - Đồ Ăn & Nước Uống', desc: 'Chọn combo bắp nước & đồ ăn kèm cho buổi xem phim (không bắt buộc)' },
  3: { title: '03. Ưu Đãi / Mã Giảm Giá', desc: 'Chọn voucher sẵn có hoặc nhập mã giảm giá' },
  4: { title: '04. Phương Thức Thanh Toán', desc: 'Chọn phương thức thanh toán phù hợp nhất' },
}[currentStep.value]))

// Bước 1: phải chọn số lượng vé > 0 và chọn ĐÚNG số ghế bằng tổng số vé
const canProceed = computed(() => {
  if (currentStep.value === 1) {
    const seatsSelectedTickets = store.selectedSeats.reduce((acc, seat) => acc + (seat.seatType === 'SWEETBOX' ? 2 : 1), 0);
    return store.totalTickets > 0 && seatsSelectedTickets === store.totalTickets;
  }
  return true
})

const scrollTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

// Bắt buộc đăng nhập trước khi rời bước chọn ghế. Store Pinia giữ nguyên suất/ghế đã chọn
// qua điều hướng SPA, nên sau khi đăng nhập quay lại sẽ vào đúng bước 2 với ghế còn nguyên.
const showLoginPrompt = ref(false)
const ensureAuthForBooking = () => {
  if (authStore.isAuthenticated) return true
  showLoginPrompt.value = true // hiện modal nhắc đăng nhập thay vì chuyển thẳng
  return false
}
const goToLogin = () => {
  showLoginPrompt.value = false
  router.push({ name: 'login', query: { redirect: '/booking?step=2' } })
}

const validateSeatGap = () => {
  if (store.selectedSeats.length === 0) return true;
  
  const selectedIds = store.selectedSeats.map(s => s.seatId);
  const rows = {};
  store.availableSeats.forEach(s => {
    if (s.gridRow != null && s.gridCol != null) {
      if (!rows[s.gridRow]) rows[s.gridRow] = [];
      rows[s.gridRow].push(s);
    }
  });

  for (const rowKey in rows) {
    const seatsInRow = rows[rowKey];
    const hasSelection = seatsInRow.some(s => selectedIds.includes(s.seatId));
    if (!hasSelection) continue;

    const maxCol = Math.max(...seatsInRow.map(s => s.gridCol));
    if (maxCol < 0) continue;

    const state = new Array(maxCol + 1).fill('X');
    seatsInRow.forEach(s => {
      const col = s.gridCol;
      if (s.status !== 'AVAILABLE' && s.seatStatus !== 'AVAILABLE') {
        state[col] = 'X';
      } else if (selectedIds.includes(s.seatId)) {
        state[col] = 'S';
      } else if (isSeatLockedByOthers(s) || (s.status === 'HOLD' || s.status === 'SOLD')) {
        state[col] = 'O';
      } else {
        state[col] = 'E';
      }

      if (s.seatType === 'SWEETBOX' && col + 1 <= maxCol) {
        state[col + 1] = 'X';
      }
    });

    for (let c = 0; c <= maxCol; c++) {
      if (state[c] === 'E') {
        const leftBarrier = (c === 0) || state[c - 1] !== 'E';
        const rightBarrier = (c === maxCol) || state[c + 1] !== 'E';
        if (leftBarrier && rightBarrier) {
          const causedByUser = (c > 0 && state[c - 1] === 'S') || (c < maxCol && state[c + 1] === 'S');
          if (causedByUser) {
            return false;
          }
        }
      }
    }
  }
  return true;
};

const goNext = () => {
  if (holdExpiredNow()) { handleHoldExpired(); return } // hết giờ giữ chỗ → không cho đi tiếp
  if (currentStep.value < steps.length && canProceed.value) {
    // Rời bước chọn ghế (1 → 2): yêu cầu đăng nhập, chưa đăng nhập thì dắt đi đăng nhập
    if (currentStep.value === 1) {
      if (!validateSeatGap()) {
        toast.warning('Vui lòng không để trống 1 ghế đơn lẻ bên cạnh hoặc sát lối đi');
        return;
      }
      if (!ensureAuthForBooking()) return;
    }
    currentStep.value++
    scrollTop()
  }
}
const goBack = () => {
  if (currentStep.value > 1) {
    currentStep.value--
    scrollTop()
    return
  }
  // Ở bước 1 (chọn ghế): quay về trang trước (thường là màn chọn suất/lịch chiếu);
  // nếu không có lịch sử điều hướng thì về trang Lịch chiếu.
  if (window.history.length > 1) router.back()
  else router.push('/lich-chieu')
}
const goToStep = (id) => {
  // Cho phép quay lại bất kỳ bước trước; chỉ chặn nhảy tới khi chưa chọn ghế
  if (id > currentStep.value && store.selectedSeats.length === 0) return
  // Nhảy từ bước chọn ghế sang bước sau cũng cần đăng nhập
  if (currentStep.value === 1 && id > 1 && !ensureAuthForBooking()) return
  currentStep.value = id
  scrollTop()
}

// Giữ ghế (tạo đơn) SẴN ở nền ngay khi mở bước Thanh toán → lúc bấm "Xác nhận" chỉ còn completePayment (nhanh hơn)
let holdPromise = null
const ensureHeld = async () => {
  if (held.value) return true
  if (holdExpiredNow()) { handleHoldExpired(); return false } // hết giờ → không tạo đơn giữ ghế
  if (!holdPromise) {
    holding.value = true
    holdPromise = store.holdSeatsAndProceed(paymentMethod.value).then(ok => {
      held.value = ok
      holding.value = false
      holdPromise = null
      return ok
    })
  }
  return holdPromise // người bấm "Xác nhận" sớm sẽ chờ chung promise giữ ghế đang chạy ở nền
}
watch(currentStep, (s) => { if (s === 4) ensureHeld() })
// Đổi ghế/loại vé/combo/voucher → đơn đã giữ không còn đúng, sẽ giữ lại khi vào bước 4 lần tới
watch(() => [store.selectedSeats.length, JSON.stringify(store.ticketQuantities), store.selectedFnbs.length, store.selectedVoucher?.id], () => {
  held.value = false
  store.bookingId = null
  store.heldAt = null
})

// ===== Đồng hồ đếm ngược thời gian giữ chỗ (bắt đầu từ khi chọn ghế) =====
const holdMinutes = ref(10)        // số phút giữ ghế (admin cấu hình, mặc định 10)
const nowTs = ref(Date.now())
const holdStartTs = ref(0)         // mốc bắt đầu phiên giữ chỗ (lúc chọn ghế đầu tiên)
let countdownTimer = null
let expiredHandled = false

const holdDeadline = computed(() => holdStartTs.value ? holdStartTs.value + holdMinutes.value * 60000 : 0)
const isCountingDown = computed(() => holdStartTs.value > 0)
// Đã quá hạn giữ chỗ — kiểm ĐỒNG BỘ để chặn đặt vé ngay cả khi watcher secondsLeft chưa kịp chạy (tránh race)
const holdExpiredNow = () => holdStartTs.value > 0 && Date.now() >= holdDeadline.value
const secondsLeft = computed(() => {
  if (!isCountingDown.value) return null
  return Math.max(0, Math.floor((holdDeadline.value - nowTs.value) / 1000))
})
const countdownLabel = computed(() => {
  const s = secondsLeft.value
  if (s == null) return ''
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})

// Bắt đầu đếm khi khách chọn ghế đầu tiên; dừng/đặt lại khi bỏ hết ghế
watch(() => store.selectedSeats.length, (n) => {
  if (n > 0 && holdStartTs.value === 0) {
    holdStartTs.value = Date.now()
    expiredHandled = false
  } else if (n === 0) {
    holdStartTs.value = 0
  }
})

// Hết giờ giữ chỗ = PHIÊN CŨ ĐÃ CHẾT → dọn SẠCH mọi state tạm để bắt đầu phiên mới ở bước 1,
// tránh rò rỉ state (ghế/combo/voucher/giá) làm sai logic API lượt sau.
const handleHoldExpired = async () => {
  if (expiredHandled) return
  expiredHandled = true
  holdStartTs.value = 0
  held.value = false
  // 1) Nhả khóa real-time TỪNG ghế trên server (in-memory) trước khi xoá lựa chọn
  store.selectedSeats.forEach(seat => seatRealtime.deselect(seat.seatId))
  // 2) Nhả đơn đang giữ ghế dưới DB (nếu đã tạo) → mở ghế cho khách khác mua ngay
  await store.releaseHold()
  // 3) Xoá sạch toàn bộ state tạm của phiên (ghế/vé/combo/voucher/giá) — giữ movie + showtime
  store.resetSelections()
  // 4) Dọn state cục bộ của màn đặt vé (ô nhập mã, số giảm, thông báo, kết quả preview voucher)
  voucherCode.value = ''
  discountAmount.value = 0
  voucherError.value = ''
  voucherSuccess.value = ''
  voucherEvals.value = {}
  // 5) Làm mới sơ đồ ghế + đưa về bước 1
  await store.fetchSeats()
  currentStep.value = 1
  scrollTop()
  toast.warning(`Đã hết thời gian giữ chỗ (${holdMinutes.value} phút). Vui lòng chọn lại ghế.`)
}

watch(secondsLeft, (s) => { if (s === 0) handleHoldExpired() })

onMounted(async () => {
  try {
    const { data } = await settingsApi.getAll()
    const v = parseInt(data.find(i => i.settingKey === 'SEAT_HOLD_MINUTES')?.settingValue)
    if (!isNaN(v)) holdMinutes.value = Math.min(30, Math.max(3, v))
    const mt = parseInt(data.find(i => i.settingKey === 'MAX_TICKETS_PER_BOOKING')?.settingValue)
    if (!isNaN(mt)) store.maxTicketsPerBooking = Math.min(20, Math.max(1, mt))
  } catch (e) { /* lỗi tải cấu hình → giữ mặc định */ }
  countdownTimer = setInterval(() => { nowTs.value = Date.now() }, 1000)
})
onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  seatRealtime.disconnect() // rời trang → nhả khóa ghế + ngắt WebSocket
})

// Tạm tính riêng phần ghế (đã gồm giá theo đối tượng) để hiển thị ở sidebar.
// Tính TRỰC TIẾP từ ticketBreakdown — độc lập hoàn toàn với F&B, tránh lỗi
// "rò" phụ thu combo vào tiền ghế khi trừ ngược từ totalPrice.
const seatsSubtotal = computed(() =>
  ticketBreakdown.value.reduce((sum, t) => sum + t.price, 0)
)

const showStudentVerificationWarning = computed(() => {
  const hasSweetbox = store.selectedSeats.some(s => s.seatType === 'SWEETBOX');
  const hasRestrictedTicket = store.audienceAssignment.some(code => ['U22', 'CHILD', 'SENIOR'].includes(code));
  return hasSweetbox && hasRestrictedTicket;
})

const formattedSeatSummary = computed(() => {
  const normalCount = store.selectedSeats.filter(s => s.seatType !== 'SWEETBOX').length;
  const sweetCount = store.selectedSeats.filter(s => s.seatType === 'SWEETBOX').length;
  let str = [];
  if (normalCount > 0) str.push(`${normalCount} ghế`);
  if (sweetCount > 0) str.push(`${sweetCount} ghế đôi (${sweetCount * 2} chỗ)`);
  return str.join(', ');
})

const ticketBreakdown = computed(() => {
  const breakdown = {};
  
  // Khởi tạo toàn bộ vé từ store (Single Source of Truth)
  Object.entries(store.ticketQuantities).forEach(([code, q]) => {
    if (q > 0) {
      const label = store.audienceLabels[code] || code;
      const basePrice = (store.priceTable['STANDARD'] && store.priceTable['STANDARD'][code]) || 0;
      breakdown[code] = { label, qty: q, price: 0, _unassigned: q, _basePrice: basePrice };
    }
  });

  // Map các ghế đã chọn vào vé để tính giá thực tế (bao gồm phụ thu VIP/Sweetbox)
  const assign = store.audienceAssignment;
  let assignIdx = 0;
  
  store.selectedSeats.forEach(seat => {
    const capacity = seat.seatType === 'SWEETBOX' ? 2 : 1;
    for (let j = 0; j < capacity; j++) {
      const code = assign[assignIdx] || 'ADULT';
      if (breakdown[code] && breakdown[code]._unassigned > 0) {
        breakdown[code]._unassigned--;
        
        const byAud = store.priceTable[seat.seatType] || {};
        const actualPrice = (byAud[code] != null) ? Number(byAud[code]) : (seat.price || 0);
        breakdown[code].price += actualPrice;
      }
      assignIdx++;
    }
  });
  
  // Cộng giá cơ bản cho các vé chưa được chọn ghế
  Object.values(breakdown).forEach(item => {
    if (item._unassigned > 0) {
      item.price += (item._unassigned * item._basePrice);
    }
  });
  
  return Object.values(breakdown);
})

// ===== Phân trang danh sách combo / F&B (6 món = 2 cột x 3 hàng / trang) =====
const fnbPage = ref(1)
const fnbPageSize = 6
const fnbTotalPages = computed(() => Math.max(1, Math.ceil(store.availableFnbs.length / fnbPageSize)))
const pagedFnbs = computed(() => {
  const start = (fnbPage.value - 1) * fnbPageSize
  return store.availableFnbs.slice(start, start + fnbPageSize)
})
// Kẹp lại số trang nếu danh sách thay đổi (vd sau khi tải xong)
watch(fnbTotalPages, (total) => { if (fnbPage.value > total) fnbPage.value = total })

// State for FnbOptionModal
const isFnbModalOpen = ref(false)
const selectedFnbForModal = ref(null)

// Tổng số lượng của một combo trong giỏ (gộp mọi bộ tuỳ chọn) — hiển thị trên card.
const fnbQtyOf = (fnbItem) =>
  store.selectedFnbs.filter(f => f.fnbItem.id === fnbItem.id).reduce((s, f) => s + f.quantity, 0)

// Giảm 1 ở dòng (bộ tuỳ chọn) được thêm GẦN NHẤT của combo này.
// Combo có slot có thể có nhiều bộ tuỳ chọn khác nhau → giảm từ dòng cuối cùng.
const decrementFnb = (fnbItem) => {
  for (let i = store.selectedFnbs.length - 1; i >= 0; i--) {
    const f = store.selectedFnbs[i]
    if (f.fnbItem.id === fnbItem.id) {
      store.updateFnb(f.fnbItem, f.quantity - 1, f.options)
      return
    }
  }
}

const openFnbModal = (fnbItem) => {
  if (fnbItem.slots && fnbItem.slots.length > 0) {
    selectedFnbForModal.value = fnbItem
    isFnbModalOpen.value = true
  } else {
    // If no options, just add directly
    store.updateFnb(fnbItem, (store.selectedFnbs.find(f => f.fnbItem.id === fnbItem.id && (!f.options || f.options.length === 0))?.quantity || 0) + 1, [])
  }
}

const handleFnbOptionConfirm = ({ options }) => {
  const fnbItem = selectedFnbForModal.value
  const existing = store.selectedFnbs.find(f => {
    if (f.fnbItem.id !== fnbItem.id) return false;
    const aIds = (f.options || []).map(o => o.optionItemId).sort().join(',');
    const bIds = options.map(o => o.optionItemId).sort().join(',');
    return aIds === bIds;
  })
  
  store.updateFnb(fnbItem, (existing?.quantity || 0) + 1, options)
}

const vouchers = ref([])
const voucherCode = ref('')
const voucherError = ref('')
const voucherSuccess = ref('')
const discountAmount = ref(0)
// Kết quả preview từ server theo giỏ hiện tại: voucherId -> { applicable, reason, discountAmount, hideFromUI }
const voucherEvals = ref({})

const finalPaymentPrice = computed(() => {
  const total = store.totalPrice
  const final = total - discountAmount.value
  return final < 0 ? 0 : final
})

// ===== Thanh toán chuyển khoản (VietQR tự sinh — giống POS) =====
const bankInfo = ref({ code: '', name: '', accountNo: '', accountName: '' })

const removeDiacritics = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')

const transferContent = computed(() => {
  const seats = store.selectedSeats.map(s => s.rowChar + s.colNum).join('')
  return removeDiacritics(`DevCine ve ${seats}`).slice(0, 50)
})

const crc16 = (str) => {
  let crc = 0xFFFF
  for (let i = 0; i < str.length; i++) {
    crc ^= str.charCodeAt(i) << 8
    for (let j = 0; j < 8; j++) {
      crc = (crc & 0x8000) ? ((crc << 1) ^ 0x1021) : (crc << 1)
      crc &= 0xFFFF
    }
  }
  return crc.toString(16).toUpperCase().padStart(4, '0')
}

const buildVietQrPayload = () => {
  const b = bankInfo.value
  if (!b.code || !b.accountNo) return ''
  const tlv = (id, val) => id + String(val.length).padStart(2, '0') + val
  const acquirer = tlv('00', b.code) + tlv('01', b.accountNo)
  const f38 = tlv('38', tlv('00', 'A000000727') + tlv('01', acquirer) + tlv('02', 'QRIBFTTA'))
  const amount = Math.round(finalPaymentPrice.value || 0)
  const f54 = amount > 0 ? tlv('54', String(amount)) : ''
  const f62 = transferContent.value ? tlv('62', tlv('08', transferContent.value)) : ''
  const partial = tlv('00', '01') + tlv('01', '11') + f38 + tlv('53', '704') + f54 + tlv('58', 'VN') + f62 + '6304'
  return partial + crc16(partial)
}

const transferQrUrl = computed(() => {
  const payload = buildVietQrPayload()
  if (!payload) return ''
  return `https://api.qrserver.com/v1/create-qr-code/?size=300x300&margin=0&ecc=M&data=${encodeURIComponent(payload)}`
})

const loadBankInfo = async () => {
  try {
    const { data } = await settingsApi.getAll()
    const map = {}
    data.forEach(s => { map[s.settingKey] = s.settingValue })
    bankInfo.value = {
      code: map.PAYMENT_BANK_CODE || '',
      name: map.PAYMENT_BANK_NAME || '',
      accountNo: map.PAYMENT_ACCOUNT_NO || '',
      accountName: map.PAYMENT_ACCOUNT_NAME || ''
    }
  } catch (err) {
    // Không chặn luồng đặt vé nếu lỗi — phần QR sẽ báo chưa cấu hình
  }
}

onMounted(async () => {
  if (!store.selectedShowtime) {
    // If accessed directly without a showtime, redirect back
    router.push('/lich-chieu')
    return
  }
  await store.fetchSeats()
  await store.fetchFnbs()
  loadBankInfo()
  seatRealtime.connect(store.selectedShowtime.id) // bật khóa ghế real-time

  // Quay lại sau khi đăng nhập: khôi phục đúng bước nếu ghế vẫn còn chọn đủ
  const resumeStep = Number(route.query.step)
  if (authStore.isAuthenticated && resumeStep > 1
      && store.totalTickets > 0 && store.selectedSeats.length === store.totalTickets) {
    currentStep.value = resumeStep
  }

  if (authStore.isAuthenticated && authStore.user?.id) {
    // Fetch active vouchers
    try {
      const voucherRes = await voucherApi.getActiveVouchers(authStore.user.id)
      vouchers.value = voucherRes.data
    } catch (err) {
      console.error('Failed to fetch vouchers', err)
    }
  }
})

const isApplyingVoucher = ref(false)

const refreshVouchers = async () => {
  if (!authStore.user?.id) return
  try {
    const voucherRes = await voucherApi.getActiveVouchers(authStore.user.id)
    vouchers.value = voucherRes.data
  } catch (err) {
    // giữ danh sách cũ nếu refresh lỗi
  }
}

// Thông báo áp dụng thành công dùng SỐ GIẢM THỰC (từ server), không phải giá trị mã thô
const appliedSuccessText = (amount) =>
  `Áp dụng thành công! Được giảm ${Number(amount || 0).toLocaleString('vi-VN')}đ`

// Chấm điều kiện voucher theo giỏ hiện tại (server = nguồn sự thật). Làm mờ mã không đủ điều kiện,
// đồng bộ số giảm thực, và tự bỏ chọn nếu voucher đang chọn trở nên không hợp lệ.
const fetchVoucherEvals = async () => {
  if (!authStore.user?.id || vouchers.value.length === 0) { voucherEvals.value = {}; return }
  try {
    const assign = store.audienceAssignment
    const seatPrices = store.selectedSeats.map((seat, i) => {
      const aud = assign[i] || 'ADULT'
      const byAud = store.priceTable[seat.seatType]
      return (byAud && byAud[aud] != null) ? Number(byAud[aud]) : (seat.price || 0)
    })
    const fnbTotal = store.selectedFnbs.reduce((acc, f) => {
      const surcharge = (f.options || []).reduce((s, o) => s + (o.surchargePrice || 0), 0)
      return acc + (f.fnbItem.price + surcharge) * f.quantity
    }, 0)
    const { data } = await voucherApi.preview({
      customerId: authStore.user.id,
      movieId: store.selectedMovie?.id ?? null,
      seatPrices,
      fnbTotal
    })
    const map = {}
    for (const e of data) map[e.voucherId] = e
    voucherEvals.value = map

    const sel = store.selectedVoucher
    if (sel && map[sel.id]) {
      if (!map[sel.id].applicable) {
        removeVoucher()
        voucherError.value = map[sel.id].reason || 'Đơn không đủ điều kiện để áp dụng mã này.'
      } else {
        discountAmount.value = Number(map[sel.id].discountAmount || 0)
        voucherSuccess.value = appliedSuccessText(discountAmount.value)
      }
    }
  } catch (e) { /* preview lỗi → không chặn luồng, giữ nguyên hiển thị */ }
}

const applyVoucherCode = async () => {
  voucherError.value = ''
  voucherSuccess.value = ''
  if (!voucherCode.value.trim()) return

  if (!authStore.isAuthenticated || !authStore.user?.id) {
    toast.warning('Vui lòng đăng nhập để sử dụng mã giảm giá!')
    return
  }

  isApplyingVoucher.value = true
  try {
    // /apply: dùng voucher đã lưu, hoặc tự lưu mã hợp lệ rồi áp dụng (chặn mã đổi-điểm / hết hạn / không tồn tại / sai đối tượng)
    const { data } = await voucherApi.applyCode(authStore.user.id, voucherCode.value.trim())
    voucherCode.value = ''
    await refreshVouchers()
    await fetchVoucherEvals()
    // Điều kiện theo giỏ (đơn tối thiểu / theo phim) chấm ở server — không đủ thì KHÔNG chọn, chỉ báo
    const ev = voucherEvals.value[data.id]
    if (ev && !ev.applicable) {
      // Không đủ điều kiện theo giỏ → hiện dòng lỗi ngay dưới ô nhập mã (không dùng toast)
      store.selectedVoucher = null
      discountAmount.value = 0
      voucherSuccess.value = ''
      voucherError.value = ev.reason || 'Đơn không đủ điều kiện để áp dụng mã này.'
    } else {
      store.selectedVoucher = data
      discountAmount.value = ev ? Number(ev.discountAmount || 0) : (calculateDiscount(), discountAmount.value)
      voucherSuccess.value = appliedSuccessText(discountAmount.value)
      voucherError.value = ''
    }
  } catch (err) {
    store.selectedVoucher = null
    discountAmount.value = 0
    voucherError.value = friendlyError(err, 'Mã giảm giá không hợp lệ!')
  } finally {
    isApplyingVoucher.value = false
  }
}

const selectVoucher = (v) => {
  const ev = voucherEvals.value[v.id]
  if (ev && !ev.applicable) {
    voucherSuccess.value = ''
    voucherError.value = ev.reason || 'Đơn không đủ điều kiện để áp dụng mã này.'
    return
  }
  store.selectedVoucher = {
    id: v.id,
    code: v.promotion.code,
    discountType: v.promotion.discountType,
    discountValue: v.promotion.discountValue,
    maxTicketQuantity: v.promotion.maxTicketQuantity,
    maxDiscountAmount: v.promotion.maxDiscountAmount
  }
  if (ev) discountAmount.value = Number(ev.discountAmount || 0)
  else calculateDiscount()
  voucherSuccess.value = appliedSuccessText(discountAmount.value)
  voucherError.value = ''
}

const removeVoucher = () => {
  store.selectedVoucher = null
  discountAmount.value = 0
  voucherSuccess.value = ''
  voucherError.value = ''
}

const calculateDiscount = () => {
  const v = store.selectedVoucher
  if (!v) {
    discountAmount.value = 0
    return
  }
  const seatTotal = store.selectedSeats.reduce((acc, s) => acc + s.price, 0)
  const fnbTotal = store.selectedFnbs.reduce((acc, f) => {
    const surcharge = (f.options || []).reduce((s, o) => s + (o.surchargePrice || 0), 0)
    return acc + (f.fnbItem.price + surcharge) * f.quantity
  }, 0)
  const total = seatTotal + fnbTotal

  // Base được tính giảm: mặc định cả đơn; nếu mã giới hạn số vé → chỉ X vé đắt nhất
  let base = total
  const maxTickets = Number(v.maxTicketQuantity || 0)
  if (maxTickets > 0) {
    base = store.selectedSeats
      .map(s => s.price)
      .sort((a, b) => b - a)
      .slice(0, maxTickets)
      .reduce((acc, p) => acc + p, 0)
  }

  let discount = v.discountType === 'PERCENTAGE'
    ? base * v.discountValue / 100
    : Math.min(v.discountValue, base)

  // Trần giảm tối đa (capping)
  const maxDiscount = Number(v.maxDiscountAmount || 0)
  if (maxDiscount > 0 && discount > maxDiscount) discount = maxDiscount

  discountAmount.value = Math.min(discount, total)
}

// Recalculate discount if seat or fnb selections change
watch(() => [store.selectedSeats.length, store.selectedFnbs.length], () => {
  calculateDiscount()
  // Giỏ đổi → chấm lại điều kiện/số giảm ở server (chỉ khi đã tới bước Ưu đãi trở đi)
  if (currentStep.value >= 3) fetchVoucherEvals()
})
// Vào bước "Ưu đãi" (3): chấm điều kiện toàn bộ voucher theo giỏ hiện tại để làm mờ mã không đủ
watch(currentStep, (s) => { if (s === 3) fetchVoucherEvals() })

/**
 * Danh sách voucher HIỆN THỊ cho khách tại bước ĐẶT VÉ:
 * - Loại bỏ hoàn toàn (hideFromUI=true): hết lượt, sai đối tượng, sai phim, chương trình bị ẩn.
 * - Giữ lại (kể cả applicable=false): chưa đủ đơn tối thiểu → hiển mờ, có thể đủ khi thêm ghế/FnB.
 * Khi server chưa trả kết quả preview (voucherEvals rỗng), giữ tất cả để tránh UI nhấp nháy.
 */
const visibleVouchers = computed(() => {
  if (Object.keys(voucherEvals.value).length === 0) return vouchers.value
  return vouchers.value.filter(v => {
    const ev = voucherEvals.value[v.id]
    if (!ev) return true          // chưa có kết quả → giữ (sẽ ẩn sau khi eval xong nếu cần)
    return !ev.hideFromUI         // hideFromUI=true → loại bỏ
  })
})

// ══════ BLOCK SELECTOR (chọn theo khối ghế liền nhau — mô hình Lotte) ══════
let blockCounter = 0
const hoverBlockIds = ref([])

// Ghế đưa được vào khối mới: trống, không bảo trì, không bị khoá real-time, CHƯA được chọn.
const isSeatFreeForBlock = (s) =>
  !!s && s.status === 'AVAILABLE' && !isSeatMaintenance(s) && !isSeatLockedByOthers(s)
  && !store.selectedSeats.some(sel => sel.seatId === s.seatId)

// ══════ LƯỚI GHẾ + snapBlockAt — NGUỒN SỰ THẬT DUY NHẤT ══════
// Cùng một hàm phục vụ cả 3 nơi: tô dấu X (unselectableSeatIds), preview khi rê chuột
// (onSeatEnter) và lúc bấm (onSeatClick) ⇒ hiển thị và hành vi KHÔNG THỂ lệch nhau.

// Ghế đôi: SWEETBOX/DOUBLE chiếm 2 CỘT lưới và ngồi được 2 người (khớp isDoubleSeat của renderer).
const isCoupleSeat = (c) =>
  !!c && (c.span === 2 || ['SWEETBOX', 'DOUBLE'].includes(String(c.seatType || '').toUpperCase()))

// Bề rộng một ô chiếm trên lưới: ghế đôi 2 cột, lối đi theo span backend trả (lối đi rộng).
const cellSpanCols = (c) =>
  c.kind === 'AISLE' ? Math.max(1, Number(c.span) || 1) : (isCoupleSeat(c) ? 2 : 1)

// Dựng LƯỚI THEO HÀNG một lần cho cả lượt quét: MỌI cột mặc định là TƯỜNG, ghế mới ghi đè lên.
//   FREE = ghế trống đặt được · BUSY = ghế bận (bán/giữ/khoá/bảo trì/đang chọn)
//   WALL = lối đi, khoảng nhảy gridCol, hoặc ngoài khung · SPAN = nửa phải bị che của ghế đôi
// Mặc-định-là-tường là điểm mấu chốt: hàng bị lối đi cắt khúc (A1,A2 | A3..A10 | A11,A12) có
// khoảng nhảy gridCol giữa hai cụm — cột nhảy KHÔNG có ô nào nên nghiễm nhiên là WALL, khiến
// seatRowRuns cắt cụm ngay tại mép thay vì lẹm sang cụm kế tiếp.
// Bề rộng khung suy TỪ CHÍNH các ô có thật, KHÔNG dùng store.matrixCol — ghế nằm ngoài
// matrixCol (dữ liệu suất cũ) không bị cắt cụt khiến phép đo khoảng trống lệch.
const WALL_SLOT = Object.freeze({ state: 'WALL', w: 1 })

const seatRowSlots = computed(() => {
  const byRow = new Map()
  for (const c of store.availableSeats) {
    if (!c || c.gridRow == null || c.gridCol == null) continue
    if (!byRow.has(c.gridRow)) byRow.set(c.gridRow, [])
    byRow.get(c.gridRow).push(c)
  }

  const grid = new Map()
  for (const [row, cells] of byRow) {
    let width = 0
    for (const c of cells) width = Math.max(width, c.gridCol + cellSpanCols(c))

    // Mọi cột khởi tạo là TƯỜNG → lối đi tường minh và khoảng nhảy gridCol đều là rào cản.
    const slots = new Array(width)
    for (let i = 0; i < width; i++) slots[i] = WALL_SLOT

    // Chỉ ô SEAT mới ghi đè lên tường.
    for (const c of cells) {
      if (c.kind === 'AISLE') continue
      slots[c.gridCol] = {
        state: isSeatFreeForBlock(c) ? 'FREE' : 'BUSY',
        w: isCoupleSeat(c) ? 2 : 1,
        cell: c,
      }
    }
    // Nửa phải của ghế đôi: chỉ đánh dấu nếu cột đó không có ghế thật nào khác.
    for (const c of cells) {
      if (c.kind === 'AISLE' || !isCoupleSeat(c)) continue
      const next = c.gridCol + 1
      if (next < width && slots[next] === WALL_SLOT) slots[next] = { state: 'SPAN', w: 1, ownerCol: c.gridCol }
    }
    grid.set(row, slots)
  }
  return grid
})

// ══════ Tầng A: CẮT HÀNG THÀNH CỤM TRỐNG (rowRuns) ══════
// Mỗi cụm = dãy ghế FREE liền nhau, bị 2 đầu chặn bởi tường/lối đi/ghế bận (khoảng nhảy
// gridCol cũng là tường vì seatRowSlots khởi tạo mọi cột là WALL). Bước theo bề rộng ô (w)
// để ghế đôi nhảy trọn 2 cột, KHÔNG đâm vào ô SPAN. caps[i] = số CHỖ ô thứ i chiếm
// (ghế đôi = 2 chỗ); capacity = tổng chỗ của cụm — chính là `L` trong Luật Sức chứa.
const seatRowRuns = computed(() => {
  const byRow = new Map()
  for (const [row, slots] of seatRowSlots.value) {
    const runs = []
    let cur = null
    let col = 0
    while (col < slots.length) {
      const s = slots[col]
      if (s && s.state === 'FREE') {
        if (!cur) { cur = { seats: [], caps: [], capacity: 0 }; runs.push(cur) }
        cur.seats.push(s.cell)
        cur.caps.push(s.w)
        cur.capacity += s.w
        col += s.w // ghế đôi nhảy 2 cột, bỏ qua ô SPAN kế bên
      } else {
        cur = null
        col += 1
      }
    }
    byRow.set(row, runs)
  }
  return byRow
})

// Cụm chứa `anchorCell` + vị trí (index) của mỏ neo trong cụm; null nếu ghế không thuộc cụm nào.
const runContaining = (anchorCell) => {
  if (!anchorCell || anchorCell.gridRow == null || anchorCell.seatId == null) return null
  const runs = seatRowRuns.value.get(anchorCell.gridRow)
  if (!runs) return null
  for (const run of runs) {
    const idx = run.seats.findIndex(s => s.seatId === anchorCell.seatId)
    if (idx >= 0) return { run, anchorIdx: idx }
  }
  return null
}

// ══════ Tầng B: LIỆT KÊ MỌI VỊ TRÍ ĐẶT KHỐI `size` chỗ trong 1 cụm ══════
// Trả các placement { seats, startIdx, endIdx, leftGap, rightGap } — leftGap/rightGap tính bằng CHỖ.
// cap !== size ⇒ loại (khối chạm mép cụm, hoặc khối lẻ đâm vào ghế đôi làm thừa 1 chỗ)
// ⇒ ràng buộc chẵn/lẻ & SWEETBOX tự đúng, không cần luật riêng cho mỏ neo.
const placementsIn = (run, size) => {
  const out = []
  const n = run.seats.length
  const odd = size % 2 === 1
  for (let j = 0; j < n; j++) {
    let cap = 0
    let e = j
    let hasCouple = false
    while (e < n && cap < size) { if (run.caps[e] === 2) hasCouple = true; cap += run.caps[e]; e++ }
    if (cap !== size) continue
    // Khối vé LẺ tuyệt đối không chứa ghế đôi (Sweetbox 2 chỗ, giá riêng, vách cho đúng 2 người).
    if (odd && hasCouple) continue
    let leftGap = 0
    for (let t = 0; t < j; t++) leftGap += run.caps[t]
    let rightGap = 0
    for (let t = e; t < n; t++) rightGap += run.caps[t]
    out.push({ seats: run.seats.slice(j, e), startIdx: j, endIdx: e - 1, leftGap, rightGap })
  }
  return out
}

// Vị trí KHÔNG sinh ghế mồ côi ⇔ không để lại đúng 1 chỗ trống ở bất kỳ bên nào.
const orphanFree = (p) => p.leftGap !== 1 && p.rightGap !== 1

// So sánh điểm theo THỨ TỰ TỪ ĐIỂN (tuple nhỏ hơn = tốt hơn).
const lexLess = (a, b) => {
  for (let i = 0; i < a.length; i++) { if (a[i] !== b[i]) return a[i] < b[i] }
  return false
}

// ══════ Tầng C: AUTO-SNAP — chấm điểm & chọn 1 vị trí tối ưu trong cụm ══════
// Tuple [covers, shift, wallDir, snug] — ứng với 3 bước UX đã duyệt:
//   covers  : khối phủ đúng ghế con trỏ?            (0 tốt) → B1 "gom cơ bản" luôn thắng nếu hợp lệ
//   shift   : trượt ít cột nhất so với mỏ neo                → B2 né vách / B3 né mồ côi tối thiểu 1 nhịp
//   wallDir : hoà shift → ưu tiên trượt về VÁCH GẦN NHẤT     (mỏ neo cách đều 2 vách → fallback trái)
//   snug    : ưu tiên khít 1 vách (gap=0) → dồn ghế vào góc, chừa khoảng giữa cho khách khác
const scorePlacement = (p, anchorIdx, runLen) => {
  const covers = (p.startIdx <= anchorIdx && anchorIdx <= p.endIdx) ? 0 : 1
  const shift = Math.abs(p.startIdx - anchorIdx)
  const leftNearer = anchorIdx <= (runLen - 1 - anchorIdx) // cách đều → coi vách trái gần hơn
  const moveLeft = p.startIdx <= anchorIdx
  const wallDir = leftNearer ? (moveLeft ? 0 : 1) : (moveLeft ? 1 : 0)
  const snug = (p.leftGap === 0 || p.rightGap === 0) ? 0 : 1
  return [covers, shift, wallDir, snug]
}

/**
 * ★ NGUỒN SỰ THẬT DUY NHẤT (thay `findBlockAt`) — lấy `anchorCell` làm mỏ neo, tự "nắn" khối
 * `currentBlockSize` để né vách & né ghế mồ côi. Trả MẢNG GHẾ đã nắn, hoặc `null` nếu bất khả.
 * Dùng chung cho tô X (unselectableSeatIds), hover (onSeatEnter) và click (onSeatClick)
 * ⇒ hiển thị & hành vi KHÔNG THỂ lệch nhau; "không có X ⇔ bấm là đặt được" vẫn là bất biến.
 */
const snapBlockAt = (anchorCell) => {
  const size = store.currentBlockSize
  if (!size) return null
  const found = runContaining(anchorCell)
  if (!found) return null
  const { run, anchorIdx } = found
  const cands = placementsIn(run, size)
  if (cands.length === 0) return null // cụm không đủ `size` chỗ liền mạch → Luật Sức chứa (L < size)

  // ── LUẬT VÉ LẺ: size == 1 → KHÔNG snap; giữ luật chống mồ côi khắt khe TẠI ĐÚNG mỏ neo ──
  if (size === 1) {
    const p = cands.find(c => c.startIdx === anchorIdx)
    return (p && orphanFree(p)) ? p.seats : null
  }

  // ── size >= 2: chỉ giữ vị trí KHÔNG mồ côi rồi Auto-Snap ──
  // safe rỗng ⇔ L == size+1 (mọi vị trí đều để lại gap = 1) → X trọn cụm, chặn trước lỗi 400 backend.
  const safe = cands.filter(orphanFree)
  if (safe.length === 0) return null

  let best = safe[0]
  let bestScore = scorePlacement(best, anchorIdx, run.seats.length)
  for (let i = 1; i < safe.length; i++) {
    const sc = scorePlacement(safe[i], anchorIdx, run.seats.length)
    if (lexLess(sc, bestScore)) { best = safe[i]; bestScore = sc }
  }
  return best.seats
}

// Quét toàn sơ đồ bằng CHÍNH `snapBlockAt` → ghế không có X thì bấm chắc chắn đặt được.
// Phụ thuộc reactive: availableSeats · selectedSeats · currentBlockSize · totalTickets · khoá
// real-time ⇒ tự tính lại khi đổi số vé, đổi khối, hoặc vừa chọn/bỏ chọn ghế.
const unselectableSeatIds = computed(() => {
  const ids = new Set()
  // Chưa chọn vé (lưới đã bị làm mờ) hoặc đã chọn đủ ghế: bỏ qua để không phủ X toàn sơ đồ.
  if (store.totalTickets === 0 || !store.currentBlockSize) return ids

  for (const slots of seatRowSlots.value.values()) {
    for (const e of slots) {
      if (!e || e.state !== 'FREE') continue // ghế bận/lối đi đã có style riêng
      if (!snapBlockAt(e.cell)) ids.add(e.cell.seatId)
    }
  }
  return ids
})

const isSeatUnselectable = (seat) => !!seat && unselectableSeatIds.value.has(seat.seatId)

const onSeatEnter = (seat) => {
  if (!seat || store.totalTickets === 0 || store.remainingCapacity === 0) {
    // Vẫn cho preview khối SẼ GỠ khi rê vào ghế đã chọn (kể cả khi đã đặt đủ)
    const sel = seat && store.selectedSeats.find(s => s.seatId === seat.seatId)
    hoverBlockIds.value = sel ? store.selectedSeats.filter(s => s.blockId === sel.blockId).map(s => s.seatId) : []
    return
  }
  const sel = store.selectedSeats.find(s => s.seatId === seat.seatId)
  if (sel) { hoverBlockIds.value = store.selectedSeats.filter(s => s.blockId === sel.blockId).map(s => s.seatId); return }
  const block = snapBlockAt(seat)
  hoverBlockIds.value = block ? block.map(s => s.seatId) : []
}
const onSeatLeave = () => { hoverBlockIds.value = [] }

const onSeatClick = (seat) => {
  if (!seat || seat.status !== 'AVAILABLE') return
  // Click ghế đã chọn → gỡ NGUYÊN khối chứa nó.
  const sel = store.selectedSeats.find(s => s.seatId === seat.seatId)
  if (sel) {
    store.selectedSeats.filter(s => s.blockId === sel.blockId).forEach(s => seatRealtime.deselect(s.seatId))
    store.removeSeatBlock(sel.blockId)
    store.autoPickBlockSize()
    hoverBlockIds.value = []
    return
  }
  if (store.totalTickets === 0) { toast.toasts = []; toast.warning('Vui lòng chọn số lượng vé trước.'); return }
  if (!store.currentBlockSize) { toast.toasts = []; toast.warning('Bạn đã chọn đủ ghế cho số vé này.'); return }
  const block = snapBlockAt(seat)
  if (!block) {
    toast.toasts = []
    toast.warning(`Khu vực này không đủ ${store.currentBlockSize} ghế liền nhau. Vui lòng chọn khu vực khác.`)
    return
  }
  const blockId = ++blockCounter
  store.addSeatBlock(block, blockId)
  block.forEach(s => seatRealtime.select(s.seatId)) // giữ ghế real-time (ai click trước thắng)
  store.autoPickBlockSize() // phần còn lại → chọn khối mặc định mới
  hoverBlockIds.value = []
}

// An toàn: nếu khối đang chọn không còn hợp lệ cho phần còn lại (hoặc chưa khởi tạo) → chọn lại.
watch(() => store.remainingCapacity, () => {
  if (store.remainingCapacity > 0 && !store.validBlockSizes.includes(store.currentBlockSize)) {
    store.autoPickBlockSize()
  }
}, { immediate: true })

// Reset toàn bộ ghế đã chọn (khi đổi số lượng vé) + nhả khoá real-time + chọn lại khối mặc định.
const resetSeatSelection = () => {
  store.selectedSeats.forEach(s => seatRealtime.deselect(s.seatId))
  store.clearSeats()
  store.autoPickBlockSize()
  hoverBlockIds.value = []
}

// Viền preview trên sơ đồ: vàng = sẽ đặt, đỏ = sẽ gỡ.
const seatPreviewClass = (seat) => {
  if (!seat || !hoverBlockIds.value.includes(seat.seatId)) return ''
  const isSel = store.selectedSeats.some(s => s.seatId === seat.seatId)
  return isSel
    ? 'bg-red-500/25 text-white ring-2 ring-red-400 ring-offset-2 ring-offset-black/40'
    : 'bg-primary text-on-primary ring-2 ring-primary ring-offset-2 ring-offset-black/40 scale-[1.03]'
}

const isSeatSelected = (seat) => {
  if (!seat) return false
  return store.selectedSeats.some(s => s.seatId === seat.seatId)
}

// Nội suy tọa độ dùng chung: cellAt = ô bất kỳ (ghế/lối đi) cho template; getSeatAt = CHỈ ghế
// (lối đi → null) nên toàn bộ logic khối/chống-mồ-côi coi lối đi là rào cản như trước.
const { cellAt, seatAt, isAisle } = useSeatGridRender(() => store.availableSeats)
const getSeatAt = seatAt

const getRowChar = (row) => {
  // Ưu tiên rowChar thật của ghế (payload cũ); snapshot không mang rowChar → suy theo vị trí lưới.
  const seat = store.availableSeats.find(s => s.gridRow === row && s.rowChar)
  return seat ? seat.rowChar : String.fromCharCode(65 + row)
}

// Tăng/giảm số lượng vé theo đối tượng (chọn trước khi chọn ghế).
// Block Selector: đổi số vé → RESET ghế đã đặt + chọn lại khối mặc định (theo yêu cầu Lotte).
const setQty = (code, delta) => {
  const before = store.totalTickets
  store.setTicketQuantity(code, (store.ticketQuantities[code] || 0) + delta)
  if (store.totalTickets !== before) resetSeatSelection()
}

const isHiddenBecauseSweetbox = (row, col) => {
  if (col === 0) return false;
  const prevSeat = getSeatAt(row, col - 1);
  return prevSeat && prevSeat.seatType === 'SWEETBOX';
}

const getBookingSeatClass = (seat) => {
  const isSelected = isSeatSelected(seat);
  // Ghế bị nơi khác giữ real-time coi như không khả dụng (khóa xám, không click được)
  const isAvailable = seat.status === 'AVAILABLE' && !isSeatLockedByOthers(seat);
  const type = seat.seatType;

  const baseClasses = 'flex items-center justify-center text-[10px] font-bold transition-all duration-200';
  const shadowClasses = 'shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1)]';
  const standardSize = 'aspect-square w-10';
  const doubleSize = 'h-10 w-[5.5rem]'; // w-10 (40px) * 2 + gap-2 (8px) = 88px = 5.5rem

  // Ghế bảo trì/khóa: nền đỏ mờ, gạch chéo, không click được
  if (isSeatMaintenance(seat)) {
    const sizeClass = type === 'SWEETBOX' ? `${doubleSize} rounded-xl` : `${standardSize} rounded-lg`;
    return `${baseClasses} ${sizeClass} bg-surface-container-highest border border-white/10 text-red-500 cursor-not-allowed pointer-events-none opacity-60`;
  }

  if (!isAvailable) {
    const sizeClass = type === 'SWEETBOX' ? `${doubleSize} rounded-xl` : `${standardSize} rounded-lg`;
    return `${baseClasses} ${sizeClass} bg-surface-container-high border border-white/5 text-white/20 cursor-not-allowed pointer-events-none opacity-50`;
  }
  
  if (isSelected) {
     const sizeClass = type === 'SWEETBOX' ? `${doubleSize} rounded-t-2xl rounded-b-lg` : `${standardSize} rounded-lg`;
     return `${baseClasses} ${sizeClass} bg-gradient-to-br from-primary to-amber-600 text-on-primary shadow-[0_0_20px_rgba(245,197,24,0.3)] scale-[1.02] border-none cursor-pointer`;
  }
  
  // Available and Not Selected
  switch (type) {
    case 'VIP': 
      return `${baseClasses} ${standardSize} ${shadowClasses} rounded-lg bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 text-red-100 shadow-[0_0_15px_rgba(220,38,38,0.2)] hover:shadow-[0_0_20px_rgba(220,38,38,0.4)] hover:-translate-y-0.5 hover:brightness-110 cursor-pointer`;
    case 'SWEETBOX': 
      return `${baseClasses} ${doubleSize} ${shadowClasses} rounded-t-2xl rounded-b-lg bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 text-purple-100 shadow-[0_0_15px_rgba(147,51,234,0.2)] hover:shadow-[0_0_20px_rgba(147,51,234,0.4)] hover:-translate-y-0.5 hover:brightness-110 cursor-pointer`;
    default: // STANDARD
      return `${baseClasses} ${standardSize} ${shadowClasses} rounded-lg bg-slate-800/80 border border-slate-600/50 text-slate-300 hover:brightness-125 hover:-translate-y-0.5 hover:shadow-lg cursor-pointer`;
  }
}

const proceedToPayment = async () => {
  if (holdExpiredNow()) { handleHoldExpired(); return } // hết giờ giữ chỗ → chặn thanh toán
  // Thường ghế đã được giữ sẵn ở nền khi mở bước 4 → ensureHeld trả về ngay; nếu chưa thì giữ ở đây
  const success = await ensureHeld()
  if (success) {
    if (paymentMethod.value === 'VNPAY') {
      try {
        // Dùng giá cuối do backend tính ở bước giữ ghế (đã trừ voucher) để tránh lệch/giảm 2 lần
        const { data } = await paymentApi.createPayment(store.finalPrice, store.bookingId);
        if (data.code === '00') {
          sessionStorage.setItem('bookingState', JSON.stringify(store.$state));
          window.location.href = data.data; // Redirect to VNPAY Sandbox
        } else {
          toast.error('Không thể tạo liên kết thanh toán. Vui lòng thử lại.');
        }
      } catch (err) {
        console.error(err);
        toast.error(friendlyError(err, 'Không tạo được cổng thanh toán, vui lòng thử lại.'));
      }
    } else {
      const paid = await store.confirmPayment(paymentMethod.value)
      if (paid) {
        router.push('/success')
      } else {
        toast.error('Thanh toán chưa thành công, vui lòng thử lại.')
      }
    }
  } else {
    held.value = false
    // store.lastHoldError là message backend → chuẩn hoá sang tiếng Việt, không lộ chuỗi kỹ thuật
    toast.error(friendlyError(store.lastHoldError, 'Giữ ghế thất bại, vui lòng thử lại.'))
    // Làm mới sơ đồ ghế để cập nhật trạng thái mới nhất
    await store.fetchSeats()
  }
}

</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-10">
    <!-- Stepper / Thanh tiến trình các bước -->
    <div class="mb-12">
      <div class="flex items-center justify-between max-w-3xl mx-auto">
        <template v-for="(s, idx) in steps" :key="s.id">
          <button
            type="button"
            @click="goToStep(s.id)"
            class="flex flex-col items-center gap-2 group flex-shrink-0"
          >
            <div
              :class="currentStep === s.id
                ? 'bg-primary text-on-primary border-primary shadow-[0_0_20px_rgba(245,197,24,0.4)] scale-110'
                : currentStep > s.id
                  ? 'bg-primary/20 text-primary border-primary/40'
                  : 'bg-surface-container-high text-on-surface-variant border-outline-variant/20'"
              class="w-12 h-12 rounded-2xl border-2 flex items-center justify-center transition-all duration-300"
            >
              <span v-if="currentStep > s.id" class="material-symbols-outlined">check</span>
              <span v-else class="material-symbols-outlined">{{ s.icon }}</span>
            </div>
            <span
              :class="currentStep === s.id ? 'text-primary' : 'text-on-surface-variant'"
              class="text-[10px] font-bold uppercase tracking-widest transition-colors"
            >
              {{ s.id }}. {{ s.label }}
            </span>
          </button>
          <div
            v-if="idx < steps.length - 1"
            :class="currentStep > s.id ? 'bg-primary/50' : 'bg-outline-variant/20'"
            class="flex-grow h-0.5 mx-2 -mt-6 transition-colors duration-300"
          ></div>
        </template>
      </div>
    </div>

    <!-- Banner đếm ngược thời gian giữ ghế (hiện khi đã giữ ghế) -->
    <transition name="fade">
      <div v-if="isCountingDown"
           :class="secondsLeft <= 60 ? 'bg-red-500/10 border-red-500/40 text-red-400' : 'bg-primary/10 border-primary/30 text-primary'"
           class="mb-8 flex items-center justify-center gap-3 px-6 py-3.5 rounded-2xl border backdrop-blur-sm">
        <span class="material-symbols-outlined text-xl" :class="{ 'animate-pulse': secondsLeft <= 60 }">timer</span>
        <span class="text-sm font-bold">Vui lòng hoàn tất đặt vé trong</span>
        <span class="font-mono font-black text-xl tabular-nums tracking-wider">{{ countdownLabel }}</span>
      </div>
    </transition>

    <!-- Modal nhắc đăng nhập trước khi rời bước chọn ghế -->
    <transition name="fade">
      <div v-if="showLoginPrompt" class="fixed inset-0 z-[300] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showLoginPrompt = false"></div>
        <div class="relative w-full max-w-md bg-surface-container-low border border-outline-variant/15 rounded-3xl p-8 shadow-2xl text-center">
          <div class="w-16 h-16 mx-auto mb-5 rounded-full bg-primary/10 border border-primary/30 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary text-3xl">lock</span>
          </div>
          <h3 class="font-headline text-2xl font-bold text-on-surface mb-2">Bạn chưa đăng nhập</h3>
          <p class="text-sm text-on-surface-variant mb-7">Hãy đăng nhập để tiếp tục đặt vé nhé! Ghế bạn đang chọn sẽ được giữ lại khi quay lại.</p>
          <div class="flex gap-3">
            <button @click="showLoginPrompt = false" class="flex-1 py-3.5 rounded-xl border border-outline-variant/25 text-on-surface-variant font-bold text-xs uppercase tracking-widest hover:bg-white/5 transition-colors">Để sau</button>
            <button @click="goToLogin" class="flex-1 py-3.5 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-widest hover:brightness-110 active:scale-95 transition-all flex items-center justify-center gap-2">
              <span class="material-symbols-outlined text-base">login</span> Đăng nhập
            </button>
          </div>
        </div>
      </div>
    </transition>

    <div class="flex flex-col lg:flex-row gap-12">
    <!-- Main Content Area -->
    <div class="flex-grow min-w-0">
      <!-- Header chung của bước hiện tại (trong cột trái để card tóm tắt căn ngang title/mô tả) -->
      <div class="mb-10 flex items-start justify-between gap-4">
        <div>
          <h1 class="font-headline text-3xl font-bold tracking-tight mb-2 uppercase italic text-primary-container">{{ stepMeta.title }}</h1>
          <div v-if="currentStep === 1 && store.selectedShowtime" class="mt-4 mb-2 bg-primary-container/10 border border-primary-container/30 rounded-xl p-4 flex flex-col xl:flex-row xl:items-center justify-between gap-4">
            <div class="flex flex-wrap items-center gap-3 text-on-surface font-bold text-sm md:text-base">
              <span class="flex items-center gap-1.5"><span class="material-symbols-outlined text-primary-container">location_on</span> Rạp: {{ store.selectedShowtime.cinema?.cinemaName }}</span>
              <span class="hidden md:inline w-1.5 h-1.5 rounded-full bg-primary/40"></span>
              <span class="flex items-center gap-1.5"><span class="material-symbols-outlined text-primary-container">meeting_room</span> {{ store.selectedShowtime.roomName || 'Phòng chiếu' }}</span>
              <span class="hidden md:inline w-1.5 h-1.5 rounded-full bg-primary/40"></span>
              <span class="flex items-center gap-1.5"><span class="material-symbols-outlined text-primary-container">schedule</span> Suất: {{ new Date(store.selectedShowtime.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }} - {{ new Date(store.selectedShowtime.startTime).toLocaleDateString() }}</span>
            </div>
            <button @click="goBack" class="flex-shrink-0 px-4 py-2 bg-surface-container-highest border border-outline-variant/30 text-primary-container font-bold text-[10px] uppercase tracking-widest rounded-lg hover:bg-white/5 transition-all flex items-center gap-1.5 w-fit">
              <span class="material-symbols-outlined text-sm">sync</span> Đổi suất khác
            </button>
          </div>
          <p v-else class="text-sm text-on-surface-variant">{{ stepMeta.desc }}</p>
        </div>
        <!-- Nút Quay lại: bước >1 lùi 1 bước; bước 1 quay về trang trước (chọn suất/lịch chiếu) -->
        <button
          @click="goBack"
          class="flex-shrink-0 px-5 py-2.5 rounded-xl border border-outline-variant/30 text-on-surface-variant font-bold text-xs uppercase tracking-widest hover:border-primary/40 hover:text-primary transition-all flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-lg">arrow_back</span> Quay lại
        </button>
      </div>

      <!-- Section 1: Seat Selection -->
      <section v-show="currentStep === 1" class="space-y-6">
        <!-- Bước 1a: Chọn SỐ LƯỢNG vé theo đối tượng (bắt buộc trước khi chọn ghế) -->
        <div class="glass-card glass-shine-edge p-6 md:p-8 rounded-3xl">
          <h3 class="font-headline font-bold uppercase tracking-tight text-sm mb-1 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary-container text-lg">confirmation_number</span>
            Loại vé
          </h3>
          <p class="text-xs text-on-surface-variant mb-5">Chọn số lượng vé theo đối tượng, sau đó chọn đúng số ghế tương ứng.</p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div v-for="(label, code) in store.audienceLabels" :key="code"
                 class="flex items-center justify-between bg-surface-container-high/40 border border-outline-variant/20 rounded-xl px-4 py-3">
              <span class="font-bold text-sm">{{ label }}</span>
              <div class="flex items-center gap-3">
                <button @click="setQty(code, -1)" :disabled="(store.ticketQuantities[code] || 0) <= 0"
                        class="w-8 h-8 flex items-center justify-center rounded-full bg-surface-container-high disabled:opacity-30 hover:text-primary-container transition-colors">
                  <span class="material-symbols-outlined text-base">remove</span>
                </button>
                <span class="w-6 text-center font-bold tabular-nums">{{ store.ticketQuantities[code] || 0 }}</span>
                <button @click="setQty(code, 1)" :disabled="store.totalTickets >= store.maxTicketsPerBooking"
                        class="w-8 h-8 flex items-center justify-center rounded-full bg-surface-container-high disabled:opacity-30 hover:text-primary-container transition-colors">
                  <span class="material-symbols-outlined text-base">add</span>
                </button>
              </div>
            </div>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm">
            <span class="text-on-surface-variant">Tổng số vé <span class="text-on-surface-variant/60">(tối đa {{ store.maxTicketsPerBooking }} vé/lần)</span></span>
            <span class="font-bold text-primary-container">{{ store.totalTickets }} vé · đã chọn {{ store.selectedSeats.length }} ghế</span>
          </div>
        </div>

        <div class="relative glass-card glass-shine-edge p-12 overflow-hidden rounded-3xl">
          <!-- Nhắc chọn số lượng vé trước -->
          <div v-if="store.totalTickets === 0" class="mb-8 text-center bg-primary-container/10 border border-primary-container/30 rounded-2xl py-4 px-6 text-sm text-on-surface-variant">
            Vui lòng chọn số lượng vé ở trên trước khi chọn ghế.
          </div>

          <!-- Block Selector: chọn kích thước khối ghế liền nhau (chống ghế mồ côi kiểu Lotte) -->
          <div v-else class="mb-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-3">
            <span class="text-sm font-bold text-on-surface-variant flex items-center gap-1.5">
              <span class="material-symbols-outlined text-base text-primary">chair</span> Chọn ghế liền nhau
              <span class="material-symbols-outlined text-sm text-on-surface-variant/50 cursor-help"
                    title="Chọn kích thước khối, rồi rê chuột lên sơ đồ để đặt các ghế ngồi cạnh nhau. Hệ thống tự khoá các lựa chọn làm dư 1 ghế lẻ.">info</span>
            </span>
            <div class="flex items-center gap-2.5">
              <button v-for="b in [1, 2, 3, 4]" :key="b"
                      type="button"
                      :disabled="!store.validBlockSizes.includes(b)"
                      @click="store.setBlockSize(b)"
                      :title="`Khối ${b} ghế`"
                      :class="store.currentBlockSize === b
                        ? 'border-primary bg-primary/10 shadow-[0_0_15px_rgba(245,197,24,0.25)]'
                        : 'border-outline-variant/20 hover:border-outline-variant/40'"
                      class="flex items-center gap-2 px-3 py-2 rounded-xl border-2 transition-all disabled:opacity-25 disabled:cursor-not-allowed">
                <span class="w-3.5 h-3.5 rounded-full border-2 flex-shrink-0 transition-colors"
                      :class="store.currentBlockSize === b ? 'border-primary bg-primary' : 'border-outline-variant/50'"></span>
                <span class="flex gap-1">
                  <span v-for="k in b" :key="k" class="w-4 h-4 rounded-sm transition-colors"
                        :class="store.validBlockSizes.includes(b) ? 'bg-on-surface' : 'bg-on-surface/25'"></span>
                </span>
              </button>
            </div>
            <span v-if="store.remainingCapacity === 0" class="text-xs font-bold text-green-400 flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">check_circle</span> Đã chọn đủ ghế
            </span>
            <span v-else class="text-xs text-on-surface-variant">Còn <b class="text-primary">{{ store.remainingCapacity }}</b> ghế cần chọn</span>
          </div>
          <!-- Screen -->
          <div class="w-full flex flex-col items-center flex-shrink-0 relative py-8 mb-12">
            <div class="absolute top-0 w-full h-[100px] bg-gradient-to-b from-primary/5 to-transparent pointer-events-none"></div>
            <div class="w-2/3 h-1.5 bg-primary/70 rounded-full shadow-[0_2px_15px_rgba(245,197,24,0.2)] mb-4 border border-primary/20"></div>
            <p class="text-[9px] font-bold uppercase tracking-[0.6em] text-primary/50 relative z-10">MÀN HÌNH / SCREEN</p>
          </div>
          
          <!-- Seats Grid -->
          <div class="seat-grid w-full overflow-x-auto flex flex-col gap-3 mb-16 relative transition-opacity" :class="{ 'opacity-40 pointer-events-none': store.totalTickets === 0 }" v-if="store.availableSeats.length">
            <div class="absolute inset-0 opacity-[0.15] pointer-events-none" style="background-image: radial-gradient(rgba(255, 255, 255, 0.4) 1px, transparent 1px); background-size: 24px 24px;"></div>
            <div class="relative z-10 flex flex-col gap-3 mx-auto min-w-max pb-4 bg-black/40 backdrop-blur-sm p-8 rounded-3xl border border-white/5 shadow-2xl">
              <SeatGridRenderer
                :seats="store.availableSeats"
                :matrix-row="store.matrixRow"
                :matrix-col="store.matrixCol"
                :selected-seats="store.selectedSeats"
                mode="booking"
                :is-seat-locked-by-others="isSeatLockedByOthers"
                :is-seat-maintenance="isSeatMaintenance"
                :is-seat-unselectable="isSeatUnselectable"
                :seat-preview-class="seatPreviewClass"
                @seat-click="onSeatClick"
                @seat-enter="onSeatEnter"
                @seat-leave="onSeatLeave"
              />
            </div>
          </div>
          
          <!-- Legend: gom 2 nhóm "Loại ghế" | "Trạng thái" trong 1 panel bo góc -->
          <div class="mt-10 rounded-2xl border border-white/5 bg-black/20 px-5 py-5 sm:px-6">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-stretch lg:gap-8">

              <!-- Nhóm 1: LOẠI GHẾ -->
              <div class="flex flex-col gap-3">
                <span class="text-[10px] font-bold uppercase tracking-[0.12em] text-on-surface-variant/60">Loại ghế</span>
                <div class="flex flex-wrap items-center gap-x-5 gap-y-3">
                  <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-lg bg-slate-800/80 border border-slate-600/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1)]"></div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-slate-300">Standard</span>
                  </div>
                  <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-lg bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1),0_0_15px_rgba(220,38,38,0.2)]"></div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-red-400">VIP</span>
                  </div>
                  <div class="flex items-center gap-2.5">
                    <div class="w-11 h-7 rounded-t-xl rounded-b-md bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1),0_0_15px_rgba(147,51,234,0.2)]"></div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-purple-400">Sweetbox</span>
                  </div>
                </div>
              </div>

              <!-- Vách ngăn -->
              <div class="hidden lg:block w-px self-stretch bg-white/10"></div>
              <div class="lg:hidden h-px w-full bg-white/10"></div>

              <!-- Nhóm 2: TRẠNG THÁI -->
              <div class="flex flex-col gap-3 lg:flex-1">
                <span class="text-[10px] font-bold uppercase tracking-[0.12em] text-on-surface-variant/60">Trạng thái</span>
                <div class="flex flex-wrap items-center gap-x-5 gap-y-3">
                  <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-primary to-amber-600 shadow-[0_0_20px_rgba(245,197,24,0.3)]"></div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-primary">Đang chọn</span>
                  </div>
                  <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-lg bg-surface-container-high border border-white/5 opacity-50"></div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-on-surface-variant">Đã đặt</span>
                  </div>
                  <div class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-lg bg-surface-container-highest border border-white/10 flex items-center justify-center text-red-500 opacity-60">
                      <span class="material-symbols-outlined text-sm">build</span>
                    </div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-on-surface-variant">Bảo trì</span>
                  </div>
                  <div class="flex items-center gap-2.5">
                    <div class="legend-unselectable relative w-7 h-7 rounded-lg border border-white/5"></div>
                    <span class="text-[11px] font-semibold uppercase tracking-wider text-on-surface-variant">Không thể chọn</span>
                  </div>
                </div>
              </div>

            </div>
          </div>

        </div>
      </section>
      
      <!-- Section 2: Combo / F&B Selection -->
      <section v-show="currentStep === 2">
        <!-- Empty state khi rạp chưa có combo -->
        <div v-if="store.availableFnbs.length === 0" class="glass-card p-10 rounded-2xl text-center">
          <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">fastfood</span>
          <p class="text-sm text-on-surface-variant">Hiện chưa có combo nào. Bạn có thể tiếp tục đặt vé.</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="glass-card p-6 flex gap-6 hover:border-primary-container/30 transition-all group rounded-2xl" v-for="fnb in pagedFnbs" :key="fnb.id">
            <div class="w-28 h-28 flex-shrink-0 bg-black overflow-hidden relative rounded-xl">
              <img :src="fnb.imageUrl || '/images/Hopper.webp'" class="w-full h-full object-cover opacity-80 group-hover:scale-110 transition-transform duration-500"/>
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
            </div>
            <div class="flex flex-col justify-between flex-grow">
              <div>
                <h3 class="font-headline font-bold text-lg mb-1">{{ fnb.name }}</h3>
                <p class="text-xs text-on-surface-variant line-clamp-2">{{ fnb.description }}</p>
              </div>
              <div class="flex items-center justify-between mt-2">
                <span class="font-headline font-bold text-primary-container">{{ fnb.price?.toLocaleString('vi-VN') }} VNĐ</span>
                <!-- Chưa có trong giỏ → nút Chọn; đã có → stepper +/- điều khiển tổng số lượng -->
                <button
                  v-if="fnbQtyOf(fnb) === 0"
                  @click="openFnbModal(fnb)"
                  class="bg-surface-container-high hover:bg-primary-container/20 hover:text-primary-container text-on-surface rounded-full px-4 py-1.5 text-xs font-bold transition-colors flex items-center gap-1"
                >
                  <span class="material-symbols-outlined text-sm">add</span> Chọn
                </button>
                <div v-else class="flex items-center gap-1 bg-surface-container-high rounded-full p-1 border border-outline-variant/10">
                  <button @click="decrementFnb(fnb)" class="w-7 h-7 rounded-full flex items-center justify-center hover:bg-primary-container/20 hover:text-primary-container transition-colors" title="Bớt 1">
                    <span class="material-symbols-outlined text-sm">remove</span>
                  </button>
                  <span class="min-w-[1.75rem] text-center text-sm font-bold">{{ fnbQtyOf(fnb) }}</span>
                  <button @click="openFnbModal(fnb)" class="w-7 h-7 rounded-full flex items-center justify-center hover:bg-primary-container/20 hover:text-primary-container transition-colors" title="Thêm 1">
                    <span class="material-symbols-outlined text-sm">add</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Phân trang combo / F&B -->
        <div v-if="store.availableFnbs.length > 0 && fnbTotalPages > 1" class="flex items-center justify-center gap-2 mt-8">
          <button
            @click="fnbPage > 1 && (fnbPage--)"
            :disabled="fnbPage === 1"
            class="w-10 h-10 flex items-center justify-center rounded-xl border border-outline-variant/20 text-on-surface-variant hover:border-primary/40 hover:text-primary transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-lg">chevron_left</span>
          </button>
          <button
            v-for="p in fnbTotalPages"
            :key="p"
            @click="fnbPage = p"
            :class="fnbPage === p ? 'bg-primary text-on-primary border-primary' : 'border-outline-variant/20 text-on-surface-variant hover:border-primary/40'"
            class="w-10 h-10 flex items-center justify-center rounded-xl border text-sm font-bold transition-all"
          >
            {{ p }}
          </button>
          <button
            @click="fnbPage < fnbTotalPages && (fnbPage++)"
            :disabled="fnbPage === fnbTotalPages"
            class="w-10 h-10 flex items-center justify-center rounded-xl border border-outline-variant/20 text-on-surface-variant hover:border-primary/40 hover:text-primary transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-lg">chevron_right</span>
          </button>
        </div>
      </section>

      <!-- Section 3: Voucher / Khuyến mãi -->
      <section v-show="currentStep === 3">
        <div class="glass-card p-6 rounded-2xl space-y-6">
          <!-- Code input -->
          <div class="flex flex-col sm:flex-row gap-4">
            <input 
              v-model="voucherCode" 
              type="text" 
              placeholder="Nhập mã giảm giá..."
              class="flex-grow bg-surface-container-high border border-outline-variant/30 focus:border-primary focus:ring-1 focus:ring-primary rounded-xl px-4 py-3 text-sm text-on-surface font-mono uppercase tracking-wider"
            >
            <button @click="applyVoucherCode" :disabled="isApplyingVoucher" class="bg-primary text-on-primary font-bold px-6 py-3 rounded-xl hover:brightness-115 active:scale-[0.98] transition-all cursor-pointer disabled:opacity-60">
              {{ isApplyingVoucher ? 'Đang áp dụng...' : 'Áp dụng' }}
            </button>
          </div>
          <!-- Thất bại: đỏ -->
          <div v-if="voucherError" class="flex items-center gap-2 px-3 py-2.5 rounded-lg bg-red-500/10 border border-red-500/30">
            <span class="material-symbols-outlined text-red-400 text-base shrink-0">error</span>
            <p class="text-xs text-red-400 font-bold">{{ voucherError }}</p>
          </div>
          <!-- Thành công: xanh -->
          <div v-if="voucherSuccess" class="flex items-center gap-2 px-3 py-2.5 rounded-lg bg-green-500/10 border border-green-500/30">
            <span class="material-symbols-outlined text-green-400 text-base shrink-0">check_circle</span>
            <p class="text-xs text-green-400 font-bold flex-1">{{ voucherSuccess }}</p>
            <button v-if="store.selectedVoucher" @click="removeVoucher" class="shrink-0 text-[11px] text-on-surface-variant hover:text-red-400 font-bold flex items-center gap-1 transition-colors">
              <span class="material-symbols-outlined text-sm">close</span> Bỏ chọn
            </button>
          </div>

          <!-- Active Vouchers list -->
          <div v-if="visibleVouchers.length > 0" class="space-y-3 pt-4 border-t border-outline-variant/10">
            <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Voucher của bạn:</p>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div
                v-for="v in visibleVouchers"
                :key="v.id"
                @click="selectVoucher(v)"
                :class="[
                  store.selectedVoucher?.id === v.id ? 'border-primary bg-primary/5' : 'border-outline-variant/20 bg-surface-container-high/40',
                  voucherEvals[v.id] && !voucherEvals[v.id].applicable ? 'opacity-50 pointer-events-none' : 'cursor-pointer hover:border-primary/50'
                ]"
                class="border p-4 rounded-xl flex items-center justify-between transition-colors"
              >
                <div>
                  <p class="font-mono font-bold text-sm text-primary uppercase">{{ v.promotion.code }}</p>
                  <p class="text-[10px] text-on-surface-variant mt-1">Giảm {{ v.promotion.discountType === 'PERCENTAGE' ? v.promotion.discountValue + '%' : v.promotion.discountValue.toLocaleString() + 'đ' }}</p>
                  <p v-if="voucherEvals[v.id] && !voucherEvals[v.id].applicable" class="text-[10px] text-error font-bold mt-1 flex items-center gap-1">
                    <span class="material-symbols-outlined text-[13px]">block</span>
                    {{ voucherEvals[v.id].reason || 'Đơn không đủ điều kiện để áp dụng mã này' }}
                  </p>
                </div>
                <span v-if="store.selectedVoucher?.id === v.id" class="material-symbols-outlined text-primary">check_circle</span>
              </div>
            </div>
          </div>
        </div>

      </section>

      <!-- Section 4: Payment -->
      <section v-show="currentStep === 4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'VNPAY'}">
                <input type="radio" value="VNPAY" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="material-symbols-outlined text-primary-container">credit_card</span>
                <span class="font-bold">Thanh toán qua VNPAY</span>
            </label>
            <label class="glass-card p-4 rounded-xl flex items-center gap-4 cursor-pointer hover:border-primary-container transition-colors" :class="{'border-primary-container': paymentMethod === 'TRANSFER'}">
                <input type="radio" value="TRANSFER" v-model="paymentMethod" class="w-4 h-4 text-primary-container focus:ring-primary-container border-outline-variant/30 bg-transparent">
                <span class="material-symbols-outlined text-primary-container">qr_code_2</span>
                <span class="font-bold">Thanh toán bằng chuyển khoản</span>
            </label>
        </div>

        <!-- Khối QR chuyển khoản (VietQR tự sinh, giống POS) -->
        <div v-if="paymentMethod === 'TRANSFER'" class="glass-card p-8 rounded-2xl">
          <div v-if="transferQrUrl" class="flex flex-col md:flex-row gap-8 items-center">
            <div class="w-56 h-56 bg-white rounded-2xl p-3 flex-shrink-0">
              <img :src="transferQrUrl" alt="VietQR chuyển khoản" class="w-full h-full object-contain" />
            </div>
            <div class="flex-grow space-y-3 w-full">
              <h3 class="font-headline font-bold text-lg uppercase tracking-tight text-primary-container">Quét mã để chuyển khoản</h3>
              <div class="space-y-2 pt-2 border-t border-outline-variant/10">
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Số tiền</span><span class="font-bold text-primary-container">{{ finalPaymentPrice.toLocaleString('vi-VN') }} VNĐ</span></div>
                <div class="flex justify-between text-sm"><span class="text-on-surface-variant">Nội dung</span><span class="font-bold font-mono text-xs">{{ transferContent }}</span></div>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8">
            <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">account_balance</span>
            <p class="text-sm text-on-surface-variant">Rạp chưa cấu hình tài khoản nhận chuyển khoản. Vui lòng chọn VNPAY.</p>
          </div>
        </div>
      </section>
    </div>

    <!-- Persistent Sidebar Summary -->
    <aside class="w-full lg:w-[340px] flex-shrink-0 self-start sticky top-24 z-10">
      <div class="glass-card glass-shine-edge shadow-2xl rounded-3xl">
        <!-- Movie Header -->
        <div class="p-6 pb-5 border-b border-outline-variant/10">
          <div class="flex gap-6">
            <div class="w-16 h-24 flex-shrink-0 shadow-lg">
              <img :src="store.selectedMovie?.posterUrl || '/images/Hopper.webp'" class="w-full h-full object-cover rounded-lg"/>
            </div>
            <div class="flex flex-col justify-center min-w-0">
              <div class="flex items-center gap-1.5 mb-1">
                <span class="bg-error-container text-[9px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded w-fit text-white">
                  {{ store.selectedMovie?.ageRating || 'T18' }}
                </span>
                <span v-if="store.selectedShowtime?.formatName" class="border border-primary-container/60 text-primary-container text-[9px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded w-fit">
                  {{ store.selectedShowtime.formatName }}
                </span>
              </div>
              <h2 class="font-headline text-base font-bold leading-tight uppercase tracking-tight mb-1 line-clamp-2">
                {{ store.selectedMovie?.title || 'Phim đã chọn' }}
              </h2>
              <p class="text-[11px] text-on-surface-variant font-label truncate">
                {{ store.selectedShowtime?.cinema?.cinemaName }} • {{ store.selectedShowtime?.roomName }}
              </p>
              <p v-if="store.selectedShowtime?.startTime" class="text-[11px] text-primary-container/90 font-label mt-0.5">
                {{ new Date(store.selectedShowtime.startTime).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'}) }} · {{ new Date(store.selectedShowtime.startTime).toLocaleDateString('vi-VN') }}
              </p>
            </div>
          </div>
        </div>
        <!-- Detailed Selections -->
        <div class="p-8 space-y-6">
          <div>
            <div class="flex justify-between items-center mb-3">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ghế đã chọn</span>
              <span class="text-xs font-bold text-primary-container">{{ store.selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
            </div>
            <div v-for="t in ticketBreakdown" :key="t.label + t.price" class="flex justify-between text-sm">
              <span class="text-on-surface/60">{{ t.qty }} x {{ t.label }} - {{ (t.price).toLocaleString('vi-VN') }}đ</span>
            </div>
            <div class="flex justify-between text-sm pt-1">
              <span class="text-on-surface/60">{{ formattedSeatSummary }}</span>
              <span class="font-semibold">{{ seatsSubtotal.toLocaleString('vi-VN') }}đ</span>
            </div>
          </div>
          <div class="pt-6 border-t border-outline-variant/10" v-if="store.selectedFnbs.length > 0">
            <div class="flex justify-between items-center mb-3">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Bắp nước</span>
              <span class="text-xs font-bold text-primary-container">{{ store.selectedFnbs.reduce((acc, f) => acc + f.quantity, 0) }} sản phẩm</span>
            </div>
            <!-- Danh sách F&B: giới hạn chiều cao + cuộn để tổng tiền/nút Tiếp tục luôn thấy -->
            <div class="max-h-[13rem] overflow-y-auto pr-1 -mr-1 fnb-scroll">
              <div class="flex justify-between items-start gap-2 mt-3 first:mt-0" v-for="(fnb, idx) in store.selectedFnbs" :key="idx">
                <div class="flex-grow min-w-0">
                  <div class="flex items-center gap-2">
                     <span class="shrink-0 min-w-[1.5rem] text-center bg-surface-container-high rounded-md px-1.5 py-0.5 text-[10px] font-bold border border-outline-variant/10">×{{ fnb.quantity }}</span>
                     <div class="font-medium text-sm text-on-surface/90 truncate">{{ formatComboTitle(fnb.fnbItem.name).title }}</div>
                  </div>
                  <div v-if="fnb.options && fnb.options.length > 0" class="text-xs text-on-surface-variant/70 mt-1 pl-8 flex flex-wrap gap-1">
                     <span v-for="opt in fnb.options" :key="opt.optionItemId" class="bg-surface-container-highest px-1.5 py-0.5 rounded text-[10px]">
                        {{ opt.optionName }}<span v-if="opt.surchargePrice > 0" class="text-amber-400 font-medium ml-0.5">(+{{ Number(opt.surchargePrice).toLocaleString('vi-VN') }}đ)</span>
                     </span>
                  </div>
                  <div v-else-if="formatComboTitle(fnb.fnbItem.name).desc" class="text-xs text-on-surface-variant/70 mt-1 pl-8">{{ formatComboTitle(fnb.fnbItem.name).desc }}</div>
                </div>
                <div class="flex flex-col items-end gap-1 shrink-0">
                  <span class="font-semibold whitespace-nowrap">{{ ((fnb.fnbItem.price + (fnb.options || []).reduce((sum, o) => sum + (o.surchargePrice || 0), 0)) * fnb.quantity).toLocaleString('vi-VN') }}đ</span>
                  <button @click="store.updateFnb(fnb.fnbItem, 0, fnb.options)" class="text-on-surface-variant/40 hover:text-error-container transition-colors flex items-center" title="Bỏ khỏi đơn">
                    <span class="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
          <!-- Total Calculation -->
          <div class="pt-6 border-t border-outline-variant/10">
            <div class="bg-black/40 border border-white/5 p-5 rounded-xl space-y-2">
              <div class="flex justify-between items-center text-xs text-on-surface-variant">
                <span>Tạm tính</span>
                <span>{{ store.totalPrice.toLocaleString('vi-VN') }}đ</span>
              </div>
              <div v-if="discountAmount > 0" class="flex justify-between items-center text-xs text-green-400">
                <span>Khuyến mãi (voucher):</span>
                <span>-{{ discountAmount.toLocaleString('vi-VN') }}đ</span>
              </div>
              <div class="flex flex-col gap-1 border-t border-outline-variant/10 pt-3 mb-2 mt-1">
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tổng tiền</span>
                <span class="text-3xl font-headline font-extrabold text-primary-container text-right leading-none">{{ finalPaymentPrice.toLocaleString('vi-VN') }}<span class="text-sm ml-1 text-primary-container/70">đ</span></span>
              </div>
              <p class="text-[10px] text-outline-variant text-right italic">(VAT & Phí dịch vụ đã bao gồm)</p>
            </div>
          </div>
          <!-- Action Button (theo bước hiện tại) -->
          <button
            v-if="currentStep < steps.length"
            @click="goNext"
            :disabled="!canProceed"
            class="group w-full bg-gradient-to-r from-primary to-amber-500 text-black py-4 rounded-2xl font-headline font-extrabold text-sm tracking-[0.12em] uppercase shadow-[0_8px_24px_-6px_rgba(245,197,24,0.5)] hover:shadow-[0_10px_30px_-4px_rgba(245,197,24,0.65)] hover:brightness-105 active:scale-[0.98] transition-all flex items-center justify-center gap-2.5 disabled:grayscale disabled:opacity-50 disabled:shadow-none disabled:cursor-not-allowed"
          >
            Tiếp tục
            <span class="material-symbols-outlined text-xl group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </button>
          <button
            v-else
            @click="proceedToPayment"
            :disabled="store.selectedSeats.length === 0 || isPaying"
            class="group w-full bg-gradient-to-r from-primary to-amber-500 text-black py-4 rounded-2xl font-headline font-extrabold text-sm tracking-[0.12em] uppercase shadow-[0_8px_24px_-6px_rgba(245,197,24,0.5)] hover:shadow-[0_10px_30px_-4px_rgba(245,197,24,0.65)] hover:brightness-105 active:scale-[0.98] transition-all flex items-center justify-center gap-2.5 disabled:grayscale disabled:opacity-50 disabled:shadow-none disabled:cursor-not-allowed"
          >
            <svg v-if="isPaying" class="animate-spin -ml-1 mr-2 h-5 w-5 text-black" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span v-else class="material-symbols-outlined text-xl">lock</span>
            {{ isPaying ? 'Đang xử lý...' : 'Xác nhận thanh toán' }}
          </button>
        </div>
      </div>
      <div v-if="showStudentVerificationWarning" class="mt-4 bg-orange-500/10 border border-orange-500/30 rounded-xl p-3 flex gap-3 text-orange-400">
        <span class="material-symbols-outlined text-base mt-0.5 shrink-0">info</span>
        <p class="text-[11px] font-medium leading-relaxed">Lưu ý: Cả 2 người xem ghế Sweetbox bắt buộc xuất trình Thẻ HSSV/CCCD chính chủ khi soát vé.</p>
      </div>
    </aside>
    </div>

    <FnbOptionModal
      :isOpen="isFnbModalOpen"
      :fnbItem="selectedFnbForModal"
      @close="isFnbModalOpen = false"
      @confirm="handleFnbOptionConfirm"
    />
  </main>
</template>

<style scoped>
/* Thanh cuộn mảnh cho danh sách F&B trong sidebar */
.fnb-scroll { scrollbar-width: thin; scrollbar-color: rgba(245,197,24,0.35) transparent; }
.fnb-scroll::-webkit-scrollbar { width: 4px; }
.fnb-scroll::-webkit-scrollbar-thumb { background: rgba(245,197,24,0.35); border-radius: 4px; }
.fnb-scroll::-webkit-scrollbar-track { background: transparent; }
.seat-grid { perspective: 1000px; }
.screen-curve { box-shadow: 0 -20px 50px -10px rgba(245, 197, 24, 0.3); }
/* Ô mẫu trong chú thích: vẽ lại dấu "X" giống ghế không thể chọn trên sơ đồ */
.legend-unselectable::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  --x-line: rgba(255, 255, 255, 0.9);
  background-color: rgba(28, 31, 38, 0.85);
  background-image:
    linear-gradient(to bottom right, transparent calc(50% - 1px), var(--x-line) calc(50% - 1px), var(--x-line) calc(50% + 1px), transparent calc(50% + 1px)),
    linear-gradient(to bottom left, transparent calc(50% - 1px), var(--x-line) calc(50% - 1px), var(--x-line) calc(50% + 1px), transparent calc(50% + 1px));
}
</style>
