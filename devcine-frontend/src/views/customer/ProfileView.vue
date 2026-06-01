<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="pt-24 pb-20 max-w-[1440px] mx-auto px-10">
    <!-- Header Title -->
    <div class="mb-10">
      <h1 class="text-3xl font-extrabold tracking-tight uppercase font-headline">Tài khoản của tôi</h1>
    </div>
    
    <!-- Layout Container with Sidebar -->
    <div class="flex flex-col lg:flex-row gap-10">
      <!-- Sidebar Navigation -->
      <aside class="w-full lg:w-64 shrink-0">
        <nav class="flex flex-col gap-1">
          <router-link to="/profile" exact-active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-3 px-4 py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-sm border-l-4 border-transparent">
            <span class="material-symbols-outlined text-xl">person</span>
            Thông tin cá nhân
          </router-link>
          <router-link to="/profile/history" active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-3 px-4 py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-sm border-l-4 border-transparent">
            <span class="material-symbols-outlined text-xl">confirmation_number</span>
            Lịch sử đặt vé
          </router-link>
          <router-link to="/profile/vouchers" active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-3 px-4 py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-sm border-l-4 border-transparent">
            <span class="material-symbols-outlined text-xl">card_giftcard</span>
            Ưu đãi của tôi
          </router-link>
          <router-link to="/profile/password" active-class="bg-primary-container/10 !text-primary-container border-primary-container font-bold" class="flex items-center gap-3 px-4 py-3 text-neutral-400 hover:bg-white/5 hover:text-on-surface transition-colors text-sm border-l-4 border-transparent">
            <span class="material-symbols-outlined text-xl">lock_reset</span>
            Đổi mật khẩu
          </router-link>
          <hr class="my-2 border-white/5"/>
          <router-link to="/" class="flex items-center gap-3 px-4 py-3 text-error hover:bg-error/5 transition-colors text-sm border-l-4 border-transparent">
            <span class="material-symbols-outlined text-xl">logout</span>
            Đăng xuất
          </router-link>
        </nav>
      </aside>
      
      <!-- Main Content Area -->
      <div class="flex-grow">
        <router-view></router-view>
      </div>
    </div>
  </main>
</template>
