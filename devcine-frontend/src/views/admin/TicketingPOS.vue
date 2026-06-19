<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ticketingApi, settingsApi } from '@/api/admin/index'
import AppButton from '../../components/common/AppButton.vue'

const currentStep = ref(1) // 1: Showtime, 2: Seats, 3: Confirm, 4: F&B, 5: Payment, 6: Done

const showtimes = ref([])
const combos = ref([])
const isLoading = ref(false)

const selectedShowtime = ref(null)
const seatData = ref({ matrixRow: 9, matrixCol: 10, seats: [] })
const isLoadingSeats = ref(false)
const selectedSeats = ref([]) // seat objects from map
const selectedCombos = ref([]) // { id, name, price, quantity }

const member = ref(null)
const cardNumberInput = ref('')
const isCheckingCard = ref(false)
const cardError = ref('')

const paymentMethod = ref('CASH')
const isPaying = ref(false)
const completedBooking = ref(null)

// Thanh toán: tài khoản nhận tiền (QR) + modal tiền mặt / QR
const bankInfo = ref({ code: '', name: '', accountNo: '', accountName: '' })
const showCashModal = ref(false)
const showQrModal = ref(false)
const cashGiven = ref(0)

const error = ref('')

// Toast
const toast = ref({ show: false, type: 'success', message: '' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  toast.value = { show: true, type, message }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 3000)
}

const seatTypeLabel = (t) => ({ NORMAL: 'Thường', STANDARD: 'Thường', VIP: 'VIP', SWEETBOX: 'Sweetbox' }[t] || t)

const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const [stRes, cbRes] = await Promise.all([
      ticketingApi.getShowtimes(),
      ticketingApi.getCombos()
    ])
    showtimes.value = stRes.data.data ?? stRes.data
    combos.value = cbRes.data.data ?? cbRes.data
  } catch (err) {
    error.value = 'Không tải được dữ liệu bán vé. Kiểm tra đăng nhập/quyền.'
  } finally {
    isLoading.value = false
  }
}

const selectShowtime = async (st) => {
  selectedShowtime.value = st
  selectedSeats.value = []
  isLoadingSeats.value = true
  currentStep.value = 2
  try {
    const { data } = await ticketingApi.getSeats(st.id)
    seatData.value = data.seats ? data : { matrixRow: 9, matrixCol: 10, seats: Array.isArray(data) ? data : [] }
  } catch (err) {
    showToast('Không tải được sơ đồ ghế.', 'error')
    seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  } finally {
    isLoadingSeats.value = false
  }
}

const seatAt = (row, col) => seatData.value.seats.find(s => s.gridRow === row && s.gridCol === col)
const isSelected = (seat) => selectedSeats.value.some(s => s.seatId === seat.seatId)
// Nhãn hàng (A, B, C...) suy từ ghế đầu tiên có trên hàng đó
const rowLabel = (gridRow) => {
  const s = seatData.value.seats.find(x => x.gridRow === gridRow)
  return s ? s.rowChar : ''
}

const toggleSeat = (seat) => {
  if (!seat || seat.status !== 'AVAILABLE') return
  const idx = selectedSeats.value.findIndex(s => s.seatId === seat.seatId)
  if (idx > -1) selectedSeats.value.splice(idx, 1)
  else selectedSeats.value.push(seat)
}

const seatClass = (seat) => {
  const base = 'w-8 h-8 rounded-lg flex items-center justify-center text-[9px] font-bold border transition-all leading-none'
  if (!seat) return ''
  if (seat.status === 'SOLD') return `${base} bg-surface-container-high border-white/5 text-on-surface-variant/20 cursor-not-allowed opacity-40`
  if (seat.status === 'HOLD') return `${base} bg-yellow-500/10 border-yellow-500/30 text-yellow-500/60 cursor-not-allowed`
  if (isSelected(seat)) return `${base} bg-primary border-primary text-on-primary shadow-lg shadow-primary/30 cursor-pointer scale-105`
  const byType = {
    VIP: 'bg-red-900/40 border-red-500/40 text-red-200 hover:border-red-400',
    SWEETBOX: 'bg-purple-900/40 border-purple-500/40 text-purple-200 hover:border-purple-400'
  }[seat.seatType] || 'bg-surface-container-high border-outline-variant/10 text-on-surface-variant/60 hover:border-primary/40'
  return `${base} ${byType} cursor-pointer`
}

// F&B
const addCombo = (cb) => {
  const existing = selectedCombos.value.find(c => c.id === cb.id)
  if (existing) existing.quantity++
  else selectedCombos.value.push({ id: cb.id, name: cb.name, price: Number(cb.price), quantity: 1 })
}
const changeComboQty = (item, delta) => {
  item.quantity += delta
  if (item.quantity <= 0) {
    const idx = selectedCombos.value.findIndex(c => c.id === item.id)
    if (idx > -1) selectedCombos.value.splice(idx, 1)
  }
}

const seatTotal = computed(() => selectedSeats.value.reduce((a, s) => a + Number(s.price || 0), 0))
const comboTotal = computed(() => selectedCombos.value.reduce((a, c) => a + c.price * c.quantity, 0))
const totalPrice = computed(() => seatTotal.value + comboTotal.value)

const seatTypeBreakdown = computed(() => {
  const map = {}
  for (const s of selectedSeats.value) {
    const key = s.seatType
    if (!map[key]) map[key] = { type: key, count: 0, subtotal: 0 }
    map[key].count++
    map[key].subtotal += Number(s.price || 0)
  }
  return Object.values(map)
})

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')

// ---- Thanh toán tiền mặt ----
const changeDue = computed(() => Math.max(0, Number(cashGiven.value || 0) - totalPrice.value))
const canConfirmCash = computed(() => Number(cashGiven.value || 0) >= totalPrice.value && totalPrice.value > 0)
const cashSuggestions = computed(() => {
  const t = totalPrice.value
  if (t <= 0) return []
  const set = new Set([t])
  for (const note of [50000, 100000, 200000, 500000]) set.add(Math.ceil(t / note) * note)
  return Array.from(set).filter(v => v >= t).sort((a, b) => a - b).slice(0, 5)
})

// ---- Thanh toán QR (VietQR) ----
const removeDiacritics = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')
const transferContent = computed(() => {
  const seats = selectedSeats.value.map(s => s.rowChar + s.colNum).join('')
  return removeDiacritics(`DevCine ve ${seats}`).slice(0, 50)
})
const vietQrUrl = computed(() => {
  const b = bankInfo.value
  if (!b.code || !b.accountNo) return ''
  return `https://img.vietqr.io/image/${b.code}-${b.accountNo}-compact2.png?amount=${totalPrice.value}&addInfo=${encodeURIComponent(transferContent.value)}&accountName=${encodeURIComponent(b.accountName || '')}`
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
    // Không chặn POS nếu lỗi — modal QR sẽ báo "chưa cấu hình"
  }
}

const openCashModal = () => {
  if (selectedSeats.value.length === 0) { showToast('Chưa chọn ghế.', 'error'); return }
  cashGiven.value = 0
  showCashModal.value = true
}
const openQrModal = () => {
  if (selectedSeats.value.length === 0) { showToast('Chưa chọn ghế.', 'error'); return }
  showQrModal.value = true
}

