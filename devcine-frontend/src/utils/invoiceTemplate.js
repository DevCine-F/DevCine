// Bộ dựng hoá đơn DevCine dùng chung (POS in hoá đơn & Quản lý hoá đơn in lại).
// inv = {
//   bookingCode, movie, room, format, dateStr, counter,
//   seatRows: [{ label, seats, count, unit, subtotal }],
//   combos: [{ name, quantity, price }],
//   seatTotal, comboTotal, discount, grandTotal, seatCount,
//   paymentLabel, memberName, memberTier,
//   tickets: [{ seatLabel, qrCode }]
// }

const esc = (v) => String(v ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
const qrUrl = (code) => `https://api.qrserver.com/v1/create-qr-code/?size=200x200&margin=0&data=${encodeURIComponent(code)}`

export const paymentLabel = (m) =>
  ({ CASH: 'Tiền mặt', CARD: 'Thẻ / QR', TRANSFER: 'Chuyển khoản QR', VNPAY: 'VNPAY' }[m] || m || '—')

export function buildInvoiceHtml(inv) {
  const movie = esc(inv.movie)
  const room = esc(inv.room)
  const format = esc(inv.format)
  const bookingCode = esc(inv.bookingCode)
  const dateStr = inv.dateStr || ''
  const printedAt = new Date().toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  const tickets = inv.tickets || []
  const seatTotal = Number(inv.seatTotal || 0)
  const comboTotal = Number(inv.comboTotal || 0)
  const discount = Number(inv.discount || 0)
  const grandTotal = inv.grandTotal != null ? Number(inv.grandTotal) : Math.max(0, seatTotal + comboTotal - discount)
  const seatCount = inv.seatCount ?? (inv.seatRows || []).reduce((a, b) => a + (b.count || 0), 0)

  const itemRow = (name, qty, unit, total) =>
    `<tr><td>${name}</td><td class="c">${qty}</td><td class="r">${fmt(unit)}đ</td><td class="r b">${fmt(total)}đ</td></tr>`

  const seatRows = (inv.seatRows || []).map(b =>
    itemRow(`Ghế ${esc(b.label)} <span class="muted">${esc(b.seats || '')}</span>`, b.count, b.unit, b.subtotal)
  ).join('')

  const comboRows = (inv.combos || []).map(c =>
    itemRow(esc(c.name), c.quantity, c.price, c.price * c.quantity)
  ).join('')

  const seatSection = seatRows ? `<tr class="grp"><td colspan="4">Vé xem phim</td></tr>${seatRows}` : ''
  const comboSection = comboRows ? `<tr class="grp"><td colspan="4">Bắp nước &amp; Combo</td></tr>${comboRows}` : ''

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
            <div class="tk-stub-t">Mã đặt vé — quét tại quầy</div>
            <img src="${qrUrl(bookingCode)}" alt="QR ${bookingCode}" />
            <div class="tk-code">${bookingCode}</div>
            <div class="tk-note">1 mã QR dùng chung cho cả đơn</div>
          </div>
        </article>`).join('')
    : '<div class="ticket"><p class="muted" style="padding:28px">Không có dữ liệu vé QR.</p></div>'

  const memberLine = inv.memberName
    ? `<br/>Thành viên: <b>${esc(inv.memberName)}</b>${inv.memberTier ? ` · ${esc(inv.memberTier)}` : ''}` : ''

  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
<title>Hoá đơn ${bookingCode} — DevCine</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;0,700;0,800;0,900;1,500&family=Inter:wght@400;500;600;700;800&display=swap');
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,Arial,sans-serif;background:#efe8da;color:#26221b;padding:34px 20px;font-size:14px;-webkit-print-color-adjust:exact;print-color-adjust:exact}
  .serif{font-family:'Playfair Display',Georgia,serif}
  .g{background:linear-gradient(135deg,#e6c878,#c4992f);-webkit-background-clip:text;background-clip:text;color:transparent}
  .bar{display:flex;justify-content:center;gap:12px;max-width:880px;margin:0 auto 26px}
  .bar button{border:0;border-radius:999px;padding:13px 30px;font-weight:700;font-size:11px;letter-spacing:.14em;text-transform:uppercase;cursor:pointer}
  .btn-print{background:linear-gradient(135deg,#dcb869,#b8902f);color:#1c1a17;box-shadow:0 8px 22px rgba(184,144,47,.4)}
  .btn-close{background:#fff;color:#6b6456;border:1px solid #ddd4c1}
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
    .ticket:first-of-type{break-before:auto}
    .ticket::before{display:none}
    .tk-stub::before,.tk-stub::after{background:#fff}
  }
</style></head>
<body>
  ${inv.isCheckIn ? '' : `
  <div class="bar">
    <button class="btn-print" onclick="window.print()">🖨 In vé / hoá đơn</button>
    <button class="btn-close" onclick="window.close()">Đóng</button>
  </div>
  `}

  ${inv.isCheckIn ? '' : `
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
          Nguồn: <b>${esc(inv.counter || 'POS · Lễ tân')}</b>
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
      <div class="s-row"><span>Tạm tính vé · ${seatCount} ghế</span><b>${fmt(seatTotal)}đ</b></div>
      ${comboTotal > 0 ? `<div class="s-row"><span>Bắp nước &amp; combo</span><b>${fmt(comboTotal)}đ</b></div>` : ''}
      <div class="s-row"><span>Số tiền được giảm</span><b class="${discount > 0 ? 'cut' : ''}">${discount > 0 ? '−' + fmt(discount) : '0'}đ</b></div>
      <div class="s-grand"><span>Tổng thanh toán</span><b class="serif">${fmt(grandTotal)}<span class="u">đ</span></b></div>
    </div>

    <div class="foot">
      <div class="pm">Phương thức: <b>${esc(inv.paymentLabel || '—')}</b>${memberLine}</div>
      <div class="stamp">Đã thanh toán</div>
    </div>
    <div class="thanks serif">Cảm ơn quý khách & hẹn gặp lại tại DevCine</div>
  </section>

  <div class="divider"><span>Vé xem phim · ${tickets.length} vé</span></div>
  `}

  ${ticketSlips}
</body></html>`
}

// Mở hoá đơn ở tab mới. Trả về false nếu bị chặn pop-up.
export function openInvoice(inv) {
  const win = window.open('', '_blank')
  if (!win) return false
  win.document.open()
  win.document.write(buildInvoiceHtml(inv))
  win.document.close()
  return true
}
