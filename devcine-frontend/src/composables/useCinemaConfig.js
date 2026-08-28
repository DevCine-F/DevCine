import { reactive, ref, computed, nextTick } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { useConfirmStore } from "@/stores/confirm";
import { friendlyError } from "@/utils/friendlyError";
import {
  collapseSpaces,
  rawDigits,
  formatHotline,
  normalizeAmenities,
  validateName,
  validateHotline,
  validateAddress,
  validateCity,
  validateDistrict,
  validateDescription,
  validateLatitude,
  validateLongitude,
} from "@/utils/cinemaValidators";

// Toàn bộ cấu hình một cụm rạp giờ nằm trong MỘT form + MỘT lần lưu (1 PUT).
// Lý do: updateCinema (PUT /api/v1/cinemas/{id}) ghi đè toàn bộ entity, nên gộp
// mọi trường vào một payload là cách tự nhiên nhất — thay cho accordion nhiều nút Lưu.
export function useCinemaConfig(selectedCinema) {
  const API_BASE_URL = "/v1/cinemas";
  const toast = useToastStore();
  const confirm = useConfirmStore();

  // Trạng thái CÒN BÁN VÉ (chưa đóng cửa) — khớp isSellable() phía backend.
  const isSellableStatus = (s) => !s || s.toUpperCase() === "ACTIVE";

  // ===== Model form (12 trường editable, khớp CinemaRequest) =====
  const form = reactive({
    name: "",
    address: "",
    city: "",
    district: "",
    hotline: "",
    type: "STANDARD",
    amenities: "",
    description: "",
    status: "ACTIVE",
    openingTime: "08:00",
    closingTime: "23:30",
    latitude: "",
    longitude: "",
  });

  // Các trường KHÔNG sửa nhưng updateCinema ghi đè -> phải gửi kèm để không mất dữ liệu.
  // Giữ ngoài `form` để không lọt vào so sánh isDirty.
  const preserved = reactive({ managerId: null, rooms: null });

  const errors = reactive({ name: "", address: "", city: "", district: "", hotline: "", description: "", latitude: "", longitude: "" });

  const original = ref({}); // snapshot đã chuẩn hoá, chụp sau mỗi lần load/save
  const provinces = ref([]);
  const districts = ref([]);
  const loadingDistricts = ref(false);
  const saving = ref(false);
  const closingCinema = ref(false);
  const reopeningCinema = ref(false);

  // ===== Chuẩn hoá để so sánh isDirty (không nhiễu bởi format hiển thị) =====
  const normalize = (f) => ({
    name: collapseSpaces(f.name),
    address: collapseSpaces(f.address),
    city: (f.city || "").trim(),
    district: (f.district || "").trim(),
    hotline: rawDigits(f.hotline),
    type: f.type || "STANDARD",
    amenities: normalizeAmenities(f.amenities),
    description: (f.description || "").trim(),
    status: f.status || "ACTIVE",
    openingTime: f.openingTime || "",
    closingTime: f.closingTime || "",
    latitude: f.latitude === "" || f.latitude == null ? null : Number(f.latitude),
    longitude: f.longitude === "" || f.longitude == null ? null : Number(f.longitude),
  });

  const isDirty = computed(() => JSON.stringify(normalize(form)) !== JSON.stringify(original.value));
  const hasErrors = computed(() => Object.values(errors).some(Boolean));

  // ===== Validate =====
  const runField = {
    name: () => (errors.name = validateName(form.name)),
    hotline: () => (errors.hotline = validateHotline(form.hotline)),
    address: () => (errors.address = validateAddress(form.address)),
    city: () => (errors.city = validateCity(form.city)),
    district: () => (errors.district = validateDistrict(form.district)),
    description: () => (errors.description = validateDescription(form.description)),
    latitude: () => (errors.latitude = validateLatitude(form.latitude)),
    longitude: () => (errors.longitude = validateLongitude(form.longitude)),
  };
  const validateField = (f) => {
    if (runField[f]) runField[f]();
    return !errors[f];
  };
  const validateAll = () => {
    Object.keys(runField).forEach((f) => runField[f]());
    return !hasErrors.value;
  };
  const clearErrors = () => Object.keys(errors).forEach((k) => (errors[k] = ""));

  // ===== Danh mục Tỉnh/Thành & Quận/Huyện =====
  const fetchProvinces = async () => {
    if (provinces.value.length) return; // nạp 1 lần / vòng đời composable
    try {
      const { data } = await axios.get("/locations/provinces");
      provinces.value = data || [];
    } catch (e) {
      toast.error(friendlyError(e, "Không tải được danh sách Tỉnh/Thành."));
    }
  };
  const fetchDistricts = async (province) => {
    if (!province) { districts.value = []; return; }
    loadingDistricts.value = true;
    try {
      const { data } = await axios.get("/locations/districts", { params: { province } });
      districts.value = data || [];
    } catch (e) {
      toast.error(friendlyError(e, "Không tải được danh sách Quận/Huyện."));
      districts.value = [];
    } finally {
      loadingDistricts.value = false;
    }
  };

  // Đổi Tỉnh/Thành DO NGƯỜI DÙNG -> reset quận cũ + nạp lại quận/huyện.
  // suppressCityWatch chặn reset nhầm khi loadConfig gán city (giữ nguyên district đã lưu).
  let suppressCityWatch = false;

  const loadConfig = (cinema) => {
    if (!cinema) return;
    suppressCityWatch = true;
    form.name = cinema.name || "";
    form.address = cinema.address || "";
    form.city = cinema.city || "";
    form.district = cinema.district || "";
    form.hotline = formatHotline(cinema.hotline || "");
    form.type = cinema.type || "STANDARD";
    form.amenities = cinema.amenities || "";
    form.description = cinema.description || "";
    form.status = cinema.status || "ACTIVE";
    form.openingTime = cinema.openingTime || "08:00";
    form.closingTime = cinema.closingTime || "23:30";

    form.latitude = cinema.latitude ?? "";
    form.longitude = cinema.longitude ?? "";
    preserved.managerId = cinema.managerId ?? null;
    preserved.rooms = cinema.rooms ?? null;

    original.value = normalize(form);
    clearErrors();
    fetchProvinces();
    if (form.city) fetchDistricts(form.city);
    nextTick(() => { suppressCityWatch = false; });
  };

  // Gọi từ watch(form.city) trong component.
  const onCityChange = (val, old) => {
    if (suppressCityWatch || val === old) return;
    form.district = "";
    errors.district = "";
    fetchDistricts(val);
  };

  // Hoàn tác: nạp lại từ cụm rạp đang chọn (state cuối đã lưu/đã load).
  const resetForm = () => loadConfig(selectedCinema.value);

  // ===== Lưu cấu hình: 1 PUT gộp toàn bộ =====
  const saveConfig = async () => {
    const c = selectedCinema.value;
    if (!c) return;
    if (!validateAll()) {
      toast.error("Vui lòng kiểm tra lại các trường được tô đỏ.");
      return;
    }
    if (!form.openingTime || !form.closingTime) {
      toast.error("Vui lòng nhập đủ giờ mở cửa và giờ bắt đầu suất cuối.");
      return;
    }

    const n = normalize(form);
    saving.value = true;
    try {
      const payload = {
        name: n.name,
        address: n.address,
        city: n.city,
        district: n.district,
        hotline: n.hotline,
        type: n.type,
        amenities: n.amenities || null,
        description: n.description || null,
        status: n.status,
        openingTime: form.openingTime,
        closingTime: form.closingTime,
        latitude: n.latitude,
        longitude: n.longitude,
        managerId: preserved.managerId,
        rooms: preserved.rooms,
      };
      await axios.put(`${API_BASE_URL}/${c.id}`, payload);

      Object.assign(c, {
        name: n.name,
        address: n.address,
        city: n.city,
        district: n.district,
        hotline: n.hotline,
        type: n.type,
        amenities: n.amenities,
        description: n.description,
        status: n.status,
        openingTime: form.openingTime,
        closingTime: form.closingTime,
        latitude: n.latitude,
        longitude: n.longitude,
      });

      original.value = normalize(form);
      toast.success("Đã lưu cấu hình cụm rạp.");
    } catch (e) {
      console.error("[Config] Lưu cấu hình thất bại:", e);
      toast.error(
        friendlyError(
          e,
          "Lưu thất bại — kiểm tra Tên (5–100), Địa chỉ (≥10), Hotline (8–11 số), giờ HH:mm."
        )
      );
    } finally {
      saving.value = false;
    }
  };
  const closeCinema = async () => {
    const c = selectedCinema.value;
    if (!c) return false;

    const ok = await confirm.show({
      title: "Đóng cụm rạp",
      message: `Đóng và ẩn cụm rạp "${c.name}" khỏi hệ thống khách hàng?`,
      confirmText: "Đóng cụm rạp",
      cancelText: "Hủy",
      tone: "danger",
    });
    if (!ok) return false;

    closingCinema.value = true;
    try {
      const { data } = await axios.patch(`${API_BASE_URL}/${c.id}/close`);
      const updated = data?.data || data;
      c.status = "CLOSED";
      form.status = "CLOSED";
      original.value = normalize(form);
      toast.success(`Đã đóng cụm rạp "${c.name}" và ẩn khỏi hệ thống client.`);
      return true;
    } catch (e) {
      console.error("[Config] Đóng cụm rạp thất bại:", e);
      toast.error(
        friendlyError(
          e,
          "Không thể đóng cụm rạp do còn suất chiếu chưa kết thúc."
        )
      );
      return false;
    } finally {
      closingCinema.value = false;
    }
  };

  // ===== Mở lại cụm rạp =====
  const reopenCinema = async () => {
    const c = selectedCinema.value;
    if (!c) return false;

    const ok = await confirm.show({
      title: "Mở lại cụm rạp",
      message: `Mở lại và hiển thị cụm rạp "${c.name}" cho khách hàng?`,
      confirmText: "Mở lại rạp",
      cancelText: "Hủy",
      tone: "primary",
    });
    if (!ok) return false;

    reopeningCinema.value = true;
    try {
      const { data } = await axios.patch(`${API_BASE_URL}/${c.id}/reopen`);
      const updated = data?.data || data;
      c.status = "ACTIVE";
      form.status = "ACTIVE";
      original.value = normalize(form);
      toast.success(`Đã mở lại cụm rạp "${c.name}" thành công.`);
      return true;
    } catch (e) {
      console.error("[Config] Mở lại cụm rạp thất bại:", e);
      toast.error(friendlyError(e, "Mở lại cụm rạp thất bại."));
      return false;
    } finally {
      reopeningCinema.value = false;
    }
  };

  return {
    // state
    form,
    errors,
    provinces,
    districts,
    loadingDistricts,
    saving,
    closingCinema,
    reopeningCinema,
    isDirty,
    hasErrors,
    // actions
    loadConfig,
    onCityChange,
    resetForm,
    validateField,
    saveConfig,
    closeCinema,
    reopenCinema,
  };
}
