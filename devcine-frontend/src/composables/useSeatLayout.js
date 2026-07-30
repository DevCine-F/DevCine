import { ref } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";

/**
 * State + I/O cho Trình thiết kế sơ đồ ghế.
 * UI chỉnh sửa nằm 100% ở SeatMapBuilder.vue (công cụ standard/vip/double/aisle/maintenance/remove
 * + sửa label thủ công). Composable này chỉ giữ state phòng đang xem và luồng tải/lưu.
 */
export function useSeatLayout() {
  const toast = useToastStore();
  const viewingHall = ref(null);
  const currentSeatMap = ref({});
  const tempRows = ref(10);
  const tempCols = ref(16);
  const isSavingLayout = ref(false);

  const initializeSeatMap = () => {
    const map = {};
    for (let r = 0; r < tempRows.value; r++) {
      for (let c = 0; c < tempCols.value; c++) {
        map[`${r}-${c}`] = {
          type: "standard",
          label: `${String.fromCharCode(65 + r)}${c + 1}`,
          status: "AVAILABLE",
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
      if (res.data?.seats?.length > 0) {
        const data = res.data;
        tempRows.value = data.matrixRow || hall.rows;
        tempCols.value = data.matrixCol || hall.cols;

        const map = {};
        data.seats.forEach((seat) => {
          let seatType = "standard";
          if (seat.seatType === "VIP") seatType = "vip";
          if (seat.seatType === "SWEETBOX") seatType = "double";

          map[`${seat.gridRow}-${seat.gridCol}`] = {
            type: seatType,
            // Ưu tiên label lưu sẵn (có thể đã được Admin sửa tay); fallback về rowChar+colNum
            label: seat.label || `${seat.rowChar}${seat.colNum}`,
            // seatStatus = trạng thái vật lý (AVAILABLE/MAINTENANCE/LOCKED); status runtime bị bỏ qua ở builder
            status: seat.seatStatus || "AVAILABLE",
            custom: !!seat.label,
          };
        });
        currentSeatMap.value = map;
        viewingHall.value = hall;
        return;
      }
    } catch (error) {
      console.error("Failed to load saved seat layout, initializing default:", error);
    }

    initializeSeatMap();
    viewingHall.value = hall;
  };

  const resetMap = () => {
    if (!viewingHall.value) return;
    initializeSeatMap();
  };

  const saveSeatLayout = async () => {
    if (!viewingHall.value) return;

    isSavingLayout.value = true;
    try {
      const seatsList = [];
      Object.entries(currentSeatMap.value).forEach(([key, seatData]) => {
        // Lối đi (aisle), ô ẩn (hidden) và ô đã xóa (absent) KHÔNG phải ghế → không lưu
        if (!seatData || !seatData.type || seatData.type === "hidden" || seatData.type === "aisle") {
          return;
        }

        const [gridRowStr, gridColStr] = key.split("-");
        const gridRow = parseInt(gridRowStr);
        const gridCol = parseInt(gridColStr);

        // Tách rowChar/colNum từ label (custom hoặc auto). Vẫn giữ 2 trường này cho tương thích
        // (email, gom nhóm loại ghế...), nhưng label mới là nguồn hiển thị chuẩn.
        let rowChar = "";
        let colNum = 1;
        if (seatData.label) {
          const match = seatData.label.match(/^([A-Za-z]+)(\d+)$/);
          if (match) {
            rowChar = match[1].toUpperCase();
            colNum = parseInt(match[2]);
          } else {
            rowChar = String.fromCharCode(65 + gridRow);
            colNum = gridCol + 1;
          }
        }

        seatsList.push({
          rowChar,
          colNum,
          gridRow,
          gridCol,
          type: seatData.type,
          label: seatData.label,
          status: seatData.status || "AVAILABLE",
        });
      });

      const payload = {
        matrixRow: tempRows.value,
        matrixCol: tempCols.value,
        seats: seatsList,
      };

      await axios.post(`/seats/layout/${viewingHall.value.id}`, payload);
      toast.success("Lưu cấu trúc ghế thành công!");
    } catch (error) {
      console.error("Error saving seat layout:", error);
      toast.error(friendlyError(error, "Có lỗi xảy ra khi lưu cấu trúc ghế."));
    } finally {
      isSavingLayout.value = false;
    }
  };

  return {
    viewingHall,
    currentSeatMap,
    tempRows,
    tempCols,
    isSavingLayout,
    openHallDetail,
    resetMap,
    saveSeatLayout,
  };
}
