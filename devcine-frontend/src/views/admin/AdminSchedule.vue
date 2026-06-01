<script setup>
import { ref, computed } from 'vue'

const selectedDate = ref('20/10')
const selectedShow = ref(null)
const isSidebarOpen = ref(false)

const openSidebar = (show) => {
  selectedShow.value = show
  isSidebarOpen.value = true
}

const closeSidebar = () => {
  isSidebarOpen.value = false
  setTimeout(() => {
    selectedShow.value = null
  }, 300)
}

// Trạng thái AI Auto-Scheduling
const isAIModalOpen = ref(false)
const isAIGenerating = ref(false)
const aiProgress = ref(0)
const aiConfig = ref({ optimizeFor: 'revenue', includePrimeTime: true })

const runAIAlgorithm = () => {
  isAIGenerating.value = true
  aiProgress.value = 0
  
  // Giả lập tiến trình quét
  const timer = setInterval(() => {
    aiProgress.value += 15
    if (aiProgress.value >= 100) {
      clearInterval(timer)
      applyGeneratedSchedule()
    }
  }, 250)
}

const applyGeneratedSchedule = () => {
  // Sinh dữ liệu lịch chiếu giả lập thông minh (xóa lịch cũ)
  const movies = [
    { title: 'OPPENHEIMER', duration: 180, format: 'IMAX 2D', color: '#4A0E0E' },
    { title: 'AVATAR 2', duration: 195, format: '3D Atmos', color: '#0F2027' },
    { title: 'JOHN WICK 4', duration: 165, format: '2D Phụ đề', color: '#1A1A2E' },
    { title: 'DORAEMON', duration: 90, format: '2D Lồng tiếng', color: '#0E3A2F' }
  ]
  
  const newShowtimes = []
  let idCounter = 1
  
  rooms.value.forEach((room) => {
    let currentHour = 8 // Bắt đầu từ 8h sáng
    let currentMin = 0
    
    while (currentHour < 22) { // Xếp kín đến 22h tối
      const movie = movies[Math.floor(Math.random() * movies.length)]
      
      const startTime = `${currentHour.toString().padStart(2, '0')}:${currentMin.toString().padStart(2, '0')}`
      
      // Tính giờ kết thúc
      let endTotalMins = currentHour * 60 + currentMin + movie.duration
      // Nghỉ 30 phút dọn rạp
      endTotalMins += 30
      
      newShowtimes.push({
        id: idCounter++,
        roomId: room.id,
        movie: movie.title,
        format: movie.format,
        startTime: startTime,
        duration: movie.duration,
        color: movie.color,
        status: (currentHour < 12) ? 'past' : (currentHour === 12 ? 'ongoing' : 'future') // Mô phỏng trạng thái
      })
      
      currentHour = Math.floor(endTotalMins / 60)
      currentMin = endTotalMins % 60
    }
  })
  
  showtimes.value = newShowtimes
  isAIGenerating.value = false
  isAIModalOpen.value = false
}

const rooms = ref([
  { id: 1, name: 'Phòng IMAX 01', type: 'Premium' },
  { id: 2, name: 'Phòng P02', type: 'Standard' },
  { id: 3, name: 'Phòng P03', type: 'Standard' },
  { id: 4, name: 'Phòng GOLD CLASS', type: 'Luxury' },
])

const showtimes = ref([
  { id: 1, roomId: 1, movie: 'OPPENHEIMER', format: 'IMAX 2D', startTime: '09:30', duration: 180, color: '#4A0E0E', status: 'past' },
  { id: 2, roomId: 1, movie: 'DORAEMON', format: '2D Lồng tiếng', startTime: '13:00', duration: 90, color: '#0E3A2F', status: 'ongoing' },
  { id: 3, roomId: 2, movie: 'JOHN WICK 4', format: '2D Phụ đề', startTime: '10:15', duration: 165, color: '#1A1A2E', status: 'future' },
  { id: 4, roomId: 3, movie: 'AVATAR 2', format: '3D Atmos', startTime: '11:45', duration: 195, color: '#0F2027', status: 'future' },
  { id: 5, roomId: 1, movie: 'THE SILENT WITNESS', format: 'IMAX 2D', startTime: '16:00', duration: 120, color: '#2C0E37', status: 'future' },
])

