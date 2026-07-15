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
    banners: false,
  });

  const toggleSection = (key) => {
    openSections[key] = !openSections[key];
  };

  const configSaving = reactive({
    basic: false,
    hours: false,
    cleaning: false,
    seats: false,
    formats: false,
    banners: false,
  });

  const configSuccess = reactive({
    basic: false,
    hours: false,
    cleaning: false,
    seats: false,
    formats: false,
    banners: false,
  });

  const configBasic = reactive({
    name: "",
    address: "",
    phone: "",
    email: "",
    openTime: "08:00",
  });

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

  const allFormats = ["2D", "3D", "IMAX", "4DX", "Dolby", "ScreenX"];
  const configFormats = reactive({
    supported: ["2D", "3D"],
  });

  const configBanners = reactive({
    banners: [],
    newBannerUrl: "",
    newBannerTitle: "",
  });

  const loadConfigForCinema = (cinema) => {
    configBasic.name = cinema.name || "";
    configBasic.address = cinema.address || "";
    configBasic.phone = cinema.phone || "";
    configBasic.email = cinema.email || "";
    configBasic.openTime = cinema.openTime || "08:00";
    configHours.openTime = cinema.openTime || "08:00";
    configHours.closeTime = cinema.closeTime || "23:30";
    configHours.holidays = cinema.holidays || "";
    configCleaning.cleaningMinutes = cinema.cleaningMinutes || 20;
    if (cinema.supportedFormats) configFormats.supported = [...cinema.supportedFormats];
    if (cinema.banners) configBanners.banners = [...cinema.banners];
  };

  const showConfigSuccess = (section) => {
    configSuccess[section] = true;
    setTimeout(() => { configSuccess[section] = false; }, 2500);
  };

  const saveConfigBasic = async () => {
    if (!selectedCinema.value) return;
    configSaving.basic = true;
    try {
      await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/basic`, {
        name: configBasic.name,
        address: configBasic.address,
        phone: configBasic.phone,
        email: configBasic.email,
        openTime: configBasic.openTime,
      });
      selectedCinema.value.name = configBasic.name;
    } catch (e) {
      console.warn("[Config] Endpoint /config/basic chưa có ở backend:", e?.response?.status);
    } finally {
      showConfigSuccess("basic");
      configSaving.basic = false;
    }
  };

  const saveConfigHours = async () => {
    if (!selectedCinema.value) return;
    configSaving.hours = true;
    try {
      await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/hours`, {
        openTime: configHours.openTime,
        closeTime: configHours.closeTime,
        holidays: configHours.holidays,
      });
    } catch (e) {
      console.warn("[Config] Endpoint /config/hours chưa có ở backend:", e?.response?.status);
    } finally {
      showConfigSuccess("hours");
      configSaving.hours = false;
    }
  };

  const saveConfigCleaning = async (tempCleaningTimeRef) => {
    if (!selectedCinema.value) return;
    configSaving.cleaning = true;
    try {
      await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/cleaning`, {
        cleaningMinutes: configCleaning.cleaningMinutes,
      });
      if (tempCleaningTimeRef) tempCleaningTimeRef.value = configCleaning.cleaningMinutes;
    } catch (e) {
      console.warn("[Config] Endpoint /config/cleaning chưa có ở backend:", e?.response?.status);
    } finally {
      showConfigSuccess("cleaning");
      configSaving.cleaning = false;
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

  const saveConfigFormats = async () => {
    if (!selectedCinema.value) return;
    configSaving.formats = true;
    try {
      await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/formats`, {
        supportedFormats: configFormats.supported,
      });
    } catch (e) {
      console.warn("[Config] Endpoint /config/formats chưa có ở backend:", e?.response?.status);
    } finally {
      showConfigSuccess("formats");
      configSaving.formats = false;
    }
  };

  const toggleFormat = (fmt) => {
    const idx = configFormats.supported.indexOf(fmt);
    if (idx === -1) configFormats.supported.push(fmt);
    else configFormats.supported.splice(idx, 1);
  };

  const addBanner = () => {
    if (!configBanners.newBannerUrl.trim()) return;
    configBanners.banners.push({
      id: Date.now(),
      url: configBanners.newBannerUrl,
      title: configBanners.newBannerTitle || "Banner",
    });
    configBanners.newBannerUrl = "";
    configBanners.newBannerTitle = "";
  };

  const removeBanner = (id) => {
    configBanners.banners = configBanners.banners.filter((b) => b.id !== id);
  };

  const saveConfigBanners = async () => {
    if (!selectedCinema.value) return;
    configSaving.banners = true;
    try {
      await axios.put(`${API_BASE_URL}/${selectedCinema.value.id}/config/banners`, {
        banners: configBanners.banners,
      });
    } catch (e) {
      console.warn("[Config] Endpoint /config/banners chưa có ở backend:", e?.response?.status);
    } finally {
      showConfigSuccess("banners");
      configSaving.banners = false;
    }
  };

  loadSeatTypes(); // nạp loại ghế thật ngay khi mở panel cấu hình

  return {
    openSections,
    toggleSection,
    configSaving,
    configSuccess,
    configBasic,
    configHours,
    configCleaning,
    configSeats,
    loadSeatTypes,
    configFormats,
    configBanners,
    allFormats,
    loadConfigForCinema,
    saveConfigBasic,
    saveConfigHours,
    saveConfigCleaning,
    saveConfigSeats,
    saveConfigFormats,
    toggleFormat,
    addBanner,
    removeBanner,
    saveConfigBanners
  };
}
