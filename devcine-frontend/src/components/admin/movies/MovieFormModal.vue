<script setup>
import { ref, computed, watch } from "vue";
import axios from "@/api/axios";
import { useToastStore } from "@/stores/toast";
import { friendlyError } from "@/utils/friendlyError";
import { validateImageFile, compressImage } from "@/utils/imageUpload";

const toast = useToastStore();

const props = defineProps({
  open: { type: Boolean, default: false },
  isEditing: { type: Boolean, default: false },
  movieData: { type: Object, default: null }, // chi tiết phim khi sửa
  genres: { type: Array, default: () => [] },
  availableFormats: { type: Array, default: () => [] },
  availableAgeRatings: { type: Array, default: () => [] },
});

const emit = defineEmits(["close", "save"]);

const blankMovie = () => ({
  title: "",
  duration: "",
  country: "Mỹ",
  productionYear: new Date().getFullYear().toString(),
  language: "Phụ đề Tiếng Việt",
  status: "upcoming",
  rating: "5.0",
  ageRating: "P",
  originalLanguage: "Tiếng Anh",
  versionType: "Phụ đề Tiếng Việt",
  releaseDate: "",
  startDate: "",
  endDate: "",
  description: "",
  posterUrl: "",
  bannerUrl: "",
  showOnBanner: true,
  trailerUrl: "",
  format: "2D",
  internalNotes: "",
  director: "",
  castMembers: "",
  ratingCount: 0,
});

const newMovie = ref(blankMovie());
const selectedGenres = ref([]);
const selectedFormats = ref([]);

// Khởi tạo form mỗi khi mở
watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return;
    errors.value = {};
    if (props.isEditing && props.movieData) {
      const m = props.movieData;
      newMovie.value = { ...m, duration: m.durationMins ? m.durationMins.toString() : "" };
      selectedGenres.value = m.genres ? [...m.genres] : [];
      selectedFormats.value = m.supportedFormats ? m.supportedFormats.split(", ") : [];
    } else {
      newMovie.value = blankMovie();
      selectedGenres.value = [];
      selectedFormats.value = [];
    }
  },
);

const optimizeCloudinaryUrl = (url) => {
  if (!url) return "";
  if (url.includes("cloudinary.com") && url.includes("/upload/") && !url.includes("f_auto")) {
    return url.replace("/upload/", "/upload/f_auto,q_auto/");
  }
  return url;
};

// ===== Upload ảnh =====
// Poster/Banner phim chỉ nhận ảnh tĩnh — CHẶN GIF (ảnh động làm nặng & xấu banner trang chủ).
const MOVIE_IMAGE_TYPES = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
// Guard riêng cho GIF: validateImageFile báo chung "…WEBP, GIF" nên chặn trước để thông điệp đúng.
const validateMovieImage = (file, maxMB = 5) => {
  if (file.type === "image/gif") throw new Error("Form phim không nhận ảnh GIF (ảnh động). Chỉ dùng JPG, PNG, WEBP.");
  validateImageFile(file, { types: MOVIE_IMAGE_TYPES, maxMB });
};
const posterInput = ref(null);
const bannerInput = ref(null);
const isUploadingPoster = ref(false);
const isUploadingBanner = ref(false);

const triggerPosterInput = () => {
  if (!isUploadingPoster.value) posterInput.value.click();
};
const triggerBannerInput = () => bannerInput.value.click();

const uploadImage = async (file) => {
  const prepared = await compressImage(file); // nén/thu nhỏ trước khi upload
  const formData = new FormData();
  formData.append("file", prepared);
  const response = await axios.post("/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data.url;
};

const onPosterChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  try { validateMovieImage(file); } catch (err) { toast.error(err.message); e.target.value = ""; return; }
  const oldUrl = newMovie.value.posterUrl;
  newMovie.value.posterUrl = URL.createObjectURL(file);
  isUploadingPoster.value = true;
  try {
    newMovie.value.posterUrl = await uploadImage(file);
  } catch (error) {
    console.error("Lỗi upload ảnh:", error);
    toast.error(friendlyError(error, "Tải ảnh Poster lên Cloudinary thất bại!"));
    newMovie.value.posterUrl = oldUrl;
  } finally {
    isUploadingPoster.value = false;
  }
};

const onBannerChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  try { validateMovieImage(file, 8); } catch (err) { toast.error(err.message); e.target.value = ""; return; } // banner cho tối đa 8MB
  const oldUrl = newMovie.value.bannerUrl;
  newMovie.value.bannerUrl = URL.createObjectURL(file);
  isUploadingBanner.value = true;
  try {
    newMovie.value.bannerUrl = await uploadImage(file);
  } catch (error) {
    console.error("Lỗi upload ảnh:", error);
    toast.error(friendlyError(error, "Tải ảnh Banner lên Cloudinary thất bại!"));
    newMovie.value.bannerUrl = oldUrl;
  } finally {
    isUploadingBanner.value = false;
  }
};

// ===== Chọn thể loại / định dạng =====
const toggleGenre = (genre) => {
  const index = selectedGenres.value.findIndex((g) => g.id === genre.id);
  index === -1 ? selectedGenres.value.push(genre) : selectedGenres.value.splice(index, 1);
  clearErr("genres");
};
const resetGenres = () => (selectedGenres.value = []);
const toggleFormat = (format) => {
  const index = selectedFormats.value.indexOf(format);
  if (index === -1) {
    selectedFormats.value.push(format);
    // Chưa có định dạng chiếu chính hợp lệ -> gán luôn cái vừa tick.
    if (!selectedFormats.value.includes(newMovie.value.format)) newMovie.value.format = format;
  } else {
    selectedFormats.value.splice(index, 1);
    // Bỏ tick định dạng đang là "chiếu chính" -> tự chọn lại cái đầu còn lại (hoặc rỗng).
    if (newMovie.value.format === format) newMovie.value.format = selectedFormats.value[0] || "";
  }
  clearErr("formats");
};
// Định dạng chiếu chính CHỈ được chọn trong số định dạng hỗ trợ (ràng buộc chéo).
const selectMainFormat = (fmt) => {
  if (selectedFormats.value.includes(fmt)) { newMovie.value.format = fmt; clearErr("formats"); }
};

// Đạo diễn: chỉ cho chữ (kể cả có dấu), khoảng trắng, dấu chấm & gạch nối; chặn số/ký tự đặc biệt; tối đa 100.
const onDirectorInput = (e) => {
  const clean = e.target.value.replace(/[^\p{L}\s.\-]/gu, "").slice(0, 100);
  newMovie.value.director = clean;
  e.target.value = clean;
};

const genreContainer = ref(null);
const scrollLeft = () => genreContainer.value?.scrollBy({ left: -200, behavior: "smooth" });
const scrollRight = () => genreContainer.value?.scrollBy({ left: 200, behavior: "smooth" });
const handleWheel = (e) => {
  if (!genreContainer.value) return;
  if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
    e.preventDefault();
    genreContainer.value.scrollLeft += e.deltaY;
  }
};

// ===== Format tự động khi gõ (đồng bộ phân hệ Voucher) =====
// Thời lượng: số nguyên, tối đa 3 chữ số (≤ 400).
const onDurationInput = (e) => {
  const digits = e.target.value.replace(/\D/g, "").slice(0, 3);
  newMovie.value.duration = digits;
  e.target.value = digits;
  clearErr("duration");
};
// Năm sản xuất: đúng 4 chữ số.
const onYearInput = (e) => {
  const digits = e.target.value.replace(/\D/g, "").slice(0, 4);
  newMovie.value.productionYear = digits;
  e.target.value = digits;
  clearErr("productionYear");
};

// ===== Validate theo thời gian thực (viền đỏ + khóa nút Xuất bản, không tự sửa dữ liệu) =====
const todayStr = computed(() => new Date().toLocaleDateString("en-CA")); // yyyy-mm-dd theo giờ máy
const YOUTUBE_RE = /^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be)\/.*$/;

