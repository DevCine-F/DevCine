import { setActivePinia, createPinia } from 'pinia'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { usePosStore } from './usePosStore'

const mockLocalStorage = (() => {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => { store[key] = value.toString() }),
    clear: vi.fn(() => { store = {} })
  }
})()
vi.stubGlobal('localStorage', mockLocalStorage)

vi.stubGlobal('window', {
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
})

// Mock dependencies
vi.mock('@/api/admin/index', () => ({
  ticketingApi: {
    releaseHold: vi.fn().mockResolvedValue({}),
  },
  posPendingOrderApi: {
    cancel: vi.fn().mockResolvedValue({}),
    hold: vi.fn().mockResolvedValue({}),
    resume: vi.fn().mockResolvedValue({}),
  },
}))

vi.mock('@/stores/toast', () => ({
  useToastStore: () => ({
    push: vi.fn(),
  }),
}))

describe('usePosStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
    localStorage.clear()
  })

  it('Test Case 1: holdOrder encodes Vietnamese characters correctly in LocalStorage', async () => {
    const store = usePosStore()
    const payload = {
      mode: 'TICKET',
      member: { customerName: 'Nguyễn Quang Huy' }
    }
    
    await store.holdOrder(payload)
    
    // Check local storage directly
    const raw = localStorage.getItem('devcine_pos_held_orders')
    expect(raw).toBeTruthy()
    
    // Decode and verify
    const decoded = decodeURIComponent(atob(raw))
    const parsed = JSON.parse(decoded)
    
    expect(parsed.length).toBe(1)
    expect(parsed[0].member.customerName).toBe('Nguyễn Quang Huy')
  })

  it('Test Case 2: Hydration handles bad Base64 gracefully', () => {
    // Inject bad base64
    localStorage.setItem('devcine_pos_held_orders', 'Not A Valid Base64 String 🚀')
    
    // This should not throw
    const store = usePosStore()
    
    expect(store.heldOrders.length).toBe(0)
    // Verify it was reset in local storage after a save operation or memory is clean
    expect(store.heldOrders).toEqual([])
  })

  it('Test Case 3: Single Ticker Loop clears interval when empty', async () => {
    const store = usePosStore()
    
    const code = await store.holdOrder({
      mode: 'TICKET',
      bookingId: 123
    })
    
    // Let's spy on global clearInterval
    const clearIntervalSpy = vi.spyOn(global, 'clearInterval')

    // Fast forward to expire the ticket (15 minutes default = 900 seconds)
    vi.advanceTimersByTime(901 * 1000)
    
    // After expiration, updateTimers should remove the expired TICKET order
    // and since array becomes empty, it should clear the interval
    expect(store.heldOrders.length).toBe(0)
    
    expect(clearIntervalSpy).toHaveBeenCalled()
  })
})
