import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

/**
 * Lắng nghe sự kiện voucher thời gian thực (Realtime Voucher Sync)
 * Giúp màn hình Online và POS tự động cập nhật ngay khi bên kia giữ / nhả / dùng voucher
 */
export function useVoucherRealtime({ onVoucherChange } = {}) {
  const connected = ref(false)
  let client = null
  let customerSub = null
  let updatesSub = null
  let currentCustomerId = null

  const wsUrl = () =>
    (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws'

  function subscribeCustomer(customerId) {
    if (!customerId) return
    currentCustomerId = customerId
    if (!client) {
      connect()
    } else if (connected.value) {
      resubscribe()
    }
  }

  function resubscribe() {
    if (customerSub) {
      try { customerSub.unsubscribe() } catch (_) {}
      customerSub = null
    }
    if (updatesSub) {
      try { updatesSub.unsubscribe() } catch (_) {}
      updatesSub = null
    }
    if (client && connected.value && currentCustomerId) {
      customerSub = client.subscribe(`/topic/customer/${currentCustomerId}/vouchers`, (msg) => {
        try {
          const ev = JSON.parse(msg.body)
          onVoucherChange?.(ev)
        } catch (_) {}
      })
      updatesSub = client.subscribe('/topic/voucher-updates', (msg) => {
        try {
          const ev = JSON.parse(msg.body)
          onVoucherChange?.(ev)
        } catch (_) {}
      })
    }
  }

  function connect() {
    if (client) return
    client = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        resubscribe()
      },
      onWebSocketClose: () => { connected.value = false },
    })
    client.activate()
  }

  function disconnect() {
    if (customerSub) {
      try { customerSub.unsubscribe() } catch (_) {}
      customerSub = null
    }
    if (updatesSub) {
      try { updatesSub.unsubscribe() } catch (_) {}
      updatesSub = null
    }
    if (client) {
      try { client.deactivate() } catch (_) {}
      client = null
    }
    connected.value = false
  }

  return {
    connected,
    subscribeCustomer,
    disconnect
  }
}