// Hàm tính toán Grid Column Start và Span (mỗi unit = 15 phút)
const getGridStyle = (startTime, duration) => {
  const [hour, minute] = startTime.split(':').map(Number)
  const startUnit = (hour - 8) * 4 + (Math.floor(minute / 15)) + 1
  const spanUnit = Math.ceil(duration / 15)
  return {
    gridColumnStart: startUnit,
    gridColumnEnd: `span ${spanUnit}`
  }
}

const dates = [
  { day: 'Thứ 2', date: '19/10' },
  { day: 'Thứ 3', date: '20/10' },
  { day: 'Thứ 4', date: '21/10' },
  { day: 'Thứ 5', date: '22/10' },
  { day: 'Thứ 6', date: '23/10' },
]

// Hàm tính giờ kết thúc
const getEndTime = (startTime, duration) => {
  const [hour, minute] = startTime.split(':').map(Number)
  const totalMinutes = hour * 60 + minute + duration
  const endHour = Math.floor(totalMinutes / 60)
  const endMin = totalMinutes % 60
  return `${endHour.toString().padStart(2, '0')}:${endMin.toString().padStart(2, '0')}`
}
</script>

<template>
  <div class="p-8 flex flex-col h-screen overflow-hidden bg-surface-container-lowest text-on-surface select-none">
    <!-- Header: Date Picker -->
    <header class="flex justify-between items-center mb-8 flex-shrink-0">
      <div class="flex items-center gap-4">
        <button v-for="d in dates" :key="d.date" 
          @click="selectedDate = d.date"
          :class="selectedDate === d.date ? 'bg-primary text-on-primary border-primary' : 'bg-surface-container-high text-on-surface-variant border-outline-variant/10'"
          class="flex flex-col items-center min-w-[70px] py-2 rounded border transition-all hover:bg-white/5">
          <span class="text-[9px] font-bold uppercase opacity-50">{{ d.day }}</span>
          <span class="text-xs font-black">{{ d.date }}</span>
        </button>
      </div>

      <button @click="isAIModalOpen = true" class="px-6 py-3 bg-primary text-on-primary font-black text-[10px] uppercase tracking-[0.2em] rounded-sm shadow-xl shadow-primary/10 flex items-center gap-2 italic hover:brightness-110 transition-all">
         <span class="material-symbols-outlined text-sm">bolt</span> Auto-Scheduling AI
      </button>
    </header>

    <!-- Main Timeline Matrix -->
    <div class="flex-grow flex flex-col bg-surface-container-low border border-outline-variant/10 rounded-lg overflow-hidden relative shadow-2xl">
      
      <!-- Scrollable Area -->
      <div class="flex-grow overflow-x-auto overflow-y-auto scrollbar-hide relative">
        
        <!-- Main Wrapper ensuring 2000px width for the grid -->
        <div class="min-w-[2000px] flex flex-col min-h-full relative">
          
          <!-- Sticky Time Ruler (Top) -->
          <div class="flex border-b border-outline-variant/10 bg-surface-container-low flex-shrink-0 sticky top-0 z-40 shadow-[0_10px_15px_-3px_var(--shadow-color)]">
            <!-- Sticky Top-Left Corner -->
            <div class="w-60 flex-shrink-0 p-4 border-r border-outline-variant/10 flex items-center justify-center font-black text-primary uppercase tracking-[0.3em] text-[9px] italic bg-surface-container-low sticky left-0 z-50 shadow-[10px_0_15px_-5px_var(--shadow-color)]">
               Studio \ Timeline
            </div>
            <!-- Time Markers -->
            <div class="flex-grow grid grid-cols-[repeat(64,minmax(0,1fr))] relative h-12">
               <div v-for="hour in 16" :key="hour" class="col-span-4 border-r border-outline-variant/10 flex items-center justify-center text-[10px] font-black text-on-surface-variant/40">
                  {{ (hour + 7).toString().padStart(2, '0') }}:00
               </div>
            </div>
          </div>

          <!-- Vertical Grid Lines (Background) -->
          <div class="absolute inset-0 top-12 grid grid-cols-[repeat(64,minmax(0,1fr))] pointer-events-none pl-60 z-0">
             <div v-for="i in 64" :key="i" :class="i % 4 === 0 ? 'border-r border-outline-variant/30' : 'border-r border-outline-variant/10'" class="h-full"></div>
          </div>

          <!-- Rows Container -->
          <div class="flex-grow relative z-10 flex flex-col">
          <!-- Room Rows -->
          <div v-for="room in rooms" :key="room.id" class="flex items-center flex-1 border-b border-outline-variant/20 group hover:bg-white/[0.01] transition-all min-h-[110px] relative">
            
            <!-- Room Labels (Sticky Column) -->
            <div class="w-60 flex-shrink-0 p-6 border-r border-outline-variant/10 flex items-center gap-4 bg-surface-container-low sticky left-0 z-20 shadow-[10px_0_15px_-5px_var(--shadow-color)] self-stretch">
              <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 border border-primary/20">
                 <span class="material-symbols-outlined text-primary text-xl">screenshot_monitor</span>
              </div>
              <div class="flex flex-col text-left overflow-hidden">
                <h3 class="text-sm font-black uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors italic truncate">{{ room.name }}</h3>
                <p class="text-[9px] font-bold text-on-surface-variant/60 uppercase tracking-widest mt-0.5 italic flex items-center gap-1 whitespace-nowrap">
                   {{ room.type }} <span class="opacity-30">•</span> 120 Ghế
                </p>
              </div>
            </div>

            <!-- Showtime Container -->
            <div class="flex-grow grid grid-cols-[repeat(64,minmax(0,1fr))] grid-rows-1 gap-x-0 relative p-0 items-center min-w-[1800px]">
             <div v-for="show in showtimes.filter(s => s.roomId === room.id)" :key="show.id" 
                @click="openSidebar(show)"
                :style="{ ...getGridStyle(show.startTime, show.duration), backgroundColor: show.color, borderColor: 'rgba(255,255,255,0.1)' }"
                class="relative h-[76px] mx-0.5 border rounded-lg px-3 pb-2 pt-2.5 cursor-pointer group/card transition-all duration-300 hover:z-30 hover:scale-[1.03] hover:ring-1 hover:ring-white/50 hover:brightness-110 shadow-2xl overflow-hidden flex flex-col justify-between">
                
                <!-- Card Tier 1: Title & Format -->
                <div class="flex justify-between items-start">
                   <h4 class="text-[10px] font-black text-white leading-tight uppercase truncate pr-2 tracking-tight">{{ show.movie }}</h4>
                   <div class="px-1.5 py-0.5 bg-on-surface/20 rounded-sm text-[7px] font-bold text-primary border border-primary/20 whitespace-nowrap shadow-lg">
                      {{ show.format }}
                   </div>
                </div>

                <!-- Card Tier 2: Time & Icon -->
                <div class="flex flex-col gap-1.5">
                   <div class="flex items-center gap-1.5 text-[8px] font-bold text-white/70 uppercase">
                       <span>{{ show.startTime }} - {{ getEndTime(show.startTime, show.duration) }}</span>
                       <span class="ml-auto opacity-40 italic">{{ show.duration }}m</span>
                    </div>

                   <!-- Progress Bar (Visual Flair) -->
                   <div class="relative h-[3px] w-full bg-black/40 rounded-full overflow-hidden">
                      <div :class="show.status === 'ongoing' ? 'bg-primary shadow-[0_0_8px_#F5C518]' : 'bg-gray-500/50'" 
                           :style="{ width: show.status === 'past' ? '100%' : (show.status === 'ongoing' ? '45%' : '0%') }"
                           class="h-full transition-all duration-500">
                      </div>
                   </div>
                </div>

                <!-- Glass Reflection Effect -->
                <div class="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent pointer-events-none"></div>
             </div>
          </div>
          </div> <!-- End Room Rows -->
          </div> <!-- End Rows Container -->
        </div> <!-- End Main Wrapper -->
      </div> <!-- End Scrollable Area -->
    </div> <!-- End Main Timeline Matrix -->

    <!-- Legend -->
    <footer class="mt-4 flex items-center gap-10 px-6 py-3 bg-surface-container-high/20 rounded border border-outline-variant/10">
       <div class="flex items-center gap-3">
          <div class="w-2 h-2 rounded-full bg-primary shadow-[0_0_8px_#F5C518]"></div>
          <span class="text-[9px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">Suất đang chiếu</span>
       </div>
       <div class="flex items-center gap-3">
          <div class="w-2 h-2 rounded-full bg-gray-600"></div>
          <span class="text-[9px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">Suất sắp chiếu</span>
       </div>
       <div class="ml-auto flex items-center gap-2 opacity-30">
          <span class="text-[8px] font-bold uppercase tracking-widest italic">DevCine Management System v2.1</span>
       </div>
    </footer>

    <!-- Overlay -->
    <div v-if="isSidebarOpen" @click="closeSidebar" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity"></div>

    <!-- Right Sidebar (Show Details) -->
    <div :class="isSidebarOpen ? 'translate-x-0' : 'translate-x-full'" class="fixed right-0 top-0 h-screen w-[400px] bg-surface-container-low border-l border-outline-variant/20 z-50 transition-transform duration-300 shadow-2xl flex flex-col">
      <!-- Header -->
      <div class="h-16 border-b border-outline-variant/10 flex items-center justify-between px-6 bg-surface-container-high/50 flex-shrink-0">
        <h3 class="text-sm font-black uppercase tracking-widest text-on-surface">Chi tiết Suất chiếu</h3>
        <button @click="closeSidebar" class="material-symbols-outlined text-on-surface-variant hover:text-on-surface transition-colors">close</button>
      </div>

      <!-- Body -->
      <div class="flex-grow overflow-y-auto p-6" v-if="selectedShow">
        <!-- Poster / Visual -->
        <div class="h-48 rounded-xl w-full mb-6 relative overflow-hidden flex items-end p-4 border border-outline-variant/10 shadow-inner" :style="{ backgroundColor: selectedShow.color }">
           <div class="absolute inset-0 bg-gradient-to-t from-black/90 to-transparent"></div>
           <div class="relative z-10 w-full">
             <div class="px-2 py-0.5 bg-primary/20 rounded text-[9px] font-bold text-primary border border-primary/30 inline-block mb-2">{{ selectedShow.format }}</div>
             <h2 class="text-2xl font-black text-white uppercase tracking-tighter leading-none">{{ selectedShow.movie }}</h2>
           </div>
        </div>

        <!-- Info Grid -->
        <div class="space-y-6">
           <div>
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-2">Thời gian chiếu</p>
              <div class="flex items-center gap-4 bg-on-surface/5 p-4 rounded-lg border border-outline-variant/5">
                 <div class="text-center">
                    <p class="text-[10px] text-on-surface-variant">Bắt đầu</p>
                    <p class="text-lg font-black text-on-surface">{{ selectedShow.startTime }}</p>
                 </div>
                 <div class="flex-grow flex items-center justify-center relative">
                    <div class="w-full h-[1px] bg-outline-variant/20"></div>
                    <span class="absolute px-2 bg-surface-container-low text-[10px] font-bold italic text-primary">{{ selectedShow.duration }} phút</span>
                 </div>
                 <div class="text-center">
                    <p class="text-[10px] text-on-surface-variant">Kết thúc</p>
                    <p class="text-lg font-black text-on-surface">{{ getEndTime(selectedShow.startTime, selectedShow.duration) }}</p>
                 </div>
              </div>
           </div>

           <div>
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phòng chiếu</p>
              <div class="flex items-center gap-3 p-4 rounded-lg border border-outline-variant/5 bg-on-surface/5">
                 <div class="w-10 h-10 rounded bg-white/5 flex items-center justify-center">
                    <span class="material-symbols-outlined text-on-surface">meeting_room</span>
                 </div>
                 <div>
                    <p class="text-sm font-black text-on-surface uppercase">{{ rooms.find(r => r.id === selectedShow.roomId)?.name || 'Phòng chiếu' }}</p>
                    <p class="text-[10px] text-on-surface-variant">{{ rooms.find(r => r.id === selectedShow.roomId)?.type || 'Standard' }} • 120 Ghế</p>
                 </div>
              </div>
           </div>

           <!-- Trạng thái vé (Mock) -->
           <div>
              <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-2">Tình trạng vé</p>
              <div class="p-4 rounded-lg border border-outline-variant/5 bg-on-surface/5">
                 <div class="flex justify-between items-end mb-2">
                    <p class="text-sm font-bold text-on-surface">85 / 120 <span class="text-[10px] text-on-surface-variant font-normal">vé đã bán</span></p>
                    <p class="text-xs font-bold text-green-400">70%</p>
                 </div>
                 <div class="w-full h-1.5 bg-black rounded-full overflow-hidden">
                    <div class="h-full bg-green-500 rounded-full" style="width: 70%"></div>
                 </div>
              </div>
           </div>
        </div>
      </div>

      <!-- Footer Actions -->
      <div class="p-6 border-t border-outline-variant/10 bg-surface-container-high/50 flex gap-3">
         <button class="flex-1 py-3 bg-surface-container-highest text-on-surface font-bold text-[10px] uppercase tracking-widest rounded hover:bg-white/10 transition-colors">Chỉnh sửa</button>
         <button class="flex-1 py-3 bg-primary text-on-primary font-bold text-[10px] uppercase tracking-widest rounded hover:brightness-110 shadow-xl shadow-primary/10 transition-colors">Sơ đồ ghế</button>
      </div>
    </div>

    <!-- AI Auto-Scheduling Modal -->
    <div v-if="isAIModalOpen" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[60] flex items-center justify-center p-4 transition-opacity">
      <div class="bg-surface-container-low border border-outline-variant/20 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden flex flex-col relative">
        <!-- Close Button -->
        <button v-if="!isAIGenerating" @click="isAIModalOpen = false" class="absolute top-6 right-6 material-symbols-outlined text-on-surface-variant hover:text-white transition-colors">close</button>
        
        <div class="p-10 pb-6 border-b border-outline-variant/10 bg-gradient-to-br from-primary/10 to-transparent">
          <h2 class="text-2xl font-black uppercase tracking-tight text-on-surface italic">Lumière <span class="text-primary">AI</span> Scheduler</h2>
          <p class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mt-2">Hệ thống tự động xếp lịch và tối ưu hóa phòng chiếu</p>
        </div>

        <div class="p-10 space-y-8" v-if="!isAIGenerating">
          <div class="space-y-4">
             <label class="block text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chiến lược phân bổ</label>
             <select v-model="aiConfig.optimizeFor" class="w-full bg-surface-container-high border border-outline-variant/10 rounded-lg px-4 py-3 text-sm text-on-surface outline-none">
                <option value="revenue">Tối đa hóa Doanh Thu (Ưu tiên Bom tấn)</option>
                <option value="occupancy">Tối đa hóa Tỷ Lệ Lấp Đầy (Đa dạng thể loại)</option>
             </select>
          </div>

          <div class="space-y-4">
             <div class="flex items-center justify-between p-4 bg-surface-container-high/50 rounded-lg border border-outline-variant/5">
                <div>
                   <p class="text-xs font-bold text-on-surface uppercase tracking-tight">Khai thác triệt để Giờ Vàng</p>
                   <p class="text-[9px] text-on-surface-variant mt-1">Ép AI chèn các phim IMAX và 3D vào khung 18:00 - 21:00</p>
                </div>
                <button @click="aiConfig.includePrimeTime = !aiConfig.includePrimeTime" :class="aiConfig.includePrimeTime ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="w-12 h-6 rounded-full relative transition-all">
                   <div :class="aiConfig.includePrimeTime ? 'translate-x-6' : 'translate-x-1'" class="absolute top-1 w-4 h-4 bg-white rounded-full transition-transform"></div>
                </button>
             </div>
          </div>
          
          <div class="p-4 bg-primary/5 rounded border border-primary/20 flex gap-3">
             <span class="material-symbols-outlined text-primary text-lg">info</span>
             <p class="text-[9px] text-primary/80 uppercase tracking-widest leading-relaxed">Khi chạy thuật toán, toàn bộ lịch chiếu trống trên lưới hiện tại sẽ bị xóa và thay thế bằng lưới tối ưu mới do AI tạo ra.</p>
          </div>

          <button @click="runAIAlgorithm" class="w-full py-4 bg-primary text-on-primary font-black text-xs uppercase tracking-[0.2em] rounded-lg shadow-[0_0_20px_rgba(245,197,24,0.3)] hover:brightness-110 transition-all flex justify-center items-center gap-2">
             <span class="material-symbols-outlined text-sm">smart_toy</span> Chạy Mô phỏng Lịch chiếu
          </button>
        </div>

        <div class="p-16 flex flex-col items-center justify-center text-center space-y-8" v-else>
           <span class="material-symbols-outlined text-6xl text-primary animate-spin">sync</span>
           <div class="w-full space-y-4">
              <h3 class="text-sm font-black text-on-surface uppercase tracking-widest">Đang tính toán ma trận...</h3>
              <div class="w-full h-2 bg-surface-container-highest rounded-full overflow-hidden">
                 <div class="h-full bg-primary transition-all duration-300" :style="{ width: aiProgress + '%' }"></div>
              </div>
              <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-widest">{{ aiProgress }}% Hoàn tất</p>
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar { display: none; }
</style>
