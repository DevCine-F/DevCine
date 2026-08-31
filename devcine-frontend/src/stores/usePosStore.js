import { defineStore } from 'pinia'
import { ref } from 'vue'
import { posPendingOrderApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'

const POS_TERMINAL_ID_KEY = 'devcine_pos_terminal_id'
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

const HELD_KEY = 'devcine_pos_held_orders'
const HELD_SEQ_KEY = 'devcine_pos_hold_seq'
const HELD_MAX = 10
const DEFAULT_POS_HOLD_SECONDS = 15 * 60

export const usePosStore = defineStore('posStore', () => {
  const heldOrders = ref([])
  let timerId = null

  let posTerminalId = localStorage.getItem(POS_TERMINAL_ID_KEY)
  if (!posTerminalId) {
    posTerminalId = generateUUID()
    localStorage.setItem(POS_TERMINAL_ID_KEY, posTerminalId)
  }
  const getPosTerminalId = () => posTerminalId

  const toastStore = useToastStore()
  const showToast = (message, type = 'success') => toastStore.push(message, type)

  const loadFromLocalStorage = () => {
    try {
      const raw = localStorage.getItem(HELD_KEY)
      if (raw) {
        const decoded = decodeURIComponent(atob(raw))
        heldOrders.value = JSON.parse(decoded)
        // Hydration: update remaining time
        updateTimers()
      } else {
        heldOrders.value = []
      }
    } catch (e) {
      heldOrders.value = [] // Reset to prevent UI crash on bad data
    }
  }

  const persistToLocalStorage = () => {
    try {
      const encoded = btoa(encodeURIComponent(JSON.stringify(heldOrders.value)))
      localStorage.setItem(HELD_KEY, encoded)
    } catch (e) {
      console.error('Failed to save held orders', e)
    }
  }

  const nextHoldCode = () => {
    const seq = parseInt(localStorage.getItem(HELD_SEQ_KEY) || '0', 10) + 1
    localStorage.setItem(HELD_SEQ_KEY, String(seq))
    return 'HOLD-' + String(seq).padStart(3, '0')
  }

  const updateTimers = () => {
    const now = Date.now()
    let hasExpired = false
    let hasActiveTickets = false

    heldOrders.value = heldOrders.value.map(order => {
      let remaining = 0
      if (order.expiresAt) {
        remaining = Math.max(0, Math.floor((order.expiresAt - now) / 1000))
      } else {
        const holdSec = order.holdMinutes ? order.holdMinutes * 60 : DEFAULT_POS_HOLD_SECONDS
        const elapsed = Math.floor((now - order.createdAt) / 1000)
        remaining = Math.max(0, holdSec - elapsed)
      }
      order.holdRemaining = remaining

      if (remaining <= 0) {
        hasExpired = true
        if (order.bookingId) {
           posPendingOrderApi.cancel(order.bookingId, posTerminalId).catch(() => {})
        }
      } else {
        hasActiveTickets = true
      }
      return order
    })

    if (hasExpired) {
      const expiredCount = heldOrders.value.filter(o => o.holdRemaining <= 0).length
      heldOrders.value = heldOrders.value.filter(o => o.holdRemaining > 0)
      persistToLocalStorage()
      if (expiredCount > 0) {
        showToast('Một số đơn chờ đã hết thời gian giữ ghế và bị huỷ.', 'error')
      }
    }

    // Manage single ticker loop
    if (hasActiveTickets && !timerId) {
      timerId = setInterval(updateTimers, 1000)
    } else if (!hasActiveTickets && timerId) {
      clearInterval(timerId)
      timerId = null
    }
  }

  const startSync = () => {
    window.addEventListener('storage', (e) => {
      if (e.key === HELD_KEY) {
        loadFromLocalStorage()
      }
    })
  }

  const holdOrder = async (orderData) => {
    if (heldOrders.value.length >= HELD_MAX) {
      showToast(`Tối đa ${HELD_MAX} đơn chờ cùng lúc. Hãy xử lý bớt đơn đang treo trước.`, 'error')
      return false
    }

    const holdSec = orderData.holdMinutes ? orderData.holdMinutes * 60 : DEFAULT_POS_HOLD_SECONDS
    const newOrder = {
      ...orderData,
      code: orderData.code || nextHoldCode(),
      createdAt: orderData.createdAt || Date.now(),
      expiresAt: orderData.expiresAt || (Date.now() + holdSec * 1000),
      holdRemaining: 0 // calculated in updateTimers
    }

    heldOrders.value.unshift(newOrder) // Pinia state first
    persistToLocalStorage() // then sync
    updateTimers()
    
    return newOrder.code
  }

  const removeOrder = (code) => {
    heldOrders.value = heldOrders.value.filter(x => x.code !== code)
    persistToLocalStorage()
    updateTimers()
  }

  const deleteHeldOrder = (order) => {
    if (order.bookingId) {
      posPendingOrderApi.cancel(order.bookingId, posTerminalId).catch(() => {})
    }
    removeOrder(order.code)
    showToast(`Đã huỷ đơn chờ ${order.code}${order.bookingId ? ' — ghế được giải phóng' : ''}.`, 'success')
  }

  // Initialize
  loadFromLocalStorage()
  startSync()

  return {
    posTerminalId,
    getPosTerminalId,
    heldOrders,
    holdOrder,
    removeOrder,
    deleteHeldOrder,
    updateTimers
  }
})
