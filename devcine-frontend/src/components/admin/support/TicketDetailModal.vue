<script setup>
import { computed, ref, watch } from 'vue'
import AppModal from '@/components/common/AppModal.vue'
import {
  issueTypeLabel, statusLabel, statusClass, parseSupportContent, formatTime
} from '@/utils/supportTicket'

const props = defineProps({
  show: Boolean,
  ticket: { type: Object, default: null },
  canEdit: Boolean,
  submitting: Boolean
})

const emit = defineEmits(['close', 'reply', 'update-status'])

const replyMessage = ref('')

const content = computed(() => parseSupportContent(props.ticket || {}))

// Đổi ticket / mở lại modal → xoá nội dung soạn dở.
watch(() => props.ticket?.id, () => { replyMessage.value = '' })
watch(() => props.show, (v) => { if (!v) replyMessage.value = '' })

const handleSendReply = () => {
  const msg = replyMessage.value.trim()
  if (!msg) return
  emit('reply', msg)
}
</script>

<template>
  <AppModal :show="show" :title="`Ticket #${ticket?.id ?? ''}`" @close="$emit('close')">
    <div v-if="ticket" class="max-h-[70vh] overflow-y-auto -m-1 p-1 space-y-6">
      <!-- Header: chủ đề + trạng thái -->
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="text-lg font-black text-on-surface tracking-tight">{{ issueTypeLabel(ticket.issueType) }}</p>
          <p class="text-[11px] text-on-surface-variant uppercase font-bold tracking-wider mt-1">
            {{ ticket.customerName }} • {{ formatTime(ticket.createdAt) }}
          </p>
        </div>
        <span :class="statusClass(ticket.status)"
              class="text-[9px] font-black uppercase tracking-widest px-3 py-1 rounded-full border whitespace-nowrap">
          {{ statusLabel(ticket.status) }}
        </span>
      </div>

      <!-- Liên hệ -->
      <div class="flex flex-wrap gap-2">
        <span v-if="ticket.customerEmail"
              class="inline-flex items-center gap-1.5 bg-surface-container-high border border-outline-variant/10 rounded-lg px-3 py-1.5 text-xs text-on-surface">
          <span class="material-symbols-outlined text-sm text-primary">mail</span>{{ ticket.customerEmail }}
        </span>
        <span v-if="content.phone"
              class="inline-flex items-center gap-1.5 bg-surface-container-high border border-outline-variant/10 rounded-lg px-3 py-1.5 text-xs text-on-surface">
          <span class="material-symbols-outlined text-sm text-primary">call</span>{{ content.phone }}
        </span>
      </div>

      <!-- Nội dung yêu cầu -->
      <div>
        <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant mb-2">Nội dung yêu cầu</p>
        <p class="text-sm text-on-surface leading-relaxed whitespace-pre-wrap break-words bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">{{ content.message || '(Không có nội dung)' }}</p>
      </div>

      <!-- Lịch sử phản hồi -->
      <div v-if="ticket.adminReply">
        <p class="text-[10px] font-black uppercase tracking-widest text-primary mb-2">
          Đã phản hồi • {{ formatTime(ticket.repliedAt) }}
        </p>
        <p class="text-sm text-on-surface leading-relaxed whitespace-pre-wrap break-words bg-primary/5 border border-primary/20 rounded-xl p-4">{{ ticket.adminReply }}</p>
      </div>

      <!-- Soạn phản hồi -->
      <div v-if="canEdit && ticket.status !== 'CLOSED'" class="space-y-3 pt-2 border-t border-outline-variant/10">
        <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">
          {{ ticket.adminReply ? 'Gửi phản hồi bổ sung' : 'Soạn phản hồi qua email' }}
        </p>
        <textarea v-model="replyMessage" rows="4"
                  placeholder="Nhập nội dung phản hồi gửi tới email khách hàng..."
                  class="w-full bg-surface-container-low border border-outline-variant/20 focus:border-primary text-on-surface text-sm px-4 py-3 rounded-xl outline-none transition-all resize-none"></textarea>
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex gap-2">
            <button v-if="ticket.status === 'OPEN'" @click="$emit('update-status', 'IN_PROGRESS')"
                    class="px-4 py-2 bg-surface-container-high text-on-surface text-[10px] font-black uppercase tracking-widest rounded-md hover:bg-white/5 transition-all">Nhận xử lý</button>
            <button @click="$emit('update-status', 'CLOSED')"
                    class="px-4 py-2 bg-surface-container-high text-on-surface text-[10px] font-black uppercase tracking-widest rounded-md hover:bg-white/5 transition-all">Đóng ticket</button>
          </div>
          <button @click="handleSendReply" :disabled="submitting || !replyMessage.trim()"
                  class="inline-flex items-center gap-2 px-6 py-2 bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest rounded-md hover:brightness-110 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            <span v-if="submitting" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
            <span v-else class="material-symbols-outlined text-sm">send</span>
            Gửi phản hồi
          </button>
        </div>
      </div>

      <div v-else-if="ticket.status === 'CLOSED'" class="pt-2 border-t border-outline-variant/10">
        <p class="text-xs text-on-surface-variant italic text-center py-2">Ticket đã đóng.</p>
      </div>
    </div>
  </AppModal>
</template>