const checkMemberCard = async () => {
  cardError.value = ''
  if (!cardNumberInput.value.trim()) return
  isCheckingCard.value = true
  try {
    const { data } = await ticketingApi.memberCard(cardNumberInput.value.trim())
    member.value = data.data ?? data
  } catch (err) {
    cardError.value = err.response?.data?.error || 'Không tìm thấy thẻ thành viên.'
    member.value = null
  } finally {
    isCheckingCard.value = false
  }
}
const clearMember = () => { member.value = null; cardNumberInput.value = ''; cardError.value = '' }

const processPayment = async (method) => {
  if (selectedSeats.value.length === 0) {
    showToast('Chưa chọn ghế.', 'error')
    return
  }
  paymentMethod.value = method
  isPaying.value = true
  try {
    const payload = {
      showtimeId: selectedShowtime.value.id,
      seatIds: selectedSeats.value.map(s => s.seatId),
      fnbs: selectedCombos.value.map(c => ({ fnbItemId: c.id, quantity: c.quantity })),
      customerId: member.value ? member.value.customerId : null,
      paymentMethod: method
    }
    const { data } = await ticketingApi.pay(payload)
    if (data.success) {
      completedBooking.value = data
      showCashModal.value = false
      showQrModal.value = false
      currentStep.value = 6
    } else {
      showToast(data.message || 'Thanh toán thất bại.', 'error')
    }
  } catch (err) {
    showToast(err.response?.data?.message || 'Thanh toán thất bại (ghế có thể đã bán).', 'error')
  } finally {
    isPaying.value = false
  }
}

