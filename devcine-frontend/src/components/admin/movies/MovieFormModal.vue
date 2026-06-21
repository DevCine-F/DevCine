<script setup>
import { ref, watch } from "vue";
import axios from "@/api/axios";

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
  titleVietnamese: "",
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
  basePrice: 85000,
  description: "",
  posterUrl: "",
  bannerUrl: "",
  showOnBanner: true,
  trailerUrl: "",
  format: "2D",
  internalNotes: "",
  distributor: "",
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
const posterInput = ref(null);
const bannerInput = ref(null);
const isUploadingPoster = ref(false);
const isUploadingBanner = ref(false);

const triggerPosterInput = () => {
  if (!isUploadingPoster.value) posterInput.value.click();
};
const triggerBannerInput = () => bannerInput.value.click();

const uploadImage = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await axios.post("/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data.url;
};

const onPosterChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const oldUrl = newMovie.value.posterUrl;
  newMovie.value.posterUrl = URL.createObjectURL(file);
  isUploadingPoster.value = true;
  try {
    newMovie.value.posterUrl = await uploadImage(file);
  } catch (error) {
    console.error("Lỗi upload ảnh:", error);
    alert("Tải ảnh Poster lên Cloudinary thất bại!");
    newMovie.value.posterUrl = oldUrl;
  } finally {
    isUploadingPoster.value = false;
  }
};

const onBannerChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const oldUrl = newMovie.value.bannerUrl;
  newMovie.value.bannerUrl = URL.createObjectURL(file);
  isUploadingBanner.value = true;
  try {
    newMovie.value.bannerUrl = await uploadImage(file);
  } catch (error) {
    console.error("Lỗi upload ảnh:", error);
    alert("Tải ảnh Banner lên Cloudinary thất bại!");
    newMovie.value.bannerUrl = oldUrl;
  } finally {
    isUploadingBanner.value = false;
  }
};

