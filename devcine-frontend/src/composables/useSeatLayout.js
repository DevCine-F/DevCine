import { ref } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";

export function useSeatLayout() {
  const toast = useToastStore();
  const viewingHall = ref(null);
  const currentSeatMap = ref({});
  const isMouseDown = ref(false);
  const activeBrush = ref("standard");
  const tempRows = ref(10);
  const tempCols = ref(16);
  const isSavingLayout = ref(false);

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

  const openHallDetail = async (hall) => {
    tempRows.value = hall.rows || 10;
    tempCols.value = hall.cols || 16;
    
    try {
      const res = await axios.get(`/seats/room/${hall.id}?t=${Date.now()}`);
      if (res.data && res.data.seats && res.data.seats.length > 0) {
        tempRows.value = res.data.matrixRow || hall.rows;
        tempCols.value = res.data.matrixCol || hall.cols;
        
        const map = {};
        
        // Override with actual fetched seats
        res.data.seats.forEach(seat => {
          let seatType = "standard";
          if (seat.seatType === "VIP") seatType = "vip";
          if (seat.seatType === "SWEETBOX") seatType = "double";
          if (seat.seatType === "PAVE") seatType = "aisle";

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

      await axios.post(`/seats/layout/${viewingHall.value.id}`, payload);
      toast.success('Lưu cấu trúc ghế thành công!');
    } catch (error) {
      console.error('Error saving seat layout:', error);
      toast.error(friendlyError(error, 'Có lỗi xảy ra khi lưu cấu trúc ghế.'));
    } finally {
      isSavingLayout.value = false;
    }
  };

  const toggleSeat = (r, c) => {
    const key = `${r}-${c}`;
    const nextKey = `${r}-${c + 1}`;
    const prevKey = `${r}-${c - 1}`;

    if (activeBrush.value === "sweetbox") {
      if (c < viewingHall.value.cols - 1) {
        currentSeatMap.value[key].type = "sweetbox";
        currentSeatMap.value[nextKey].type = "hidden";
      }
      return;
    }

    if (currentSeatMap.value[key]?.type === "sweetbox") {
      if (currentSeatMap.value[nextKey])
        currentSeatMap.value[nextKey].type = "standard";
    }

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

  return {
    viewingHall,
    currentSeatMap,
    isMouseDown,
    activeBrush,
    tempRows,
    tempCols,
    isSavingLayout,
    seatBrushes,
    openHallDetail,
    regenerateGrid,
    resetMap,
    saveSeatLayout,
    toggleSeat
  };
}