const durationError = computed(() => {
  const d = newMovie.value.duration;
  if (d === "" || d == null) return "";
  const n = Number(d);
  if (!Number.isInteger(n) || n < 30 || n > 300) return "Thời lượng phim phải là số nguyên từ 30 đến 300 phút.";
  return "";
});
const yearError = computed(() => {
  const y = newMovie.value.productionYear;
  if (y === "" || y == null) return "";
  const n = Number(y);
  const now = new Date().getFullYear();
  const minYear = now - 5, maxYear = now + 5;
  if (!Number.isInteger(n) || String(y).length !== 4 || n < minYear || n > maxYear)
    return `Năm sản xuất phải gồm 4 chữ số, từ ${minYear} đến ${maxYear}.`;
  return "";
});
const trailerError = computed(() => {
  const t = (newMovie.value.trailerUrl || "").trim();
  if (!t) return "";
  if (!YOUTUBE_RE.test(t)) return "Đường dẫn Trailer không hợp lệ. Vui lòng nhập link từ Youtube.";
  return "";
});
// Tóm tắt nội dung: bắt buộc 50–1000 ký tự (áp dụng cả thêm mới lẫn sửa).
const synopsisError = computed(() => {
  const d = (newMovie.value.description || "").trim();
  if (d.length > 1000) return "Tóm tắt nội dung tối đa 1000 ký tự.";
  if (d.length > 0 && d.length < 50) return "Tóm tắt nội dung phải từ 50 ký tự trở lên.";
  return "";
});
const dateError = computed(() => {
  const { startDate, endDate } = newMovie.value;
  // Ngày khởi chiếu ≥ hôm nay: chỉ áp dụng khi TẠO MỚI (sửa phim đang chiếu giữ nguyên ngày cũ).
  if (!props.isEditing && startDate && startDate < todayStr.value) return "Ngày khởi chiếu phải từ hôm nay trở đi.";
  if (startDate && endDate && endDate <= startDate) return "Ngày kết thúc phải sau ngày khởi chiếu ít nhất 1 ngày.";
  return "";
});

// Gom tất cả điều kiện lỗi → khóa nút Xuất bản/Cập nhật khi bất kỳ trường nào vi phạm.
// Áp dụng ĐỒNG NHẤT cho cả THÊM MỚI lẫn SỬA PHIM: giá trị sai luôn chặn, và các trường
// bắt buộc cũng phải đủ ở cả 2 chế độ (chỉ riêng "ngày khởi chiếu ≥ hôm nay" là create-only).
const isFormInvalid = computed(() => {
  const m = newMovie.value;

  // Giá trị sai định dạng/ngưỡng → luôn chặn.
  if (!m.title || m.title.trim().length < 2 || m.title.trim().length > 150) return true;
  if (!m.trailerUrl || !m.trailerUrl.trim()) return true;
  if (durationError.value || yearError.value || trailerError.value || priceError.value) return true;
  if (dateError.value || synopsisError.value) return true;

  // Bắt buộc điền — áp cho cả tạo mới & sửa.
  if (!m.duration) return true;
  if (!m.productionYear) return true;
  if (selectedGenres.value.length === 0) return true;
  if (selectedFormats.value.length === 0) return true;
  if (!selectedFormats.value.includes(m.format)) return true; // định dạng chính phải nằm trong hỗ trợ
  if (!m.startDate || !m.endDate) return true;
  if (!m.posterUrl) return true; // poster bắt buộc
  if ((m.description || "").trim().length < 50) return true; // tóm tắt ≥ 50 ký tự
  if (m.showOnBanner && !m.bannerUrl) return true; // bật banner trang chủ -> cần ảnh banner
  return false;
});

// ===== Lỗi hiển thị khi bấm Lưu (bổ sung thông báo "bắt buộc" cho trường bỏ trống) =====
const errors = ref({});
const clearErr = (key) => {
  if (errors.value[key]) {
    const e = { ...errors.value };
    delete e[key];
    errors.value = e;
  }
};

