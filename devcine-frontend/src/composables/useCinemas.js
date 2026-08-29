import { ref, reactive } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";

export function useCinemas() {
  const toast = useToastStore();
  const API_BASE_URL = "/v1/cinemas";

  const cinemas = ref([]);
  const selectedCinema = ref(null);
  const isLoadingDetail = ref(false);

  const showCreateModal = ref(false);
  const newCinema = reactive({
    name: '',
    address: '',
    city: '',
    district: '',
    hotline: '',
    type: 'STANDARD',
    imageUrl: '',
    description: '',
    status: 'ACTIVE'
  });

  // Chuẩn hoá một phòng chiếu từ API -> cấu trúc card.
  const mapHall = (r) => ({
    id: r.id,
    name: r.name,
    type: r.type,
    rows: r.matrixRow || 0,
    cols: r.matrixCol || 0,
    // Sức chứa thật từ BE (ghế đôi=2, bỏ lối đi/ghế xóa) — KHÔNG dùng rows*cols vì phép nhân lưới sai.
    seatCount: r.seatCount ?? 0,
    status: r.status,
    turnaroundTimeMins: r.turnaroundTimeMins
  });

  // Chuẩn hoá một suất chiếu từ API -> cấu trúc card timeline.
  // openMin = giờ mở cửa của rạp (phút). Suất có giờ đồng hồ < openMin được quy về
  // "ngày vận hành" HÔM TRƯỚC (thói quen điều phối: suất 00:30 thuộc đêm hôm trước).
  const mapShow = (s, openMin = 480) => {
    const st = s.startTime;
    const pad = (n) => n.toString().padStart(2, '0');
    let startTimeStr = "00:00";
    let fullDateTimeStr = "";
    let y = 1970, mo = 1, d = 1;
    if (Array.isArray(st)) {
      // [year, month, day, hour, minute]
      startTimeStr = `${pad(st[3])}:${pad(st[4])}`;
      y = st[0]; mo = st[1]; d = st[2];
      fullDateTimeStr = `${st[0]}-${pad(st[1])}-${pad(st[2])}T${startTimeStr}:00`;
    } else if (typeof st === 'string') {
      // "2026-06-11T13:00:00" -> "13:00"
      startTimeStr = st.substring(11, 16);
      const parts = st.split('T')[0].split('-');
      y = Number(parts[0]); mo = Number(parts[1]); d = Number(parts[2]);
      fullDateTimeStr = st;
    }
    // Ngày HIỂN THỊ trên timeline: lùi 1 ngày nếu suất khuya (giờ < giờ mở cửa).
    const [sh, sm] = startTimeStr.split(':').map(Number);
    const dateObj = new Date(y, mo - 1, d);
    if (sh * 60 + sm < openMin) dateObj.setDate(dateObj.getDate() - 1);
    const dateStr = `${pad(dateObj.getDate())}/${pad(dateObj.getMonth() + 1)}`;
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
  };

  // Số liệu thống kê hiện là mock (chưa nối API báo cáo) — giữ nguyên hành vi cũ.
  const defaultStats = () => ({
    revenue: (Math.random() * 500 + 300).toFixed(0) + ".000.000đ",
    admissions: Math.floor(Math.random() * 5000 + 5000).toLocaleString("vi-VN"),
    occupancy: (Math.random() * 20 + 70).toFixed(0) + "%",
    facility: (Math.random() * 5 > 4) ? "95% Active" : "100% Active"
  });

  // Trang DANH SÁCH: chỉ nạp thông tin cụm rạp (1 request). Trước đây nạp thêm
  // rooms + shows + staff cho MỌI rạp -> N+1 phía client (1 + 3N request) khiến
  // trang tải lâu. Chi tiết (halls/shows/staff) nạp LƯỜI khi mở từng rạp.
  const fetchCinemas = async () => {
    try {
      const res = await axios.get(API_BASE_URL, { params: { all: true } });
      const list = Array.isArray(res.data) ? res.data : [];
      // Giữ lại chi tiết đã nạp (nếu có) để không mất dữ liệu rạp đang mở khi refresh list.
      const prev = new Map(cinemas.value.map(c => [String(c.id), c]));
      cinemas.value = list.map(c => {
        const old = prev.get(String(c.id));
        // Không đặt halls khi chưa nạp -> card fallback về c.rooms cho đúng số phòng.
        return old
          ? { ...c, stats: old.stats, halls: old.halls, shows: old.shows, staff: old.staff, inventory: old.inventory }
          : { ...c };
      });
      if (selectedCinema.value) {
        const base = cinemas.value.find(c => String(c.id) === String(selectedCinema.value.id));
        selectedCinema.value = base || null;
      }
    } catch (error) {
      console.error("Error fetching cinemas:", error);
    }
  };

  // Nạp CHI TIẾT một cụm rạp (phòng chiếu + suất chiếu) tối ưu không chặn UI.
  const loadCinemaDetail = async (cinema) => {
    if (!cinema) return;
    // Hiện khung chi tiết ngay (rỗng) để cảm giác nhanh, rồi nạp dần.
    selectedCinema.value = {
      ...cinema,
      stats: cinema.stats ?? defaultStats(),
      halls: cinema.halls ?? [],
      shows: cinema.shows ?? [],
      staff: cinema.staff ?? [],
      inventory: cinema.inventory ?? []
    };
    isLoadingDetail.value = true;
    try {
      // 1. Nạp danh sách phòng để Tab "Phòng chiếu" hiển thị ngay lập tức
      const roomsRes = await axios.get(`/rooms/cinema/${cinema.id}`).catch(e => { console.error(e); return { data: [] }; });
      const sortedHalls = (roomsRes.data || []).map(mapHall).sort((a, b) =>
        (a.name || '').localeCompare(b.name || '', undefined, { numeric: true, sensitivity: 'base' })
      );

      selectedCinema.value = {
        ...selectedCinema.value,
        halls: sortedHalls
      };
      isLoadingDetail.value = false; // Giao diện sẵn sàng ngay lập tức!

      // 2. Nạp song song Suất chiếu (đã tối ưu DTO Projection + Cache < 15ms)
      const [oh, om] = (cinema.openingTime || "08:00").split(":").map(Number);
      const openMin = (oh || 8) * 60 + (om || 0);
      const showsRes = await axios.get(`/showtimes/cinema/${cinema.id}`).catch(e => { console.error(e); return { data: [] }; });

      const enriched = {
        ...selectedCinema.value,
        shows: (showsRes.data || []).map((s) => mapShow(s, openMin))
      };

      // Đồng bộ vào list để số phòng trên card đúng, và giữ chi tiết cho refresh sau.
      const idx = cinemas.value.findIndex(c => String(c.id) === String(cinema.id));
      if (idx !== -1) cinemas.value[idx] = enriched;
      selectedCinema.value = enriched;
    } finally {
      isLoadingDetail.value = false;
    }
  };

  const handleCreateCinema = async () => {
    // Validate/format đã xử lý ở CreateCinemaModal; tại đây chỉ chuẩn hoá payload gửi BE.
    try {
      const payload = {
        name: newCinema.name.trim().replace(/\s+/g, ' '),
        address: newCinema.address.trim().replace(/\s+/g, ' '),
        type: newCinema.type,
        hotline: (newCinema.hotline || '').replace(/\D/g, ''),   // RAW: chỉ chữ số
        city: newCinema.city.trim(),
        district: newCinema.district.trim(),
        imageUrl: (newCinema.imageUrl || '').trim() || null,
        description: (newCinema.description || '').trim() || null,
        status: newCinema.status || "ACTIVE"
      };

      const res = await axios.post(API_BASE_URL, payload);

      // Bổ sung field UI mà BE chưa trả (để khớp cấu trúc card trong CinemaManager)
      const savedCinema = {
        ...res.data,
        stats: defaultStats(),
        halls: [],
        staff: [],
        inventory: [],
        shows: []
      };
      cinemas.value.push(savedCinema);
      toast.success(`Đã tạo cụm rạp "${payload.name}"`);

      // Reset form
      newCinema.name = '';
      newCinema.address = '';
      newCinema.city = '';
      newCinema.district = '';
      newCinema.hotline = '';
      newCinema.type = 'Standard';
      newCinema.imageUrl = '';
      newCinema.description = '';
      newCinema.status = 'ACTIVE';
      showCreateModal.value = false;
    } catch (error) {
      console.error("Error creating cinema:", error);
      toast.error(friendlyError(error, "Lỗi khi thêm cụm rạp mới!"));
    }
  };

  // ===== Quản lý Phòng chiếu (CRUD) =====
  const showRoomModal = ref(false);
  const roomModalMode = ref('create');   // 'create' | 'edit'
  const editingRoom = ref(null);

  const openAddRoom = () => { roomModalMode.value = 'create'; editingRoom.value = null; showRoomModal.value = true; };
  const openEditRoom = (hall) => { 
    roomModalMode.value = 'edit'; 
    const hasShows = selectedCinema.value?.shows?.some(s => s.roomId === hall.id) || false;
    editingRoom.value = { ...hall, hasShowtimes: hasShows }; 
    showRoomModal.value = true; 
  };

  const submitRoom = async (payload) => {
    if (!selectedCinema.value) return;
    const { onSuccess, onError, ...roomData } = payload;
    try {
      if (roomModalMode.value === 'edit' && editingRoom.value) {
        await axios.put(`/rooms/${editingRoom.value.id}`, roomData);
        toast.success(`Đã cập nhật phòng "${roomData.name}"`);
      } else {
        await axios.post(`/rooms/cinema/${selectedCinema.value.id}`, roomData);
        toast.success(`Đã thêm phòng "${roomData.name}"`);
      }
      showRoomModal.value = false;
      await loadCinemaDetail(selectedCinema.value);   // refresh chi tiết rạp đang chọn
      if (onSuccess) onSuccess();
    } catch (error) {
      console.error("Error saving room:", error);
      toast.error(friendlyError(error, "Lưu phòng thất bại."));
      if (onError) onError();
    }
  };

  const deleteRoom = async (hall) => {
    try {
      await axios.delete(`/rooms/${hall.id}`);
      toast.success(`Đã xoá phòng "${hall.name}"`);
      await loadCinemaDetail(selectedCinema.value);
    } catch (error) {
      console.error("Error deleting room:", error);
      toast.error(friendlyError(error, "Xoá phòng thất bại."));
    }
  };

  return {
    cinemas,
    selectedCinema,
    isLoadingDetail,
    fetchCinemas,
    loadCinemaDetail,
    showCreateModal,
    newCinema,
    handleCreateCinema,
    // Phòng chiếu
    showRoomModal,
    roomModalMode,
    editingRoom,
    openAddRoom,
    openEditRoom,
    submitRoom,
    deleteRoom
  };
}
