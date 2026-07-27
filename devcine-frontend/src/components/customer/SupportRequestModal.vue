<script setup>
import AppModal from '@/components/common/AppModal.vue'
import SupportRequestForm from '@/components/customer/SupportRequestForm.vue'

defineProps({
  show: Boolean
})
const emit = defineEmits(['close'])
</script>

<template>
  <AppModal :show="show" title="Gửi yêu cầu hỗ trợ" @close="emit('close')">
    <p class="text-sm text-on-surface-variant mb-6 leading-relaxed">
      Không tìm thấy câu trả lời trong Câu hỏi thường gặp? Gửi yêu cầu để bộ phận CSKH hỗ trợ bạn trực tiếp.
    </p>
    <SupportRequestForm @submitted="emit('close')" />
  </AppModal>
</template>