// ===== Chọn thể loại / định dạng =====
const toggleGenre = (genre) => {
  const index = selectedGenres.value.findIndex((g) => g.id === genre.id);
  index === -1 ? selectedGenres.value.push(genre) : selectedGenres.value.splice(index, 1);
};
const resetGenres = () => (selectedGenres.value = []);
const toggleFormat = (format) => {
  const index = selectedFormats.value.indexOf(format);
  index === -1 ? selectedFormats.value.push(format) : selectedFormats.value.splice(index, 1);
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

// ===== Lưu =====
const handleSave = () => {
  if (!newMovie.value.title) {
    alert("Vui lòng nhập tên phim!");
    return;
  }
  if (newMovie.value.startDate && newMovie.value.endDate) {
    if (new Date(newMovie.value.endDate) < new Date(newMovie.value.startDate)) {
      alert("⚠️ Lỗi vận hành: Ngày kết thúc không được phép nhỏ hơn ngày khởi chiếu!");
      return;
    }
  }
  const payload = {
    ...newMovie.value,
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
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Tên phim</label>
              <input v-model="newMovie.title" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-6 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: Oppenheimer" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Tên Tiếng Việt</label>
              <input v-model="newMovie.titleVietnamese" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-6 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: Kẻ Hủy Diệt" />
            </div>
            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Thời lượng (Phút)</label>
                <input v-model="newMovie.duration" type="number" min="1" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-6 text-on-surface transition-all outline-none rounded-t-lg" placeholder="120" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Trailer URL (YouTube)</label>
                <input v-model="newMovie.trailerUrl" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="https://youtube.com/..." />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Đạo diễn</label>
                <input v-model="newMovie.director" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: Christopher Nolan" />
              </div>
              <div class="space-y-2 col-span-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Diễn viên chính (cách nhau bằng dấu phẩy)</label>
                <input v-model="newMovie.castMembers" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: Cillian Murphy, Emily Blunt" />
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
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Năm sản xuất</label>
                <input v-model="newMovie.productionYear" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="2024" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Nhà phát hành</label>
                <input v-model="newMovie.distributor" type="text" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" placeholder="VD: CGV, Galaxy..." />
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
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thể loại phim</label>
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
            </div>

            <div class="grid grid-cols-2 gap-6">
              <div class="space-y-4">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Định dạng hỗ trợ (Tick chọn nhiều)</label>
                <div class="flex flex-wrap gap-2">
                  <button v-for="fmt in availableFormats" :key="fmt" type="button" @click="toggleFormat(fmt)" :class="selectedFormats.includes(fmt) ? 'bg-primary text-on-primary' : 'bg-surface-container-high/50 text-on-surface-variant'" class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-bold uppercase tracking-widest transition-all min-w-[60px]">{{ fmt }}</button>
                </div>
              </div>
              <div class="space-y-4">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Định dạng chiếu chính (Chọn một)</label>
                <div class="flex flex-wrap gap-2">
                  <button v-for="fmt in availableFormats" :key="fmt" type="button" @click="newMovie.format = fmt" :class="newMovie.format === fmt ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20' : 'bg-surface-container-high/50 text-on-surface-variant'" class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-black uppercase tracking-widest transition-all min-w-[60px] hover:border-primary/50">{{ fmt }}</button>
                </div>
              </div>
            </div>
          </section>

          <!-- 03. Vận hành & Kiểm soát -->
          <section class="space-y-6">
            <div class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3">
              <span class="text-[10px] font-black uppercase tracking-[0.2em]">03. Vận hành & Kiểm soát</span>
            </div>
            <div class="grid grid-cols-3 gap-6">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Phân loại độ tuổi</label>
                <select v-model="newMovie.ageRating" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 text-on-surface transition-all outline-none rounded-t-lg appearance-none">
                  <option v-for="rating in availableAgeRatings" :key="rating.code" :value="rating.code">{{ rating.code }} ({{ rating.name }})</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Giá vé gốc (VNĐ)</label>
                <input v-model="newMovie.basePrice" type="number" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" />
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
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Ngày khởi chiếu</label>
                <input v-model="newMovie.startDate" type="date" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" />
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Ngày kết thúc (Dự kiến)</label>
                <input v-model="newMovie.endDate" type="date" class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg" />
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1 italic">Ghi chú nội bộ cho Admin (Không hiển thị cho khách hàng)</label>
              <textarea v-model="newMovie.internalNotes" rows="2" class="w-full bg-surface-container-high border border-outline-variant/10 focus:border-primary/50 text-xs py-3 px-4 text-on-surface transition-all outline-none rounded-lg resize-none" placeholder="VD: Ưu tiên suất chiếu tối..."></textarea>
            </div>
          </section>

          <!-- 04. Media & Mô tả -->
          <section class="space-y-6">
            <div class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3">
              <span class="text-[10px] font-black uppercase tracking-[0.2em]">04. Media & Mô tả</span>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Hình ảnh Banner (Ảnh bìa ngang)</label>
              <input ref="bannerInput" type="file" @change="onBannerChange" accept="image/*" class="hidden" />
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
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1">Tóm tắt nội dung</label>
              <textarea v-model="newMovie.description" rows="5" class="w-full bg-surface-container-high border border-outline-variant/10 focus:border-primary/50 text-sm py-4 px-4 text-on-surface transition-all outline-none rounded-lg resize-none" placeholder="Viết mô tả ngắn về cốt truyện phim..."></textarea>
            </div>
          </section>
        </div>
      </div>

      <!-- Preview & Actions (1/3) -->
      <div class="md:col-span-1 bg-surface-container-high/30 p-12 flex flex-col justify-between relative overflow-hidden h-full">
        <div class="absolute top-0 right-0 w-64 h-64 bg-primary/5 blur-[100px] -z-10"></div>
        <div class="space-y-6">
          <p class="text-[10px] font-black uppercase tracking-widest text-center text-primary">Xem trước hiển thị (Click để tải Poster)</p>
          <input ref="posterInput" type="file" @change="onPosterChange" accept="image/*" class="hidden" />
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
        </div>

        <div class="flex items-center gap-3 mt-8 pt-8 border-t border-outline-variant/10">
          <button @click="emit('close')" class="flex-grow py-4 bg-white/5 text-on-surface/40 font-black text-[9px] uppercase tracking-[0.2em] rounded-xl border border-white/5 hover:bg-white/10 hover:text-white transition-all">Hủy bỏ</button>
          <button @click="handleSave" class="flex-[2] py-4 bg-gradient-to-br from-primary to-primary-container text-on-primary font-black text-[9px] uppercase tracking-[0.2em] rounded-xl hover:scale-[1.02] active:scale-[0.98] transition-all shadow-xl shadow-primary/20 flex items-center justify-center gap-2 group">
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
