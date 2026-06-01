<template>
  <section class="max-w-2xl">
    <h2 class="text-2xl font-bold tracking-tight font-headline mb-8">Đổi mật khẩu</h2>
    
    <div class="bg-surface-container-low p-8 rounded-lg border border-white/5">
      <form @submit.prevent class="flex flex-col gap-6">
        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu hiện tại</label>
          <input type="password" class="w-full bg-surface-container-highest border-none text-sm text-white px-4 py-3 rounded focus:ring-1 focus:ring-primary-container" placeholder="Nhập mật khẩu hiện tại" />
        </div>
        
        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Mật khẩu mới</label>
          <input type="password" class="w-full bg-surface-container-highest border-none text-sm text-white px-4 py-3 rounded focus:ring-1 focus:ring-primary-container" placeholder="Nhập mật khẩu mới" />
        </div>
        
        <div>
          <label class="block text-[10px] uppercase font-bold tracking-widest text-on-surface-variant mb-2">Xác nhận mật khẩu mới</label>
          <input type="password" class="w-full bg-surface-container-highest border-none text-sm text-white px-4 py-3 rounded focus:ring-1 focus:ring-primary-container" placeholder="Nhập lại mật khẩu mới" />
        </div>
        
        <div class="pt-4 border-t border-white/5 mt-2 flex justify-end">
          <button type="submit" class="bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-8 py-3 transition-transform active:scale-95 hover:bg-primary-fixed-dim rounded-sm">
            Cập nhật mật khẩu
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
