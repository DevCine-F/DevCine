import { ref, reactive } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";

export function useCinemas() {
  const toast = useToastStore();
  const API_BASE_URL = "/v1/cinemas";
  
  const cinemas = ref([]);
  const selectedCinema = ref(null);
  
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

  const fetchCinemas = async () => {
    try {
      const res = await axios.get(API_BASE_URL);
      if (res.data && res.data.length > 0) {
        const cinemaList = res.data;
        const enrichedCinemas = await Promise.all(cinemaList.map(async (c) => {
          // Fetch rooms for this cinema
          let halls = [];
          try {
              const roomsRes = await axios.get(`/rooms/cinema/${c.id}`);
              halls = roomsRes.data.map(r => ({
                  id: r.id,
                  name: r.name,
                  type: r.type,
                  rows: r.matrixRow || 0,
                  cols: r.matrixCol || 0,
                  status: r.status,
                  turnaroundTimeMins: r.turnaroundTimeMins
              }));
          } catch(e) { console.error(e); }
          
          // Fetch shows for this cinema
          let shows = [];
          try {
              const showsRes = await axios.get(`/showtimes/cinema/${c.id}`);
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

          // Fetch active staff roster for this cinema
          let staff = [];
          try {
              const staffRes = await axios.get(`/staff/cinema-roster/${c.id}`);
              staff = staffRes.data;
          } catch(e) { console.error("Error fetching staff roster:", e); }

          return {
            ...c,
            cleaningTime: 15,
            stats: {
              revenue: (Math.random() * 500 + 300).toFixed(0) + ".000.000đ",
              admissions: Math.floor(Math.random() * 5000 + 5000).toLocaleString("vi-VN"),
              occupancy: (Math.random() * 20 + 70).toFixed(0) + "%",
              facility: (Math.random() * 5 > 4) ? "95% Active" : "100% Active",
            },
            halls: halls,
            shows: shows,
            staff: staff,
            inventory: []
          };
        }));
        cinemas.value = enrichedCinemas;
        
        // If a cinema is currently selected, update it
        if (selectedCinema.value) {
            selectedCinema.value = enrichedCinemas.find(c => String(c.id) === String(selectedCinema.value.id)) || null;
        }
      }
    } catch (error) {
      console.error("Error fetching cinemas:", error);
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
        stats: { revenue: "0đ", occupancy: "0%", growth: "+0%", admissions: "0", facility: "100%" },
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
      await fetchCinemas();   // refresh halls của rạp đang chọn
    } catch (error) {
      console.error("Error saving room:", error);
      toast.error(friendlyError(error, "Lưu phòng thất bại."));
    }
  };

  const deleteRoom = async (hall) => {
    try {
      await axios.delete(`/rooms/${hall.id}`);
      toast.success(`Đã xoá phòng "${hall.name}"`);
      await fetchCinemas();
    } catch (error) {
      console.error("Error deleting room:", error);
      toast.error(friendlyError(error, "Xoá phòng thất bại."));
    }
  };

  return {
    cinemas,
    selectedCinema,
    fetchCinemas,
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
