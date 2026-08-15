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
  // Dirty check: chỉ cho phép Lưu khi sơ đồ thực sự có thay đổi so với lần tải/lưu gần nhất
  const hasChanges = ref(false);

  // Đánh dấu có thay đổi (gọi từ mọi thao tác Admin làm biến đổi cấu trúc)
  const markDirty = () => { hasChanges.value = true; };

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
          // Bỏ ô NGOÀI khung matrix (ghế "ma" sót lại từ layout cũ lớn hơn): Designer chỉ vẽ
          // tempRows×tempCols nên ô ngoài khung vô hình mà vẫn bị lưu lại → mismatch với POS/Booking.
          // Trim ngay khi nạp để map luôn khớp lưới hiển thị.
          if (seat.gridRow >= tempRows.value || seat.gridCol >= tempCols.value) return;

          // Ô lối đi được BE lưu tường minh (không còn tự đoán) → dựng lại đúng khung
          if (seat.kind === "AISLE") {
            map[`${seat.gridRow}-${seat.gridCol}`] = { type: "aisle" };
            return;
          }

          let seatType = "standard";
          if (seat.seatType === "VIP") seatType = "vip";
          if (seat.seatType === "SWEETBOX") seatType = "double";

          map[`${seat.gridRow}-${seat.gridCol}`] = {
            type: seatType,
            // Ưu tiên label lưu sẵn (có thể đã được Admin sửa tay); fallback về rowChar+colNum
            label: seat.label || `${seat.rowChar}${seat.colNum}`,
            // seatStatus = trạng thái vật lý (AVAILABLE/MAINTENANCE/LOCKED); status runtime bị bỏ qua ở builder
            status: seat.seatStatus || "AVAILABLE",
            // Chỉ chốt cứng khi Admin thực sự gõ tay (cờ custom lưu ở DB), KHÔNG suy từ việc có label
            custom: !!seat.custom,
          };
        });
        currentSeatMap.value = map;
        viewingHall.value = hall;
        hasChanges.value = false; // vừa tải từ DB → coi như chưa có thay đổi
        return;
      }
    } catch (error) {
      console.error("Failed to load saved seat layout, initializing default:", error);
    }

    initializeSeatMap();
    viewingHall.value = hall;
    hasChanges.value = false; // baseline cho phòng mới/chưa có sơ đồ
  };

  const resetMap = () => {
    if (!viewingHall.value) return;
    initializeSeatMap();
    hasChanges.value = true; // "Đặt lại" làm sơ đồ khác bản đã lưu → cho phép Lưu
  };

  const saveSeatLayout = async () => {
    if (!viewingHall.value) return;

    isSavingLayout.value = true;
    try {
      const seatsList = [];
      Object.entries(currentSeatMap.value).forEach(([key, seatData]) => {
        // Ô ẩn (hidden) và ô đã xóa (absent) KHÔNG được lưu. Lối đi (aisle) NAY ĐƯỢC lưu tường minh.
        if (!seatData || !seatData.type || seatData.type === "hidden") {
          return;
        }

        const [gridRowStr, gridColStr] = key.split("-");
        const gridRow = parseInt(gridRowStr);
        const gridCol = parseInt(gridColStr);

        // Chốt chặn: KHÔNG gửi ô ngoài khung matrix hiện tại (ghế "ma"). Nếu không, thu nhỏ
        // ROWS/COLS mà vẫn gửi ô cũ → BE giữ chúng active → Designer cắt còn POS/Booking phơi hết.
        if (gridRow >= tempRows.value || gridCol >= tempCols.value) return;

        // Lối đi: chỉ cần vị trí lưới + cờ kind; BE tự điền placeholder cho các cột NOT NULL
        if (seatData.type === "aisle") {
          seatsList.push({ gridRow, gridCol, kind: "AISLE", type: "aisle" });
          return;
        }

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
          kind: "SEAT",
          type: seatData.type,
          label: seatData.label,
          status: seatData.status || "AVAILABLE",
          custom: !!seatData.custom,
        });
      });

      const payload = {
        matrixRow: tempRows.value,
        matrixCol: tempCols.value,
        seats: seatsList,
      };

      await axios.post(`/seats/layout/${viewingHall.value.id}`, payload);
      hasChanges.value = false; // lưu thành công → hết thay đổi, khóa lại nút Lưu
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
    hasChanges,
    markDirty,
    openHallDetail,
    resetMap,
    saveSeatLayout,
  };
}
