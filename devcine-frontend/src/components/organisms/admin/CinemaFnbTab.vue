<script setup>
defineProps({
  inventory: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <div class="flex justify-between items-center mb-10">
    <h3
      class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
    >
      Hệ thống F&B & Kho
    </h3>
    <div class="flex gap-4">
      <button
        class="bg-surface-container-high px-4 py-2 rounded-xl border border-outline-variant/10 text-[10px] font-black uppercase tracking-widest text-on-surface-variant"
      >
        Báo cáo Hao hụt
      </button>
      <button
        class="bg-primary text-on-primary px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-primary/20 hover:brightness-110 transition-all flex items-center gap-2 italic"
      >
        <span class="material-symbols-outlined text-sm"
          >celebration</span
        >
        Tạo Combo
      </button>
    </div>
  </div>

  <div
    class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12"
  >
    <div
      v-for="item in inventory"
      :key="item.id"
      class="bg-surface-container-high border border-outline-variant/10 p-6 rounded-2xl group hover:border-primary/30 transition-all relative overflow-hidden"
    >
      <div
        class="absolute -right-4 -bottom-4 opacity-5 group-hover:opacity-10 transition-opacity"
      >
        <span class="material-symbols-outlined text-7xl"
          >inventory_2</span
        >
      </div>
      <div class="flex justify-between items-start mb-4">
        <p
          class="text-[9px] font-black text-primary uppercase tracking-widest"
        >
          Vật tư kho
        </p>
        <span
          v-if="item.stock < item.minStock"
          class="flex items-center gap-1 text-[8px] font-black text-red-500 animate-bounce"
        >
          <span class="material-symbols-outlined text-xs"
            >warning</span
          >
          SẮP HẾT HÀNG
        </span>
      </div>
      <h4
        class="text-lg font-black text-on-surface mb-6 uppercase tracking-tight"
      >
        {{ item.name }}
      </h4>
      <div class="flex items-end justify-between relative z-10">
        <div>
          <p
            class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-1 opacity-50"
          >
            Tồn kho hiện tại
          </p>
          <h5
            class="text-3xl font-black"
            :class="
              item.stock < item.minStock
                ? 'text-red-500'
                : 'text-on-surface'
            "
          >
            {{ item.stock }}
            <span class="text-xs font-normal opacity-50">{{
              item.unit
            }}</span>
          </h5>
        </div>
        <div class="text-right">
          <p class="text-[8px] font-black text-orange-500 uppercase">
            Hao hụt/Hỏng
          </p>
          <p class="text-sm font-black text-on-surface">
            {{ item.waste }}
          </p>
        </div>
      </div>
    </div>
  </div>

  <!-- Seasonal Combos (Special Request) -->
  <h4
    class="text-xs font-black uppercase tracking-[0.3em] text-primary italic mb-6"
  >
    Combos Theo Mùa
  </h4>
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <div
      class="bg-surface-container-high border-2 border-dashed border-outline-variant/20 p-8 rounded-2xl flex flex-col items-center justify-center text-center group hover:border-primary/50 cursor-pointer transition-all"
    >
      <div
        class="w-16 h-16 rounded-full bg-on-surface/5 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform"
      >
        <span class="material-symbols-outlined text-primary text-3xl"
          >add_circle</span
        >
      </div>
      <p
        class="text-xs font-black uppercase tracking-widest text-on-surface group-hover:text-primary transition-colors"
      >
        Tạo Combo Mùa Lễ Hội
      </p>
      <p
        class="text-[9px] text-on-surface-variant mt-2 font-bold italic opacity-60"
      >
        Thanh toán riêng, gán giá đặc biệt cho từng cụm rạp
      </p>
    </div>
    <div
      class="bg-gradient-to-br from-primary/10 to-transparent border border-primary/30 p-8 rounded-2xl flex justify-between items-center relative overflow-hidden group"
    >
      <div
        class="absolute -right-4 -bottom-4 text-primary opacity-10 group-hover:scale-110 transition-transform"
      >
        <span class="material-symbols-outlined text-9xl"
          >favorite</span
        >
      </div>
      <div>
        <h5
          class="text-xl font-black text-on-surface uppercase tracking-tight mb-2"
        >
          Combo Valentine 2024
        </h5>
        <p
          class="text-[10px] font-bold text-on-surface-variant italic mb-4"
        >
          2 Pepsi Large + 1 Bắp Phô Mai (L) + 1 Gấu Bông
        </p>
        <div class="flex items-center gap-4">
          <span class="text-2xl font-black text-primary italic"
            >199.000đ</span
          >
          <span
            class="px-2 py-1 bg-green-500/20 text-green-500 text-[8px] font-black rounded uppercase"
            >Đang áp dụng</span
          >
        </div>
      </div>
    </div>
  </div>
</template>
