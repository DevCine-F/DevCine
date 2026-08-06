import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ticketingApi } from '@/api/admin/index'
import { useToastStore } from '@/stores/toast'

const HELD_KEY = 'devcine_pos_held_orders'
const HELD_SEQ_KEY = 'devcine_pos_hold_seq'
const HELD_MAX = 10
const HOLD_SECONDS = 5 * 60

export const usePosStore = defineStore('posStore', () => {
  const heldOrders = ref([])
  let timerId = null

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
      if (order.mode !== 'TICKET' || !order.bookingId) {
        return order // F&B only doesn't expire with timer here
      }
      
      const elapsed = Math.floor((now - order.createdAt) / 1000)
      const remaining = Math.max(0, HOLD_SECONDS - elapsed)
      order.holdRemaining = remaining

      if (remaining <= 0) {
        hasExpired = true
        // Fire API call asynchronously, don't await here in loop
        ticketingApi.releaseHold(order.bookingId).catch(() => {})
      } else {
        hasActiveTickets = true
      }
      return order
    })

    if (hasExpired) {
      const expiredCount = heldOrders.value.filter(o => o.mode === 'TICKET' && o.holdRemaining <= 0).length
      heldOrders.value = heldOrders.value.filter(o => o.mode !== 'TICKET' || o.holdRemaining > 0)
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

    const newOrder = {
      ...orderData,
      code: nextHoldCode(),
      createdAt: Date.now(),
      holdRemaining: HOLD_SECONDS
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
      ticketingApi.releaseHold(order.bookingId).catch(() => {})
    }
    removeOrder(order.code)
    showToast(`Đã huỷ đơn chờ ${order.code}${order.bookingId ? ' — ghế được giải phóng' : ''}.`, 'success')
  }

  // Initialize
  loadFromLocalStorage()
  startSync()

  return {
    heldOrders,
    holdOrder,
    removeOrder,
    deleteHeldOrder,
    updateTimers
  }
})
