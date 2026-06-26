<script setup>
import { ref, computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useTheme } from '../composables/useTheme'
import { useAuthStore } from '../stores/auth'
import AppToast from '../components/common/AppToast.vue'
import ConfirmModal from '../components/common/ConfirmModal.vue'
import logo from '../assets/images/Logo_DevCine_Ngang_XoaNen.png'

const { isLightMode, toggleTheme } = useTheme()
const router = useRouter()
const authStore = useAuthStore()

const isAccountOpen = ref(false)
const displayName = computed(() => authStore.user?.fullName || authStore.user?.username || 'Quản trị viên')
const accountRole = computed(() => (authStore.role === 'admin' ? 'Quản trị cấp cao' : 'Nhân viên'))

const goProfile = () => { isAccountOpen.value = false; router.push('/admin/account') }
const goHome = () => { isAccountOpen.value = false; router.push('/') }
const handleLogout = () => { isAccountOpen.value = false; authStore.logout(); router.push('/admin/login') }
</script>

<template>
  <!-- Viewport Locking: Khóa cứng toàn bộ màn hình -->
  <div class="bg-surface text-on-surface font-body h-screen w-screen overflow-hidden flex">
    <!-- Admin Sidebar -->
    <aside class="w-72 border-r border-outline-variant/20 flex flex-col fixed h-screen bg-surface z-50">
      <div class="p-8 flex justify-center border-b border-outline-variant/10 mb-2">
        <RouterLink to="/admin/movies" class="flex flex-col items-center gap-3">
          <img :src="logo" alt="DEVCINE" class="w-36 h-auto object-contain brightness-110">
        </RouterLink>
      </div>

      <nav class="flex-grow px-4 pb-6 space-y-1.5 overflow-y-auto">
        <!-- ===== TỔNG QUAN & VẬN HÀNH ===== -->
        <div class="text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] px-4 mb-4">Tổng quan & Vận hành</div>

        <router-link to="/admin/dashboard" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">dashboard</span>
          <span class="font-semibold text-sm">Tổng quan</span>
        </router-link>

        <router-link to="/admin/ticketing" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">confirmation_number</span>
          <span class="font-semibold text-sm">Bán vé (POS)</span>
        </router-link>

        <router-link to="/admin/check-in" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">qr_code_scanner</span>
          <span class="font-semibold text-sm">Kiểm soát vé</span>
        </router-link>

        <router-link to="/admin/bookings" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">receipt_long</span>
          <span class="font-semibold text-sm">Hoá đơn</span>
        </router-link>

        <!-- ===== PHIM & NỘI DUNG ===== -->
        <div class="pt-6 text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] px-4 mb-4">Phim & Nội dung</div>

        <router-link to="/admin/movies" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">movie</span>
          <span class="font-semibold text-sm">Quản lý phim</span>
        </router-link>

        <router-link to="/admin/categories" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">category</span>
          <span class="font-semibold text-sm">Danh mục phim</span>
        </router-link>

        <router-link to="/admin/master-scheduling" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">calendar_month</span>
          <span class="font-semibold text-sm">Lịch chiếu & Điều phối</span>
        </router-link>

        <router-link to="/admin/banners" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">view_carousel</span>
          <span class="font-semibold text-sm">Quản lý Banner</span>
        </router-link>

        <!-- ===== RẠP & HẠ TẦNG ===== -->
        <div class="pt-6 text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] px-4 mb-4">Rạp & Hạ tầng</div>

        <router-link to="/admin/cinemas" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">theater_comedy</span>
          <span class="font-semibold text-sm">Cụm rạp</span>
        </router-link>

        <router-link to="/admin/seat-map" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">grid_view</span>
          <span class="font-semibold text-sm">Sơ đồ ghế</span>
        </router-link>

        <router-link to="/admin/fnb" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">fastfood</span>
          <span class="font-semibold text-sm">Thực đơn F&B / Combo</span>
        </router-link>

        <router-link to="/admin/inventory" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">inventory_2</span>
          <span class="font-semibold text-sm">Quản lý kho (F&B)</span>
        </router-link>

        <!-- ===== KINH DOANH & KHÁCH HÀNG ===== -->
        <div class="pt-6 text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] px-4 mb-4">Kinh doanh & Khách hàng</div>

        <router-link to="/admin/pricing" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">payments</span>
          <span class="font-semibold text-sm">Quản lý giá</span>
        </router-link>

        <router-link to="/admin/promotions" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">loyalty</span>
          <span class="font-semibold text-sm">Khuyến mãi</span>
        </router-link>

        <router-link to="/admin/customers" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">groups</span>
          <span class="font-semibold text-sm">Khách hàng</span>
        </router-link>

        <router-link to="/admin/customer-support" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">support_agent</span>
          <span class="font-semibold text-sm">Chăm sóc khách hàng</span>
        </router-link>

        <router-link to="/admin/faqs" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">quiz</span>
          <span class="font-semibold text-sm">Câu hỏi (FAQ)</span>
        </router-link>

        <!-- ===== NHÂN SỰ ===== -->
        <div class="pt-6 text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] px-4 mb-4">Nhân sự</div>

        <router-link to="/admin/staff" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">group</span>
          <span class="font-semibold text-sm">Nhân viên</span>
        </router-link>

        <router-link to="/admin/staff-shifts" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">event_available</span>
          <span class="font-semibold text-sm">Phê duyệt ca trực</span>
        </router-link>

        <!-- ===== HỆ THỐNG ===== -->
        <div class="pt-6 text-[10px] font-bold text-outline-variant uppercase tracking-[0.2em] px-4 mb-4">Hệ thống</div>

        <router-link to="/admin/permissions" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">admin_panel_settings</span>
          <span class="font-semibold text-sm">Phân quyền</span>
        </router-link>

        <router-link to="/admin/logs" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">manage_search</span>
          <span class="font-semibold text-sm">Nhật ký</span>
        </router-link>

        <router-link to="/admin/settings" class="flex items-center gap-4 px-4 py-3 rounded-lg text-on-surface-variant hover:bg-white/5 transition-all group" active-class="active-nav">
          <span class="material-symbols-outlined group-hover:text-primary transition-colors">settings</span>
          <span class="font-semibold text-sm">Cài đặt</span>
        </router-link>
      </nav>
    </aside>

    <!-- Main Content Area with Topbar -->
    <!-- flex-1 min-w-0 đảm bảo box này không bị bung ra (overflow) theo nội dung bên trong -->
    <div class="ml-72 flex-1 h-screen flex flex-col bg-surface-container-lowest min-w-0">
      
      <!-- Enterprise Topbar -->
      <header class="h-16 border-b border-outline-variant/10 bg-surface/95 backdrop-blur flex-shrink-0 sticky top-0 z-[100] flex justify-between items-center px-8">
        <div class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant italic">Quản lý DevCine V2.1</div>
        
        <div class="flex items-center gap-6">
          <!-- Theme Toggle -->
          <button @click="toggleTheme" class="relative p-2 hover:bg-white/5 rounded-full transition-colors cursor-pointer flex items-center">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-white transition-colors">
              {{ isLightMode ? 'dark_mode' : 'light_mode' }}
            </span>
          </button>

          <!-- Notification Bell -->
          <div class="relative group">
            <button class="relative p-2 hover:bg-white/5 rounded-full transition-colors cursor-pointer flex items-center">
              <span class="material-symbols-outlined text-on-surface-variant group-hover:text-on-surface transition-colors">notifications</span>
              <!-- Red Badge -->
              <span class="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full animate-pulse border border-surface"></span>
            </button>
            
            <!-- Notification Dropdown -->
            <div class="absolute right-0 mt-2 w-80 bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_10px_40px_-10px_var(--shadow-color)] opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 overflow-hidden">
               <div class="p-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
                  <span class="text-xs font-black uppercase tracking-widest text-on-surface">Thông báo</span>
                  <span class="text-[9px] text-primary hover:underline cursor-pointer uppercase tracking-wider">Đánh dấu đã đọc</span>
               </div>
               <div class="max-h-80 overflow-y-auto">
                  <div class="p-4 border-b border-outline-variant/5 hover:bg-white/5 transition-colors cursor-pointer flex gap-3 group/item">
                     <span class="material-symbols-outlined text-red-400 text-lg flex-shrink-0 group-hover/item:scale-110 transition-transform">warning</span>
                     <div>
                        <p class="text-[10px] font-bold text-on-surface leading-tight uppercase">Cảnh báo xung đột giờ chiếu</p>
                        <p class="text-[9px] text-on-surface-variant mt-1.5 leading-relaxed">Phòng IMAX 01 đang bị trùng lịch giữa Oppenheimer và Avatar 2.</p>
                        <p class="text-[8px] text-on-surface-variant/50 mt-2 font-bold italic">Vừa xong</p>
                     </div>
                  </div>
                  <div class="p-4 border-b border-outline-variant/5 hover:bg-white/5 transition-colors cursor-pointer flex gap-3 group/item">
                     <span class="material-symbols-outlined text-green-400 text-lg flex-shrink-0 group-hover/item:scale-110 transition-transform">trending_up</span>
                     <div>
                        <p class="text-[10px] font-bold text-on-surface leading-tight">Doanh thu đạt mốc 1 Tỷ VND</p>
                        <p class="text-[9px] text-on-surface-variant mt-1.5 leading-relaxed">Doanh thu hôm nay đã vượt mốc 100,000,000 VNĐ.</p>
                        <p class="text-[8px] text-on-surface-variant/50 mt-2 font-bold italic">2 giờ trước</p>
                     </div>
                  </div>
                  <div class="p-4 hover:bg-white/5 transition-colors cursor-pointer flex gap-3 group/item">
                     <span class="material-symbols-outlined text-blue-400 text-lg flex-shrink-0 group-hover/item:scale-110 transition-transform">support_agent</span>
                     <div>
                        <p class="text-[10px] font-bold text-on-surface leading-tight">Yêu cầu hỗ trợ mới từ KH</p>
                        <p class="text-[9px] text-on-surface-variant mt-1.5 leading-relaxed">Có 5 yêu cầu hoàn vé mới cần được duyệt gấp.</p>
                        <p class="text-[8px] text-on-surface-variant/50 mt-2 font-bold italic">5 giờ trước</p>
                     </div>
                  </div>
               </div>
            </div>
          </div>

          <!-- Account Area -->
          <div class="relative">
            <button @click="isAccountOpen = !isAccountOpen"
                    class="flex items-center gap-3 pl-6 border-l border-outline-variant/10 cursor-pointer hover:opacity-80 transition-opacity">
              <div class="text-right hidden md:block">
                 <p class="text-[10px] font-black text-on-surface uppercase tracking-wider">{{ displayName }}</p>
                 <p class="text-[8px] text-primary uppercase tracking-widest italic font-bold">{{ accountRole }}</p>
              </div>
              <div class="w-8 h-8 rounded-full bg-primary/20 border border-primary/30 flex items-center justify-center">
                 <span class="material-symbols-outlined text-primary text-sm">shield_person</span>
              </div>
              <span class="material-symbols-outlined text-on-surface-variant text-base transition-transform duration-200" :class="{ 'rotate-180': isAccountOpen }">expand_more</span>
            </button>

            <!-- Backdrop đóng dropdown khi click ra ngoài -->
            <div v-if="isAccountOpen" class="fixed inset-0 z-[105]" @click="isAccountOpen = false"></div>

            <!-- Account Dropdown -->
            <transition name="dropdown">
              <div v-if="isAccountOpen" class="absolute right-0 mt-3 w-60 bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-[0_10px_40px_-10px_var(--shadow-color)] z-[110] overflow-hidden">
                <div class="p-4 border-b border-outline-variant/10 bg-surface-container-lowest flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-primary/20 border border-primary/30 flex items-center justify-center flex-shrink-0">
                    <span class="material-symbols-outlined text-primary text-base">shield_person</span>
                  </div>
                  <div class="min-w-0">
                    <p class="text-xs font-black text-on-surface truncate">{{ displayName }}</p>
                    <p class="text-[9px] text-primary uppercase tracking-widest font-bold">{{ accountRole }}</p>
                  </div>
                </div>
                <div class="py-2">
                  <button @click="goProfile" class="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-white/5 hover:text-on-surface transition-colors text-left">
                    <span class="material-symbols-outlined text-lg">account_circle</span>
                    <span class="text-sm font-semibold">Tài khoản</span>
                  </button>
                  <button @click="goHome" class="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-white/5 hover:text-on-surface transition-colors text-left">
                    <span class="material-symbols-outlined text-lg">home</span>
                    <span class="text-sm font-semibold">Về trang chủ</span>
                  </button>
                  <div class="my-1 border-t border-outline-variant/10"></div>
                  <button @click="handleLogout" class="w-full flex items-center gap-3 px-4 py-3 text-red-400 hover:bg-red-500/10 transition-colors text-left">
                    <span class="material-symbols-outlined text-lg">logout</span>
                    <span class="text-sm font-semibold">Đăng xuất</span>
                  </button>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </header>

      <!-- Router View Container -->
      <!-- overflow-y-auto cho phép nội dung cuộn dọc độc lập nếu dài (vd trang khuyến mãi), overflow-x-hidden để chặn vỡ layout ngang -->
      <main class="flex-1 relative overflow-y-auto overflow-x-hidden flex flex-col">
        <router-view />
      </main>
    </div>

    <!-- Toast dùng chung cho toàn khu quản trị -->
    <AppToast />
    <ConfirmModal />
  </div>
</template>

<style scoped>
.active-nav {
  background: linear-gradient(90deg, rgba(245, 197, 24, 0.1) 0%, rgba(245, 197, 24, 0) 100%);
  border-left: 4px solid #f5c518;
  color: #f5c518 !important;
}
.active-nav .material-symbols-outlined {
  font-variation-settings: 'FILL' 1;
}
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
