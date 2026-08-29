import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

/**
 * Khóa ghế real-time qua WebSocket/STOMP — đồng bộ trạng thái ghế giữa các quầy POS và khách online.
 *
 * Nguyên tắc "ai click trước người đó giữ ghế" (chốt ở server theo mili-giây):
 *  - select(seatId): gửi lệnh giữ ghế. Server trả SELECT_OK (giữ được) hoặc SELECT_DENIED (quầy khác đã giữ).
 *  - Khi một máy giữ/nhả/bán ghế, server broadcast tới mọi máy đang xem suất đó → cập nhật `othersLocked`.
 *
 * @param {Object} opts
 * @param {string} [opts.by]          Nhãn hiển thị máy này (vd "Quầy POS", "Khách online").
 * @param {Function} [opts.onDenied]  (seatId) → ghế bị quầy khác chiếm trước (để revert UI + toast).
 * @param {Function} [opts.onSold]    (seatIds) → ghế vừa được bán ở nơi khác.
 * @param {Function} [opts.onReleased] (seatIds) → ghế vừa được nhả (hết hạn/huỷ) — cập nhật status sang AVAILABLE.
 * @param {Function} [opts.onHeld]    (seatIds) → ghế vừa bị giữ bởi quầy khác — cập nhật status sang HOLD.
 * @param {Function} [opts.onChange]  () → trạng thái khóa của người khác vừa đổi (để ép re-render nếu cần).
 * @param {Function} [opts.onFnbUpdate] (ev) → sự kiện cập nhật F&B real-time từ admin.
 * @param {Function} [opts.onMaintenance] (seatIds, status) → ghế chuyển sang bảo trì hoặc mở lại.
 * @param {Function} [opts.onPricingUpdate] (ev) → bảng giá vé nền được admin cập nhật.
 * @param {Function} [opts.onShowtimeCancelled] (ev) → suất chiếu bị hủy.
 * @param {Function} [opts.onShowtimeUpdated] (ev) → suất chiếu được cập nhật thông tin.
 */
