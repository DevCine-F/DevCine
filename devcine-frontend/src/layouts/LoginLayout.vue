<script setup>
import { RouterView } from 'vue-router'
import AppToast from '../components/common/AppToast.vue'
</script>

<template>
  <div class="min-h-screen bg-surface relative overflow-hidden">
    <!-- Subtle Background Elements -->
    <div class="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/5 blur-[120px] rounded-full pointer-events-none"></div>
    <div class="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-primary/10 blur-[120px] rounded-full pointer-events-none"></div>

    <!-- Layout Branding (Optional overlay) -->
    <div class="absolute top-8 left-1/2 -translate-x-1/2 z-[100] text-center pointer-events-none md:hidden">
      <h1 class="text-3xl font-black tracking-tighter text-primary font-headline">DEVCINE</h1>
    </div>

    <router-view />

    <!-- Toast dùng chung cho màn đăng nhập -->
    <AppToast />
  </div>
</template>

<style scoped>
/* Any auth layout specific styles */
</style>
