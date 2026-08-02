import { ref, computed, onScopeDispose } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";

// Bề rộng mỗi ô 15 phút trên timeline (px) — dùng tính min-width vùng cuộn.
export const PX_PER_COL = 34;

export function useShowtimes(selectedCinema) {
  const toast = useToastStore();
  const draggedShow = ref(null);

  const parseHM = (s, fallback) => {
    if (!s || typeof s !== "string") return fallback;
    const [h, m] = s.split(":").map(Number);
    if (Number.isNaN(h) || Number.isNaN(m)) return fallback;
    return h * 60 + m;
  };

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

  // ===== Cửa sổ giờ hoạt động động (co giãn theo cụm rạp) =====
  // openMin/closeMin theo phút; nếu đóng ≤ mở ⇒ qua nửa đêm (closeMin += 1440).
  // endMin = max(closeMin, kết thúc suất muộn nhất trong ngày) làm tròn lên giờ ⇒ suất khuya luôn render trọn.
  const gridWindow = computed(() => {
    const c = selectedCinema.value;
    const openMin = parseHM(c?.openingTime, 8 * 60);
    let closeMin = parseHM(c?.closingTime, 23 * 60 + 30);
    if (closeMin <= openMin) closeMin += 1440;

    let endMin = closeMin;
    const shows = (c?.shows || []).filter((s) => s.date === selectedDate.value);
    for (const s of shows) {
      const hall = c?.halls?.find((h) => h.id === s.roomId);
      const turn = Number(hall?.turnaroundTimeMins ?? 15);
      let startPos = parseHM(s.startTime, openMin);
      if (startPos < openMin) startPos += 1440; // suất khuya thuộc phần đuôi timeline
      const e = startPos + Number(s.duration) + turn;
      if (e > endMin) endMin = e;
    }
    endMin = Math.ceil(endMin / 60) * 60; // làm tròn mép phải lên giờ
    const cols = Math.max(4, Math.ceil((endMin - openMin) / 15));
    return { openMin, closeMin, endMin, cols };
  });

  const gridCols = computed(() => gridWindow.value.cols);

  // Nhãn giờ trên thước — đặt theo % trái, bền vững kể cả giờ mở lệch 30'.
  const hourMarks = computed(() => {
    const { openMin, endMin } = gridWindow.value;
    const span = endMin - openMin || 1;
    const marks = [];
    const firstHour = Math.ceil(openMin / 60) * 60;
    for (let m = firstHour; m <= endMin; m += 60) {
      marks.push({
        label: `${String(Math.floor((m / 60) % 24)).padStart(2, "0")}:00`,
        leftPct: `${((m - openMin) / span) * 100}%`,
      });
    }
    return marks;
  });

  const getGridStyle = (startTime, duration) => {
    const { openMin } = gridWindow.value;
    let startPos = parseHM(startTime, openMin);
    if (startPos < openMin) startPos += 1440; // suất sau nửa đêm → phần đuôi
    const startUnit = Math.round((startPos - openMin) / 15) + 1;
    const spanUnit = Math.ceil(duration / 15);
    return {
      gridColumnStart: startUnit,
      gridColumnEnd: `span ${spanUnit}`,
      gridRow: '1',
    };
  };

  // ===== Chỉ báo "thời gian hiện tại" theo cửa sổ động =====
  const clockNow = () => { const d = new Date(); return d.getHours() * 60 + d.getMinutes(); };
  const nowMin = ref(clockNow());
  const nowTimer = setInterval(() => { nowMin.value = clockNow(); }, 60000);
  onScopeDispose(() => clearInterval(nowTimer));

  const showNowIndicator = computed(() => {
    if (!isToday.value) return false;
    const { openMin, endMin } = gridWindow.value;
    let n = nowMin.value;
    if (n < openMin) n += 1440;
    return n >= openMin && n <= endMin;
  });

  const currentTimeLeft = computed(() => {
    const { openMin, endMin } = gridWindow.value;
    let n = nowMin.value;
    if (n < openMin) n += 1440;
    const span = endMin - openMin || 1;
    return `${Math.min(100, Math.max(0, ((n - openMin) / span) * 100))}%`;
  });

  const getEndTime = (startTime, duration = 120) => {
    const [hour, minute] = startTime.split(":").map(Number);
    const totalMinutes = hour * 60 + minute + Number(duration);
    const endHourRaw = Math.floor(totalMinutes / 60);
    const endMin = totalMinutes % 60;
    const endHour = endHourRaw >= 24 ? endHourRaw - 24 : endHourRaw;
    return `${endHour.toString().padStart(2, "0")}:${endMin.toString().padStart(2, "0")}`;
  };

  const timeToMinutes = (time) => {
    const [h, m] = time.split(":").map(Number);
    return h * 60 + m;
  };

  const checkConflict = (hallId, show) => {
    if (!selectedCinema.value) return false;
    const hallShows = selectedCinema.value.shows.filter(
      (s) => s.roomId === hallId && s.date === selectedDate.value && s.id !== show.id,
    );
    // NGUỒN DUY NHẤT: thời gian dọn dẹp bốc từ chính phòng (turnaroundTimeMins), khớp với backend.
    const hall = selectedCinema.value.halls?.find((h) => h.id === hallId);
    const CLEANING_TIME = Number(hall?.turnaroundTimeMins ?? 15);

    const showStart = timeToMinutes(show.startTime);
    const showEnd = showStart + Number(show.duration) + CLEANING_TIME;

    return hallShows.some((other) => {
      const otherStart = timeToMinutes(other.startTime);
      const otherEnd = otherStart + Number(other.duration) + CLEANING_TIME;
      return showStart < otherEnd && showEnd > otherStart;
    });
  };

  const checkFormatMismatch = (hall, format) => {
    if (!hall || !format) return false;
    const roomType = hall.type?.trim().toUpperCase() || 'STANDARD';
    const fn = (format.name || format).trim().toUpperCase();
    if (roomType === 'SUPERPLEX') return false; // Supports everything
    if (roomType === 'STANDARD' || roomType === 'CINE_COMFORT') {
      const allowed = ['2D PHỤ ĐỀ', '2D LỒNG TIẾNG', '3D PHỤ ĐỀ', '3D LỒNG TIẾNG'];
      return !allowed.includes(fn);
    }
    return true; // Mismatch if room type is unknown
  };

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
      toast.error("Xung đột lịch chiếu hoặc đè lên thời gian dọn dẹp. Vui lòng chọn giờ/phòng khác!");
    }
    
    draggedShow.value = null;
  };

  const handlePublish = async () => {
    if (!selectedCinema.value) return;
    const dirtyShows = selectedCinema.value.shows.filter(s => s.isDirty);
    if (dirtyShows.length === 0) {
      toast.warning("Không có lịch chiếu nào thay đổi để xuất bản!");
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
        
        await axios.patch(`/showtimes/${show.id}`, {
          roomId: show.roomId,
          startTime: localIsoString
        });
        
        show.isDirty = false;
        show.fullDateTime = localIsoString;
      }
      toast.success("Lịch chiếu đã được xuất bản thành công vào Database!");
      window.dispatchEvent(new Event('showtimes-updated'));
    } catch (err) {
      console.error(err);
      toast.error(friendlyError(err, "Có lỗi xảy ra khi lưu lịch chiếu!"));
    }
  };

  return {
    dates,
    selectedDate,
    isToday,
    gridCols,
    hourMarks,
    showNowIndicator,
    currentTimeLeft,
    getGridStyle,
    getEndTime,
    timeToMinutes,
    checkConflict,
    checkFormatMismatch,
    onDragStart,
    onDrop,
    handlePublish
  };
}
