import { defineStore } from 'pinia'
import { staffShiftApi, shiftHandoverApi } from '@/api/admin/index'
import { useAuthStore } from '@/stores/auth'
import { friendlyError } from '@/utils/friendlyError'

const POSITION_LABELS = {
  POS_TICKETING: 'Bán vé POS',
  CHECK_IN: 'Kiểm soát vé',
  FNB: 'Quầy F&B',
  PROJECTION: 'Phòng chiếu',
  SHIFT_LEAD: 'Trưởng ca',
}

export const useShiftStore = defineStore('shift', {
  state: () => ({
    current: null,
    isLoading: false,
    error: '',
    loaded: false,
  }),
  getters: {
    hasActiveShift: (state) => !!state.current,
    positionLabel: () => (value) => POSITION_LABELS[value] || value || 'Chưa phân công',
    shiftLabel(state) {
      if (!state.current) return 'Chưa có ca đang hoạt động'
      const position = POSITION_LABELS[state.current.workPosition] || state.current.workPosition
      const time = [state.current.startTime, state.current.endTime].filter(Boolean).join(' - ')
      return [position, time, state.current.location].filter(Boolean).join(' · ')
    },
    canUse: (state) => (positions) => {
      const auth = useAuthStore()
      if (!auth.isStaff || auth.isAdmin) return true
      if (!state.current) return false
      const allowed = new Set([...positions, 'SHIFT_LEAD'])
      return allowed.has(state.current.workPosition)
    },
    lockedMessage: (state) => (label) => {
      if (!state.current) return `Bạn chưa có ca ${label} đang hoạt động.`
      return `Ca hiện tại không được phân công cho ${label}.`
    },
  },
  actions: {
    async fetchCurrent(force = false) {
      const auth = useAuthStore()
      if (!auth.isAuthenticated || !['admin', 'staff'].includes(auth.role)) {
        this.current = null
        this.loaded = true
        return null
      }
      if (this.loaded && !force) return this.current

      this.isLoading = true
      this.error = ''
      try {
        const { data } = await staffShiftApi.current()
        this.current = data?.data ?? data ?? null
        this.loaded = true
        return this.current
      } catch (error) {
        this.current = null
        this.error = friendlyError(error, 'Không tải được ca hiện tại.')
        this.loaded = true
        return null
      } finally {
        this.isLoading = false
      }
    },
    async fetchCurrentSummary() {
      const { data } = await shiftHandoverApi.currentSummary()
      return data?.data ?? data
    },
    reset() {
      this.current = null
      this.isLoading = false
      this.error = ''
      this.loaded = false
    },
  },
})
