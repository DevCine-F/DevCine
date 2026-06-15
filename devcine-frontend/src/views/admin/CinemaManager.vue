<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from "vue";
import axios from "axios";
import SeatMapBuilder from "@/components/admin/SeatMapBuilder.vue";
import ShowtimeDrawer from "@/components/admin/ShowtimeDrawer.vue";

const API_BASE_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080") + "/api/v1/cinemas";

const selectedCinema = ref(null);
const activeTab = ref("infrastructure");
const viewingHall = ref(null);

// Drawer state
const selectedShowtime = ref(null);
const showDrawer = ref(false);

// Add Showtime Drawer State
const showAddShowtimeDrawer = ref(false);

const openShowtimeDetails = (show) => {
  selectedShowtime.value = show;
  showDrawer.value = true;
};

const closeDrawer = () => {
  showDrawer.value = false;
  setTimeout(() => {
    selectedShowtime.value = null;
  }, 300);
};

// Seat Map Modal State for Drawer
const showSeatMapModal = ref(false);

// Scheduling State
const showCleaningSettingsModal = ref(false);
const tempCleaningTime = ref(20);
const draggedShow = ref(null);

const onDragStart = (event, show) => {
  draggedShow.value = show;
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
};

const onDrop = (event, hallId) => {
  if (!draggedShow.value) return;
  const show = draggedShow.value;
  
  const rect = event.currentTarget.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const colWidth = rect.width / 64;
  const colIndex = Math.floor(x / colWidth);
  
  const totalMinutes = colIndex * 15;
  const newHour = Math.floor(totalMinutes / 60) + 8;
  const newMinute = totalMinutes % 60;
  const newStartTime = `${newHour.toString().padStart(2, '0')}:${newMinute.toString().padStart(2, '0')}`;
  
  const originalRoomId = show.roomId;
  const originalStartTime = show.startTime;
  
  show.roomId = hallId;
  show.startTime = newStartTime;
  show.isDirty = true;
  
  if (checkConflict(hallId, show)) {
    show.roomId = originalRoomId;
    show.startTime = originalStartTime;
    alert("Xung đột lịch chiếu hoặc đè lên thời gian dọn dẹp. Vui lòng chọn giờ/phòng khác!");
  }
  
  draggedShow.value = null;
};

const handlePublish = async () => {
  if (!selectedCinema.value) return;
  const dirtyShows = selectedCinema.value.shows.filter(s => s.isDirty);
  if (dirtyShows.length === 0) {
    alert("Không có lịch chiếu nào thay đổi để xuất bản!");
    return;
  }
  
  try {
    for (const show of dirtyShows) {
      const [newHour, newMin] = show.startTime.split(':').map(Number);
      
      let baseDate = new Date(show.fullDateTime);
      // setHours will correctly advance the day if newHour >= 24
      baseDate.setHours(newHour);
      baseDate.setMinutes(newMin);
      
      const year = baseDate.getFullYear();
      const month = String(baseDate.getMonth() + 1).padStart(2, '0');
      const date = String(baseDate.getDate()).padStart(2, '0');
      const hour = String(baseDate.getHours()).padStart(2, '0');
      const minute = String(baseDate.getMinutes()).padStart(2, '0');
      const localIsoString = `${year}-${month}-${date}T${hour}:${minute}:00`;
      
      await axios.patch(`http://localhost:8080/api/showtimes/${show.id}`, {
        roomId: show.roomId,
        startTime: localIsoString
      });
      
      show.isDirty = false;
      show.fullDateTime = localIsoString;
    }
    alert("Lịch chiếu đã được xuất bản thành công vào Database!");
  } catch (err) {
    console.error(err);
    alert("Có lỗi xảy ra khi lưu lịch chiếu!");
  }
};

// Helper function for mocking sold tickets based on movie
const getSoldTickets = (movieName) => {
  if (!movieName) return 0;
  return movieName.includes('DORAEMON') ? 45 : 112;
};

// Modal Create Cinema State
const showCreateModal = ref(false);
const newCinema = reactive({
  name: '',
  address: '',
  hotline: '',
  type: 'Standard',
  rooms: 1
});

const handleCreateCinema = async () => {
  if (!newCinema.name || !newCinema.address || !newCinema.rooms) {
    alert("Vui lòng điền đầy đủ các thông tin bắt buộc!");
    return;
  }
  
  try {
    const res = await axios.post(API_BASE_URL, {
      name: newCinema.name,
      address: newCinema.address,
      type: newCinema.type,
      hotline: newCinema.hotline,
      rooms: newCinema.rooms,
      city: "Hồ Chí Minh" // Default for now
    });
    
    // Add missing mock properties for UI since BE doesn't have them yet
    const savedCinema = {
      ...res.data,
      stats: {
        revenue: "0đ",
        occupancy: "0%",
        growth: "+0%",
      },
      halls: Array.from({ length: newCinema.rooms }, (_, i) => ({
        id: `New_H${i+1}`,
        name: `Phòng ${i+1}`,
        type: "2D Standard",
        rows: 10,
        cols: 12,
        status: "Active"
      })),
      staff: [],
      inventory: [],
      shows: []
    };

    cinemas.value.push(savedCinema);
    
    // Reset form
    newCinema.name = '';
    newCinema.address = '';
    newCinema.hotline = '';
    newCinema.type = 'Standard';
    newCinema.rooms = 1;
    showCreateModal.value = false;
  } catch (error) {
    console.error("Error creating cinema:", error);
    alert("Lỗi khi thêm cụm rạp mới!");
  }
};

const cinemas = ref([]);


const fetchCinemas = async () => {
  try {
    const res = await axios.get(API_BASE_URL);
    if (res.data && res.data.length > 0) {
      const cinemaList = res.data;
      const enrichedCinemas = await Promise.all(cinemaList.map(async (c) => {
        // Fetch rooms for this cinema
        let halls = [];
        try {
            const roomsRes = await axios.get(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/rooms/cinema/${c.id}`);
            halls = roomsRes.data.map(r => ({
                id: r.id,
                name: r.name,
                type: r.type,
                rows: 10, // Mocked for now since DB doesn't have rows/cols
                cols: 16,
                status: r.status
            }));
        } catch(e) { console.error(e); }
        
        // Fetch shows for this cinema
        let shows = [];
        try {
            const showsRes = await axios.get(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/showtimes/cinema/${c.id}`);
            shows = showsRes.data.map(s => {
                let st = s.startTime;
                let startTimeStr = "00:00";
                let fullDateTimeStr = "";
                let dateStr = "";
                if (Array.isArray(st)) {
                    // [year, month, day, hour, minute]
                    startTimeStr = `${st[3].toString().padStart(2, '0')}:${st[4].toString().padStart(2, '0')}`;
                    dateStr = `${st[2].toString().padStart(2, '0')}/${st[1].toString().padStart(2, '0')}`;
                    fullDateTimeStr = `${st[0]}-${st[1].toString().padStart(2, '0')}-${st[2].toString().padStart(2, '0')}T${startTimeStr}:00`;
                } else if (typeof st === 'string') {
                    // "2026-06-11T13:00:00" -> "13:00"
                    startTimeStr = st.substring(11, 16);
                    const parts = st.split('T')[0].split('-');
                    dateStr = `${parts[2]}/${parts[1]}`;
                    fullDateTimeStr = st;
                }

                return {
                    id: s.id,
                    roomId: s.roomId,
                    movie: s.movie,
                    format: s.formatName,
                    startTime: startTimeStr,
                    date: dateStr,
                    duration: s.duration || 120,
                    status: s.status,
                    price: 120000, // Default mock price
                    color: "#f5c518", // Default mock color (yellow)
                    fullDateTime: fullDateTimeStr,
                    isDirty: false
                };
            });
        } catch(e) { console.error(e); }

        return {
          ...c,
          cleaningTime: 15,
          stats: {
            revenue: (Math.random() * 500 + 300).toFixed(0) + ".000.000đ",
            occupancy: (Math.random() * 20 + 70).toFixed(0) + "%",
            growth: "+" + (Math.random() * 15).toFixed(1) + "%",
          },
          halls: halls,
          shows: shows,
          staff: [],
          inventory: []
        };
      }));
      cinemas.value = enrichedCinemas;
      
      // If a cinema is currently selected, update it
      if (selectedCinema.value) {
          selectedCinema.value = enrichedCinemas.find(c => c.id === selectedCinema.value.id) || null;
      }
    }
  } catch (error) {
    console.error("Error fetching cinemas:", error);
  }
};

const currentMinuteOffset = ref(0);
const currentTimeLeft = computed(() => {
  const totalMinutes = 18 * 60; // 18 hours from 8:00 to 02:00
  const offset = Math.max(0, Math.min(currentMinuteOffset.value, totalMinutes));
  return `${(offset / totalMinutes) * 100}%`;
});

const updateCurrentTime = () => {
  const now = new Date();
  const hour = now.getHours();
  const minute = now.getMinutes();
  currentMinuteOffset.value = (hour - 8) * 60 + minute;
};

