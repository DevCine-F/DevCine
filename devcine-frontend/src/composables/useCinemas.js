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
    city: 'Hồ Chí Minh',
    hotline: '',
    type: 'Standard',
    rooms: 1,
    imageUrl: '',
    description: '',
    latitude: null,
    longitude: null,
    amenities: '',
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
                  rows: 10, // Mocked for now since DB doesn't have rows/cols
                  cols: 16,
                  status: r.status
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
            staff: [],
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
    if (!newCinema.name || !newCinema.address || !newCinema.rooms) {
      toast.warning("Vui lòng điền đầy đủ các thông tin bắt buộc!");
      return;
    }
    
    try {
      const res = await axios.post(API_BASE_URL, {
        name: newCinema.name,
        address: newCinema.address,
        type: newCinema.type,
        hotline: newCinema.hotline,
        rooms: newCinema.rooms,
        city: newCinema.city || "Hồ Chí Minh",
        imageUrl: newCinema.imageUrl || null,
        description: newCinema.description || null,
        latitude: newCinema.latitude,
        longitude: newCinema.longitude,
        amenities: newCinema.amenities || null,
        status: newCinema.status || "ACTIVE"
      });
      
      // Add missing mock properties for UI since BE doesn't have them yet
      const savedCinema = {
        ...res.data,
        stats: {
          revenue: "0đ",
          occupancy: "0%",
          growth: "+0%",
          admissions: "0",
          facility: "100%"
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
      newCinema.city = 'Hồ Chí Minh';
      newCinema.hotline = '';
      newCinema.type = 'Standard';
      newCinema.rooms = 1;
      newCinema.imageUrl = '';
      newCinema.description = '';
      newCinema.latitude = null;
      newCinema.longitude = null;
      newCinema.amenities = '';
      newCinema.status = 'ACTIVE';
      showCreateModal.value = false;
    } catch (error) {
      console.error("Error creating cinema:", error);
      toast.error(friendlyError(error, "Lỗi khi thêm cụm rạp mới!"));
    }
  };

  return {
    cinemas,
    selectedCinema,
    fetchCinemas,
    showCreateModal,
    newCinema,
    handleCreateCinema
  };
}
