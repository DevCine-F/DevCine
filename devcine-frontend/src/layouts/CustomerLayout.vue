<script setup>
import { useRoute } from 'vue-router'
import { RouterView } from 'vue-router'
import Navbar from '../components/customer/Navbar.vue'
import Footer from '../components/customer/Footer.vue'
import StarryBackground from '../components/common/StarryBackground.vue'
import AppToast from '../components/common/AppToast.vue'

const route = useRoute()
</script>

<template>
  <div class="min-h-screen flex flex-col text-on-surface bg-transparent relative z-0">
    <StarryBackground />
    <Navbar />
    
    <main class="flex-grow">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <Footer />
    <AppToast />
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
