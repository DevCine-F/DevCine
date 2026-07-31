import { reactive } from "vue";
import axios from "@/api/axios";

export function useCinemaConfig(selectedCinema) {
  const API_BASE_URL = "/v1/cinemas";

  const openSections = reactive({
    basic: false,
    hours: false,
    cleaning: false,
    seats: false,
    formats: false,
  });

  const toggleSection = (key) => {
    openSections[key] = !openSections[key];
  };

  const configSaving = reactive({
    basic: false,
    hours: false,
    seats: false,
  });

  const configSuccess = reactive({
    basic: false,
    hours: false,
    seats: false,
  });

  // Chỉ Mục 1 (Thông tin cơ bản) còn ghi thật -> giữ 4 trường khớp updateCinema.
  const configBasic = reactive({
    name: "",
    address: "",
    hotline: "",
    description: "",
  });
  const configError = reactive({ basic: "", hours: "" }); // thông báo lỗi lưu (hiển thị inline, không giả "Đã lưu")

  const configHours = reactive({
    openTime: "08:00",
    closeTime: "23:30",
    holidays: "",
  });

  const configCleaning = reactive({
    cleaningMinutes: 20,
  });

  // Loại ghế là dữ liệu TOÀN CỤC (bảng seat_types), nạp thật từ /pricing/config.
  // `surcharge` = phụ thu cộng thêm vào giá nền (seat_types.price_modifier), KHÔNG phải giá trọn gói.
  const configSeats = reactive({ types: [] });

  // Nạp loại ghế thật từ backend (dùng chung endpoint với màn Bảng giá).
  const loadSeatTypes = async () => {
    try {
      const { data } = await axios.get("/pricing/config");
      const list = data?.seatTypes || [];
      configSeats.types = list.map((s) => ({
        id: s.id,
        name: s.name,
        color: s.colorCode || "#6B7280",
        surcharge: Number(s.priceModifier) || 0,
      }));
    } catch (e) {
      console.warn("[Config] Không tải được loại ghế từ /pricing/config:", e?.response?.status);
    }
  };

  // Lotte chỉ khai thác 2D/3D (không có IMAX/4DX/ScreenX — độc quyền CGV).
  const allFormats = ["2D", "3D"];
  const configFormats = reactive({
    supported: ["2D", "3D"],
  });

  const loadConfigForCinema = (cinema) => {
    // Mục 1 (ghi thật): 4 trường khớp updateCinema.
    configBasic.name = cinema.name || "";
    configBasic.address = cinema.address || "";
    configBasic.hotline = cinema.hotline || "";
    configBasic.description = cinema.description || "";
    configError.basic = "";
    configError.hours = "";
    // Mục 2 (Giờ hoạt động): GHI THẬT — đọc openingTime/closingTime từ entity Cinema.
    configHours.openTime = cinema.openingTime || "08:00";
    configHours.closeTime = cinema.closingTime || "23:30";
    // Ngày nghỉ lễ vẫn là thông tin tĩnh tham khảo (chưa ghi DB).
    configHours.holidays = cinema.holidays || "01/01\n30/04\n01/05\n02/09";
    configCleaning.cleaningMinutes = cinema.cleaningMinutes || 20;
    if (cinema.supportedFormats) configFormats.supported = [...cinema.supportedFormats];
  };

  const showConfigSuccess = (section) => {
    configSuccess[section] = true;
    setTimeout(() => { configSuccess[section] = false; }, 2500);
  };

  // Mục 1: ghi THẬT qua PUT /api/v1/cinemas/{id} (updateCinema). updateCinema ghi đè toàn bộ
  // nên phải gửi kèm các trường không sửa (city/district/type/status...) để không mất dữ liệu.
  const saveConfigBasic = async () => {
    const c = selectedCinema.value;
    if (!c) return;
    configSaving.basic = true;
    configError.basic = "";
    try {
      await axios.put(`${API_BASE_URL}/${c.id}`, {
        name: configBasic.name,
        address: configBasic.address,
        description: configBasic.description || null,
        hotline: (configBasic.hotline || "").replace(/\D/g, ""), // API yêu cầu chuỗi số thuần
        city: c.city,
        district: c.district,
        type: c.type,
        status: c.status,
        rooms: c.rooms,
        latitude: c.latitude,
        longitude: c.longitude,
        amenities: c.amenities,
        managerId: c.managerId ?? null,
        imageUrl: null, // null = giữ nguyên ảnh hiện có (updateCinema chỉ ghi khi != null)
      });
      // Đồng bộ vào object đang chọn để danh sách/thẻ phản ánh ngay
      c.name = configBasic.name;
      c.address = configBasic.address;
      c.hotline = configBasic.hotline;
      c.description = configBasic.description;
      showConfigSuccess("basic");
    } catch (e) {
      configError.basic = e?.response?.data?.message
        || "Lưu thất bại — kiểm tra Tên (5–100 ký tự), Địa chỉ (≥10 ký tự), Hotline (8–11 số).";
      console.error("[Config] Lưu thông tin cơ bản thất bại:", e);
    } finally {
      configSaving.basic = false;
    }
  };

  // Mục 2: ghi THẬT giờ hoạt động qua PUT /api/v1/cinemas/{id}. updateCinema ghi đè các trường
  // cơ bản nên phải gửi kèm để không mất dữ liệu (giống saveConfigBasic).
  const saveConfigHours = async () => {
    const c = selectedCinema.value;
    if (!c) return;
    configError.hours = "";
    if (!configHours.openTime || !configHours.closeTime) {
      configError.hours = "Vui lòng nhập đủ giờ mở và giờ đóng cửa.";
      return;
    }
    if (configHours.openTime === configHours.closeTime) {
      configError.hours = "Giờ mở và giờ đóng cửa không được trùng nhau.";
      return;
    }
    configSaving.hours = true;
    try {
      await axios.put(`${API_BASE_URL}/${c.id}`, {
        name: c.name,
        address: c.address,
        hotline: (c.hotline || "").replace(/\D/g, ""),
        city: c.city,
        district: c.district,
        type: c.type,
        status: c.status,
        rooms: c.rooms,
        latitude: c.latitude,
        longitude: c.longitude,
        amenities: c.amenities,
        managerId: c.managerId ?? null,
        imageUrl: null, // null = giữ nguyên ảnh hiện có
        openingTime: configHours.openTime,
        closingTime: configHours.closeTime,
      });
      // Đồng bộ vào cụm rạp đang chọn để TIMELINE co giãn lại ngay lập tức.
      c.openingTime = configHours.openTime;
      c.closingTime = configHours.closeTime;
      showConfigSuccess("hours");
    } catch (e) {
      configError.hours = e?.response?.data?.message
        || "Lưu giờ hoạt động thất bại — kiểm tra định dạng HH:mm.";
      console.error("[Config] Lưu giờ hoạt động thất bại:", e);
    } finally {
      configSaving.hours = false;
    }
  };

  // Lưu tên + màu + phụ thu loại ghế THẬT vào seat_types qua /pricing/seat-types.
  // (Loại ghế là cấu hình toàn cục nên không gắn theo selectedCinema.)
  const saveConfigSeats = async () => {
    configSaving.seats = true;
    try {
      await axios.put("/pricing/seat-types", {
        items: configSeats.types.map((t) => ({
          id: t.id,
          name: t.name,
          colorCode: t.color,
          priceModifier: Number(t.surcharge) || 0,
        })),
      });
      showConfigSuccess("seats"); // chỉ báo thành công khi lưu thật sự OK
    } catch (e) {
      console.error("[Config] Lưu phụ thu loại ghế thất bại:", e?.response?.status, e);
    } finally {
      configSaving.seats = false;
    }
  };

  loadSeatTypes(); // nạp loại ghế thật ngay khi mở panel cấu hình

  return {
    openSections,
    toggleSection,
    configSaving,
    configSuccess,
    configError,
    configBasic,
    configHours,
    configCleaning,
    configSeats,
    loadSeatTypes,
    configFormats,
    allFormats,
    loadConfigForCinema,
    saveConfigBasic,
    saveConfigHours,
    saveConfigSeats,
  };
}