export function useSeatRealtime({ by = 'Quầy khác', onDenied, onSold, onReleased, onHeld, onChange, onFnbUpdate, onMaintenance, onPricingUpdate, onShowtimeCancelled, onShowtimeUpdated } = {}) {
  const connected = ref(false)
  const othersLocked = ref(new Set()) // seatId đang bị NGƯỜI KHÁC giữ/bán → disable trên UI máy này
  const mySeats = new Set()           // seatId chính máy này đang giữ → bỏ qua echo broadcast của mình

  let client = null
  let showtimeId = null
  let subs = []

  const wsUrl = () =>
    (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws'

  const touch = () => { othersLocked.value = new Set(othersLocked.value); onChange?.() }

  function connect(stId = null) {
    if (client && showtimeId === stId) return // đã kết nối đúng suất này rồi → bỏ qua
    if (client) disconnect()
    showtimeId = stId
    client = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        if (showtimeId) {
          const topic = `/topic/showtime/${showtimeId}`
          subs.push(client.subscribe(topic, (msg) => handleBroadcast(JSON.parse(msg.body))))
          // Kết quả riêng cho lệnh select của chính máy này
          subs.push(client.subscribe('/user/queue/seat-result', (msg) => handleResult(JSON.parse(msg.body))))
          // Đồng bộ danh sách ghế đang bị khóa lúc mới vào
          subs.push(client.subscribe('/user/queue/seat-sync', (msg) => handleSync(JSON.parse(msg.body))))
          client.publish({ destination: `/app/showtime/${showtimeId}/sync`, body: '{}' })
        }
        // Đồng bộ cập nhật thực đơn F&B real-time (toàn hệ thống)
        subs.push(client.subscribe('/topic/fnb-updates', (msg) => {
          try {
            const ev = JSON.parse(msg.body)
            onFnbUpdate?.(ev)
          } catch (_) {}
        }))
        // Đồng bộ cập nhật bảng giá vé nền real-time
        subs.push(client.subscribe('/topic/pricing-updates', (msg) => {
          try {
            const ev = JSON.parse(msg.body)
            onPricingUpdate?.(ev)
          } catch (_) {}
        }))
      },
      onWebSocketClose: () => { connected.value = false },
    })
    client.activate()
  }

  function handleBroadcast(ev) {
    const ids = ev.seatIds || []
    if (ev.type === 'SEAT_LOCKED') {
      ids.forEach(id => { if (!mySeats.has(id)) othersLocked.value.add(id) })
      touch()
      // Thông báo để cập nhật status ghế → HOLD trong seatData
      if (ids.some(id => !mySeats.has(id))) onHeld?.(ids.filter(id => !mySeats.has(id)))
    } else if (ev.type === 'SEAT_RELEASED') {
      ids.forEach(id => othersLocked.value.delete(id))
      touch()
      // Thông báo để cập nhật status ghế → AVAILABLE trong seatData
      onReleased?.(ids)
    } else if (ev.type === 'SEAT_SOLD') {
      // Bỏ qua ghế của chính mình (mình là người vừa bán) → không tự báo "đã bán ở quầy khác"
      const notMine = ids.filter(id => !mySeats.has(id))
      notMine.forEach(id => othersLocked.value.add(id))
      touch()
      if (notMine.length) onSold?.(notMine)
    } else if (ev.type === 'SEAT_MAINTENANCE') {
      ids.forEach(id => othersLocked.value.delete(id))
      touch()
      onMaintenance?.(ids, ev.status || 'MAINTENANCE')
    } else if (ev.type === 'SHOWTIME_CANCELLED') {
      onShowtimeCancelled?.(ev)
    } else if (ev.type === 'SHOWTIME_UPDATED') {
      onShowtimeUpdated?.(ev)
    }
  }

  function handleResult(ev) {
    if (ev.type === 'SELECT_DENIED') {
      mySeats.delete(ev.seatId)
      othersLocked.value.add(ev.seatId) // ghế thuộc về người đến trước → khóa trên máy này
      touch()
      onDenied?.(ev.seatId)
    }
    // SELECT_OK: không cần làm gì thêm, ghế đã được giữ optimistic ở select()
  }

  function handleSync(ev) {
    (ev.seatIds || []).forEach(id => { if (!mySeats.has(id)) othersLocked.value.add(id) })
    touch()
  }

  /** Thử giữ ghế (optimistic): đánh dấu là của mình rồi gửi lên server chờ xác nhận. */
  function select(seatId) {
    if (!client || !connected.value) return
    mySeats.add(seatId)
    client.publish({
      destination: `/app/showtime/${showtimeId}/select`,
      body: JSON.stringify({ seatId, by }),
    })
  }

  /** Bỏ chọn ghế — nhả khóa cho máy khác chọn được ngay. */
  function deselect(seatId) {
    if (!client || !connected.value) return
    mySeats.delete(seatId)
    client.publish({
      destination: `/app/showtime/${showtimeId}/deselect`,
      body: JSON.stringify({ seatId }),
    })
  }

  /** Nhả hết ghế của mình + ngắt kết nối (rời bước chọn ghế / unmount). */
  function disconnect() {
    try {
      if (client && connected.value) {
        mySeats.forEach(id => client.publish({
          destination: `/app/showtime/${showtimeId}/deselect`,
          body: JSON.stringify({ seatId: id }),
        }))
      }
    } catch (_) { /* ignore */ }
    mySeats.clear()
    othersLocked.value = new Set()
    subs.forEach(s => { try { s.unsubscribe() } catch (_) {} })
    subs = []
    if (client) { try { client.deactivate() } catch (_) {} client = null }
    connected.value = false
  }

  const isLockedByOthers = (seatId) => othersLocked.value.has(seatId)

  return { connected, othersLocked, connect, disconnect, select, deselect, isLockedByOthers }
}