let timeInterval;
onMounted(async () => {
  await fetchCinemas();
  updateCurrentTime();
  timeInterval = setInterval(updateCurrentTime, 60000);
});

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval);
});

const getEndTime = (startTime, duration = 120) => {
  const [hour, minute] = startTime.split(":").map(Number);
  const totalMinutes = hour * 60 + minute + duration;
  const endHourRaw = Math.floor(totalMinutes / 60);
  const endMin = totalMinutes % 60;
  const endHour = endHourRaw >= 24 ? endHourRaw - 24 : endHourRaw;
  return `${endHour.toString().padStart(2, "0")}:${endMin.toString().padStart(2, "0")}`;
};

// Seat Map State
const currentSeatMap = ref({});
const isMouseDown = ref(false);
const activeBrush = ref("standard");
const tempRows = ref(10);
const tempCols = ref(16);

// Schedule State
const generateDates = () => {
  const today = new Date();
  return Array.from({ length: 5 }, (_, i) => {
    const d = new Date(today);
    d.setDate(today.getDate() + i - 1);
    const dayNames = ['CN', '2', '3', '4', '5', '6', '7'];
    return {
      day: i === 1 ? 'Hôm nay' : `Thứ ${dayNames[d.getDay()]}`,
      date: `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}`,
      isToday: i === 1
    };
  });
};

const dates = generateDates();
const selectedDate = ref(dates[1].date);
const isToday = computed(() => selectedDate.value === dates[1].date);

const getGridStyle = (startTime, duration) => {
  const [hour, minute] = startTime.split(":").map(Number);
  const startUnit = (hour - 8) * 4 + Math.floor(minute / 15) + 1;
  const spanUnit = Math.ceil(duration / 15);
  return {
    gridColumnStart: startUnit,
    gridColumnEnd: `span ${spanUnit}`,
    gridRow: '1',
  };
};

// Logic Advanced Constraints
const checkConflict = (hallId, show) => {
  if (!selectedCinema.value) return false;
  const hallShows = selectedCinema.value.shows.filter(
    (s) => s.roomId === hallId && s.date === selectedDate.value && s.id !== show.id,
  );
  const CLEANING_TIME = selectedCinema.value.cleaningTime || 20;

  const showStart = timeToMinutes(show.startTime);
  const showEnd = showStart + show.duration + CLEANING_TIME;

  return hallShows.some((other) => {
    const otherStart = timeToMinutes(other.startTime);
    const otherEnd = otherStart + other.duration + CLEANING_TIME;
    return showStart < otherEnd && showEnd > otherStart;
  });
};

const checkFormatMismatch = (hall, format) => {
  if (format.includes("IMAX") && !hall.type.includes("IMAX")) return true;
  return false;
};

const timeToMinutes = (time) => {
  const [h, m] = time.split(":").map(Number);
  return h * 60 + m;
};

const seatBrushes = [
  {
    id: "standard",
    label: "Tiêu chuẩn",
    icon: "event_seat",
    color: "bg-slate-500/40",
  },
  { id: "vip", label: "Premium VIP", icon: "stars", color: "bg-[#f5c518]" },
  { id: "sweetbox", label: "Sweetbox", icon: "favorite", color: "bg-red-500" },
  { id: "aisle", label: "Lối đi", icon: "space_bar", color: "bg-white/10" },
  {
    id: "broken",
    label: "Ghế hỏng",
    icon: "heart_broken",
    color: "bg-white/5",
  },
];

const openCinemaDetail = (cinema) => {
  selectedCinema.value = cinema;
  activeTab.value = "infrastructure";
};

const closeDetail = () => {
  selectedCinema.value = null;
  viewingHall.value = null;
};

const openHallDetail = async (hall) => {
  tempRows.value = hall.rows || 10;
  tempCols.value = hall.cols || 16;
  
  try {
    const res = await axios.get(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/seats/room/${hall.id}?t=${Date.now()}`);
    if (res.data && res.data.seats && res.data.seats.length > 0) {
      tempRows.value = res.data.matrixRow || hall.rows;
      tempCols.value = res.data.matrixCol || hall.cols;
      
      const map = {};
      
      // Override with actual fetched seats
      res.data.seats.forEach(seat => {
        let seatType = "standard";
        if (seat.seatType === "VIP") seatType = "vip";
        if (seat.seatType === "SWEETBOX") seatType = "double";
        if (seat.seatType === "PAVE") seatType = "aisle"; // Assuming we map pave to aisle

        map[`${seat.gridRow}-${seat.gridCol}`] = {
          type: seatType,
          label: `${seat.rowChar}${seat.colNum}`
        };
      });
      currentSeatMap.value = map;
      viewingHall.value = hall;
      return;
    }
  } catch (error) {
    console.error("Failed to load saved seat layout, initializing default:", error);
  }
  
  initializeSeatMap(hall);
  viewingHall.value = hall;
};

const initializeSeatMap = (hall) => {
  const map = {};
  for (let r = 0; r < tempRows.value; r++) {
    for (let c = 0; c < tempCols.value; c++) {
      map[`${r}-${c}`] = {
        type: "standard",
        label: `${String.fromCharCode(65 + r)}${c + 1}`,
      };
    }
  }
  currentSeatMap.value = map;
};

const regenerateGrid = () => {
  if (!viewingHall.value) return;
  viewingHall.value.rows = tempRows.value;
  viewingHall.value.cols = tempCols.value;
  initializeSeatMap(viewingHall.value);
};

const resetMap = () => {
  if (!viewingHall.value) return;
  initializeSeatMap(viewingHall.value);
};

const isSavingLayout = ref(false);

const saveSeatLayout = async () => {
  if (!viewingHall.value) return;
  
  isSavingLayout.value = true;
  try {
    const seatsList = [];
    Object.entries(currentSeatMap.value).forEach(([key, seatData]) => {
      if (seatData.type && seatData.type !== 'hidden' && seatData.type !== 'aisle') {
        const [gridRowStr, gridColStr] = key.split('-');
        const gridRow = parseInt(gridRowStr);
        const gridCol = parseInt(gridColStr);
        
        let rowChar = '';
        let colNum = 1;
        
        if (seatData.label) {
          // Parse label like "A1", "A10"
          const match = seatData.label.match(/^([A-Z]+)(\d+)$/);
          if (match) {
            rowChar = match[1];
            colNum = parseInt(match[2]);
          }
        }
        
        seatsList.push({
          rowChar,
          colNum,
          gridRow,
          gridCol,
          type: seatData.type,
          label: seatData.label
        });
      }
    });

    const payload = {
      matrixRow: tempRows.value,
      matrixCol: tempCols.value,
      seats: seatsList
    };

    await axios.post(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/seats/layout/${viewingHall.value.id}`, payload);
    alert('Lưu cấu trúc ghế thành công!');
  } catch (error) {
    console.error('Error saving seat layout:', error);
    alert('Có lỗi xảy ra khi lưu cấu trúc ghế.');
  } finally {
    isSavingLayout.value = false;
  }
};

const toggleSeat = (r, c) => {
  const key = `${r}-${c}`;
  const nextKey = `${r}-${c + 1}`;
  const prevKey = `${r}-${c - 1}`;

  // Handle Sweetbox logic (Double Seat)
  if (activeBrush.value === "sweetbox") {
    if (c < viewingHall.value.cols - 1) {
      currentSeatMap.value[key].type = "sweetbox";
      currentSeatMap.value[nextKey].type = "hidden";
    }
    return;
  }

  // Restore if changing from a sweetbox
  if (currentSeatMap.value[key]?.type === "sweetbox") {
    if (currentSeatMap.value[nextKey])
      currentSeatMap.value[nextKey].type = "standard";
  }

  // If clicking the hidden part of a sweetbox, restore both
  if (currentSeatMap.value[key]?.type === "hidden") {
    if (currentSeatMap.value[prevKey]) {
      currentSeatMap.value[prevKey].type = activeBrush.value;
      currentSeatMap.value[key].type = "standard";
      return;
    }
  }

  if (currentSeatMap.value[key]) {
    currentSeatMap.value[key].type = activeBrush.value;
  }
};

const tabs = [
  { id: "infrastructure", label: "Cơ sở vật chất", icon: "domain" },
  { id: "showtimes", label: "Lịch chiếu", icon: "schedule" },
  { id: "staff", label: "Nhân sự", icon: "badge" },
  { id: "fnb", label: "Dịch vụ Bắp nước", icon: "fastfood" },
  { id: "analytics", label: "Báo cáo", icon: "analytics" },
  { id: "config", label: "Cấu hình", icon: "settings" },
];

// ===================== CONFIG TAB STATE =====================
// Accordion open/close state (section 1 mở mặc định)
const openSections = reactive({
  basic: true,
  hours: false,
  cleaning: false,
  seats: false,
  formats: false,
  banners: false,
});

const toggleSection = (key) => {
  openSections[key] = !openSections[key];
};

const configSaving = reactive({
  basic: false,
  hours: false,
  cleaning: false,
  seats: false,
  formats: false,
  banners: false,
});