// ===== In hoá đơn (mở tab mới: hoá đơn + vé QR soát cổng) =====
const esc = (v) => String(v ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const qrUrl = (code) => `https://api.qrserver.com/v1/create-qr-code/?size=200x200&margin=0&data=${encodeURIComponent(code)}`
const paymentLabel = (m) => ({ CASH: 'Tiền mặt', CARD: 'Thẻ / QR', TRANSFER: 'Chuyển khoản QR', WALLET: 'Ví điện tử' }[m] || m)

const buildInvoiceHtml = () => {
  const st = selectedShowtime.value
  const movie = esc(st?.movieTitle)
  const room = esc(st?.roomName)
  const format = esc(st?.formatName)
  const bookingCode = esc(completedBooking.value?.bookingCode)
  const tickets = completedBooking.value?.tickets || []
  const dateStr = st?.startTime
    ? new Date(st.startTime).toLocaleString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    : ''
  const printedAt = new Date().toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

  const itemRow = (name, qty, unit, total) =>
    `<tr><td>${name}</td><td class="c">${qty}</td><td class="r">${fmt(unit)}đ</td><td class="r b">${fmt(total)}đ</td></tr>`

  const seatRows = seatTypeBreakdown.value.map(b => {
    const labels = selectedSeats.value.filter(s => s.seatType === b.type).map(s => s.rowChar + s.colNum).join(', ')
    const unit = b.count ? b.subtotal / b.count : 0
    return itemRow(`Ghế ${esc(seatTypeLabel(b.type))} <span class="muted">${esc(labels)}</span>`, b.count, unit, b.subtotal)
  }).join('')

  const comboRows = selectedCombos.value
    .map(c => itemRow(esc(c.name), c.quantity, c.price, c.price * c.quantity)).join('')

  // Mỗi ghế = 1 tờ vé riêng (không gộp), tách trang khi in
  const ticketSlips = tickets.length
    ? tickets.map((t, i) => `
        <article class="ticket">
          <div class="tk-main">
            <div class="tk-head">
              <div class="mono serif">D</div>
              <div>
                <div class="tk-brand">DEV<span class="g">CINE</span></div>
                <div class="tk-sub">Vé xem phim · Admit One</div>
              </div>
              <div class="tk-no serif">${String(i + 1).padStart(2, '0')}<span>/${String(tickets.length).padStart(2, '0')}</span></div>
            </div>
            <div class="tk-seat-row">
              <div>
                <div class="tk-k">Ghế ngồi</div>
                <div class="tk-seat serif">${esc(t.seatLabel)}</div>
              </div>
              <div class="tk-movie">${movie}</div>
            </div>
            <dl class="tk-meta">
              <div><dt>Phòng chiếu</dt><dd>${room} · ${format}</dd></div>
              <div><dt>Suất chiếu</dt><dd>${esc(dateStr)}</dd></div>
              <div><dt>Mã đơn</dt><dd>${bookingCode}</dd></div>
              <div><dt>Loại vé</dt><dd>Người lớn</dd></div>
            </dl>
          </div>
          <div class="tk-stub">
            <div class="tk-stub-t">Quét để vào cổng</div>
            <img src="${qrUrl(t.qrCode)}" alt="QR ${esc(t.seatLabel)}" />
            <div class="tk-code">${esc(t.qrCode)}</div>
            <div class="tk-note">Xuất trình tại cổng soát vé</div>
          </div>
        </article>`).join('')
    : '<div class="ticket"><p class="muted" style="padding:28px">Không có dữ liệu vé QR.</p></div>'

  const seatCount = selectedSeats.value.length
  const seatSection = seatRows ? `<tr class="grp"><td colspan="4">Vé xem phim</td></tr>${seatRows}` : ''
  const comboSection = comboRows ? `<tr class="grp"><td colspan="4">Bắp nước &amp; Combo</td></tr>${comboRows}` : ''

  // Số tiền giảm khi áp mã/voucher (0 nếu không có). Đọc từ BE nếu có để tương thích về sau.
  const discount = Number(completedBooking.value?.discountAmount || 0)
  const grandTotal = Math.max(0, seatTotal.value + comboTotal.value - discount)

  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<title>Hoá đơn ${bookingCode} — DevCine</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;0,800;0,900;1,500&family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,Arial,sans-serif;background:#efe8da;color:#26221b;padding:34px 20px;font-size:14px;-webkit-print-color-adjust:exact;print-color-adjust:exact}
  .serif{font-family:'Playfair Display',Georgia,serif}
  .mono-num{font-variant-numeric:tabular-nums}
  .g{background:linear-gradient(135deg,#e6c878,#c4992f);-webkit-background-clip:text;background-clip:text;color:transparent}

  .bar{display:flex;justify-content:center;gap:12px;max-width:880px;margin:0 auto 26px}
  .bar button{border:0;border-radius:999px;padding:13px 30px;font-weight:700;font-size:11px;letter-spacing:.14em;text-transform:uppercase;cursor:pointer}
  .btn-print{background:linear-gradient(135deg,#dcb869,#b8902f);color:#1c1a17;box-shadow:0 8px 22px rgba(184,144,47,.4)}
  .btn-close{background:#fff;color:#6b6456;border:1px solid #ddd4c1}

  /* ===== BILL ===== */
  .bill{max-width:880px;margin:0 auto;background:#fffdf8;border-radius:20px;overflow:hidden;box-shadow:0 30px 70px rgba(40,34,22,.18);border:1px solid #ece3d0}
  .bill-head{display:flex;justify-content:space-between;align-items:center;padding:36px 44px 30px;background:linear-gradient(160deg,#211d16,#14110c);color:#f3ecdc;position:relative}
  .bill-head::after{content:'';position:absolute;left:0;right:0;bottom:0;height:3px;background:linear-gradient(90deg,#b8902f,#e6c878,#b8902f)}
  .brand{display:flex;align-items:center;gap:16px}
  .mono{width:54px;height:54px;border-radius:15px;background:linear-gradient(135deg,#e9cd80,#b8902f);display:flex;align-items:center;justify-content:center;font-size:32px;font-weight:900;color:#1c1a17;box-shadow:0 8px 18px rgba(0,0,0,.35);flex:none}
  .brand-name{font-size:27px;font-weight:800;letter-spacing:.2em;line-height:1}
  .brand-tag{font-size:9px;letter-spacing:.32em;text-transform:uppercase;color:#a89c81;margin-top:7px}
  .doc{text-align:right}
  .doc-t{font-size:19px;letter-spacing:.34em;text-transform:uppercase;color:#e6c878}
  .doc-meta{margin-top:13px;font-size:11px;line-height:1.95;color:#a89c81}
  .doc-meta b{color:#f3ecdc;font-weight:600}

  .feature{display:flex;align-items:center;gap:16px;padding:24px 44px;border-bottom:1px solid #efe6d3}
  .feature .ico{width:46px;height:46px;border-radius:13px;background:#f7efe0;display:flex;align-items:center;justify-content:center;font-size:22px;flex:none}
  .feature h2{font-size:23px;font-weight:700;line-height:1.15;color:#211d16}
  .feature p{color:#8c836d;font-size:12.5px;margin-top:5px;letter-spacing:.01em}

  table{width:100%;border-collapse:collapse}
  thead th{font-size:9.5px;letter-spacing:.18em;text-transform:uppercase;color:#a89c81;text-align:left;padding:20px 44px 8px;font-weight:700}
  thead th.c{text-align:center}thead th.r{text-align:right}
  tbody td{padding:13px 44px;font-size:13.5px;border-top:1px solid #f3ecdd;color:#3a342a}
  td.c{text-align:center}td.r{text-align:right;font-variant-numeric:tabular-nums}td.b{font-weight:700;color:#211d16}
  tr.grp td{padding:18px 44px 6px;font-size:10px;letter-spacing:.2em;text-transform:uppercase;color:#b8902f;font-weight:800;border-top:0}
  tr.grp+tr td{border-top:0}
  .muted{color:#b3a994;font-weight:500;font-size:12px}

  .summary{padding:14px 44px 6px;display:flex;flex-direction:column;align-items:flex-end}
  .s-row{display:flex;justify-content:space-between;width:min(340px,100%);padding:10px 0;font-size:13px;color:#6f6755;border-top:1px solid #f3ecdd}
  .s-row b{color:#26221b;font-weight:600;font-variant-numeric:tabular-nums}
  .s-row b.cut{color:#3b6d11}
  .s-grand{display:flex;justify-content:space-between;align-items:baseline;width:min(340px,100%);margin-top:12px;padding-top:18px;border-top:2px solid #211d16}
  .s-grand span{font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8c836d;font-weight:700}
  .s-grand b{font-size:33px;font-weight:800;color:#211d16;font-variant-numeric:tabular-nums;line-height:1}
  .s-grand b .u{font-size:17px;color:#b8902f;margin-left:3px}

  .foot{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-top:20px;padding:22px 44px;background:#faf4e9;border-top:1px solid #efe6d3}
  .foot .pm{font-size:12px;color:#6f6755;line-height:1.7}
  .foot .pm b{color:#26221b;font-weight:600}
  .stamp{font-size:10px;font-weight:800;letter-spacing:.16em;text-transform:uppercase;color:#b8902f;border:1.5px solid #dcb869;border-radius:9px;padding:9px 15px;transform:rotate(-4deg);white-space:nowrap;flex:none}
  .thanks{text-align:center;font-style:italic;font-size:15px;color:#9a8f76;padding:20px}

  .divider{max-width:880px;margin:34px auto 20px;display:flex;align-items:center;gap:18px;color:#9a8f76}
  .divider::before,.divider::after{content:'';flex:1;height:1px}
  .divider::before{background:linear-gradient(90deg,transparent,#c9bda1)}
  .divider::after{background:linear-gradient(90deg,#c9bda1,transparent)}
  .divider span{font-size:10px;letter-spacing:.28em;text-transform:uppercase;font-weight:700;white-space:nowrap}

  /* ===== TICKET ===== */
  .ticket{max-width:880px;margin:0 auto 22px;display:flex;background:#fffdf8;border-radius:18px;box-shadow:0 22px 54px rgba(40,34,22,.15);overflow:hidden;border:1px solid #ece3d0;position:relative}
  .ticket::before{content:'';position:absolute;left:0;top:0;bottom:0;width:7px;background:linear-gradient(180deg,#e9cd80,#b8902f)}
  .tk-main{flex:1;padding:28px 34px;min-width:0}
  .tk-head{display:flex;align-items:center;gap:13px}
  .tk-head .mono{width:40px;height:40px;border-radius:11px;font-size:22px}
  .tk-brand{font-size:16px;font-weight:800;letter-spacing:.16em;line-height:1}
  .tk-sub{font-size:9px;letter-spacing:.2em;text-transform:uppercase;color:#a89c81;margin-top:4px}
  .tk-no{margin-left:auto;font-size:28px;font-weight:800;color:#211d16}
  .tk-no span{font-size:15px;color:#b3a994}
  .tk-seat-row{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin:22px 0 20px;padding-bottom:20px;border-bottom:1px dashed #ddd2bb}
  .tk-k{font-size:9px;letter-spacing:.2em;text-transform:uppercase;color:#a89c81;font-weight:700}
  .tk-seat{font-size:50px;font-weight:900;line-height:.95;color:#211d16;margin-top:4px}
  .tk-movie{font-size:15px;font-weight:700;text-transform:uppercase;text-align:right;color:#3a342a;letter-spacing:.01em;max-width:55%}
  .tk-meta{display:grid;grid-template-columns:1fr 1fr;gap:14px 24px}
  .tk-meta dt{font-size:9px;letter-spacing:.16em;text-transform:uppercase;color:#a89c81;font-weight:700}
  .tk-meta dd{font-size:13px;font-weight:600;color:#26221b;margin-top:4px}

  .tk-stub{width:218px;flex:none;padding:24px 22px;text-align:center;background:#faf4e9;display:flex;flex-direction:column;align-items:center;justify-content:center;border-left:2px dashed #d7ccb3;position:relative}
  .tk-stub::before,.tk-stub::after{content:'';position:absolute;left:-12px;width:22px;height:22px;border-radius:50%;background:#efe8da}
  .tk-stub::before{top:-11px}
  .tk-stub::after{bottom:-11px}
  .tk-stub-t{font-size:9px;letter-spacing:.18em;text-transform:uppercase;color:#a89c81;font-weight:700;margin-bottom:12px}
  .tk-stub img{width:152px;height:152px;display:block}
  .tk-code{font-family:'Courier New',monospace;font-size:9.5px;color:#8c836d;margin-top:11px;word-break:break-all;letter-spacing:.02em}
  .tk-note{font-size:9px;letter-spacing:.1em;text-transform:uppercase;color:#b8902f;font-weight:700;margin-top:9px}

  @media print{
    body{background:#fff;padding:0}
    .bar,.divider{display:none}
    .bill,.ticket{box-shadow:none;border-radius:0;max-width:100%;border:0;margin:0}
    .ticket{break-before:page}
    .ticket::before{display:none}
    .tk-stub::before,.tk-stub::after{background:#fff}
  }
</style></head>
<body>
  <div class="bar">
    <button class="btn-print" onclick="window.print()">🖨 In hoá đơn</button>
    <button class="btn-close" onclick="window.close()">Đóng</button>
  </div>

  <section class="bill">
    <div class="bill-head">
      <div class="brand">
        <div class="mono serif">D</div>
        <div>
          <div class="brand-name">DEV<span class="g">CINE</span></div>
          <div class="brand-tag">Cinema · Hệ thống rạp chiếu phim</div>
        </div>
      </div>
      <div class="doc">
        <div class="doc-t serif">Hoá Đơn</div>
        <div class="doc-meta">
          Số: <b>${bookingCode}</b><br/>
          Ngày in: <b>${esc(printedAt)}</b><br/>
          Quầy: <b>POS · Lễ tân</b>
        </div>
      </div>
    </div>

    <div class="feature">
      <div class="ico">🎬</div>
      <div>
        <h2 class="serif">${movie}</h2>
        <p>${format} · ${room} · Suất ${esc(dateStr)}</p>
      </div>
    </div>

    <table>
      <thead><tr><th>Nội dung</th><th class="c">SL</th><th class="r">Đơn giá</th><th class="r">Thành tiền</th></tr></thead>
      <tbody>${seatSection}${comboSection}</tbody>
    </table>

    <div class="summary">
      <div class="s-row"><span>Tạm tính vé · ${seatCount} ghế</span><b>${fmt(seatTotal.value)}đ</b></div>
      ${comboTotal.value > 0 ? `<div class="s-row"><span>Bắp nước &amp; combo</span><b>${fmt(comboTotal.value)}đ</b></div>` : ''}
      <div class="s-row"><span>Số tiền được giảm</span><b class="${discount > 0 ? 'cut' : ''}">${discount > 0 ? '−' + fmt(discount) : '0'}đ</b></div>
      <div class="s-grand"><span>Tổng thanh toán</span><b class="serif">${fmt(grandTotal)}<span class="u">đ</span></b></div>
    </div>

    <div class="foot">
      <div class="pm">Phương thức: <b>${esc(paymentLabel(paymentMethod.value))}</b>${member.value ? `<br/>Thành viên: <b>${esc(member.value.fullName)}</b> · ${esc(member.value.membershipTier)}` : ''}</div>
      <div class="stamp">Đã thanh toán</div>
    </div>
    <div class="thanks serif">Cảm ơn quý khách & hẹn gặp lại tại DevCine</div>
  </section>

  <div class="divider"><span>Vé xem phim · ${tickets.length} vé</span></div>

  ${ticketSlips}
</body></html>`
}

const printInvoice = () => {
  if (!completedBooking.value) return
  const win = window.open('', '_blank')
  if (!win) {
    showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để in hoá đơn.', 'error')
    return
  }
  win.document.open()
  win.document.write(buildInvoiceHtml())
  win.document.close()
}

// ===== Hiển thị mã QR toàn màn hình (tab mới) cho khách quét =====
const buildQrPageHtml = () => {
  const b = bankInfo.value
  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Quét mã chuyển khoản — DevCine</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700;800;900&family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,Arial,sans-serif;background:#efe8da;color:#26221b;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
  .card{width:100%;max-width:580px;background:#fffdf8;border-radius:24px;overflow:hidden;box-shadow:0 30px 70px rgba(40,34,22,.2);border:1px solid #ece3d0}
  .head{background:linear-gradient(160deg,#211d16,#14110c);color:#f3ecdc;padding:24px 30px;display:flex;align-items:center;gap:14px;position:relative}
  .head::after{content:'';position:absolute;left:0;right:0;bottom:0;height:3px;background:linear-gradient(90deg,#b8902f,#e6c878,#b8902f)}
  .mono{width:46px;height:46px;border-radius:13px;background:linear-gradient(135deg,#e9cd80,#b8902f);display:flex;align-items:center;justify-content:center;font-size:27px;font-weight:900;color:#1c1a17;font-family:'Playfair Display',serif;flex:none}
  .brand{font-size:22px;font-weight:800;letter-spacing:.18em;line-height:1}
  .brand .g{color:#e6c878}
  .brand small{display:block;font-size:9px;letter-spacing:.28em;color:#a89c81;margin-top:6px;font-weight:600;text-transform:uppercase}
  .body{padding:28px 30px 32px;text-align:center}
  .qr{width:460px;max-width:100%;aspect-ratio:1;margin:0 auto;background:#fff;border-radius:18px;padding:12px;border:1px solid #eee}
  .qr img{width:100%;height:100%;object-fit:contain}
  .hint{font-size:13px;color:#8c836d;margin:16px 0 22px}
  .rows{text-align:left;border-top:1px solid #efe6d3;padding-top:18px}
  .row{display:flex;justify-content:space-between;align-items:center;padding:9px 0;font-size:15px;border-bottom:1px solid #f3ecdd}
  .row span{color:#8c836d}
  .row b{color:#26221b;font-weight:700}
  .row .mono-no{font-family:'Courier New',monospace}
  .amount{margin-top:18px;background:#211d16;border-radius:14px;padding:16px 20px;display:flex;justify-content:space-between;align-items:center}
  .amount span{font-size:11px;letter-spacing:.16em;text-transform:uppercase;color:#a89c81;font-weight:700}
  .amount b{font-family:'Playfair Display',serif;font-size:30px;font-weight:800;color:#e6c878}
  .note{margin-top:18px;font-size:12px;color:#9a8f76;font-style:italic}
</style></head>
<body>
  <div class="card">
    <div class="head">
      <div class="mono">D</div>
      <div class="brand">DEV<span class="g">CINE</span><small>Quét mã để chuyển khoản</small></div>
    </div>
    <div class="body">
      <div class="qr"><img src="${vietQrUrl.value}" alt="VietQR" /></div>
      <p class="hint">Mở app ngân hàng → quét mã. Số tiền &amp; nội dung tự điền.</p>
      <div class="rows">
        <div class="row"><span>Ngân hàng</span><b>${esc(b.name)}</b></div>
        <div class="row"><span>Số tài khoản</span><b class="mono-no">${esc(b.accountNo)}</b></div>
        <div class="row"><span>Chủ tài khoản</span><b>${esc(b.accountName)}</b></div>
        <div class="row"><span>Nội dung</span><b>${esc(transferContent.value)}</b></div>
      </div>
      <div class="amount"><span>Số tiền</span><b>${fmt(totalPrice.value)}đ</b></div>
      <p class="note">Sau khi khách chuyển khoản, nhân viên bấm "Xác nhận đã chuyển khoản" trên màn hình POS.</p>
    </div>
  </div>
</body></html>`
}

const openQrFullscreen = () => {
  if (!vietQrUrl.value) return
  const win = window.open('', '_blank')
  if (!win) {
    showToast('Trình duyệt đã chặn cửa sổ. Hãy cho phép pop-up để hiển thị mã.', 'error')
    return
  }
  win.document.open()
  win.document.write(buildQrPageHtml())
  win.document.close()
}

const resetPOS = () => {
  currentStep.value = 1
  selectedShowtime.value = null
  seatData.value = { matrixRow: 9, matrixCol: 10, seats: [] }
  selectedSeats.value = []
  selectedCombos.value = []
  member.value = null
  cardNumberInput.value = ''
  cardError.value = ''
  completedBooking.value = null
  showCashModal.value = false
  showQrModal.value = false
  cashGiven.value = 0
  fetchData()
}

onMounted(() => { fetchData(); loadBankInfo() })
onUnmounted(() => { if (toastTimer) clearTimeout(toastTimer) })
</script>

<template>
  <div class="h-full flex flex-col p-6 space-y-5 bg-surface-container-lowest">
    <!-- Header -->
    <header class="flex justify-between items-center bg-surface px-5 py-3 rounded-2xl border border-outline-variant/10 shadow-xl">
      <div class="flex items-center gap-4">
        <div class="w-9 h-9 bg-primary rounded-xl flex items-center justify-center text-on-primary shadow-lg shadow-primary/20">
          <span class="material-symbols-outlined text-xl">point_of_sale</span>
        </div>
        <div>
          <h1 class="text-lg font-black tracking-tighter uppercase italic text-on-surface leading-none">Ticketing <span class="text-primary">POS</span></h1>
          <p class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mt-0.5">Hệ thống bán vé & F&B v2.1</p>
        </div>
      </div>

      <div class="flex items-center gap-1.5">
        <div v-for="i in 6" :key="i" class="flex items-center gap-1.5">
          <div :class="currentStep >= i ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant/40'"
               class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black transition-all">{{ i }}</div>
          <div v-if="i < 6" class="w-6 h-0.5 bg-outline-variant/20"></div>
        </div>
      </div>

      <AppButton variant="outline" @click="resetPOS">Hủy giao dịch</AppButton>
    </header>

    <main class="flex-grow grid grid-cols-12 gap-5 overflow-hidden">
      <div class="col-span-9 bg-surface border border-outline-variant/10 rounded-3xl shadow-2xl overflow-hidden flex flex-col">

        <!-- Step 1: Showtime -->
        <div v-if="currentStep === 1" class="p-6 space-y-8 overflow-y-auto custom-scrollbar">
          <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
            <span class="w-8 h-1 bg-primary rounded-full"></span> 1. Chọn phim & suất chiếu
          </h2>

          <div v-if="isLoading" class="grid grid-cols-2 gap-6">
            <div v-for="i in 4" :key="i" class="h-44 bg-surface-container-high rounded-3xl animate-pulse"></div>
          </div>
          <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-2xl text-red-400 text-sm">{{ error }}</div>
          <div v-else-if="showtimes.length === 0" class="py-20 text-center border border-dashed border-outline-variant/20 rounded-3xl">
            <span class="material-symbols-outlined text-5xl text-on-surface-variant/40 mb-3">event_busy</span>
            <p class="text-on-surface-variant font-semibold">Không có suất chiếu nào hôm nay/sắp tới.</p>
            <p class="text-xs text-on-surface-variant/60 mt-1">Tạo suất chiếu ở "Lịch chiếu & Điều phối".</p>
          </div>

          <div v-else class="grid grid-cols-2 gap-6">
            <div v-for="st in showtimes" :key="st.id" @click="selectShowtime(st)"
                 class="p-6 bg-surface-container-high rounded-3xl border border-outline-variant/10 hover:border-primary/50 hover:bg-primary/5 transition-all cursor-pointer group">
              <div class="flex gap-6">
                <div class="w-24 h-36 bg-surface-container-highest rounded-xl overflow-hidden shadow-lg border border-outline-variant/10">
                  <img :src="st.moviePoster || '/images/Hopper.webp'" class="w-full h-full object-cover group-hover:scale-105 transition-transform" />
                </div>
                <div class="flex flex-col justify-between py-1">
                  <div>
                    <h3 class="font-black text-lg uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors">{{ st.movieTitle }}</h3>
                    <p class="text-[10px] font-bold text-on-surface-variant uppercase mt-1">{{ st.formatName }} • {{ st.roomName }}</p>
                    <p class="text-[10px] font-bold text-on-surface-variant/70 mt-1">{{ new Date(st.startTime).toLocaleDateString('vi-VN') }}</p>
                  </div>
                  <span class="px-4 py-2 bg-primary/10 text-primary text-sm font-black italic rounded-xl border border-primary/20 w-fit mt-3">
                    {{ new Date(st.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Seats -->
        <div v-if="currentStep === 2" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 2. Chọn ghế ({{ selectedSeats.length }})
            </h2>
            <AppButton variant="ghost" @click="currentStep = 1">Quay lại</AppButton>
          </div>
          <div class="w-full flex flex-col items-center gap-1.5 mb-3 shrink-0">
            <div class="w-2/3 h-1.5 bg-gradient-to-r from-transparent via-primary/40 to-transparent rounded-full blur-[2px]"></div>
            <p class="text-[10px] font-black uppercase tracking-[0.3em] text-primary/60">Màn hình</p>
          </div>

          <div v-if="isLoadingSeats" class="flex-grow flex items-center justify-center">
            <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
          </div>
          <div v-else-if="seatData.seats.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant">
            Phòng chưa có sơ đồ ghế.
          </div>
          <div v-else class="flex-grow min-h-0 flex items-center justify-center overflow-auto custom-scrollbar">
            <div class="flex flex-col gap-1.5 mx-auto my-auto">
              <div v-for="row in seatData.matrixRow" :key="row" class="flex gap-1.5 items-center justify-center">
                <span class="w-5 text-[10px] font-bold text-on-surface-variant/50 text-center shrink-0">{{ rowLabel(row - 1) }}</span>
                <template v-for="col in seatData.matrixCol" :key="col">
                  <div v-if="seatAt(row - 1, col - 1)" :class="seatClass(seatAt(row - 1, col - 1))"
                       @click="toggleSeat(seatAt(row - 1, col - 1))" :title="seatAt(row - 1, col - 1).rowChar + seatAt(row - 1, col - 1).colNum">
                    {{ seatAt(row - 1, col - 1).rowChar }}{{ seatAt(row - 1, col - 1).colNum }}
                  </div>
                  <div v-else class="w-8 h-8"></div>
                </template>
                <span class="w-5 text-[10px] font-bold text-on-surface-variant/50 text-center shrink-0">{{ rowLabel(row - 1) }}</span>
              </div>
            </div>
          </div>

          <div class="mt-3 flex items-center justify-between shrink-0">
            <div class="flex gap-4 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high border border-outline-variant/20"></span>Thường</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-red-900/40 border border-red-500/40"></span>VIP</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-purple-900/40 border border-purple-500/40"></span>Sweetbox</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-surface-container-high opacity-40"></span>Đã bán</span>
            </div>
            <AppButton @click="currentStep = 3" :disabled="selectedSeats.length === 0">3. Xác nhận vé</AppButton>
          </div>
        </div>

        <!-- Step 3: Confirm ticket types (by seat type) -->
        <div v-if="currentStep === 3" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 3. Xác nhận vé & loại ghế
            </h2>
            <AppButton variant="ghost" @click="currentStep = 2">Quay lại</AppButton>
          </div>

          <div class="flex-grow overflow-y-auto custom-scrollbar space-y-4 pr-2">
            <div v-for="b in seatTypeBreakdown" :key="b.type"
                 class="p-6 bg-surface-container-high rounded-[28px] border border-outline-variant/10 flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary border border-primary/20">
                  <span class="material-symbols-outlined">event_seat</span>
                </div>
                <div>
                  <p class="text-sm font-black text-on-surface uppercase">Ghế {{ seatTypeLabel(b.type) }}</p>
                  <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{{ b.count }} ghế</p>
                </div>
              </div>
              <p class="text-lg font-black italic text-primary">{{ fmt(b.subtotal) }}đ</p>
            </div>
            <div class="px-2 pt-2 text-xs font-bold text-on-surface-variant">
              Ghế đã chọn: <span class="text-primary">{{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
            </div>
          </div>

          <div class="mt-6 flex justify-end">
            <AppButton @click="currentStep = 4">4. Combo / Đồ ăn</AppButton>
          </div>
        </div>

        <!-- Step 4: F&B -->
        <div v-if="currentStep === 4" class="p-6 flex flex-col h-full overflow-hidden">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 4. Combo / Đồ ăn & Nước uống
            </h2>
            <AppButton variant="ghost" @click="currentStep = 3">Quay lại</AppButton>
          </div>

          <div v-if="combos.length === 0" class="flex-grow flex items-center justify-center text-on-surface-variant text-sm">
            Chưa có combo. Thêm ở "Thực đơn F&B / Combo".
          </div>
          <div v-else class="grid grid-cols-2 gap-6 flex-grow overflow-y-auto custom-scrollbar pr-2">
            <div v-for="cb in combos" :key="cb.id" class="p-5 bg-surface-container-high rounded-3xl border border-outline-variant/10 flex gap-4 hover:border-primary/30 transition-all">
              <div class="w-16 h-16 rounded-2xl overflow-hidden bg-surface-container-highest shrink-0 flex items-center justify-center">
                <img v-if="cb.imageUrl" :src="cb.imageUrl" class="w-full h-full object-cover" />
                <span v-else class="material-symbols-outlined text-on-surface-variant/40">fastfood</span>
              </div>
              <div class="flex flex-col justify-between flex-grow min-w-0">
                <div>
                  <h3 class="text-sm font-black uppercase text-on-surface truncate">{{ cb.name }}</h3>
                  <p class="text-[11px] text-on-surface-variant line-clamp-1">{{ cb.description }}</p>
                </div>
                <div class="flex items-center justify-between mt-2">
                  <span class="text-base font-black text-primary italic">{{ fmt(cb.price) }}đ</span>
                  <button @click="addCombo(cb)" class="w-8 h-8 rounded-full bg-primary/10 text-primary border border-primary/20 flex items-center justify-center hover:bg-primary/20 transition-colors">
                    <span class="material-symbols-outlined text-sm">add</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 rounded-2xl border border-primary/20 bg-primary/5 flex items-center gap-3 pl-4 pr-3 py-2.5">
            <!-- Label -->
            <div class="flex items-center gap-2 shrink-0">
              <span class="material-symbols-outlined text-primary text-lg">shopping_bag</span>
              <h3 class="text-[10px] font-black uppercase tracking-widest text-primary leading-none">Giỏ F&B</h3>
              <span class="text-[10px] font-bold text-on-surface-variant whitespace-nowrap">· {{ selectedCombos.length }} món</span>
            </div>

            <!-- Items (cuộn ngang nếu nhiều) -->
            <div class="flex-grow min-w-0 flex items-center gap-2 overflow-x-auto custom-scrollbar">
              <p v-if="selectedCombos.length === 0" class="text-[11px] text-on-surface-variant/60 whitespace-nowrap">Chưa chọn combo nào — không bắt buộc.</p>
              <div v-for="item in selectedCombos" :key="item.id"
                   class="shrink-0 flex items-stretch gap-3 bg-surface-container-high rounded-2xl border border-outline-variant/10 hover:border-primary/30 transition-colors pl-4 pr-2.5 py-2">
                <div class="flex flex-col justify-center leading-tight">
                  <span class="text-[11px] font-bold text-on-surface whitespace-nowrap">{{ item.name }}</span>
                  <span class="text-[12px] font-black italic text-primary whitespace-nowrap tabular-nums">{{ fmt(item.price * item.quantity) }}đ</span>
                </div>
                <div class="flex items-center gap-1.5 pl-3 border-l border-outline-variant/10">
                  <button @click="changeComboQty(item, -1)"
                          class="w-6 h-6 rounded-lg bg-surface-container-lowest border border-outline-variant/15 text-on-surface-variant flex items-center justify-center hover:bg-primary hover:text-black hover:border-primary active:scale-90 transition-all">
                    <span class="material-symbols-outlined text-[16px] leading-none">remove</span>
                  </button>
                  <span class="w-5 text-center text-[13px] font-black tabular-nums text-on-surface">{{ item.quantity }}</span>
                  <button @click="changeComboQty(item, 1)"
                          class="w-6 h-6 rounded-lg bg-primary/15 border border-primary/30 text-primary flex items-center justify-center hover:bg-primary hover:text-black active:scale-90 transition-all">
                    <span class="material-symbols-outlined text-[16px] leading-none">add</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Subtotal + action -->
            <div class="shrink-0 flex items-center gap-3 pl-3 border-l border-primary/15">
              <div class="text-right hidden sm:block">
                <p class="text-[9px] font-bold uppercase tracking-wider text-on-surface-variant leading-none mb-0.5">Tạm tính</p>
                <p class="text-base font-black italic text-primary tracking-tighter leading-none">{{ fmt(comboTotal) }}đ</p>
              </div>
              <AppButton @click="currentStep = 5">5. Thanh toán</AppButton>
            </div>
          </div>
        </div>

        <!-- Step 5: Payment -->
        <div v-if="currentStep === 5" class="p-6 space-y-8 overflow-y-auto custom-scrollbar h-full">
          <div class="flex justify-between items-center">
            <h2 class="text-xl font-black uppercase italic tracking-tighter text-on-surface flex items-center gap-3">
              <span class="w-8 h-1 bg-primary rounded-full"></span> 5. Thanh toán
            </h2>
            <AppButton variant="ghost" @click="currentStep = 4">Quay lại</AppButton>
          </div>
          <div class="grid grid-cols-2 gap-8">
            <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 space-y-5">
              <p class="text-[10px] font-black text-primary uppercase tracking-widest">Chi tiết hóa đơn</p>
              <h3 class="text-xl font-black italic uppercase text-on-surface">{{ selectedShowtime?.movieTitle }}</h3>
              <div class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>Vé (x{{ selectedSeats.length }})</span>
                <span class="text-on-surface">{{ fmt(seatTotal) }}đ</span>
              </div>
              <div v-if="comboTotal > 0" class="flex justify-between text-xs font-bold text-on-surface-variant uppercase border-b border-outline-variant/10 pb-3">
                <span>F&B / Combo</span>
                <span class="text-on-surface">{{ fmt(comboTotal) }}đ</span>
              </div>
              <div class="pt-3 flex justify-between items-end">
                <p class="text-[10px] font-black text-on-surface-variant uppercase">Tổng cộng</p>
                <p class="text-4xl font-black italic text-primary tracking-tighter">{{ fmt(totalPrice) }}đ</p>
              </div>
            </div>

            <div class="space-y-6">
              <div class="bg-primary/5 border border-primary/20 p-8 rounded-3xl space-y-4">
                <p class="text-[10px] font-black text-primary uppercase tracking-widest">Thành viên (tùy chọn — để tích điểm)</p>
                <div v-if="!member" class="space-y-3">
                  <input v-model="cardNumberInput" placeholder="Số thẻ (ID khách hàng)..." class="w-full bg-surface-container-high border border-outline-variant/10 rounded-2xl py-3 px-5 text-on-surface text-sm font-bold outline-none focus:border-primary/50" />
                  <p v-if="cardError" class="text-xs text-red-400 font-bold">{{ cardError }}</p>
                  <AppButton variant="primary" class="w-full" @click="checkMemberCard" :disabled="isCheckingCard">{{ isCheckingCard ? 'Đang kiểm tra...' : 'Kiểm tra' }}</AppButton>
                </div>
                <div v-else class="space-y-2 text-on-surface">
                  <div class="flex justify-between items-center">
                    <p class="text-sm font-black uppercase">{{ member.fullName }}</p>
                    <span class="px-2 py-1 bg-primary text-on-primary text-[8px] font-black rounded uppercase">{{ member.membershipTier }}</span>
                  </div>
                  <p class="text-xs text-on-surface-variant">Điểm tích lũy: <span class="text-primary font-bold">{{ fmt(member.loyaltyPoints) }}</span></p>
                  <button @click="clearMember" class="text-[10px] text-on-surface-variant hover:text-red-400 font-bold uppercase">Bỏ thẻ</button>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openCashModal" :disabled="isPaying">
                  <span class="material-symbols-outlined">payments</span> Tiền mặt
                </AppButton>
                <AppButton variant="outline" class="flex flex-col gap-1 py-6" @click="openQrModal" :disabled="isPaying">
                  <span class="material-symbols-outlined">qr_code_2</span> Chuyển khoản QR
                </AppButton>
              </div>
              <p v-if="isPaying" class="text-center text-xs text-on-surface-variant">Đang xử lý thanh toán...</p>
            </div>
          </div>
        </div>

        <!-- Step 6: Done -->
        <div v-if="currentStep === 6" class="p-6 flex flex-col items-center justify-center text-center h-full space-y-8">
          <div class="w-24 h-24 bg-green-500/20 text-green-500 rounded-full flex items-center justify-center shadow-2xl shadow-green-500/20">
            <span class="material-symbols-outlined text-6xl">check_circle</span>
          </div>
          <div>
            <h2 class="text-4xl font-black uppercase italic tracking-tighter text-on-surface">Thanh toán thành công</h2>
            <p class="text-on-surface-variant font-bold mt-2 uppercase tracking-widest text-xs">Xuất vé và bàn giao cho khách</p>
          </div>

          <div class="bg-surface-container-high p-8 rounded-3xl border border-outline-variant/10 w-full max-w-md space-y-6 text-left">
            <div class="flex justify-between items-start">
              <div>
                <p class="text-[10px] font-black text-primary uppercase">Mã đặt vé</p>
                <p class="text-xl font-black text-on-surface">{{ completedBooking?.bookingCode }}</p>
              </div>
              <div class="text-right">
                <p class="text-[10px] font-black text-primary uppercase">Phòng</p>
                <p class="text-xl font-black text-on-surface">{{ selectedShowtime?.roomName }}</p>
              </div>
            </div>
            <div class="border-t border-dashed border-outline-variant/20 pt-6">
              <p class="text-[10px] font-black text-on-surface-variant uppercase mb-2">Thông tin vé</p>
              <div class="flex justify-between text-sm font-bold text-on-surface">
                <span>{{ selectedSeats.length }} ghế: {{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</span>
                <span class="text-primary italic">{{ fmt(totalPrice) }}đ</span>
              </div>
            </div>
          </div>

          <div class="flex gap-4">
            <AppButton variant="primary" size="lg" class="flex items-center gap-3" @click="printInvoice">
              <span class="material-symbols-outlined">print</span> In hoá đơn
            </AppButton>
            <AppButton variant="outline" size="lg" class="flex items-center gap-3" @click="resetPOS">
              <span class="material-symbols-outlined">add_circle</span> Giao dịch mới
            </AppButton>
          </div>
        </div>
      </div>

      <!-- Right: Cart summary -->
      <div class="col-span-3 bg-surface-container-low border border-outline-variant/10 rounded-3xl shadow-2xl p-6 flex flex-col">
        <div class="flex items-center gap-2 pb-5 mb-5 border-b border-outline-variant/10">
          <span class="material-symbols-outlined text-primary">receipt_long</span>
          <h2 class="text-sm font-black uppercase tracking-[0.2em] text-primary">Biên lai tạm tính</h2>
        </div>
        <div v-if="selectedShowtime" class="space-y-5 flex-grow overflow-y-auto custom-scrollbar pr-1">
          <div class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1.5">Phim & Suất</p>
            <h3 class="text-base font-black uppercase italic text-on-surface leading-tight">{{ selectedShowtime.movieTitle }}</h3>
            <p class="text-xs text-on-surface-variant mt-1.5">{{ selectedShowtime.roomName }} • {{ selectedShowtime.formatName }}</p>
            <p class="text-xs text-on-surface-variant">{{ new Date(selectedShowtime.startTime).toLocaleString('vi-VN', { weekday: 'short', hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) }}</p>
          </div>
          <div v-if="selectedSeats.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">Ghế ({{ selectedSeats.length }})</p>
            <p class="text-sm text-primary font-black mb-3">{{ selectedSeats.map(s => s.rowChar + s.colNum).join(', ') }}</p>
            <div v-for="b in seatTypeBreakdown" :key="b.type" class="flex justify-between text-xs font-semibold text-on-surface-variant mb-1">
              <span>Ghế {{ seatTypeLabel(b.type) }} <span class="text-on-surface-variant/60">x{{ b.count }}</span></span>
              <span class="text-on-surface">{{ fmt(b.subtotal) }}đ</span>
            </div>
          </div>
          <div v-if="selectedCombos.length" class="pb-5 border-b border-outline-variant/10">
            <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-2">F&B / Combo</p>
            <div v-for="c in selectedCombos" :key="c.id" class="flex justify-between text-xs font-semibold mb-1">
              <span class="text-on-surface-variant">{{ c.name }} <span class="text-on-surface-variant/60">x{{ c.quantity }}</span></span>
              <span class="text-on-surface">{{ fmt(c.price * c.quantity) }}đ</span>
            </div>
          </div>
        </div>
        <div v-else class="flex-grow flex flex-col items-center justify-center text-on-surface-variant/40 gap-2">
          <span class="material-symbols-outlined text-4xl">shopping_cart</span>
          <p class="text-xs font-semibold">Chưa chọn suất chiếu</p>
        </div>

        <div class="pt-5 mt-3 border-t border-outline-variant/10 flex justify-between items-center">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Tổng tiền</p>
          <p class="text-3xl font-black italic tracking-tighter text-primary">{{ fmt(totalPrice) }}đ</p>
        </div>
      </div>
    </main>

    <!-- Modal: Thanh toán tiền mặt -->
    <transition name="fade">
      <div v-if="showCashModal" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="showCashModal = false">
        <div class="w-full max-w-md bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="px-7 py-5 border-b border-outline-variant/10 flex items-center gap-3">
            <span class="material-symbols-outlined text-primary">payments</span>
            <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Thanh toán tiền mặt</h3>
          </div>
          <div class="p-7 space-y-6">
            <div class="flex justify-between items-end">
              <span class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tổng phải trả</span>
              <span class="text-3xl font-black italic text-primary tracking-tighter">{{ fmt(totalPrice) }}đ</span>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tiền khách đưa</label>
              <input v-model.number="cashGiven" type="number" min="0" placeholder="0"
                     class="w-full bg-surface-container-high border border-outline-variant/10 rounded-2xl py-3.5 px-5 text-on-surface text-xl font-black outline-none focus:border-primary/50 tabular-nums" />
              <div class="flex flex-wrap gap-2 pt-1">
                <button v-for="s in cashSuggestions" :key="s" @click="cashGiven = s"
                        class="px-3 py-1.5 rounded-lg bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold text-on-surface hover:border-primary/40 hover:text-primary transition-all tabular-nums">{{ fmt(s) }}đ</button>
              </div>
            </div>
            <div class="flex justify-between items-center p-4 rounded-2xl bg-surface-container-high border border-outline-variant/10">
              <span class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tiền trả lại</span>
              <span class="text-2xl font-black italic tracking-tighter" :class="changeDue > 0 ? 'text-green-400' : 'text-on-surface'">{{ fmt(changeDue) }}đ</span>
            </div>
          </div>
          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="showCashModal = false">Hủy</AppButton>
            <AppButton variant="primary" class="flex-1" :disabled="!canConfirmCash || isPaying" @click="processPayment('CASH')">
              {{ isPaying ? 'Đang xử lý...' : 'Xác nhận thanh toán' }}
            </AppButton>
          </div>
        </div>
      </div>
    </transition>

    <!-- Modal: Chuyển khoản QR (VietQR) -->
    <transition name="fade">
      <div v-if="showQrModal" class="fixed inset-0 z-[1200] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="showQrModal = false">
        <div class="w-full max-w-lg bg-surface border border-outline-variant/15 rounded-3xl shadow-2xl overflow-hidden">
          <div class="px-7 py-5 border-b border-outline-variant/10 flex items-center gap-3">
            <span class="material-symbols-outlined text-primary">qr_code_2</span>
            <h3 class="text-lg font-black uppercase italic tracking-tighter text-on-surface">Chuyển khoản QR</h3>
          </div>

          <div v-if="!vietQrUrl" class="p-10 text-center space-y-3">
            <span class="material-symbols-outlined text-5xl text-on-surface-variant/40">account_balance</span>
            <p class="text-sm font-bold text-on-surface">Chưa cấu hình tài khoản nhận tiền.</p>
            <p class="text-xs text-on-surface-variant">Vào <b class="text-on-surface">Cài đặt → Tài khoản nhận tiền</b> để thêm Ngân hàng + STK.</p>
          </div>

          <div v-else class="p-7 space-y-5">
            <div class="flex flex-col items-center gap-3">
              <div class="w-80 h-80 max-w-full rounded-2xl bg-white p-3 flex items-center justify-center shadow-lg">
                <img :src="vietQrUrl" alt="VietQR" class="w-full h-full object-contain" />
              </div>
              <p class="text-xs text-on-surface-variant text-center">Khách dùng app ngân hàng quét mã — số tiền &amp; nội dung tự điền.</p>
              <button @click="openQrFullscreen"
                      class="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary/10 border border-primary/30 text-primary text-[11px] font-black uppercase tracking-widest hover:bg-primary hover:text-black transition-all">
                <span class="material-symbols-outlined text-[18px]">open_in_full</span> Hiển thị mã cho khách
              </button>
            </div>
            <div class="space-y-2.5 text-sm">
              <div class="flex justify-between"><span class="text-on-surface-variant">Ngân hàng</span><span class="font-bold text-on-surface">{{ bankInfo.name }}</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Số tài khoản</span><span class="font-bold text-on-surface font-mono">{{ bankInfo.accountNo }}</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Chủ tài khoản</span><span class="font-bold text-on-surface uppercase">{{ bankInfo.accountName }}</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Số tiền</span><span class="font-black text-primary italic">{{ fmt(totalPrice) }}đ</span></div>
              <div class="flex justify-between"><span class="text-on-surface-variant">Nội dung</span><span class="font-bold text-on-surface">{{ transferContent }}</span></div>
            </div>
          </div>

          <div class="px-7 py-5 border-t border-outline-variant/10 flex gap-3">
            <AppButton variant="ghost" class="flex-1" @click="showQrModal = false">Hủy</AppButton>
            <AppButton variant="primary" class="flex-1" :disabled="!vietQrUrl || isPaying" @click="processPayment('TRANSFER')">
              {{ isPaying ? 'Đang xử lý...' : 'Xác nhận đã chuyển khoản' }}
            </AppButton>
          </div>
        </div>
      </div>
    </transition>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" :class="[
        'fixed bottom-6 right-6 z-[1100] px-5 py-3 rounded-xl shadow-2xl text-sm font-semibold flex items-center gap-2 border',
        toast.type === 'success' ? 'bg-green-500/15 border-green-500/30 text-green-300' : 'bg-red-500/15 border-red-500/30 text-red-300'
      ]">
        <span class="material-symbols-outlined text-base">{{ toast.type === 'success' ? 'check_circle' : 'error' }}</span>
        {{ toast.message }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(245, 197, 24, 0.2); border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(245, 197, 24, 0.4); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