// ===== Lưu =====
const handleSave = () => {
  const m = newMovie.value;
  const e = {};

  // Ràng buộc áp dụng ĐỒNG NHẤT cho cả thêm mới & sửa phim.
  // Tên phim: bắt buộc (2–150 ký tự).
  if (!m.title || !m.title.trim()) e.title = "Vui lòng nhập tên phim.";
  else if (m.title.trim().length < 2) e.title = "Tên phim phải từ 2 ký tự.";
  else if (m.title.trim().length > 150) e.title = "Tên phim tối đa 150 ký tự.";

  if (m.duration) { if (durationError.value) e.duration = durationError.value; }
  else e.duration = "Vui lòng nhập thời lượng phim.";

  if (m.productionYear) { if (yearError.value) e.productionYear = yearError.value; }
  else e.productionYear = "Vui lòng nhập năm sản xuất.";

  if ((m.trailerUrl || "").trim()) { if (trailerError.value) e.trailerUrl = trailerError.value; }
  else e.trailerUrl = "Vui lòng nhập đường dẫn Trailer.";

  if (selectedGenres.value.length === 0) e.genres = "Vui lòng chọn ít nhất 1 thể loại phim.";
  if (selectedFormats.value.length === 0) e.formats = "Vui lòng chọn ít nhất 1 định dạng hỗ trợ.";
  else if (!selectedFormats.value.includes(m.format)) e.formats = "Vui lòng chọn Định dạng chiếu chính (nằm trong Định dạng hỗ trợ).";

  // Ngày: bắt buộc + ràng buộc logic (riêng "≥ hôm nay" chỉ áp khi tạo mới, xử lý trong dateError).
  if (!m.startDate) e.dates = "Vui lòng chọn ngày khởi chiếu.";
  else if (!m.endDate) e.dates = "Vui lòng chọn ngày kết thúc (dự kiến).";
  else if (dateError.value) e.dates = dateError.value;

  // Media & mô tả (bắt buộc).
  if (!m.posterUrl) e.poster = "Vui lòng tải lên ảnh Poster.";
  if (m.showOnBanner && !m.bannerUrl) e.banner = "Đang bật hiển thị Banner trang chủ — vui lòng tải ảnh Banner.";
  const desc = (m.description || "").trim();
  if (desc.length === 0) e.description = "Vui lòng nhập tóm tắt nội dung.";
  else if (synopsisError.value) e.description = synopsisError.value;

  errors.value = e;
  if (Object.keys(e).length) {
    toast.warning("Vui lòng kiểm tra lại các trường được tô đỏ.");
    return;
  }
  // Chuẩn hoá diễn viên: cắt khoảng trắng thừa quanh mỗi tên, nối lại bằng ", ".
  const normalizedCast = (newMovie.value.castMembers || "")
    .split(",").map((s) => s.trim()).filter(Boolean).join(", ");
  const payload = {
    ...newMovie.value,
    castMembers: normalizedCast,
    durationMins: parseInt(newMovie.value.duration) || null,
    genres: selectedGenres.value,
    format: newMovie.value.format || "2D",
    supportedFormats: selectedFormats.value.join(", "),
    rating: newMovie.value.rating || "5.0",
    releaseDate: newMovie.value.releaseDate || new Date().toISOString().split("T")[0],
  };
  emit("save", payload, props.isEditing ? newMovie.value.id : null);
};
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/90 backdrop-blur-md" @click="emit('close')"></div>

    <div
      class="bg-surface-container-low w-full max-w-[1400px] rounded-[32px] border border-outline-variant/10 shadow-[0_40px_100px_rgba(0,0,0,0.6)] relative z-10 overflow-hidden grid grid-cols-1 md:grid-cols-3 h-[90vh]"
    >
      <!-- Form (2/3) -->
      <div class="md:col-span-2 p-12 overflow-y-auto custom-scrollbar border-r border-outline-variant/10">
        <header class="mb-8">
          <div class="flex items-center gap-3 mb-2">
            <span class="w-8 h-1 bg-primary rounded-full"></span>
            <h2 class="font-headline font-bold text-2xl uppercase text-on-surface">
              {{ isEditing ? "Chỉnh sửa thông tin phim" : "Thêm phim mới" }}
            </h2>
          </div>
          <p class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
            Thông tin chi tiết nội dung kỹ thuật số
          </p>
        </header>

        <div class="space-y-10">
          <!-- 01. Định danh & Nội dung -->
          <section class="space-y-6">
            <div class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3">
              <span class="text-[10px] font-black uppercase tracking-[0.2em]">01. Định danh & Nội dung</span>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Tên phim <span class="text-red-500">*</span></label>
              <input v-model="newMovie.title" @input="clearErr('title')" type="text" maxlength="150" class="w-full bg-surface-container-high border-b focus:border-primary text-sm py-3 px-6 text-on-surface transition-all outline-none rounded-t-lg" :class="errors.title ? 'border-red-500' : 'border-outline-variant/20'" placeholder="VD: Oppenheimer" />
              <p v-if="errors.title" class="text-[10px] text-red-400 font-bold">{{ errors.title }}</p>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Thời lượng (Phút) <span class="text-red-500">*</span></label>
                <input :value="newMovie.duration" @input="onDurationInput" type="text" inputmode="numeric" class="w-full bg-surface-container-high border-b focus:border-primary text-sm py-3 px-6 text-on-surface transition-all outline-none rounded-t-lg" :class="(errors.duration || durationError) ? 'border-red-500' : 'border-outline-variant/20'" placeholder="120" />
                <p v-if="errors.duration || durationError" class="text-[10px] text-red-400 font-bold">{{ errors.duration || durationError }}</p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Trailer URL (YouTube) <span class="text-red-500">*</span></label>
                <input v-model="newMovie.trailerUrl" @input="clearErr('trailerUrl')" type="text" maxlength="255" class="w-full bg-surface-container-high border-b focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" :class="(errors.trailerUrl || trailerError) ? 'border-red-500' : 'border-outline-variant/20'" placeholder="https://youtube.com/..." />
                <p v-if="errors.trailerUrl || trailerError" class="text-[10px] text-red-400 font-bold">{{ errors.trailerUrl || trailerError }}</p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Đạo diễn</label>
                <input :value="newMovie.director" @input="onDirectorInput" type="text" maxlength="100" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: Christopher Nolan" />
              </div>
              <div class="space-y-2 col-span-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Diễn viên chính (cách nhau bằng dấu phẩy)</label>
                <input v-model="newMovie.castMembers" type="text" maxlength="250" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: Cillian Murphy, Emily Blunt" />
              </div>
            </div>
          </section>

          <!-- 02. Kỹ thuật & Sản xuất -->
          <section class="space-y-6">
            <div class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3">
              <span class="text-[10px] font-black uppercase tracking-[0.2em]">02. Kỹ thuật & Sản xuất</span>
            </div>
            <div class="grid grid-cols-3 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Quốc gia</label>
                <select v-model="newMovie.country" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 text-on-surface transition-all outline-none rounded-t-lg appearance-none">
                  <option>Mỹ</option><option>Nhật Bản</option><option>Hàn Quốc</option><option>Việt Nam</option><option>Pháp</option><option>Anh</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Năm sản xuất <span class="text-red-500">*</span></label>
                <input :value="newMovie.productionYear" @input="onYearInput" type="text" inputmode="numeric" maxlength="4" class="w-full bg-surface-container-high border-b focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" :class="(errors.productionYear || yearError) ? 'border-red-500' : 'border-outline-variant/20'" placeholder="2024" />
                <p v-if="errors.productionYear || yearError" class="text-[10px] text-red-400 font-bold">{{ errors.productionYear || yearError }}</p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Ngôn ngữ gốc</label>
                <select v-model="newMovie.originalLanguage" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 text-on-surface transition-all outline-none rounded-t-lg appearance-none">
                  <option>Tiếng Anh</option><option>Tiếng Hàn</option><option>Tiếng Nhật</option><option>Tiếng Việt</option><option>Tiếng Pháp</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Loại hình hiển thị</label>
                <select v-model="newMovie.versionType" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 text-on-surface transition-all outline-none rounded-t-lg appearance-none">
                  <option>Phụ đề Tiếng Việt</option><option>Thuyết minh Tiếng Việt</option><option>Lồng tiếng Tiếng Việt</option><option>Bản gốc (No Sub)</option>
                </select>
              </div>
            </div>

            <!-- Thể loại -->
            <div class="space-y-4">
              <div class="flex justify-between items-center px-1">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thể loại phim <span class="text-red-500">*</span></label>
                <div class="flex gap-4">
                  <button @click="resetGenres" type="button" class="text-on-surface-variant/40 hover:text-red-500 transition-all"><span class="material-symbols-outlined text-base">restart_alt</span></button>
                  <button @click="scrollLeft" type="button" class="text-on-surface-variant/40 hover:text-primary transition-all"><span class="material-symbols-outlined text-base">chevron_left</span></button>
                  <button @click="scrollRight" type="button" class="text-on-surface-variant/40 hover:text-primary transition-all"><span class="material-symbols-outlined text-base">chevron_right</span></button>
                </div>
              </div>
              <div
                ref="genreContainer"
                @wheel="handleWheel"
                class="grid grid-rows-2 grid-flow-col gap-2 overflow-x-auto no-scrollbar px-1 py-1"
                style="grid-template-rows: repeat(2, minmax(0, 1fr))"
              >
                <button
                  v-for="genre in genres"
                  :key="genre.id"
                  type="button"
                  @click="toggleGenre(genre)"
                  :class="selectedGenres.some((g) => g.id === genre.id) ? 'bg-primary text-on-primary' : 'bg-surface-container-high/50 text-on-surface-variant'"
                  class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-bold uppercase tracking-widest transition-all whitespace-nowrap min-w-[100px]"
                >{{ genre.name }}</button>
              </div>
              <p v-if="errors.genres" class="text-[10px] text-red-400 font-bold px-1">{{ errors.genres }}</p>
            </div>

            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-4">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Định dạng hỗ trợ (Tick chọn nhiều) <span class="text-red-500">*</span></label>
                <div class="flex flex-wrap gap-2">
                  <button v-for="fmt in availableFormats" :key="fmt" type="button" @click="toggleFormat(fmt)" :class="selectedFormats.includes(fmt) ? 'bg-primary text-on-primary' : 'bg-surface-container-high/50 text-on-surface-variant'" class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-bold uppercase tracking-widest transition-all min-w-[60px]">{{ fmt }}</button>
                </div>
                <p v-if="errors.formats" class="text-[10px] text-red-400 font-bold">{{ errors.formats }}</p>
              </div>
              <div class="space-y-4">
                <label class="flex flex-col text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">
                  <span>Định dạng chiếu chính (Chọn một)</span>
                  <span class="normal-case font-normal text-on-surface-variant/50 mt-0.5">Chỉ trong định dạng hỗ trợ</span>
                </label>
                <div class="flex flex-wrap gap-2">
                  <button v-for="fmt in availableFormats" :key="fmt" type="button"
                    @click="selectMainFormat(fmt)"
                    :disabled="!selectedFormats.includes(fmt)"
                    :class="newMovie.format === fmt
                      ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
                      : (selectedFormats.includes(fmt) ? 'bg-surface-container-high/50 text-on-surface-variant hover:border-primary/50' : 'bg-surface-container-high/20 text-on-surface-variant/30 cursor-not-allowed')"
                    class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-black uppercase tracking-widest transition-all min-w-[60px]">{{ fmt }}</button>
                </div>
              </div>
            </div>
          </section>

          <!-- 03. Vận hành & Kiểm soát -->
          <section class="space-y-6">
            <div class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3">
              <span class="text-[10px] font-black uppercase tracking-[0.2em]">03. Vận hành & Kiểm soát</span>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Phân loại độ tuổi</label>
                <select v-model="newMovie.ageRating" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 text-on-surface transition-all outline-none rounded-t-lg appearance-none">
                  <option v-for="rating in availableAgeRatings" :key="rating.code" :value="rating.code">{{ rating.code }} ({{ rating.name }})</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Trạng thái</label>
                <select v-model="newMovie.status" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 text-on-surface transition-all outline-none rounded-t-lg appearance-none">
                  <option value="active">Đang chiếu</option>
                  <option value="upcoming">Sắp chiếu</option>
                  <option value="archived">Ngừng chiếu</option>
                </select>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Ngày khởi chiếu <span class="text-red-500">*</span></label>
                <input v-model="newMovie.startDate" @input="clearErr('dates')" type="date" :min="isEditing ? undefined : todayStr" class="w-full bg-surface-container-high border-b focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" :class="(errors.dates || dateError) ? 'border-red-500' : 'border-outline-variant/20'" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Ngày kết thúc (Dự kiến) <span class="text-red-500">*</span></label>
                <input v-model="newMovie.endDate" @input="clearErr('dates')" type="date" :min="newMovie.startDate || (isEditing ? undefined : todayStr)" class="w-full bg-surface-container-high border-b focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" :class="(errors.dates || dateError) ? 'border-red-500' : 'border-outline-variant/20'" />
              </div>
            </div>
            <p v-if="errors.dates || dateError" class="text-[10px] text-red-400 font-bold px-1 -mt-3">{{ errors.dates || dateError }}</p>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1 italic">Ghi chú nội bộ cho Admin (Không hiển thị cho khách hàng)</label>
              <textarea v-model="newMovie.internalNotes" rows="2" maxlength="500" class="w-full bg-surface-container-high border border-outline-variant/10 focus:border-primary/50 text-xs py-3 px-4 text-on-surface transition-all outline-none rounded-lg resize-none" placeholder="VD: Ưu tiên suất chiếu tối..."></textarea>
              <div class="flex justify-end"><span class="text-[10px] text-on-surface-variant/60">{{ (newMovie.internalNotes || '').length }}/500</span></div>
            </div>
          </section>

          <!-- 04. Media & Mô tả -->
          <section class="space-y-6">
            <div class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3">
              <span class="text-[10px] font-black uppercase tracking-[0.2em]">04. Media & Mô tả</span>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Hình ảnh Banner (Ảnh bìa ngang)</label>
              <input ref="bannerInput" type="file" @change="onBannerChange" accept=".jpg,.jpeg,.png,.webp" class="hidden" />
              <button @click="triggerBannerInput" type="button" :disabled="isUploadingBanner" class="w-full bg-surface-container-high border border-dashed border-outline-variant/30 hover:border-primary/50 hover:bg-primary/5 transition-all py-8 rounded-lg flex flex-col items-center justify-center gap-2 group relative overflow-hidden">
                <img v-if="newMovie.bannerUrl" :src="optimizeCloudinaryUrl(newMovie.bannerUrl)" class="absolute inset-0 w-full h-full object-cover opacity-40 group-hover:opacity-20 transition-opacity" />
                <span class="material-symbols-outlined text-3xl text-on-surface-variant group-hover:text-primary transition-colors relative z-10">add_photo_alternate</span>
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant group-hover:text-primary transition-colors relative z-10">{{ newMovie.bannerUrl ? "Thay đổi ảnh banner" : "Tải ảnh banner chính thức" }}</span>
                <div v-if="isUploadingBanner" class="absolute inset-0 bg-black/60 flex flex-col items-center justify-center text-primary z-20">
                  <span class="animate-spin material-symbols-outlined text-3xl mb-1">sync</span>
                  <span class="text-[8px] font-black uppercase tracking-widest animate-pulse">Đang tải lên...</span>
                </div>
              </button>
              <div class="flex items-center gap-3 pt-2">
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" v-model="newMovie.showOnBanner" class="sr-only peer" />
                  <div class="w-9 h-5 bg-surface-container-highest rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-on-surface-variant peer-checked:after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary"></div>
                </label>
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Hiển thị trên Banner trang chủ</span>
              </div>
              <p class="text-[10px] text-on-surface-variant/50">Định dạng JPG/PNG/WEBP · tối đa 8MB</p>
              <p v-if="errors.banner" class="text-[10px] text-red-400 font-bold">{{ errors.banner }}</p>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Tóm tắt nội dung <span class="text-red-500">*</span> <span class="normal-case font-normal text-on-surface-variant/50">(50–1000 ký tự)</span></label>
              <textarea v-model="newMovie.description" @input="clearErr('description')" rows="5" maxlength="1000" class="w-full bg-surface-container-high border focus:border-primary/50 text-sm py-4 px-4 text-on-surface transition-all outline-none rounded-lg resize-none" :class="(errors.description || synopsisError) ? 'border-red-500' : 'border-outline-variant/10'" placeholder="Viết mô tả ngắn về cốt truyện phim (tối thiểu 50 ký tự)..."></textarea>
              <div class="flex justify-between">
                <span v-if="errors.description || synopsisError" class="text-[10px] text-red-400 font-bold">{{ errors.description || synopsisError }}</span>
                <span v-else></span>
                <span class="text-[10px]" :class="(newMovie.description || '').trim().length < 50 ? 'text-amber-400' : 'text-on-surface-variant/60'">{{ (newMovie.description || '').length }}/1000</span>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- Preview & Actions (1/3) -->
      <div class="md:col-span-1 bg-surface-container-high/30 p-12 flex flex-col justify-between relative overflow-hidden h-full">
        <div class="absolute top-0 right-0 w-64 h-64 bg-primary/5 blur-[100px] -z-10"></div>
        <div class="space-y-6">
          <p class="text-[10px] font-black uppercase tracking-widest text-center text-primary">Xem trước hiển thị (Click để tải Poster)</p>
          <p class="text-[9px] text-center text-on-surface-variant/50 -mt-4">Poster bắt buộc · JPG/PNG/WEBP · tối đa 5MB</p>
          <input ref="posterInput" type="file" @change="onPosterChange" accept=".jpg,.jpeg,.png,.webp" class="hidden" />
          <div @click="triggerPosterInput" class="aspect-[2/3] max-w-[260px] mx-auto w-full rounded-2xl overflow-hidden border border-outline-variant/20 shadow-2xl bg-surface-container-highest relative group cursor-pointer hover:border-primary/50 transition-colors">
            <img v-if="newMovie.posterUrl" :src="optimizeCloudinaryUrl(newMovie.posterUrl)" class="w-full h-full object-cover transition-transform group-hover:scale-105" @error="newMovie.posterUrl = ''" />
            <div v-else class="w-full h-full flex flex-col items-center justify-center p-6 text-center text-on-surface-variant/40">
              <span class="material-symbols-outlined text-4xl mb-3">image_not_supported</span>
              <p class="text-[9px] font-bold uppercase tracking-widest">Chưa có ảnh poster</p>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/90 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-end p-6">
              <p class="text-white font-bold text-xs uppercase leading-none mb-1">{{ newMovie.title || "Tiêu đề phim" }}</p>
              <p class="text-primary text-[8px] font-black uppercase tracking-widest">{{ selectedGenres.map((g) => g.name).join(" • ") || "Thể loại" }}</p>
            </div>
            <div v-if="isUploadingPoster" class="absolute inset-0 bg-black/60 flex flex-col items-center justify-center text-primary z-20">
              <span class="animate-spin material-symbols-outlined text-4xl mb-2">sync</span>
              <span class="text-[9px] font-black uppercase tracking-widest animate-pulse">Đang tải lên...</span>
            </div>
          </div>
          <p v-if="errors.poster" class="text-[10px] text-red-400 font-bold text-center">{{ errors.poster }}</p>
        </div>

        <div class="flex items-center gap-3 mt-8 pt-8 border-t border-outline-variant/10">
          <button @click="emit('close')" class="flex-grow py-4 bg-white/5 text-on-surface/40 font-black text-[9px] uppercase tracking-[0.2em] rounded-xl border border-white/5 hover:bg-white/10 hover:text-white transition-all">Hủy bỏ</button>
          <button @click="handleSave" :disabled="isFormInvalid" class="flex-[2] py-4 bg-gradient-to-br from-primary to-primary-container text-on-primary font-black text-[9px] uppercase tracking-[0.2em] rounded-xl hover:scale-[1.02] active:scale-[0.98] transition-all shadow-xl shadow-primary/20 flex items-center justify-center gap-2 group disabled:opacity-40 disabled:grayscale disabled:cursor-not-allowed disabled:hover:scale-100">
            <span class="material-symbols-outlined text-sm group-hover:rotate-12 transition-transform">rocket_launch</span>
            {{ isEditing ? "Cập nhật" : "Xuất bản" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.05); }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(var(--primary-rgb, 245, 197, 24), 0.3); border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(var(--primary-rgb, 245, 197, 24), 0.5); }
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
