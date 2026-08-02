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
  configError,
  configBasic,
  configHours,
  configCleaning,
  configSeats,
  configFormats,
  allFormats,
  loadConfigForCinema,
  saveConfigBasic,
  saveConfigHours,
  saveConfigSeats,
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
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Hotline <span class="normal-case text-on-surface-variant/50">(8–11 chữ số)</span></label>
                <input v-model="configBasic.hotline" type="text" placeholder="VD: 1900 1234"
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all" />
              </div>
              <div class="md:col-span-2 space-y-2">
                <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Mô tả <span class="normal-case text-on-surface-variant/50">(tối đa 1000 ký tự)</span></label>
                <textarea v-model="configBasic.description" rows="3" maxlength="1000" placeholder="Giới thiệu ngắn về cụm rạp..."
                  class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none focus:border-primary/50 transition-all resize-none" />
              </div>
            </div>
            <p v-if="configError.basic" class="mt-4 text-xs text-red-400 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">error</span>{{ configError.basic }}
            </p>
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
                <textarea :value="configHours.holidays" rows="3" disabled
                  class="w-full bg-surface-container/60 border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface-variant resize-none cursor-not-allowed" />
              </div>
            </div>
            <p class="mt-3 text-[10px] text-on-surface-variant/60 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">info</span>Nếu giờ đóng ≤ giờ mở, hệ thống hiểu là rạp đóng cửa rạng sáng hôm sau (suất khuya). Ngày nghỉ lễ hiện chỉ để tham khảo.
            </p>
            <p v-if="configError.hours" class="mt-2 text-xs text-red-400 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">error</span>{{ configError.hours }}
            </p>
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
    <div v-if="false" class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
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
                <input :value="configCleaning.cleaningMinutes" type="range" min="5" max="60" step="5" disabled
                  class="w-full accent-primary h-2 rounded-full opacity-60 cursor-not-allowed" />
                <div class="flex justify-between text-[9px] font-bold text-on-surface-variant/50 uppercase mt-2">
                  <span>5 phút</span><span>30 phút</span><span>60 phút</span>
                </div>
              </div>
              <div class="w-32 h-32 rounded-3xl bg-primary/10 border border-primary/20 flex flex-col items-center justify-center">
                <span class="text-4xl font-black text-primary">{{ configCleaning.cleaningMinutes }}</span>
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">phút</span>
              </div>
            </div>
            <p class="mt-4 text-[10px] text-on-surface-variant/60 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">lock</span>Thông tin cấu hình mẫu — chỉ để tham khảo, chưa mở chỉnh sửa.
            </p>
          </div>
        </div>
      </transition>
    </div>

    <!-- Section 4: Loại ghế & Giá mặc định -->
    <div v-if="false" class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
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
    <div v-if="false" class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden transition-all duration-300"
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
          <span class="material-symbols-outlined text-on-surface-variant/50 text-lg transition-transform duration-300"
            :style="{ transform: openSections.formats ? 'rotate(180deg)' : 'rotate(0deg)' }">expand_more</span>
        </div>
      </button>
      <transition name="accordion">
        <div v-show="openSections.formats" class="accordion-body">
          <div class="px-6 pb-6 border-t border-outline-variant/10">
            <div class="pt-6 flex flex-wrap gap-3">
              <!-- Hiển thị tĩnh: định dạng rạp hỗ trợ (chưa mở chỉnh sửa) -->
              <div v-for="fmt in allFormats" :key="fmt"
                :class="configFormats.supported.includes(fmt)
                  ? 'bg-primary/15 border-primary/40 text-primary'
                  : 'bg-surface-container border-outline-variant/20 text-on-surface-variant/40'"
                class="px-6 py-3 rounded-xl border font-black text-xs uppercase tracking-widest flex items-center gap-2 select-none">
                <span class="material-symbols-outlined text-sm">{{ configFormats.supported.includes(fmt) ? 'check_circle' : 'radio_button_unchecked' }}</span>
                {{ fmt }}
              </div>
            </div>
            <p class="mt-4 text-[10px] text-on-surface-variant/60 flex items-center gap-1.5">
              <span class="material-symbols-outlined text-sm">lock</span>Thông tin cấu hình mẫu — chỉ để tham khảo, chưa mở chỉnh sửa.
            </p>
          </div>
        </div>
      </transition>
    </div>

  </div>
</template>
