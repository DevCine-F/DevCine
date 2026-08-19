import { reactive, ref, computed, nextTick } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
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
  validateImageUrl,
  validateLatitude,
  validateLongitude,
} from "@/utils/cinemaValidators";

// Toàn bộ cấu hình một cụm rạp giờ nằm trong MỘT form + MỘT lần lưu (1 PUT).
// Lý do: updateCinema (PUT /api/v1/cinemas/{id}) ghi đè toàn bộ entity, nên gộp
// mọi trường vào một payload là cách tự nhiên nhất — thay cho accordion nhiều nút Lưu.
export function useCinemaConfig(selectedCinema) {
  const API_BASE_URL = "/v1/cinemas";
  const toast = useToastStore();

  // ===== Model form (12 trường editable, khớp CinemaRequest) =====
  const form = reactive({
    name: "",
    address: "",
    city: "",
    district: "",
    hotline: "",
    imageUrl: "",
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

  const errors = reactive({ name: "", address: "", city: "", district: "", hotline: "", imageUrl: "", description: "", latitude: "", longitude: "" });

  const original = ref({}); // snapshot đã chuẩn hoá, chụp sau mỗi lần load/save
  const provinces = ref([]);
  const districts = ref([]);
  const loadingDistricts = ref(false);
  const saving = ref(false);
  const deleting = ref(false);

  // ===== Chuẩn hoá để so sánh isDirty (không nhiễu bởi format hiển thị) =====
  const normalize = (f) => ({
    name: collapseSpaces(f.name),
    address: collapseSpaces(f.address),
    city: (f.city || "").trim(),
    district: (f.district || "").trim(),
    hotline: rawDigits(f.hotline),
    imageUrl: (f.imageUrl || "").trim(),
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
    // Chỉ validate ảnh khi user THỰC SỰ đổi (nhất quán với "chỉ gửi imageUrl khi đổi").
    // Ảnh seed cũ có thể không khớp regex đuôi ảnh nhưng sẽ được gửi null (giữ nguyên) -> không chặn lưu.
    imageUrl: () => {
      const changed = (form.imageUrl || "").trim() !== (original.value.imageUrl || "");
      errors.imageUrl = changed ? validateImageUrl(form.imageUrl) : "";
    },
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
    form.imageUrl = cinema.imageUrl || "";
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
      toast.error("Vui lòng nhập đủ giờ mở và giờ đóng cửa.");
      return;
    }
    const n = normalize(form);
    saving.value = true;
    try {
      // imageUrl: CHỈ gửi khi người dùng thật sự đổi ảnh. Nếu không đổi -> null (giữ nguyên),
      // tránh để backend re-validate giá trị ảnh cũ (có thể là link seed không khớp regex đuôi .jpg/...).
      const imageChanged = n.imageUrl !== (original.value.imageUrl || "");
      const payload = {
        name: n.name,
        address: n.address,
        city: n.city,
        district: n.district,
        hotline: n.hotline, // API yêu cầu chuỗi số thuần
        type: n.type,
        amenities: n.amenities || null,
        description: n.description || null,
        status: n.status,
        // null = GIỮ ảnh hiện có (updateCinema chỉ ghi khi != null); không hỗ trợ xoá ảnh về rỗng.
        imageUrl: imageChanged && n.imageUrl ? n.imageUrl : null,
        openingTime: form.openingTime,
        closingTime: form.closingTime,
        latitude: n.latitude,
        longitude: n.longitude,
        managerId: preserved.managerId,
        rooms: preserved.rooms,
      };
      await axios.put(`${API_BASE_URL}/${c.id}`, payload);

      // Đồng bộ ngược vào cụm rạp đang chọn để card/list & timeline phản ánh ngay.
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
      if (n.imageUrl) c.imageUrl = n.imageUrl;

      original.value = normalize(form); // reset dirty
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

  // ===== Xoá cứng cụm rạp (Danger Zone) =====
  // Trả về true nếu xoá thành công (để component emit 'deleted'). BE là HARD DELETE:
  // rạp còn phòng/suất chiếu -> vỡ ràng buộc khoá ngoại -> hướng dẫn dùng "Tạm đóng".
  const deleteCinema = async () => {
    const c = selectedCinema.value;
    if (!c) return false;
    deleting.value = true;
    try {
      await axios.delete(`${API_BASE_URL}/${c.id}`);
      toast.success(`Đã xoá cụm rạp "${c.name}".`);
      return true;
    } catch (e) {
      console.error("[Config] Xoá cụm rạp thất bại:", e);
      const status = e?.response?.status;
      const msg = (e?.response?.data?.message || "") + "";
      const looksFk =
        status === 409 ||
        status === 500 ||
        /constraint|foreign|violat|referen|ràng buộc|liên kết|đang có/i.test(msg);
      if (looksFk) {
        toast.error(
          "Không thể xoá cụm rạp này. Nguyên nhân thường gặp: rạp vẫn còn phòng chiếu " +
            'hoặc suất chiếu liên kết. Hãy cuộn lên phần "Trạng thái" và đổi sang ' +
            '"Tạm đóng cửa" để ngừng hoạt động mà không mất dữ liệu.'
        );
      } else {
        toast.error(friendlyError(e, "Xoá cụm rạp thất bại."));
      }
      return false;
    } finally {
      deleting.value = false;
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
    deleting,
    isDirty,
    hasErrors,
    // actions
    loadConfig,
    onCityChange,
    resetForm,
    validateField,
    saveConfig,
    deleteCinema,
  };
}