const configSuccess = reactive({
  basic: false,
  hours: false,
  cleaning: false,
  seats: false,
  formats: false,
  banners: false,
});

// Basic Info
const configBasic = reactive({
  name: "",
  address: "",
  phone: "",
  email: "",
  openTime: "08:00",
});

// Operating Hours
const configHours = reactive({
  openTime: "08:00",
  closeTime: "23:30",
  holidays: "",
});

// Cleaning time
const configCleaning = reactive({
  cleaningMinutes: 20,
});

// Seat types & default prices
const configSeats = reactive({
  types: [
    { id: 1, name: "Thường", color: "#6B7280", defaultPrice: 75000 },
    { id: 2, name: "VIP", color: "#F5C518", defaultPrice: 120000 },
    { id: 3, name: "Đôi", color: "#EC4899", defaultPrice: 180000 },
  ],
});

// Formats
const allFormats = ["2D", "3D", "IMAX", "4DX", "Dolby", "ScreenX"];
const configFormats = reactive({
  supported: ["2D", "3D"],
});

// Banners
const configBanners = reactive({
  banners: [],
  newBannerUrl: "",
  newBannerTitle: "",
});

const loadConfigForCinema = (cinema) => {
  configBasic.name = cinema.name || "";
  configBasic.address = cinema.address || "";
  configBasic.phone = cinema.phone || "";
  configBasic.email = cinema.email || "";
  configBasic.openTime = cinema.openTime || "08:00";
  configHours.openTime = cinema.openTime || "08:00";
  configHours.closeTime = cinema.closeTime || "23:30";
  configHours.holidays = cinema.holidays || "";
  configCleaning.cleaningMinutes = cinema.cleaningMinutes || 20;
  if (cinema.supportedFormats) configFormats.supported = [...cinema.supportedFormats];
  if (cinema.banners) configBanners.banners = [...cinema.banners];
};

const showConfigSuccess = (section) => {
  configSuccess[section] = true;
  setTimeout(() => { configSuccess[section] = false; }, 2500);
};

const saveConfigBasic = async () => {
  if (!selectedCinema.value) return;
  configSaving.basic = true;
  try {
    await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/basic`, {
      name: configBasic.name,
      address: configBasic.address,
      phone: configBasic.phone,
      email: configBasic.email,
      openTime: configBasic.openTime,
    });
    selectedCinema.value.name = configBasic.name;
    showConfigSuccess("basic");
  } catch (e) {
    alert("Lỗi khi lưu thông tin cơ bản!");
  } finally {
    configSaving.basic = false;
  }
};

const saveConfigHours = async () => {
  if (!selectedCinema.value) return;
  configSaving.hours = true;
  try {
    await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/hours`, {
      openTime: configHours.openTime,
      closeTime: configHours.closeTime,
      holidays: configHours.holidays,
    });
    showConfigSuccess("hours");
  } catch (e) {
    alert("Lỗi khi lưu giờ hoạt động!");
  } finally {
    configSaving.hours = false;
  }
};

const saveConfigCleaning = async () => {
  if (!selectedCinema.value) return;
  configSaving.cleaning = true;
  try {
    await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/cleaning`, {
      cleaningMinutes: configCleaning.cleaningMinutes,
    });
    tempCleaningTime.value = configCleaning.cleaningMinutes;
    showConfigSuccess("cleaning");
  } catch (e) {
    alert("Lỗi khi lưu thời gian dọn phòng!");
  } finally {
    configSaving.cleaning = false;
  }
};

const saveConfigSeats = async () => {
  if (!selectedCinema.value) return;
  configSaving.seats = true;
  try {
    await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/seats`, {
      seatTypes: configSeats.types,
    });
    showConfigSuccess("seats");
  } catch (e) {
    alert("Lỗi khi lưu cấu hình ghế!");
  } finally {
    configSaving.seats = false;
  }
};

const saveConfigFormats = async () => {
  if (!selectedCinema.value) return;
  configSaving.formats = true;
  try {
    await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/formats`, {
      supportedFormats: configFormats.supported,
    });
    showConfigSuccess("formats");
  } catch (e) {
    alert("Lỗi khi lưu định dạng chiếu!");
  } finally {
    configSaving.formats = false;
  }
};

const toggleFormat = (fmt) => {
  const idx = configFormats.supported.indexOf(fmt);
  if (idx === -1) configFormats.supported.push(fmt);
  else configFormats.supported.splice(idx, 1);
};

const addBanner = () => {
  if (!configBanners.newBannerUrl.trim()) return;
  configBanners.banners.push({
    id: Date.now(),
    url: configBanners.newBannerUrl,
    title: configBanners.newBannerTitle || "Banner",
  });
  configBanners.newBannerUrl = "";
  configBanners.newBannerTitle = "";
};

const removeBanner = (id) => {
  configBanners.banners = configBanners.banners.filter((b) => b.id !== id);
};

const saveConfigBanners = async () => {
  if (!selectedCinema.value) return;
  configSaving.banners = true;
  try {
    await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/banners`, {
      banners: configBanners.banners,
    });
    showConfigSuccess("banners");
  } catch (e) {
    alert("Lỗi khi lưu banner!");
  } finally {
    configSaving.banners = false;
  }
};
</script>

