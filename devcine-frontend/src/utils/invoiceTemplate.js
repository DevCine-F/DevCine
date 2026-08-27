// Bộ dựng hoá đơn & vé giấy DevCine chuẩn in nhiệt K80 (80mm).
// Phân tách tự động thành 2 phiếu độc lập:
//   - Phiếu 1: Vé xem phim (theo mẫu Phieu_ve_xem_phim.txt)
//   - Phiếu 2: Phiếu nhận bắp nước [PICK-UP] (theo mẫu Phieu_dich_vu_bap_nuoc_[PICK-UP].txt)
// Quy tắc in:
//   - Đơn chỉ có vé: Chỉ in Phiếu 1.
//   - Đơn chỉ có F&B: Chỉ in Phiếu 2.
//   - Đơn có cả vé & F&B: In liên tiếp 2 phiếu riêng biệt (ngắt trang tự động cho máy in nhiệt).

const esc = (v) => String(v ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
const qrUrl = (code) => `https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=0&data=${encodeURIComponent(code)}`

export const paymentLabel = (m) => {
  if (!m) return 'Tiền mặt'
  const up = String(m).toUpperCase()
  if (up === 'CASH') return 'Tiền mặt'
  if (up === 'CARD') return 'Thẻ POS'
  if (up === 'TRANSFER') return 'Chuyển khoản (VietQR)'
  if (up === 'VNPAY') return 'VNPAY'
  if (up === 'MOMO') return 'Ví MoMo'
  if (up === 'ZALOPAY') return 'Ví ZaloPay'
  if (up === 'MEMBER_WALLET') return 'Ví thành viên'
  return m
}

export const seatTypeLabel = (t) => {
  if (!t) return 'Thường'
  const up = String(t).toUpperCase()
  if (up === 'NORMAL' || up === 'STANDARD') return 'Thường'
  if (up === 'VIP') return 'VIP'
  if (up === 'SWEETBOX' || up === 'COUPLE') return 'Sweetbox'
  return t
}

export const ticketTypeLabel = (t) => {
  if (!t) return 'Người lớn'
  const up = String(t).toUpperCase()
  if (up === 'ADULT') return 'Người lớn'
  if (up === 'U22') return 'U22'
  if (up === 'STUDENT') return 'HSSV'
  if (up === 'CHILD') return 'Trẻ em'
  if (up === 'SENIOR') return 'Cao tuổi'
  return t
}

// Chuẩn hóa định dạng ngày giờ DD/MM/YYYY HH:mm
const formatDateTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return String(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const formatDateOnly = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return String(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

const formatTimeOnly = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return String(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * Xây dựng mã HTML hoàn chỉnh cho 2 phiếu in nhiệt K80
 */
export function buildInvoiceHtml(inv) {
  const bookingCode = esc(inv.bookingCode || inv.saleCode || 'DEVCINE')
  const now = new Date()
  const printedAtStr = inv.printedAt ? formatDateTime(inv.printedAt) : formatDateTime(now)
  const posTerminal = esc(inv.posTerminal || inv.counter || '01')
  const cashierName = esc(inv.cashierName || inv.staffName || inv.checkedInBy || 'Nguyễn Quang Huy')
  const cinemaName = esc(inv.cinemaName || 'DEVCINE CINEMA')
  const cinemaAddress = esc(inv.cinemaAddress || 'Tầng 3, TTTM DevCine Plaza, Hà Nội')
  const payMethod = paymentLabel(inv.paymentMethod)

  // 1. Phân tích Dữ liệu Vé (Ticket Data)
  const rawSeats = inv.seats || inv.seatList || []
  const rawSeatRows = inv.seatRows || []
  const hasTickets = Boolean(
    rawSeats.length > 0 ||
    rawSeatRows.length > 0 ||
    inv.movieTitle ||
    inv.movie ||
    inv.showtimeStart ||
    inv.startTime
  )

  let movieTitle = esc(inv.movieTitle || inv.movie || '')
  const formatName = esc(inv.format || inv.formatName || '2D')
  if (formatName && !movieTitle.toLowerCase().includes(formatName.toLowerCase())) {
    movieTitle = `${movieTitle} (${formatName})`
  }

  const roomName = esc(inv.roomName || inv.room || 'PHÒNG 01')
  const roomType = esc(inv.roomType || 'Standard')
  const showDateStr = inv.showDate ? formatDateOnly(inv.showDate) : (inv.startTime ? formatDateOnly(inv.startTime) : (inv.showtimeStart ? formatDateOnly(inv.showtimeStart) : formatDateOnly(now)))
  
  let timeRangeStr = inv.timeRange || ''
  if (!timeRangeStr) {
    const stTime = inv.startTime || inv.showtimeStart
    if (stTime) {
      const st = formatTimeOnly(stTime)
      if (inv.endTime) {
        timeRangeStr = `${st} ~ ${formatTimeOnly(inv.endTime)}`
      } else {
        // Ước tính thời lượng 100-120p nếu không có endTime
        const d = new Date(stTime)
        const dEnd = new Date(d.getTime() + 100 * 60000)
        timeRangeStr = `${st} ~ ${formatTimeOnly(dEnd)}`
      }
    } else {
      timeRangeStr = '23:35 ~ 01:15'
    }
  }

  // Danh sách ghế ngồi
  let seatLabels = []
  if (rawSeats.length > 0) {
    seatLabels = rawSeats.map(s => typeof s === 'string' ? s : (s.seatLabel || s.label || s.seatNumber || ''))
  } else if (rawSeatRows.length > 0) {
    rawSeatRows.forEach(sr => {
      if (sr.seats && Array.isArray(sr.seats)) {
        seatLabels.push(...sr.seats)
      } else if (sr.label) {
        seatLabels.push(sr.label)
      }
    })
  }

  // Gom nhóm loại vé tính tiền
  let ticketGroups = []
  if (rawSeatRows.length > 0 && rawSeatRows[0].seats && !Array.isArray(rawSeatRows[0].seats)) {
    // Trường hợp rawSeatRows đã được gom theo loại vé
    ticketGroups = rawSeatRows.map(sr => ({
      typeName: ticketTypeLabel(sr.seats || sr.ticketType || sr.label),
      count: sr.count || 1,
      unitPrice: Number(sr.unit || sr.price || 0),
      subtotal: Number(sr.subtotal || (sr.unit || sr.price || 0) * (sr.count || 1))
    }))
  } else if (rawSeats.length > 0) {
    const groupMap = {}
    rawSeats.forEach(s => {
      const type = ticketTypeLabel(s.ticketType || s.targetType || 'ADULT')
      const price = Number(s.price || s.priceSnapshot || 0)
      if (!groupMap[type]) {
        groupMap[type] = { typeName: type, count: 0, unitPrice: price, subtotal: 0 }
      }
      groupMap[type].count += 1
      groupMap[type].subtotal += price
    })
    ticketGroups = Object.values(groupMap)
  }

  const totalTicketCount = seatLabels.length || ticketGroups.reduce((a, b) => a + b.count, 0) || 1
  const ticketSeatTotal = ticketGroups.reduce((a, b) => a + b.subtotal, 0) || Number(inv.seatTotal || 0)
  const ticketDiscount = Number(inv.ticketDiscount || 0)
  const totalTicketPrice = Math.max(0, ticketSeatTotal - ticketDiscount)

  // 2. Phân tích Dữ liệu Bắp Nước (F&B Data)
  const rawFnbs = inv.fnbs || inv.combos || []
  const hasFnbs = Boolean(rawFnbs.length > 0)

  // Danh sách món lẻ pha chế [1], [2]...
  const prepItems = []
  const comboGroups = []

  if (hasFnbs) {
    rawFnbs.forEach(f => {
      const name = esc(f.name || f.itemName || f.itemNameSnapshot || '')
      const qty = Number(f.quantity || 1)
      const baseUnitPrice = Number(f.price || f.unitPrice || f.priceSnapshot || 0)
      const options = f.options || []

      let totalOptionSurcharge = 0
      const optNames = []

      // Nở danh sách món pha chế phục vụ quầy Pick-up
      if (options.length > 0) {
        options.forEach(opt => {
          const optName = esc(opt.optionName || opt.optionNameSnapshot || opt.slotLabel || name)
          const optSurcharge = Number(opt.surcharge || opt.surchargeSnapshot || opt.surchargePrice || 0)
          totalOptionSurcharge += optSurcharge
          if (opt.optionName || opt.optionNameSnapshot) {
            optNames.push(opt.optionName || opt.optionNameSnapshot)
          }

          prepItems.push({
            name: optName,
            quantity: qty,
            surcharge: optSurcharge // Chỉ lưu số tiền phụ thu
          })
        })
      } else {
        prepItems.push({
          name: name,
          quantity: qty,
          surcharge: Number(f.surchargePrice || 0)
        })
      }

      // Nhóm combo tính tiền (giá gốc combo + tổng phụ thu)
      const comboSurcharge = Number(f.surchargePrice != null ? f.surchargePrice : totalOptionSurcharge)
      const comboUnitPrice = baseUnitPrice + comboSurcharge
      const comboLineTotal = Number(f.lineTotal != null ? f.lineTotal : (comboUnitPrice * qty))

      let comboDisplayName = name
      if (optNames.length > 0) {
        comboDisplayName = `${name} (${optNames.join(' & ')})`
      }

      comboGroups.push({
        name: comboDisplayName,
        quantity: qty,
        price: comboLineTotal
      })
    })
  }

  const totalComboCount = comboGroups.reduce((a, b) => a + b.quantity, 0)
  const comboTotal = comboGroups.reduce((a, b) => a + b.price, 0) || Number(inv.comboTotal || 0)
  const fnbDiscount = Number(inv.fnbDiscount || 0)
  const totalFnbPrice = Math.max(0, comboTotal - fnbDiscount)

  // 3. Xây dựng HTML Phiếu 1: Vé xem phim
  let ticketSlipHtml = ''
  if (hasTickets) {
    const seatItemsHtml = seatLabels.map(s => `<tr><td class="bullet">&bull; Ghế: <b>${esc(s)}</b></td></tr>`).join('')
    const ticketGroupRowsHtml = ticketGroups.map(g => `
      <tr>
        <td class="left">${esc(g.typeName)}</td>
        <td class="center">${g.count}</td>
        <td class="right">${fmt(g.subtotal)} đ</td>
      </tr>
    `).join('')

    let roomDisplay = roomName || ''
    if (roomType && !roomDisplay.toLowerCase().includes(roomType.toLowerCase())) {
      roomDisplay = `${roomDisplay} (${roomType})`
    }

    ticketSlipHtml = `
      <section class="receipt-slip ticket-slip">
        <div class="center bold title-brand">${cinemaName}</div>
        <div class="center sub-brand">HỆ THỐNG RẠP CHIẾU PHIM</div>
        <div class="center address">${cinemaAddress}</div>
        <div class="empty-line"></div>
        <div class="center bold doc-title">*** VÉ XEM PHIM ***</div>
        <div class="line-single">------------------------------------------------</div>
        
        <table class="meta-table">
          <tr><td class="label">Mã đơn  :</td><td class="val bold">${bookingCode}</td></tr>
          <tr><td class="label">Ngày in :</td><td class="val">${printedAtStr}</td></tr>
          <tr><td class="label">Quầy/POS:</td><td class="val">${posTerminal}</td></tr>
          <tr><td class="label">Thu ngân:</td><td class="val">${cashierName}</td></tr>
        </table>
        
        <div class="line-double">================================================</div>
        <div class="bold movie-line">PHIM: ${movieTitle}</div>
        <table class="showtime-table">
          <tr>
            <td class="left">Suất: ${timeRangeStr}</td>
            <td class="right">Ngày: ${showDateStr}</td>
          </tr>
        </table>
        <div class="room-line">Phòng: ${roomDisplay}</div>
        <div class="line-single">------------------------------------------------</div>
        
        <div class="bold seat-title">DANH SÁCH GHẾ:</div>
        <table class="seat-list-table">
          ${seatItemsHtml}
        </table>
        
        <div class="line-double">================================================</div>
        <div class="bold group-title">${totalTicketCount} VÉ</div>
        <table class="data-table">
          ${ticketGroupRowsHtml}
        </table>
        <div class="line-single">------------------------------------------------</div>
        
        <table class="calc-table">
          <tr>
            <td class="left">Giảm giá:</td>
            <td class="right">${fmt(ticketDiscount)} đ</td>
          </tr>
        </table>
        <div class="line-single">------------------------------------------------</div>
        
        <table class="total-table">
          <tr>
            <td class="left bold uppercase">TỔNG TIỀN VÉ:</td>
            <td class="right bold price-val">${fmt(totalTicketPrice)} đ</td>
          </tr>
          <tr>
            <td class="left">Hình thức: ${payMethod}</td>
            <td class="right bold">[ĐÃ THANH TOÁN]</td>
          </tr>
        </table>
        <div class="line-single">------------------------------------------------</div>
        
        <div class="center bold qr-header">MÃ QR</div>
        <div class="center qr-box">
          <img class="qr-img" src="${qrUrl(bookingCode)}" alt="${bookingCode}" />
        </div>
        <div class="center mono bold code-under-qr">${bookingCode}</div>
        <div class="empty-line"></div>
        <div class="center thanks-msg">Cảm ơn quý khách &amp; Hẹn gặp lại!</div>
        <div class="line-double">================================================</div>
      </section>
    `
  }

  // 4. Xây dựng HTML Phiếu 2: Phiếu nhận bắp nước [PICK-UP]
  let fnbSlipHtml = ''
  if (hasFnbs) {
    const prepItemsRowsHtml = prepItems.map((item, idx) => `
      <tr>
        <td class="left">[${idx + 1}] ${esc(item.name)}</td>
        <td class="center">${item.quantity}</td>
        <td class="right">${item.surcharge > 0 ? `${fmt(item.surcharge)} đ` : ''}</td>
      </tr>
    `).join('')

    const comboGroupRowsHtml = comboGroups.map(g => `
      <tr>
        <td class="left">${esc(g.name)}</td>
        <td class="center">${g.quantity}</td>
        <td class="right">${fmt(g.price)} đ</td>
      </tr>
    `).join('')

    fnbSlipHtml = `
      <section class="receipt-slip fnb-slip">
        <div class="center bold title-brand">${cinemaName}</div>
        <div class="center sub-brand">QUẦY BẮP NƯỚC (F&amp;B)</div>
        <div class="center address">${cinemaAddress}</div>
        <div class="empty-line"></div>
        <div class="center bold doc-title">*** PHIẾU NHẬN HÀNG ***</div>
        <div class="center bold sub-doc-title">[PICK-UP]</div>
        <div class="line-single">------------------------------------------------</div>
        
        <table class="meta-table">
          <tr><td class="label">Mã đơn  :</td><td class="val bold">${bookingCode}</td></tr>
          <tr><td class="label">Ngày in :</td><td class="val">${printedAtStr}</td></tr>
          <tr><td class="label">Quầy/POS:</td><td class="val">${posTerminal}</td></tr>
          <tr><td class="label">Thu ngân:</td><td class="val">${cashierName}</td></tr>
        </table>
        
        <div class="line-double">================================================</div>
        <table class="data-table prep-table">
          ${prepItemsRowsHtml}
        </table>
        
        <div class="line-double">================================================</div>
        <div class="bold group-title">${totalComboCount} COMBO</div>
        <table class="data-table">
          ${comboGroupRowsHtml}
        </table>
        <div class="line-single">------------------------------------------------</div>
        
        <table class="calc-table">
          <tr>
            <td class="left">Giảm giá:</td>
            <td class="right">${fmt(fnbDiscount)} đ</td>
          </tr>
        </table>
        <div class="line-single">------------------------------------------------</div>
        
        <table class="total-table">
          <tr>
            <td class="left bold uppercase">TỔNG TIỀN F&amp;B:</td>
            <td class="right bold price-val">${fmt(totalFnbPrice)} đ</td>
          </tr>
          <tr>
            <td class="left">Hình thức: ${payMethod}</td>
            <td class="right bold">[ĐÃ THANH TOÁN]</td>
          </tr>
        </table>
        <div class="line-single">------------------------------------------------</div>
        
        <div class="center bold qr-header">MÃ QR</div>
        <div class="center qr-box">
          <img class="qr-img" src="${qrUrl(bookingCode)}" alt="${bookingCode}" />
        </div>
        <div class="center mono bold code-under-qr">${bookingCode}</div>
        <div class="empty-line"></div>
        <div class="center pickup-instruction">
          (Vui lòng đưa phiếu này tại quầy Pick-up để<br/>
          nhận bắp &amp; nước)
        </div>
        <div class="line-double">================================================</div>
      </section>
    `
  }

  // Kết hợp cả 2 phiếu
  return `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Phiếu in ${bookingCode} — DevCine</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Courier+Prime:wght@400;700&family=Inter:wght@400;500;600;700&display=swap');
    
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }
    
    body {
      font-family: 'Courier Prime', 'Courier New', Consolas, monospace;
      background: #333333;
      color: #000;
      font-size: 13px;
      line-height: 1.35;
      padding: 20px 10px;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }

    .receipt-container {
      max-width: 440px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .receipt-slip {
      width: 100%;
      background: #fff;
      padding: 24px 30px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.35);
      border-radius: 4px;
    }

    /* Các định dạng văn bản căn lề */
    .center { text-align: center; }
    .left { text-align: left; }
    .right { text-align: right; }
    .bold { font-weight: 700; }
    .uppercase { text-transform: uppercase; }
    .mono { font-family: 'Courier Prime', 'Courier New', Consolas, monospace; }

    .title-brand {
      font-size: 16px;
      letter-spacing: 0.5px;
    }
    .sub-brand {
      font-size: 12px;
      margin-top: 2px;
    }
    .address {
      font-size: 11px;
      margin-top: 2px;
    }
    .doc-title {
      font-size: 15px;
      letter-spacing: 0.5px;
      margin-top: 4px;
    }
    .sub-doc-title {
      font-size: 13px;
      margin-top: 2px;
    }
    .empty-line {
      height: 8px;
    }

    .line-single, .line-double {
      text-align: center;
      font-size: 12px;
      letter-spacing: -0.5px;
      overflow: hidden;
      white-space: nowrap;
      margin: 4px 0;
      color: #111;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }
    
    td {
      padding: 2px 0;
      font-size: 12.5px;
      vertical-align: top;
    }

    .meta-table td.label {
      width: 85px;
      white-space: nowrap;
    }
    .meta-table td.val {
      padding-left: 6px;
    }

    .movie-line {
      font-size: 13.5px;
      margin: 3px 0 2px 0;
    }
    .showtime-table td {
      font-size: 12.5px;
    }
    .room-line {
      font-size: 12.5px;
      margin-top: 2px;
    }

    .seat-title {
      font-size: 13px;
      margin: 4px 0 2px 0;
    }
    .seat-list-table td.bullet {
      padding-left: 80px;
      font-size: 13px;
    }

    .group-title {
      font-size: 13px;
      margin: 3px 0 2px 0;
    }

    .data-table td.left { width: 58%; }
    .data-table td.center { width: 14%; text-align: center; }
    .data-table td.right { width: 28%; text-align: right; }

    .calc-table td, .total-table td {
      font-size: 13px;
      padding: 3px 0;
    }
    .price-val {
      font-size: 14px;
    }

    .qr-header {
      font-size: 13px;
      letter-spacing: 2px;
      margin-top: 6px;
    }
    .qr-box {
      margin: 6px 0;
    }
    .qr-img {
      width: 140px;
      height: 140px;
      display: inline-block;
      image-rendering: pixelated;
    }
    .code-under-qr {
      font-size: 13px;
      letter-spacing: 1px;
    }
    .thanks-msg {
      font-size: 12.5px;
      margin-top: 6px;
    }
    .pickup-instruction {
      font-size: 12px;
      line-height: 1.4;
      margin-top: 4px;
    }

    /* Print Rules for 80mm Thermal Printer */
    @media print {
      @page {
        size: 80mm auto;
        margin: 0;
      }
      
      html, body {
        width: 80mm;
        margin: 0;
        padding: 0;
        background: #fff;
        font-size: 12px;
      }

      .receipt-container {
        max-width: 100%;
        margin: 0;
        gap: 0;
      }

      .receipt-slip {
        width: 100%;
        box-shadow: none;
        border-radius: 0;
        padding: 6mm 7mm;
        page-break-after: always;
        break-after: page;
      }

      .receipt-slip:last-child {
        page-break-after: auto;
        break-after: auto;
      }
      
      .seat-list-table td.bullet {
        padding-left: 20mm;
      }
    }
  </style>
</head>
<body onload="window.print()">
  <main class="receipt-container">
    ${ticketSlipHtml}
    ${fnbSlipHtml}
  </main>
</body>
</html>`
}

/**
 * Mở cửa sổ in hoá đơn/vé giấy ở tab mới
 */
export function openInvoice(inv) {
  const win = window.open('', '_blank')
  if (!win) return false
  win.document.open()
  win.document.write(buildInvoiceHtml(inv))
  win.document.close()
  return true
}
