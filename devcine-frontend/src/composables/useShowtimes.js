import { ref, computed } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";

export function useShowtimes(selectedCinema) {
  const toast = useToastStore();
  const draggedShow = ref(null);
  
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
    const CLEANING_TIME = selectedCinema.value.cleaningTime || 20;

    const showStart = timeToMinutes(show.startTime);
    const showEnd = showStart + Number(show.duration) + CLEANING_TIME;

    return hallShows.some((other) => {
      const otherStart = timeToMinutes(other.startTime);
      const otherEnd = otherStart + Number(other.duration) + CLEANING_TIME;
      return showStart < otherEnd && showEnd > otherStart;
    });
  };

  const checkFormatMismatch = (hall, format) => {
    // Lotte chỉ có 2D/3D — chiếu được ở mọi hạng phòng nên không còn ràng buộc định dạng ↔ phòng.
    return false;
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
    } catch (err) {
      console.error(err);
      toast.error(friendlyError(err, "Có lỗi xảy ra khi lưu lịch chiếu!"));
    }
  };

  return {
    dates,
    selectedDate,
    isToday,
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
