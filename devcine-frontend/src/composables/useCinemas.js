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
    type: 'Standard',
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
    status: r.status,
    turnaroundTimeMins: r.turnaroundTimeMins
  });

  // Chuẩn hoá một suất chiếu từ API -> cấu trúc card timeline.
  const mapShow = (s) => {
    const st = s.startTime;
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
      const res = await axios.get(API_BASE_URL);
      const list = Array.isArray(res.data) ? res.data : [];
      // Giữ lại chi tiết đã nạp (nếu có) để không mất dữ liệu rạp đang mở khi refresh list.
      const prev = new Map(cinemas.value.map(c => [String(c.id), c]));
      cinemas.value = list.map(c => {
        const old = prev.get(String(c.id));
        // Không đặt halls khi chưa nạp -> card fallback về c.rooms cho đúng số phòng.
        return old
          ? { ...c, cleaningTime: old.cleaningTime, stats: old.stats, halls: old.halls, shows: old.shows, staff: old.staff, inventory: old.inventory }
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

  // Nạp CHI TIẾT một cụm rạp (rooms + shows + staff) song song — chỉ khi mở rạp.
  const loadCinemaDetail = async (cinema) => {
    if (!cinema) return;
    // Hiện khung chi tiết ngay (rỗng) để cảm giác nhanh, rồi nạp dần.
    selectedCinema.value = {
      ...cinema,
      cleaningTime: cinema.cleaningTime ?? 15,
      stats: cinema.stats ?? defaultStats(),
      halls: cinema.halls ?? [],
      shows: cinema.shows ?? [],
      staff: cinema.staff ?? [],
      inventory: cinema.inventory ?? []
    };
    isLoadingDetail.value = true;
    try {
      const [roomsRes, showsRes, staffRes] = await Promise.all([
        axios.get(`/rooms/cinema/${cinema.id}`).catch(e => { console.error(e); return { data: [] }; }),
        axios.get(`/showtimes/cinema/${cinema.id}`).catch(e => { console.error(e); return { data: [] }; }),
        axios.get(`/staff/cinema-roster/${cinema.id}`).catch(e => { console.error("Error fetching staff roster:", e); return { data: [] }; })
      ]);
      const enriched = {
        ...selectedCinema.value,
        halls: roomsRes.data.map(mapHall),
        shows: showsRes.data.map(mapShow),
        staff: staffRes.data
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
  const openEditRoom = (hall) => { roomModalMode.value = 'edit'; editingRoom.value = hall; showRoomModal.value = true; };

  const submitRoom = async (payload) => {
    if (!selectedCinema.value) return;
    try {
      if (roomModalMode.value === 'edit' && editingRoom.value) {
        await axios.put(`/rooms/${editingRoom.value.id}`, payload);
        toast.success(`Đã cập nhật phòng "${payload.name}"`);
      } else {
        await axios.post(`/rooms/cinema/${selectedCinema.value.id}`, payload);
        toast.success(`Đã thêm phòng "${payload.name}"`);
      }
      showRoomModal.value = false;
      await loadCinemaDetail(selectedCinema.value);   // refresh chi tiết rạp đang chọn
    } catch (error) {
      console.error("Error saving room:", error);
      toast.error(friendlyError(error, "Lưu phòng thất bại."));
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
