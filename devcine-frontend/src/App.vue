<script setup>
import { RouterView, useRouter } from 'vue-router'
import { watch, onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

// ===== Tự đăng xuất khi JWT hết hạn (chủ động, không chờ tới khi gọi API bị 401) =====
const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()
let expiryTimer = null

// ===== Lắng nghe trạng thái tài khoản thời gian thực (Kick tức thì khi bị khóa) =====
let statusClient = null
let statusSub = null

const wsUrl = () =>
  (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws'

function syncStatusListener() {
  const customerId = auth.user?.id || auth.user?.userId
  if (!auth.isAuthenticated || !customerId) {
    if (statusSub) {
      try { statusSub.unsubscribe() } catch (_) {}
      statusSub = null
    }
    if (statusClient) {
      try { statusClient.deactivate() } catch (_) {}
      statusClient = null
    }
    return
  }

  if (!statusClient) {
    statusClient = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 5000,
      onConnect: () => {
        if (statusSub) {
          try { statusSub.unsubscribe() } catch (_) {}
          statusSub = null
        }
        statusSub = statusClient.subscribe(`/topic/customer/${customerId}/status`, (msg) => {
          try {
            const ev = JSON.parse(msg.body)
            if (ev && ev.type === 'ACCOUNT_LOCKED') {
              const reason = ev.reason || 'Tài khoản bị tạm khóa bởi Quản trị viên.'
              auth.logout()
              toast.error(`Tài khoản của bạn đã bị tạm khóa: ${reason}. Vui lòng liên hệ CSKH.`)
              router.push('/login')
            }
          } catch (_) {}
        })
      },
      onWebSocketClose: () => {}
    })
    statusClient.activate()
  }
}

// Giải mã trường exp (giây) trong payload JWT -> mốc hết hạn (ms)
function tokenExpiryMs(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp ? payload.exp * 1000 : 0
  } catch {
    return 0
  }
}

function handleExpired() {
  if (!auth.isAuthenticated) return
  const wasAdmin = auth.isAdmin
  auth.logout()
  toast.warning('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
  router.push(wasAdmin ? '/admin/login' : '/login')
}

function scheduleExpiry() {
  if (expiryTimer) { clearTimeout(expiryTimer); expiryTimer = null }
  if (!auth.token) return
  const exp = tokenExpiryMs(auth.token)
  if (!exp) return // token không có exp -> bỏ qua, vẫn còn cơ chế 401 ở axios
  const remaining = exp - Date.now()
  if (remaining <= 0) {
    handleExpired()
  } else {
    // setTimeout dùng số 32-bit; kẹp trần ~24 ngày để an toàn
    expiryTimer = setTimeout(handleExpired, Math.min(remaining, 2_000_000_000))
  }
}

watch(() => auth.token, () => {
  scheduleExpiry()
  if (auth.isAuthenticated) {
    auth.fetchProfile()
  }
  syncStatusListener()
})

watch(() => auth.user, () => {
  syncStatusListener()
}, { deep: true })

onMounted(() => {
  scheduleExpiry()
  if (auth.isAuthenticated) {
    auth.fetchProfile()
  }
  syncStatusListener()
})

onUnmounted(() => {
  if (expiryTimer) clearTimeout(expiryTimer)
  if (statusSub) {
    try { statusSub.unsubscribe() } catch (_) {}
    statusSub = null
  }
  if (statusClient) {
    try { statusClient.deactivate() } catch (_) {}
    statusClient = null
  }
})
</script>

<template>
  <RouterView />
</template>

<style>
/* Global styles are in src/assets/styles/global.css */
</style>