<template>
  <div class="p-10 min-h-screen bg-surface">
    <!-- List View -->
    <div v-if="!selectedCinema">
      <header class="flex justify-between items-center mb-12 text-on-surface">
        <div>
          <h1
            class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary"
          >
            Cluster Network
          </h1>
          <p
            class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold"
          >
            Hệ thống quản lý cụm rạp DevCine toàn quốc
          </p>
        </div>
        <button
          @click="showCreateModal = true"
          class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-8 py-4 rounded-sm hover:brightness-110 transition-all flex items-center gap-3 shadow-lg shadow-primary/20"
        >
          <span class="material-symbols-outlined text-lg font-bold"
            >add_location</span
          >
          Thiết lập Cụm Rạp Mới
        </button>
      </header>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div
          v-for="cinema in cinemas"
          :key="cinema.id"
          @click="openCinemaDetail(cinema)"
          class="bg-surface-container-low border border-outline-variant/10 rounded-xl overflow-hidden hover:border-primary/40 transition-all group cursor-pointer shadow-sm hover:shadow-xl hover:-translate-y-1 duration-500"
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
          <div class="p-8">
            <h3
              class="font-headline font-bold text-xl mb-3 text-on-surface group-hover:text-primary transition-colors"
            >
              {{ cinema.name }}
            </h3>
            <p
              class="text-xs text-on-surface-variant mb-8 line-clamp-2 leading-relaxed opacity-70 italic"
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
                    {{ cinema.rooms }} Phòng
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
      </div>
    </div>

    <!-- Detailed Cinema View -->
    <div v-else class="animate-in fade-in slide-in-from-bottom-8 duration-700">
      <header
        class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10"
      >
        <div class="flex items-center gap-6">
          <button
            @click="closeDetail"
            class="w-12 h-12 flex items-center justify-center rounded-2xl bg-surface-container-high border border-outline-variant/10 text-on-surface hover:text-primary transition-all group"
          >
            <span
              class="material-symbols-outlined group-hover:-translate-x-1 transition-transform"
              >arrow_back</span
            >
          </button>
          <div>
            <div class="flex items-center gap-3 mb-1">
              <span
                class="bg-primary/20 text-primary text-[9px] font-black px-2 py-1 rounded uppercase tracking-widest border border-primary/20"
                >{{ selectedCinema.type }}</span
              >
              <span
                class="text-on-surface-variant text-[10px] font-bold uppercase tracking-widest opacity-60"
                >ID: DC-{{ selectedCinema.id }}</span
              >
            </div>
            <h1
              class="text-4xl font-extrabold tracking-tight font-headline uppercase text-on-surface"
            >
              {{ selectedCinema.name }}
            </h1>
          </div>
        </div>
      </header>

      <!-- Stats Bar -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
        <div
          v-for="(val, key) in selectedCinema.stats"
          :key="key"
          class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-2xl flex justify-between items-center group hover:border-primary/30 transition-all"
        >
          <div>
            <p
              class="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-1"
            >
              {{
                key === "revenue"
                  ? "Doanh thu tháng"
                  : key === "occupancy"
                    ? "Tỷ lệ lấp đầy"
                    : "Tăng trưởng"
              }}
            </p>
            <h4 class="text-2xl font-black text-on-surface">{{ val }}</h4>
          </div>
          <div
            class="w-12 h-12 rounded-xl bg-on-surface/5 flex items-center justify-center text-on-surface-variant group-hover:text-primary transition-colors"
          >
            <span class="material-symbols-outlined text-2xl">{{
              key === "revenue"
                ? "payments"
                : key === "occupancy"
                  ? "chair"
                  : "trending_up"
            }}</span>
          </div>
        </div>
      </div>

      <!-- Main Tabs Content -->
      <div
        v-if="!viewingHall"
        class="bg-surface-container-low border border-outline-variant/10 rounded-3xl overflow-hidden shadow-2xl"
      >
        <div
          class="flex border-b border-outline-variant/10 bg-on-surface/[0.02] p-2 overflow-x-auto no-scrollbar"
        >
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              activeTab === tab.id
                ? 'bg-surface-container-high text-primary shadow-sm border-outline-variant/20'
                : 'text-on-surface-variant hover:text-on-surface border-transparent',
            ]"
            class="flex items-center gap-3 px-8 py-5 text-xs font-black font-headline uppercase tracking-widest transition-all rounded-2xl border flex-shrink-0"
          >
            <span class="material-symbols-outlined text-xl">{{
              tab.icon
            }}</span>
            {{ tab.label }}
          </button>
        </div>

        <div class="p-10 min-h-[500px]">
          <!-- Infrastructure Tab -->
          <div
            v-if="activeTab === 'infrastructure'"
            class="animate-in fade-in slide-in-from-left-4 duration-500"
          >
            <div class="flex justify-between items-center mb-10">
              <h3
                class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
              >
                Cấu hình Phòng chiếu
              </h3>
              <button
                class="bg-primary/10 text-primary px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-primary/20 hover:bg-primary/20 transition-all flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-sm font-bold"
                  >add</span
                >
                Thêm Phòng
              </button>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div
                v-for="hall in selectedCinema.halls"
                :key="hall.id"
                class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-2xl hover:border-primary/30 transition-all group"
              >
                <div class="flex justify-between items-start mb-8">
                  <div class="flex items-center gap-5">
                    <div
                      class="w-16 h-16 rounded-2xl bg-on-surface/5 flex items-center justify-center text-on-surface-variant group-hover:text-primary transition-all group-hover:scale-105"
                    >
                      <span class="material-symbols-outlined text-3xl"
                        >tv_gen</span
                      >
                    </div>
                    <div>
                      <h4 class="text-xl font-bold text-on-surface mb-1">
                        {{ hall.name }}
                      </h4>
                      <span
                        class="text-[9px] font-black text-primary uppercase tracking-[0.2em] px-2 py-1 bg-primary/10 rounded-md"
                        >{{ hall.type }}</span
                      >
                    </div>
                  </div>
                  <div
                    :class="
                      hall.status === 'Active'
                        ? 'text-green-500'
                        : 'text-orange-500'
                    "
                    class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-black/20 text-[9px] font-black uppercase tracking-widest border border-white/5"
                  >
                    <span
                      class="w-1.5 h-1.5 rounded-full bg-current animate-pulse"
                    ></span>
                    {{ hall.status === "Active" ? "Hoạt động" : "Bảo trì" }}
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4 mb-8">
                  <div
                    class="p-4 bg-on-surface/[0.03] rounded-xl border border-white/5"
                  >
                    <p
                      class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mb-1 opacity-50"
                    >
                      Kích thước
                    </p>
                    <p class="text-lg font-black text-on-surface">
                      {{ hall.rows }}x{{ hall.cols }} Matrix
                    </p>
                  </div>
                  <div
                    class="p-4 bg-on-surface/[0.03] rounded-xl border border-white/5"
                  >
                    <p
                      class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mb-1 opacity-50"
                    >
                      Tổng số ghế
                    </p>
                    <p class="text-lg font-black text-on-surface">
                      {{ hall.rows * hall.cols }} Ghế
                    </p>
                  </div>
                </div>

                <div class="flex gap-3">
                  <button
                    @click="openHallDetail(hall)"
                    class="flex-grow py-3 bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest rounded-xl transition-all shadow-lg shadow-primary/10 hover:brightness-110"
                  >
                    Xem Chi tiết & Sơ đồ ghế
                  </button>
                  <button
                    class="w-12 h-12 flex items-center justify-center rounded-xl bg-on-surface/5 hover:bg-on-surface/10 text-on-surface-variant transition-all border border-white/5"
                  >
                    <span class="material-symbols-outlined text-sm"
                      >settings</span
                    >
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Showtimes Tab (Upgraded Timeline Matrix) -->
          <div
            v-else-if="activeTab === 'showtimes'"
            class="animate-in fade-in slide-in-from-left-4 duration-500 -mx-10 -my-10"
          >
            <!-- Header: Date Picker -->
            <header
              class="flex justify-between items-center p-8 border-b border-outline-variant/10 bg-on-surface/[0.02]"
            >
              <div class="flex items-center gap-3">
                <button
                  v-for="d in dates"
                  :key="d.date"
                  @click="selectedDate = d.date"
                  :class="
                    selectedDate === d.date
                      ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
                      : 'bg-surface-container-high text-on-surface-variant border-outline-variant/10'
                  "
                  class="flex flex-col items-center min-w-[65px] py-2 rounded-xl border transition-all hover:bg-white/5"
                >
                  <span class="text-[8px] font-black uppercase opacity-40">{{
                    d.day
                  }}</span>
                  <span class="text-xs font-black">{{ d.date }}</span>
                </button>
              </div>

              <div class="flex gap-4">
                <button
                  @click="showCleaningSettingsModal = true; tempCleaningTime = selectedCinema.cleaningTime || 20"
                  class="bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-outline-variant/10 hover:bg-white/10 transition-all flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-sm">settings</span> Cài đặt dọn dẹp
                </button>
                <button
                  class="bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-outline-variant/10 hover:bg-white/10 transition-all flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-sm">bolt</span> [Nút chờ]
                </button>
                <button
                  @click="showAddShowtimeDrawer = true"
                  class="bg-primary text-on-primary px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-primary/20 hover:brightness-110 transition-all flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-sm">add</span>
                  Thêm suất chiếu
                </button>
                <button
                  @click="handlePublish"
                  class="bg-green-500 text-white px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-green-500/20 hover:brightness-110 transition-all flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-sm">publish</span>
                  Xuất bản
                </button>
              </div>
            </header>

            <!-- Main Timeline Matrix -->
            <div class="overflow-hidden relative h-[500px] flex flex-col">
              <!-- Scrollable Area -->
              <div
                class="flex-grow overflow-x-auto overflow-y-auto scrollbar-hide relative bg-[#0b111e]"
              >
                <!-- Main Wrapper -->
                <div class="min-w-[2400px] flex flex-col min-h-full relative">
                  <!-- Time Ruler -->
                  <div
                    class="flex border-b border-outline-variant/10 bg-[#0b111e] flex-shrink-0 sticky top-0 z-40"
                  >
                    <div
                      class="w-48 flex-shrink-0 p-4 border-r border-outline-variant/10 flex items-center justify-center font-black text-primary uppercase tracking-[0.2em] text-[8px] italic bg-[#0b111e] sticky left-0 z-50"
                    >
                      Room \ Time
                    </div>
                    <div
                      class="flex-grow grid grid-cols-[repeat(72,minmax(0,1fr))] relative h-10"
                    >
                      <div
                        v-for="hour in 18"
                        :key="hour"
                        class="col-span-4 border-r border-outline-variant/10 flex items-center justify-start pl-2 text-[9px] font-black text-on-surface-variant/30"
                      >
                        {{ ((hour + 7) % 24).toString().padStart(2, "0") }}:00
                      </div>
                    </div>
                  </div>

                  <!-- Vertical Grid Lines -->
                  <div
                    class="absolute inset-0 top-10 grid grid-cols-[repeat(72,minmax(0,1fr))] pointer-events-none pl-48 z-0"
                  >
                    <div
                      v-for="i in 72"
                      :key="i"
                      :class="
                        i % 4 === 0
                          ? 'border-r border-outline-variant/20'
                          : 'border-r border-outline-variant/5'
                      "
                      class="h-full"
                    ></div>
                  </div>
                  
                  <!-- Current Time Indicator -->
                  <div
                    v-if="isToday && currentMinuteOffset >= 0 && currentMinuteOffset <= 960"
                    class="absolute top-10 bottom-0 left-48 right-0 pointer-events-none z-30"
                  >
                    <div
                      class="absolute top-0 bottom-0 w-[1px] bg-primary/80"
                      :style="{ left: currentTimeLeft }"
                    ></div>
                  </div>

                  <!-- Rows Container -->
                  <div class="flex-grow relative z-10 flex flex-col">
                    <div
                      v-for="hall in selectedCinema.halls"
                      :key="hall.id"
                      class="flex items-center border-b border-outline-variant/10 group hover:bg-white/[0.02] transition-all min-h-[100px] relative"
                    >
                      <!-- Hall Labels (Sticky Column) -->
                      <div
                        class="w-48 flex-shrink-0 p-5 border-r border-outline-variant/10 flex items-center gap-3 bg-[#0b111e] sticky left-0 z-20 self-stretch"
                      >
                        <div
                          class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 border border-primary/20"
                        >
                          <span
                            class="material-symbols-outlined text-primary text-lg"
                            >tv_gen</span
                          >
                        </div>
                        <div class="flex flex-col text-left overflow-hidden">
                          <h3
                            class="text-[11px] font-black uppercase tracking-tight text-on-surface truncate"
                          >
                            {{ hall.name }}
                          </h3>
                          <p
                            class="text-[8px] font-bold text-on-surface-variant/50 uppercase tracking-widest mt-0.5"
                          >
                            {{ hall.type }}
                          </p>
                        </div>
                      </div>

                      <!-- Showtime Container -->
                      <div
                        @dragover.prevent
                        @drop="onDrop($event, hall.id)"
                        class="flex-grow grid grid-cols-[repeat(72,minmax(0,1fr))] grid-rows-1 gap-x-0 relative p-0 items-center min-w-[2200px]"
                      >
                        <div
                          v-for="show in selectedCinema.shows.filter(
                            (s) => s.roomId === hall.id && s.date === selectedDate
                          )"
                          :key="show.id"
                          draggable="true"
                          @dragstart="onDragStart($event, show)"
                          :style="{
                            ...getGridStyle(show.startTime, show.duration),
                            backgroundColor: show.color + '33',
                            borderColor:
                              checkConflict(hall.id, show) ||
                              checkFormatMismatch(hall, show.format)
                                ? '#ef4444'
                                : show.color + '66',
                          }"
                          @click="openShowtimeDetails(show)"
                          class="relative h-[76px] mx-0.5 border rounded-xl p-2.5 cursor-pointer group/card transition-all duration-300 hover:z-30 hover:scale-[1.02] hover:brightness-125 shadow-xl flex flex-col justify-between"
                          :class="{
                            'ring-2 ring-red-500 ring-inset animate-pulse':
                              checkConflict(hall.id, show) ||
                              checkFormatMismatch(hall, show.format),
                          }"
                        >
                          <div class="flex justify-between items-start">
                            <p class="text-[12px] font-bold font-sans text-white leading-tight truncate tracking-wide flex-1 pr-1">
                              {{ show.movie }}
                            </p>
                            <div class="flex items-center gap-1 shrink-0">
                              <span v-if="checkConflict(hall.id, show)" class="material-symbols-outlined text-red-500 text-[12px]">warning</span>
                              <span v-if="checkFormatMismatch(hall, show.format)" class="material-symbols-outlined text-red-500 text-[12px]">error</span>
                              <div class="px-2 h-[18px] bg-white/10 rounded flex items-center justify-center text-[8px] leading-none font-bold font-sans text-white border border-white/20 uppercase tracking-wider">
                                {{ show.format }}
                              </div>
                            </div>
                          </div>
                          
                          <div class="text-[10px] font-bold font-sans text-white/90 tracking-wide mt-0.5">
                            {{ show.startTime }} - {{ getEndTime(show.startTime, show.duration) }}
                          </div>
                          
                          <div class="flex justify-between items-center mt-auto">
                            <span class="text-[9px] font-medium font-sans text-[#B3B3B3]">
                              {{ show.duration }}m
                            </span>
                            <span class="text-[9px] font-bold font-sans text-primary">
                              {{ show.price.toLocaleString() }}đ
                            </span>
                          </div>
                          <!-- Dynamic Glow -->
                          <div
                            class="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent pointer-events-none rounded-2xl"
                          ></div>
                          
                          <!-- Cleaning Time Tail -->
                          <div
                            class="absolute top-0 bottom-0 left-[100%] bg-[repeating-linear-gradient(45deg,transparent,transparent_4px,rgba(255,255,255,0.05)_4px,rgba(255,255,255,0.05)_8px)] border-y border-r border-white/5 rounded-r-lg pointer-events-none z-[-1]"
                            :style="{ width: `${(selectedCinema.cleaningTime || 20) / show.duration * 100}%` }"
                            title="Thời gian dọn dẹp"
                          ></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Footer Legend (Updated) -->
              <footer
                class="p-4 bg-on-surface/[0.02] border-t border-outline-variant/10 flex items-center gap-8"
              >
                <div class="flex items-center gap-2">
                  <div
                    class="w-1.5 h-1.5 rounded-full bg-primary shadow-[0_0_8px_#F5C518]"
                  ></div>
                  <span
                    class="text-[8px] font-black uppercase tracking-widest text-on-surface-variant"
                    >Đang chiếu</span
                  >
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-1.5 h-1.5 rounded-full bg-white/20"></div>
                  <span
                    class="text-[8px] font-black uppercase tracking-widest text-on-surface-variant"
                    >Sắp chiếu</span
                  >
                </div>
                <div class="flex items-center gap-2">
                  <div
                    class="w-1.5 h-1.5 rounded-full bg-red-500 shadow-[0_0_8px_#ef4444]"
                  ></div>
                  <span
                    class="text-[8px] font-black uppercase tracking-widest text-red-500"
                    >Xung đột / Sai định dạng</span
                  >
                </div>
                <div
                  class="ml-auto text-[7px] font-black uppercase tracking-[0.3em] text-on-surface-variant opacity-20 italic"
                >
                  Constraint Engine v4.0
                </div>
              </footer>
            </div>
          </div>

          <!-- Staff Tab (Advanced Roster & KPI) -->
          <div
            v-else-if="activeTab === 'staff'"
            class="animate-in fade-in slide-in-from-left-4 duration-500"
          >
            <div class="flex justify-between items-center mb-10">
              <h3
                class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
              >
                Quản trị Nhân sự
              </h3>
              <div class="flex gap-4">
                <button
                  class="bg-surface-container-high px-4 py-2 rounded-xl border border-outline-variant/10 text-[10px] font-black uppercase tracking-widest text-on-surface-variant"
                >
                  Xuất Bảng Lương
                </button>
                <button
                  class="bg-primary/10 text-primary px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-primary/20 hover:bg-primary/20 transition-all flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-sm font-bold"
                    >person_add</span
                  >
                  Tuyển dụng mới
                </button>
              </div>
            </div>

            <!-- Role-based Groups -->
            <div class="space-y-12">
              <div v-for="role in ['Box Office', 'Usher', 'F&B']" :key="role">
                <div class="flex items-center gap-4 mb-6">
                  <span class="w-8 h-[1px] bg-primary/30"></span>
                  <h4
                    class="text-xs font-black uppercase tracking-[0.3em] text-primary italic"
                  >
                    {{ role }} Team
                  </h4>
                  <span
                    class="px-2 py-0.5 bg-primary/10 text-primary text-[8px] font-black rounded"
                    >{{
                      selectedCinema.staff.filter((s) => s.role === role).length
                    }}
                    Thành viên</span
                  >
                </div>

                <div
                  class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
                >
                  <div
                    v-for="member in selectedCinema.staff.filter(
                      (s) => s.role === role,
                    )"
                    :key="member.id"
                    class="bg-surface-container-high border border-outline-variant/10 p-6 rounded-2xl group hover:border-primary/30 transition-all relative overflow-hidden"
                  >
                    <div class="flex items-center gap-4 mb-6">
                      <div
                        class="w-14 h-14 rounded-2xl bg-on-surface/5 flex items-center justify-center text-primary font-black text-xl border border-white/5 relative"
                      >
                        {{ member.name.charAt(0) }}
                        <span
                          v-if="member.status === 'On Duty'"
                          class="absolute -top-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-surface-container-high"
                        ></span>
                      </div>
                      <div>
                        <h4
                          class="text-lg font-black text-on-surface group-hover:text-primary transition-colors uppercase tracking-tight"
                        >
                          {{ member.name }}
                        </h4>
                        <p
                          class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest"
                        >
                          Ca {{ member.shift }}
                        </p>
                      </div>
                    </div>

                    <!-- KPI Stats -->
                    <div
                      class="grid grid-cols-2 gap-4 mb-6 p-4 bg-on-surface/5 rounded-xl border border-white/5"
                    >
                      <div v-if="role === 'Box Office'">
                        <p
                          class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
                        >
                          Vé đã bán
                        </p>
                        <p class="text-lg font-black text-on-surface">
                          {{ member.sales }}
                        </p>
                      </div>
                      <div v-if="role === 'F&B'">
                        <p
                          class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
                        >
                          Đơn hàng
                        </p>
                        <p class="text-lg font-black text-on-surface">
                          {{ member.sales }}
                        </p>
                      </div>
                      <div v-if="role === 'Usher'">
                        <p
                          class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
                        >
                          Suất đã soát
                        </p>
                        <p class="text-lg font-black text-on-surface">--</p>
                      </div>
                      <div>
                        <p
                          class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
                        >
                          Hiệu suất
                        </p>
                        <p class="text-lg font-black text-green-500">92%</p>
                      </div>
                    </div>

                    <div class="flex gap-2">
                      <button
                        class="flex-grow py-2.5 bg-on-surface/5 hover:bg-white/5 text-[9px] font-black uppercase tracking-widest rounded-lg border border-white/5 transition-all italic"
                      >
                        Lịch sử ca trực
                      </button>
                      <button
                        class="w-10 h-10 flex items-center justify-center rounded-lg bg-primary/10 text-primary hover:bg-primary hover:text-on-primary transition-all border border-primary/20"
                      >
                        <span class="material-symbols-outlined text-sm"
                          >trending_up</span
                        >
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- F&B Tab (Inventory & Combo Creator) -->
          <div
            v-else-if="activeTab === 'fnb'"
            class="animate-in fade-in slide-in-from-left-4 duration-500"
          >
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
                v-for="item in selectedCinema.inventory"
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
          </div>

          <!-- Analytics Tab (Operational Hub) -->
          <div
            v-else-if="activeTab === 'analytics'"
            class="animate-in fade-in slide-in-from-left-4 duration-500"
          >
            <div class="flex justify-between items-center mb-10">
              <h3
                class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
              >
                Trung tâm Điều hành
              </h3>
              <div class="flex gap-4">
                <div
                  class="flex items-center bg-surface-container-high rounded-xl border border-outline-variant/10 p-1"
                >
                  <button
                    class="px-4 py-2 bg-primary text-on-primary text-[8px] font-black uppercase tracking-widest rounded-lg"
                  >
                    Thời gian thực
                  </button>
                  <button
                    class="px-4 py-2 text-on-surface-variant text-[8px] font-black uppercase tracking-widest rounded-lg"
                  >
                    Theo tuần
                  </button>
                </div>
                <button
                  class="bg-on-surface/5 text-on-surface px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-outline-variant/10 hover:bg-white/5 transition-all flex items-center gap-2 italic"
                >
                  <span class="material-symbols-outlined text-sm">print</span>
                  Xuất Báo Cáo Cinema
                </button>
              </div>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <!-- Heatmap Doanh thu (Mockup) -->
              <div
                class="lg:col-span-2 bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden"
              >
                <div class="flex justify-between items-center mb-10">
                  <div>
                    <p
                      class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic"
                    >
                      Phân tích giờ cao điểm
                    </p>
                    <h4
                      class="text-xl font-bold text-on-surface uppercase tracking-tight"
                    >
                      Heatmap Doanh thu trong ngày
                    </h4>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-[8px] font-bold opacity-40 uppercase"
                      >Low</span
                    >
                    <div class="flex gap-1">
                      <div class="w-3 h-3 rounded-sm bg-primary/20"></div>
                      <div class="w-3 h-3 rounded-sm bg-primary/40"></div>
                      <div class="w-3 h-3 rounded-sm bg-primary/70"></div>
                      <div
                        class="w-3 h-3 rounded-sm bg-primary shadow-[0_0_8px_#F5C518]"
                      ></div>
                    </div>
                    <span class="text-[8px] font-bold opacity-40 uppercase"
                      >Peak</span
                    >
                  </div>
                </div>

                <!-- The Heatmap Grid -->
                <div class="grid grid-cols-12 gap-2 h-40">
                  <div
                    v-for="i in 24"
                    :key="i"
                    class="rounded-lg transition-all hover:scale-110 hover:z-20 cursor-pointer relative group"
                    :class="[
                      i > 18 && i < 22
                        ? 'bg-primary shadow-[0_0_15px_rgba(245,197,24,0.4)]'
                        : i > 16 && i < 24
                          ? 'bg-primary/70'
                          : i > 10 && i < 15
                            ? 'bg-primary/40'
                            : 'bg-primary/10',
                    ]"
                  >
                    <div
                      class="absolute -top-8 left-1/2 -translate-x-1/2 bg-black px-2 py-1 rounded text-[7px] font-black text-white opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-30"
                    >
                      {{ i }}:00 - Bận rộn
                    </div>
                  </div>
                </div>
                <div
                  class="flex justify-between mt-4 text-[10px] font-bold text-on-surface-variant uppercase tracking-widest opacity-80"
                >
                  <span>08:00</span>
                  <span>14:00</span>
                  <span>20:00</span>
                  <span>02:00</span>
                </div>
              </div>

              <!-- Waste & Cost Optimization -->
              <div
                class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl"
              >
                <h4
                  class="text-sm font-black uppercase tracking-[0.2em] text-on-surface-variant mb-8 flex items-center gap-2 italic"
                >
                  <span
                    class="material-symbols-outlined text-orange-500 text-lg"
                    >recycling</span
                  >
                  Báo cáo phế phẩm
                </h4>
                <div class="space-y-6">
                  <div
                    class="flex justify-between items-end border-b border-white/5 pb-4"
                  >
                    <p
                      class="text-[9px] font-black text-on-surface-variant uppercase italic"
                    >
                      Tổng Waste (Tháng)
                    </p>
                    <p class="text-2xl font-black text-red-500">
                      1.2tr
                      <span
                        class="text-[10px] font-normal text-on-surface-variant italic"
                        >VND</span
                      >
                    </p>
                  </div>
                  <div class="space-y-4">
                    <div
                      v-for="item in selectedCinema.inventory.filter(
                        (i) => i.waste > 5,
                      )"
                      :key="item.id"
                      class="flex items-center justify-between"
                    >
                      <span
                        class="text-[10px] font-bold text-on-surface uppercase"
                        >{{ item.name }}</span
                      >
                      <span class="text-[10px] font-black text-orange-400"
                        >{{ item.waste }} {{ item.unit }}</span
                      >
                    </div>
                  </div>
                  <button
                    class="w-full py-3 bg-on-surface/5 text-[9px] font-black uppercase tracking-widest rounded-xl hover:bg-white/5 transition-all border border-white/5"
                  >
                    Xem phân tích tối ưu chi phí
                  </button>
                </div>
              </div>

              <!-- Room Occupancy Detailed -->
              <div
                class="lg:col-span-3 bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl"
              >
                <div class="flex justify-between items-center mb-10">
                  <h4
                    class="text-xl font-bold text-on-surface uppercase tracking-tight"
                  >
                    Hiệu suất lấp đầy chi tiết (Occupancy Rate)
                  </h4>
                  <div
                    class="text-[10px] font-black text-primary uppercase tracking-widest italic flex items-center gap-2"
                  >
                    Target: 75%
                    <span
                      class="material-symbols-outlined text-green-500 text-xs"
                      >trending_up</span
                    >
                  </div>
                </div>
                <div
                  class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6"
                >
                  <div
                    v-for="hall in selectedCinema.halls"
                    :key="hall.id"
                    class="p-6 bg-black/20 rounded-2xl border border-white/5 group hover:border-primary/40 transition-all"
                  >
                    <div class="flex justify-between items-center mb-4">
                      <span
                        class="text-[10px] font-black text-white/40 uppercase"
                        >{{ hall.name }}</span
                      >
                      <span
                        class="text-lg font-black"
                        :class="
                          hall.id === 'H2' ? 'text-green-500' : 'text-primary'
                        "
                        >{{
                          hall.id === "H1"
                            ? "88%"
                            : hall.id === "H2"
                              ? "92%"
                              : "45%"
                        }}</span
                      >
                    </div>
                    <div
                      class="w-full h-1 bg-white/5 rounded-full overflow-hidden mb-2"
                    >
                      <div
                        class="h-full bg-current transition-all duration-1000"
                        :style="{
                          width:
                            hall.id === 'H1'
                              ? '88%'
                              : hall.id === 'H2'
                                ? '92%'
                                : '45%',
                          color: hall.id === 'H2' ? '#22c55e' : '#F5C518',
                        }"
                      ></div>
                    </div>
                    <p
                      class="text-[8px] font-bold text-on-surface-variant uppercase tracking-widest opacity-40"
                    >
                      Dựa trên 12 suất chiếu hôm nay
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Config Tab -->
          <div
            v-else-if="activeTab === 'config'"
            class="animate-in fade-in slide-in-from-left-4 duration-500 space-y-4"
          >
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
                  <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Loại ghế &amp; Giá mặc định</h4>
                  <p class="text-[10px] text-on-surface-variant mt-0.5">Cấu hình loại ghế và giá vé theo từng loại</p>
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
                      <div v-for="(seat, idx) in configSeats.types" :key="seat.id"
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
                            <label class="text-[9px] font-black uppercase tracking-widest text-on-surface-variant">Giá mặc định (VND)</label>
                            <input v-model.number="seat.defaultPrice" type="number" step="5000"
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
        </div>
      </div>

      <!-- Seat Map View -->
      <div
        v-else
        class="animate-in fade-in slide-in-from-right-8 duration-700 flex flex-col h-[calc(100vh-120px)]"
      >
        <header
          class="flex justify-between items-center mb-8 px-4 flex-shrink-0"
        >
          <div class="flex items-center gap-6">
            <button
              @click="viewingHall = null"
              class="w-10 h-10 flex items-center justify-center rounded-full bg-on-surface/5 border border-white/10 text-on-surface hover:text-primary transition-all"
            >
              <span class="material-symbols-outlined text-lg">arrow_back</span>
            </button>
            <div>
              <h1
                class="text-2xl font-black tracking-tight font-headline uppercase text-on-surface flex items-center gap-3"
              >
                {{ viewingHall.name }}
                <span class="text-primary/30 text-lg">/</span>
                <span class="text-primary text-lg">{{ viewingHall.type }}</span>
              </h1>
            </div>
          </div>
          <div class="flex gap-3">
            <button
              @click="resetMap"
              class="px-6 py-2.5 rounded-lg border border-white/10 text-on-surface-variant text-[9px] font-black uppercase tracking-widest hover:bg-white/5 transition-all"
            >
              Đặt lại
            </button>
            <button
              @click="saveSeatLayout"
              :disabled="isSavingLayout"
              class="px-8 py-2.5 rounded-lg bg-primary text-on-primary text-[9px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-xl shadow-primary/20 flex items-center gap-2 disabled:opacity-50"
            >
              <span class="material-symbols-outlined text-sm">
                {{ isSavingLayout ? 'hourglass_empty' : 'save' }}
              </span>
              {{ isSavingLayout ? 'Đang lưu...' : 'Lưu Cấu Trúc' }}
            </button>
          </div>
        </header>

        <div class="flex-grow overflow-hidden min-h-0">
          <SeatMapBuilder 
            :initial-rows="tempRows" 
            :initial-cols="tempCols" 
            :initial-seat-map="currentSeatMap" 
            @update:layout="(data) => {
              tempRows = data.rows;
              tempCols = data.cols;
              currentSeatMap = data.seats;
            }" 
          />
        </div>
        </div>
      </div>
    </div>

    <!-- Create Cinema Modal Overlay -->
    <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
      <!-- Modal Content -->
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col slide-in-from-bottom-8">
        <!-- Header -->
        <div class="px-8 py-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/30">
          <h2 class="text-xl font-black uppercase tracking-widest text-primary flex items-center gap-3">
            <span class="material-symbols-outlined">add_business</span>
            Thiết lập Cụm Rạp Mới
          </h2>
          <button @click="showCreateModal = false" class="text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Body -->
        <div class="p-8 space-y-6">
          <div class="grid grid-cols-2 gap-6">
            <!-- Tên Cụm Rạp -->
            <div class="space-y-2 col-span-2 sm:col-span-1">
              <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Tên Cụm Rạp <span class="text-red-500">*</span></label>
              <input v-model="newCinema.name" type="text" placeholder="VD: DevCine Landmark 81" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all placeholder-white/20">
            </div>
            
            <!-- Hotline -->
            <div class="space-y-2 col-span-2 sm:col-span-1">
              <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Hotline <span class="text-red-500">*</span></label>
              <input v-model="newCinema.hotline" type="text" placeholder="VD: 1900 1234" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all placeholder-white/20">
            </div>

            <!-- Địa chỉ -->
            <div class="space-y-2 col-span-2">
              <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Địa chỉ chi tiết <span class="text-red-500">*</span></label>
              <input v-model="newCinema.address" type="text" placeholder="VD: Tầng B1, Vincom Landmark 81, TP.HCM" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all placeholder-white/20">
            </div>

            <!-- Loại rạp -->
            <div class="space-y-2 col-span-2 sm:col-span-1">
              <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Loại Cụm Rạp</label>
              <select v-model="newCinema.type" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none cursor-pointer">
                <option value="Standard" class="bg-surface-container-high text-white">Standard</option>
                <option value="Premium/IMAX" class="bg-surface-container-high text-white">Premium/IMAX</option>
                <option value="Sweetbox" class="bg-surface-container-high text-white">Sweetbox</option>
                <option value="Gold Class" class="bg-surface-container-high text-white">Gold Class</option>
              </select>
            </div>

            <!-- Số lượng phòng -->
            <div class="space-y-2 col-span-2 sm:col-span-1">
              <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Số lượng phòng dự kiến</label>
              <input v-model.number="newCinema.rooms" type="number" min="1" max="20" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all">
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="px-8 py-6 border-t border-outline-variant/10 bg-surface-container-high/10 flex justify-end gap-4">
          <button @click="showCreateModal = false" class="px-6 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">
            Hủy bỏ
          </button>
          <button @click="handleCreateCinema" class="px-8 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-lg shadow-primary/20 flex items-center gap-2">
            Tạo Cụm Rạp
          </button>
        </div>
      </div>
    </div>
    
    <!-- Cleaning Settings Modal -->
    <div v-if="showCleaningSettingsModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden flex flex-col slide-in-from-bottom-8">
        <div class="px-6 py-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/30">
          <h2 class="text-lg font-black uppercase tracking-widest text-primary flex items-center gap-2">
            <span class="material-symbols-outlined">cleaning_services</span> Cài đặt dọn dẹp
          </h2>
        </div>
        <div class="p-6 space-y-4">
          <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Thời gian dọn dẹp (phút)</label>
          <input v-model.number="tempCleaningTime" type="number" min="0" step="5" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 outline-none transition-all">
          <p class="text-[9px] text-on-surface-variant italic">Áp dụng cho tất cả các phòng chiếu của cụm rạp {{ selectedCinema?.name }}.</p>
        </div>
        <div class="px-6 py-4 border-t border-outline-variant/10 bg-surface-container-high/10 flex justify-end gap-3">
          <button @click="showCleaningSettingsModal = false" class="px-4 py-2 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5">Hủy</button>
          <button @click="selectedCinema.cleaningTime = tempCleaningTime; showCleaningSettingsModal = false" class="px-6 py-2 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 shadow-lg shadow-primary/20">Lưu</button>
        </div>
      </div>
    </div>

    <!-- Showtime Details Drawer -->
    <Transition name="fade">
      <div v-if="showDrawer" class="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100]" @click="closeDrawer"></div>
    </Transition>
    
    <Transition name="drawer">
      <div v-if="showDrawer" class="fixed top-0 right-0 h-full w-[500px] bg-surface-container-high border-l border-outline-variant/10 shadow-2xl z-[101] flex flex-col">
        <!-- Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container">
          <h2 class="text-lg font-black font-headline uppercase tracking-widest text-on-surface">Chi tiết lịch chiếu</h2>
          <button @click="closeDrawer" class="w-8 h-8 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all">
            <span class="material-symbols-outlined text-white/70 text-sm">close</span>
          </button>
        </div>
        
        <!-- Body -->
        <div class="flex-1 overflow-y-auto p-6" v-if="selectedShowtime">
          <!-- Movie Info Header -->
          <div class="flex gap-4 mb-6">
            <!-- Portrait Poster -->
            <div class="w-24 h-[140px] shrink-0 rounded-xl bg-gradient-to-br from-primary/20 to-surface-variant flex items-center justify-center border border-white/10 relative overflow-hidden shadow-lg shadow-black/20">
              <span class="material-symbols-outlined text-4xl text-primary/40">movie</span>
            </div>
            
            <!-- Movie Details -->
            <div class="flex flex-col py-1">
              <div class="flex items-center gap-2 mb-1.5">
                <span :class="selectedShowtime.movie.includes('DORAEMON') ? 'bg-green-500' : 'bg-red-500'" class="px-1.5 py-0.5 rounded text-[9px] font-bold text-white uppercase tracking-wider">
                  {{ selectedShowtime.movie.includes('DORAEMON') ? 'P' : 'T18' }}
                </span>
                <span class="text-[10px] font-medium text-white/60">
                  {{ selectedShowtime.movie.includes('DORAEMON') ? 'Hoạt hình, Phiêu lưu' : 'Tâm lý, Giật gân' }}
                </span>
              </div>
              <div class="flex items-center gap-3 mb-2">
                <h3 class="text-xl font-black font-headline text-white leading-tight">{{ selectedShowtime.movie }}</h3>
                <div 
                  class="px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-widest border"
                  :class="
                    selectedShowtime.status === 'ongoing' ? 'bg-green-500/10 text-green-400 border-green-500/20' :
                    selectedShowtime.status === 'past' ? 'bg-white/5 text-white/40 border-white/10' :
                    'bg-orange-500/10 text-orange-400 border-orange-500/20'
                  "
                >
                  {{ selectedShowtime.status === 'ongoing' ? 'Đang chiếu' : selectedShowtime.status === 'past' ? 'Đã chiếu' : 'Sắp chiếu' }}
                </div>
              </div>
              
              <div class="flex flex-wrap items-center gap-2 mb-3">
                <div class="px-2 h-[18px] bg-white/10 rounded flex items-center justify-center text-[10px] leading-none font-bold font-sans text-white border border-white/20 uppercase tracking-wider">
                  {{ selectedShowtime.format }}
                </div>
                <div class="px-2 h-[18px] bg-primary/10 text-primary rounded flex items-center justify-center text-[9px] leading-none font-bold font-sans border border-primary/20 uppercase tracking-wider">
                  {{ selectedShowtime.movie.includes('DORAEMON') ? 'Lồng tiếng' : 'Phụ đề Tiếng Việt' }}
                </div>
                <span class="text-xs font-medium text-white/50">{{ selectedShowtime.duration }} phút</span>
              </div>
              
              <div class="text-[11px] text-white/50 font-medium">Đạo diễn: <span class="text-white/80">{{ selectedShowtime.movie.includes('DORAEMON') ? 'Kazuaki Imai' : 'Christopher Nolan' }}</span></div>
              <div class="text-[11px] text-white/50 font-medium mt-0.5 line-clamp-1">Diễn viên: <span class="text-white/80">{{ selectedShowtime.movie.includes('DORAEMON') ? 'Wasabi Mizuta, Megumi Ohara' : 'Cillian Murphy, Emily Blunt' }}</span></div>
            </div>
          </div>

          <!-- Synopsis -->
          <div class="mb-6">
            <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mb-2">Nội dung phim</h4>
            <p class="text-xs text-white/70 leading-relaxed line-clamp-3">
              {{ selectedShowtime.movie.includes('DORAEMON') ? 'Nobita và những người bạn tình cờ phát hiện ra một hòn đảo kỳ lạ, nơi trú ngụ của những loài động vật đã tuyệt chủng. Họ cùng nhau trải qua cuộc phiêu lưu bảo vệ hòn đảo khỏi sự tấn công của những kẻ săn trộm độc ác.' : 'Câu chuyện lịch sử về nhà vật lý J. Robert Oppenheimer và vai trò lãnh đạo của ông trong Dự án Manhattan, dẫn đến việc chế tạo ra bom nguyên tử trong Thế chiến thứ hai, thay đổi cục diện thế giới mãi mãi.' }}
            </p>
          </div>

          <!-- Divider -->
          <div class="h-px w-full bg-outline-variant/10 mb-6"></div>
          
          <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mb-4">Thông tin lịch chiếu</h4>
          <div class="space-y-4">
            <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex items-center gap-4">
              <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-primary">schedule</span>
              </div>
              <div>
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Ngày & Thời gian</p>
                <div class="flex items-center gap-2 mt-0.5">
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-white/10 text-white/90">20/10/2026</span>
                  <p class="text-sm font-bold text-white">{{ selectedShowtime.startTime }} - {{ getEndTime(selectedShowtime.startTime, selectedShowtime.duration) }}</p>
                </div>
              </div>
            </div>
            
            <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex items-center gap-4">
              <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-primary">meeting_room</span>
              </div>
              <div>
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Phòng chiếu</p>
                <p class="text-sm font-bold text-white mt-0.5">{{ selectedCinema?.halls.find(h => h.id === selectedShowtime.roomId)?.name || selectedShowtime.roomId }}</p>
              </div>
            </div>
            
            <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex items-center gap-4">
              <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-primary">payments</span>
              </div>
              <div>
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Giá vé cơ bản</p>
                <p class="text-sm font-bold text-primary mt-0.5">{{ selectedShowtime.price.toLocaleString() }}đ</p>
              </div>
            </div>
          </div>
          
          <!-- Thống kê suất chiếu -->
          <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mt-8 mb-4">Thống kê suất chiếu (Tạm tính)</h4>
          <div class="grid grid-cols-2 gap-4">
            <div 
              class="bg-black/20 p-4 rounded-xl border border-white/5 cursor-pointer hover:bg-white/5 transition-all group relative"
              @click="showSeatMapModal = true"
            >
              <div class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity rounded-xl pointer-events-none flex items-center justify-center backdrop-blur-[1px]">
                <span class="px-3 py-1.5 bg-black/80 text-white text-[10px] font-bold uppercase tracking-widest rounded-lg border border-white/10 shadow-xl">Xem sơ đồ ghế</span>
              </div>
              
              <div class="flex items-center justify-between mb-1">
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Ghế đã đặt</p>
                <span class="text-[9px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded border border-primary/20">
                  Trống: {{ 144 - getSoldTickets(selectedShowtime.movie) }}
                </span>
              </div>
              <div class="flex items-baseline gap-1">
                <span class="text-lg font-black text-white">{{ getSoldTickets(selectedShowtime.movie) }}</span>
                <span class="text-xs text-white/40">/ 144</span>
              </div>
              <div class="h-1 w-full bg-white/5 rounded-full mt-2 overflow-hidden">
                <div class="h-full bg-primary rounded-full transition-all duration-1000" :style="{ width: `${(getSoldTickets(selectedShowtime.movie) / 144) * 100}%` }"></div>
              </div>
            </div>
            
            <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex flex-col justify-center">
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest mb-1">Doanh thu dự kiến</p>
              <span class="text-lg font-black text-green-400">{{ selectedShowtime.movie.includes('DORAEMON') ? '4.275.000đ' : '16.800.000đ' }}</span>
            </div>
          </div>
        </div>
        
        <!-- Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container flex gap-4">
          <button 
            v-if="getSoldTickets(selectedShowtime?.movie) === 0"
            class="flex-1 py-3 rounded-xl border border-red-500/30 bg-red-500/10 text-red-500 text-[11px] font-black uppercase tracking-widest hover:bg-red-500/20 transition-all flex items-center justify-center gap-2"
          >
            <span class="material-symbols-outlined text-[16px]">delete</span> Xóa
          </button>
          
          <button 
            v-else
            class="flex-1 py-3 rounded-xl border border-white/10 bg-black/20 text-white/50 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 hover:text-red-400 hover:border-red-400/30 transition-all flex flex-col items-center justify-center gap-0.5 group"
          >
            <div class="flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">cancel</span> 
              <span class="group-hover:hidden">Hủy & Hoàn tiền</span>
              <span class="hidden group-hover:inline">Xác nhận Hủy</span>
            </div>
            <span class="text-[8px] font-medium text-white/30 lowercase normal-case tracking-normal">(Đã có {{ getSoldTickets(selectedShowtime?.movie) }} vé)</span>
          </button>

          <button 
            class="flex-1 py-3 rounded-xl bg-primary text-on-primary text-[11px] font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2"
            :class="getSoldTickets(selectedShowtime?.movie) > 0 ? 'opacity-80 hover:brightness-105' : 'hover:brightness-110 shadow-lg shadow-primary/20'"
          >
            <span class="material-symbols-outlined text-[16px]">edit</span> 
            {{ getSoldTickets(selectedShowtime?.movie) > 0 ? 'Sửa (Hạn chế)' : 'Chỉnh sửa' }}
          </button>
        </div>
      </div>
    </Transition>
    
    <!-- Seat Map ReadOnly Modal -->
    <Transition name="fade">
      <div v-if="showSeatMapModal" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[200] flex items-center justify-center p-8" @click.self="showSeatMapModal = false">
        <div class="bg-surface-container rounded-3xl border border-white/10 shadow-2xl w-full max-w-5xl h-[95vh] flex flex-col relative overflow-hidden">
          <!-- Modal Header -->
          <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-black/20">
            <div>
              <h2 class="text-xl font-black text-white uppercase tracking-widest font-headline">Sơ đồ phòng chiếu</h2>
              <p class="text-sm font-medium text-white/50 mt-1">
                {{ selectedCinema?.halls.find(h => h.id === selectedShowtime?.roomId)?.name || selectedShowtime?.roomId }} • {{ selectedShowtime?.movie }}
              </p>
            </div>
            <button @click="showSeatMapModal = false" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all border border-white/10">
              <span class="material-symbols-outlined text-white/70">close</span>
            </button>
          </div>
          
          <!-- Modal Body (SeatMapBuilder in ReadOnly Mode) -->
          <div class="flex-1 overflow-hidden relative">
             <SeatMapBuilder 
               :rows="selectedCinema?.halls.find(h => h.id === selectedShowtime?.roomId)?.rows || 10" 
               :cols="selectedCinema?.halls.find(h => h.id === selectedShowtime?.roomId)?.cols || 16"
               :initialMap="{}"
               :readonly="true"
               :soldTickets="getSoldTickets(selectedShowtime?.movie)"
               :canceledTickets="selectedShowtime?.movie?.includes('DORAEMON') ? 2 : 5"
               :revenue="selectedShowtime?.movie?.includes('DORAEMON') ? '4.275.000đ' : '16.800.000đ'"
             />
          </div>
        </div>
      </div>
    </Transition>

    <!-- Add Showtime Drawer -->
    <ShowtimeDrawer 
      :is-open="showAddShowtimeDrawer" 
      :cinema-id="selectedCinema?.id"
      :selected-date="selectedDate"
      @close="showAddShowtimeDrawer = false" 
      @saved="fetchCinemas" 
    />
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes slide-in-from-bottom {
  from {
    transform: translateY(20px);
  }
  to {
    transform: translateY(0);
  }
}
@keyframes slide-in-from-right {
  from {
    transform: translateX(20px);
  }
  to {
    transform: translateX(0);
  }
}
@keyframes slide-in-from-left {
  from {
    transform: translateX(-20px);
  }
  to {
    transform: translateX(0);
  }
}

.animate-in {
  animation-duration: 0.5s;
  animation-fill-mode: both;
}
.fade-in {
  animation-name: fade-in;
}
.slide-in-from-bottom-8 {
  animation-name: slide-in-from-bottom;
}
.slide-in-from-right-8 {
  animation-name: slide-in-from-right;
}
.slide-in-from-left-4 {
  animation-name: slide-in-from-left;
}

.scrollbar-hide::-webkit-scrollbar {
  display: none;
}

/* Accordion transition */
.accordion-body {
  overflow: hidden;
}

.accordion-enter-active,
.accordion-leave-active {
  transition: max-height 0.35s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.3s ease;
  max-height: 1000px;
  opacity: 1;
}

.accordion-enter-from,
.accordion-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
