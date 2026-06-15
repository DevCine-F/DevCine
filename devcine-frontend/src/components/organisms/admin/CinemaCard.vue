<script setup>
defineProps({
  cinema: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])
</script>

<template>
  <div
    @click="$emit('click')"
    class="bg-surface-container-low border border-outline-variant/10 rounded-xl overflow-hidden hover:border-primary/40 transition-all group cursor-pointer shadow-sm hover:shadow-xl hover:-translate-y-1 duration-500 flex flex-col"
  >
    <div
      class="h-56 bg-surface-container-highest relative overflow-hidden"
    >
      <img
        src="/images/Hopper.webp"
        class="w-full h-full object-cover transition-all duration-700 group-hover:scale-110 opacity-60 group-hover:opacity-100"
      />
      <div
        class="absolute inset-0 bg-gradient-to-t from-surface-container-low via-surface-container-low/40 to-transparent"
      ></div>
      <div class="absolute bottom-4 left-6 flex gap-2">
        <span
          class="bg-primary/20 text-primary text-[9px] font-black px-3 py-1.5 rounded-full uppercase tracking-widest backdrop-blur-md border border-primary/20"
          >{{ cinema.type }}</span
        >
        <span
          class="bg-green-500/20 text-green-500 text-[9px] font-black px-3 py-1.5 rounded-full uppercase tracking-widest backdrop-blur-md border border-green-500/20"
          >Online</span
        >
      </div>
    </div>
    <div class="p-8 flex flex-col flex-1">
      <h3
        class="font-headline font-bold text-xl mb-3 text-on-surface group-hover:text-primary transition-colors"
      >
        {{ cinema.name }}
      </h3>
      <p
        class="text-xs text-on-surface-variant mb-8 line-clamp-2 leading-relaxed opacity-70 italic flex-1"
      >
        {{ cinema.address }}
      </p>

      <div
        class="flex justify-between items-center pt-6 border-t border-outline-variant/5"
      >
        <div class="flex items-center gap-3 text-on-surface-variant">
          <div class="p-2 bg-on-surface/5 rounded-lg">
            <span class="material-symbols-outlined text-lg"
              >meeting_room</span
            >
          </div>
          <div>
            <p
              class="text-[10px] font-black uppercase tracking-widest text-on-surface"
            >
              {{ cinema.halls?.length ?? cinema.rooms }} Phòng
            </p>
            <p
              class="text-[8px] uppercase tracking-tighter opacity-50 font-bold"
            >
              Cinema Halls
            </p>
          </div>
        </div>
        <div
          class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity"
        >
          <button
            class="w-10 h-10 flex items-center justify-center rounded-xl bg-surface-container-high border border-outline-variant/10 text-on-surface-variant hover:text-primary hover:border-primary/40 transition-all"
          >
            <span class="material-symbols-outlined text-sm"
              >settings</span
            >
          </button>
          <div
            class="w-10 h-10 flex items-center justify-center rounded-xl bg-primary text-on-primary shadow-lg shadow-primary/20"
          >
            <span class="material-symbols-outlined text-sm"
              >arrow_forward_ios</span
            >
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
