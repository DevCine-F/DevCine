<script setup>
import { toRef, watch } from 'vue'
import { useCinemaConfig } from '@/composables/useCinemaConfig'

const props = defineProps({
  cinema: {
    type: Object,
    required: true
  }
})

const cinemaRef = toRef(props, 'cinema')
const {
  openSections,
  toggleSection,
  configSaving,
  configSuccess,
  configBasic,
  configHours,
  configCleaning,
  configSeats,
  configFormats,
  configBanners,
  allFormats,
  loadConfigForCinema,
  saveConfigBasic,
  saveConfigHours,
  saveConfigCleaning,
  saveConfigSeats,
  saveConfigFormats,
  toggleFormat,
  addBanner,
  removeBanner,
  saveConfigBanners
} = useCinemaConfig(cinemaRef)

// Watch for cinema changes to load config
watch(() => props.cinema, (newCinema) => {
  if (newCinema) {
    loadConfigForCinema(newCinema)
  }
}, { immediate: true })
</script>

<template>
  <div class="space-y-4">
    <!-- Section 1: Thông tin cơ bản -->
    <div class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
      :class="openSections.basic ? 'shadow-lg shadow-primary/5 border-primary/20' : ''">
      <!-- Accordion Header -->
      <button @click="toggleSection('basic')"
        class="w-full flex items-center gap-4 px-6 py-5 hover:bg-primary/5 transition-all duration-200 group">
        <div class="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center flex-shrink-0 transition-all duration-200"
          :class="openSections.basic ? 'bg-primary/20' : ''">
          <span class="material-symbols-outlined text-primary text-lg">store</span>
        </div>
        <div class="flex-1 text-left">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Thông tin cơ bản</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Tên, địa chỉ, liên hệ của rạp</p>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span v-if="configSuccess.basic" class="text-green-400 text-[9px] font-black uppercase tracking-widest flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">check_circle</span> Đã lưu
          </span>
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.basic ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <!-- Accordion Body -->
      <transition name="accordion">
        <div v-show="openSections.basic" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tên rạp</label>
                <input v-model="configBasic.name" type="text" placeholder="VD: DevCine Hà Nội"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Địa chỉ</label>
                <input v-model="configBasic.address" type="text" placeholder="VD: 123 Phố Huế, Hà Nội"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Số điện thoại</label>
                <input v-model="configBasic.phone" type="text" placeholder="VD: 0912 345 678"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Email</label>
                <input v-model="configBasic.email" type="email" placeholder="VD: cinema@devcine.vn"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
              </div>
            </div>
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-outline-variant/10">
              <button @click="saveConfigBasic" :disabled="configSaving.basic"
                class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-8 py-3 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="configSaving.basic" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ configSaving.basic ? 'Đang lưu...' : 'Lưu thông tin' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Section 2: Giờ hoạt động -->
    <div class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
      :class="openSections.hours ? 'shadow-lg shadow-blue-500/5 border-blue-500/20' : ''">
      <button @click="toggleSection('hours')"
        class="w-full flex items-center gap-4 px-6 py-5 hover:bg-blue-500/5 transition-all duration-200 group">
        <div class="w-10 h-10 rounded-2xl bg-blue-500/10 flex items-center justify-center flex-shrink-0 transition-all duration-200"
          :class="openSections.hours ? 'bg-blue-500/20' : ''">
          <span class="material-symbols-outlined text-blue-400 text-lg">schedule</span>
        </div>
        <div class="flex-1 text-left">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Giờ hoạt động</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Giờ mở/đóng cửa và ngày nghỉ lễ</p>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-blue-400 text-[10px] font-black">{{ configHours.openTime || '--:--' }} – {{ configHours.closeTime || '--:--' }}</span>
          <span v-if="configSuccess.hours" class="text-green-400 text-[9px] font-black uppercase tracking-widest flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">check_circle</span> Đã lưu
          </span>
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.hours ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <transition name="accordion">
        <div v-show="openSections.hours" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giờ mở cửa</label>
                <input v-model="configHours.openTime" type="time"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giờ đóng cửa</label>
                <input v-model="configHours.closeTime" type="time"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="md:col-span-2 space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ngày nghỉ lễ <span class="normal-case text-on-surface-variant/50">(mỗi ngày một dòng, định dạng DD/MM)</span></label>
                <textarea v-model="configHours.holidays" rows="3" placeholder="VD:&#10;01/01&#10;30/04&#10;01/05"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all resize-none" />
              </div>
            </div>
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-outline-variant/10">
              <button @click="saveConfigHours" :disabled="configSaving.hours"
                class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-8 py-3 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="configSaving.hours" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ configSaving.hours ? 'Đang lưu...' : 'Lưu giờ hoạt động' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Section 3: Thời gian dọn phòng -->
    <div class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
      :class="openSections.cleaning ? 'shadow-lg shadow-orange-500/5 border-orange-500/20' : ''">
      <button @click="toggleSection('cleaning')"
        class="w-full flex items-center gap-4 px-6 py-5 hover:bg-orange-500/5 transition-all duration-200 group">
        <div class="w-10 h-10 rounded-2xl bg-orange-500/10 flex items-center justify-center flex-shrink-0 transition-all duration-200"
          :class="openSections.cleaning ? 'bg-orange-500/20' : ''">
          <span class="material-symbols-outlined text-orange-400 text-lg">cleaning_services</span>
        </div>
        <div class="flex-1 text-left">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Thời gian dọn phòng</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Khoảng cách tối thiểu giữa các suất chiếu</p>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-orange-400 text-[10px] font-black">{{ configCleaning.cleaningMinutes }} phút</span>
          <span v-if="configSuccess.cleaning" class="text-green-400 text-[9px] font-black uppercase tracking-widest flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">check_circle</span> Đã lưu
          </span>
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.cleaning ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <transition name="accordion">
        <div v-show="openSections.cleaning" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 flex items-center gap-8">
              <div class="flex-1">
                <div class="flex items-center justify-between mb-3">
                  <span class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Thời gian dọn phòng</span>
                  <span class="text-2xl font-black text-primary">{{ configCleaning.cleaningMinutes }} <span class="text-sm font-normal text-on-surface-variant">phút</span></span>
                </div>
                <input v-model.number="configCleaning.cleaningMinutes" type="range" min="5" max="60" step="5"
                  class="w-full accent-primary h-2 rounded-full" />
                <div class="flex justify-between text-[9px] font-bold text-on-surface-variant/50 uppercase mt-2">
                  <span>5 phút</span><span>30 phút</span><span>60 phút</span>
                </div>
              </div>
              <div class="w-32 h-32 rounded-3xl bg-primary/10 border border-primary/20 flex flex-col items-center justify-center">
                <span class="text-4xl font-black text-primary">{{ configCleaning.cleaningMinutes }}</span>
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">phút</span>
              </div>
            </div>
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-outline-variant/10">
              <button @click="saveConfigCleaning" :disabled="configSaving.cleaning"
                class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-8 py-3 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="configSaving.cleaning" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ configSaving.cleaning ? 'Đang lưu...' : 'Lưu thời gian dọn' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Section 4: Loại ghế & Giá mặc định -->
    <div class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
      :class="openSections.seats ? 'shadow-lg shadow-pink-500/5 border-pink-500/20' : ''">
      <button @click="toggleSection('seats')"
        class="w-full flex items-center gap-4 px-6 py-5 hover:bg-pink-500/5 transition-all duration-200 group">
        <div class="w-10 h-10 rounded-2xl bg-pink-500/10 flex items-center justify-center flex-shrink-0 transition-all duration-200"
          :class="openSections.seats ? 'bg-pink-500/20' : ''">
          <span class="material-symbols-outlined text-pink-400 text-lg">chair</span>
        </div>
        <div class="flex-1 text-left">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Loại ghế &amp; Phụ thu</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Phụ thu cộng thêm vào giá nền — giá nền chạy theo đối tượng &amp; suất chiếu (cấu hình ở Bảng giá)</p>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-pink-400 text-[10px] font-black">{{ configSeats.types?.length || 0 }} loại ghế</span>
          <span v-if="configSuccess.seats" class="text-green-400 text-[9px] font-black uppercase tracking-widest flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">check_circle</span> Đã lưu
          </span>
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.seats ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <transition name="accordion">
        <div v-show="openSections.seats" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 space-y-4">
              <div v-for="seat in configSeats.types" :key="seat.id"
                class="flex items-center gap-4 p-4 bg-surface-container rounded-2xl border border-outline-variant/10">
                <div class="w-8 h-8 rounded-xl flex-shrink-0 flex items-center justify-center" :style="{ backgroundColor: seat.color + '33', border: '2px solid ' + seat.color }">
                  <span class="material-symbols-outlined text-sm" :style="{ color: seat.color }">chair</span>
                </div>
                <div class="flex-1 grid grid-cols-2 gap-4">
                  <div class="space-y-1">
                    <label class="text-[9px] font-black uppercase tracking-widest text-on-surface-variant">Tên loại ghế</label>
                    <input v-model="seat.name" type="text"
                      class="w-full bg-surface-container-high border border-outline-variant/20 rounded-lg px-3 py-2 text-xs text-on-surface focus:outline-none focus:border-primary/50 transition-all" />
                  </div>
                  <div class="space-y-1">
                    <label class="text-[9px] font-black uppercase tracking-widest text-on-surface-variant">Phụ thu loại ghế (VND)</label>
                    <input v-model.number="seat.surcharge" type="number" min="0" step="5000"
                      class="w-full bg-surface-container-high border border-outline-variant/20 rounded-lg px-3 py-2 text-xs text-on-surface focus:outline-none focus:border-primary/50 transition-all" />
                  </div>
                </div>
                <div class="space-y-1 flex-shrink-0">
                  <label class="text-[9px] font-black uppercase tracking-widest text-on-surface-variant">Màu</label>
                  <input v-model="seat.color" type="color" class="w-10 h-8 rounded-lg cursor-pointer bg-transparent border-none" />
                </div>
              </div>
            </div>
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-outline-variant/10">
              <button @click="saveConfigSeats" :disabled="configSaving.seats"
                class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-8 py-3 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="configSaving.seats" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ configSaving.seats ? 'Đang lưu...' : 'Lưu cấu hình ghế' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Section 5: Định dạng chiếu -->
    <div class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
      :class="openSections.formats ? 'shadow-lg shadow-purple-500/5 border-purple-500/20' : ''">
      <button @click="toggleSection('formats')"
        class="w-full flex items-center gap-4 px-6 py-5 hover:bg-purple-500/5 transition-all duration-200 group">
        <div class="w-10 h-10 rounded-2xl bg-purple-500/10 flex items-center justify-center flex-shrink-0 transition-all duration-200"
          :class="openSections.formats ? 'bg-purple-500/20' : ''">
          <span class="material-symbols-outlined text-purple-400 text-lg">movie</span>
        </div>
        <div class="flex-1 text-left">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Định dạng chiếu hỗ trợ</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Chọn các định dạng mà rạp hỗ trợ</p>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-purple-400 text-[10px] font-black">{{ configFormats.supported?.length || 0 }} định dạng</span>
          <span v-if="configSuccess.formats" class="text-green-400 text-[9px] font-black uppercase tracking-widest flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">check_circle</span> Đã lưu
          </span>
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.formats ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <transition name="accordion">
        <div v-show="openSections.formats" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 flex flex-wrap gap-3">
              <button v-for="fmt in allFormats" :key="fmt" @click="toggleFormat(fmt)"
                :class="configFormats.supported.includes(fmt)
                  ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
                  : 'bg-surface-container border-outline-variant/20 text-on-surface-variant hover:border-primary/40'"
                class="px-6 py-3 rounded-xl border font-black text-xs uppercase tracking-widest transition-all flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">{{ configFormats.supported.includes(fmt) ? 'check_circle' : 'radio_button_unchecked' }}</span>
                {{ fmt }}
              </button>
            </div>
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-outline-variant/10">
              <button @click="saveConfigFormats" :disabled="configSaving.formats"
                class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-8 py-3 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="configSaving.formats" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ configSaving.formats ? 'Đang lưu...' : 'Lưu định dạng chiếu' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Section 6: Banner / Quảng cáo -->
    <div class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
      :class="openSections.banners ? 'shadow-lg shadow-green-500/5 border-green-500/20' : ''">
      <button @click="toggleSection('banners')"
        class="w-full flex items-center gap-4 px-6 py-5 hover:bg-green-500/5 transition-all duration-200 group">
        <div class="w-10 h-10 rounded-2xl bg-green-500/10 flex items-center justify-center flex-shrink-0 transition-all duration-200"
          :class="openSections.banners ? 'bg-green-500/20' : ''">
          <span class="material-symbols-outlined text-green-400 text-lg">image</span>
        </div>
        <div class="flex-1 text-left">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Banner / Quảng cáo</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Hình ảnh banner riêng cho rạp này</p>
        </div>
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-green-400 text-[10px] font-black">{{ configBanners.banners?.length || 0 }} banner</span>
          <span v-if="configSuccess.banners" class="text-green-400 text-[9px] font-black uppercase tracking-widest flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">check_circle</span> Đã lưu
          </span>
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.banners ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <transition name="accordion">
        <div v-show="openSections.banners" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <div class="md:col-span-2 space-y-2">
                <label class="text-[9px] font-black uppercase tracking-widest text-on-surface-variant">URL hình ảnh</label>
                <input v-model="configBanners.newBannerUrl" type="url" placeholder="https://..."
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-[9px] font-black uppercase tracking-widest text-on-surface-variant">Tiêu đề</label>
                <div class="flex gap-2">
                  <input v-model="configBanners.newBannerTitle" type="text" placeholder="Banner..."
                    class="flex-1 bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
                  <button @click="addBanner"
                    class="bg-primary text-on-primary px-4 rounded-xl font-black text-lg hover:brightness-110 transition-all flex-shrink-0">+</button>
                </div>
              </div>
            </div>
            <div v-if="configBanners.banners.length > 0" class="space-y-3">
              <div v-for="banner in configBanners.banners" :key="banner.id"
                class="flex items-center gap-4 p-4 bg-surface-container rounded-2xl border border-outline-variant/10 group">
                <img v-if="banner.url" :src="banner.url" :alt="banner.title"
                  class="w-20 h-12 rounded-xl object-cover flex-shrink-0 border border-outline-variant/10" />
                <div v-else class="w-20 h-12 rounded-xl bg-surface-container-high flex items-center justify-center flex-shrink-0">
                  <span class="material-symbols-outlined text-on-surface-variant/40">broken_image</span>
                </div>
                <div class="flex-1">
                  <p class="text-sm font-bold text-on-surface">{{ banner.title }}</p>
                  <p class="text-[10px] text-on-surface-variant truncate">{{ banner.url }}</p>
                </div>
                <button @click="removeBanner(banner.id)"
                  class="opacity-0 group-hover:opacity-100 transition-all w-8 h-8 rounded-full bg-red-500/10 flex items-center justify-center text-red-400 hover:bg-red-500/20">
                  <span class="material-symbols-outlined text-sm">delete</span>
                </button>
              </div>
            </div>
            <div v-else class="flex flex-col items-center justify-center py-10 text-on-surface-variant/40">
              <span class="material-symbols-outlined text-4xl mb-2">image_not_supported</span>
              <p class="text-[10px] font-bold uppercase tracking-widest">Chưa có banner nào</p>
            </div>
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-outline-variant/10">
              <button @click="saveConfigBanners" :disabled="configSaving.banners"
                class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-8 py-3 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="configSaving.banners" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ configSaving.banners ? 'Đang lưu...' : 'Lưu banner' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>

  </div>
</template>
