<script setup>
import { ref, computed, onMounted } from "vue";
import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

const movies = ref([]);
const isFilterOpen = ref(false);
const isAddModalOpen = ref(false);
const isDetailModalOpen = ref(false);
const selectedMovie = ref(null);
const searchQuery = ref("");
const filterCountry = ref("Tất cả quốc gia");
const filterStatus = ref("Tất cả trạng thái");
const filterRating = ref("Tất cả đánh giá");
const filterAgeRating = ref("Tất cả độ tuổi");
const genres = ref([]);
const availableFormats = ref([]);
const availableAgeRatings = ref([]);

const fetchCategories = async () => {
  try {
    const [genresRes, formatsRes, ageRatingsRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/categories/genres`),
      axios.get(`${API_BASE_URL}/categories/formats`),
      axios.get(`${API_BASE_URL}/categories/age-ratings`)
    ]);
    genres.value = genresRes.data;
    availableFormats.value = formatsRes.data.map(f => f.name);
    availableAgeRatings.value = ageRatingsRes.data;
  } catch (error) {
    console.error("Lỗi khi tải danh mục:", error);
    // Fallback if API fails
    genres.value = [{ id: 1, name: "Hành động" }];
    availableFormats.value = ["2D", "3D", "IMAX"];
    availableAgeRatings.value = [{ code: "P", name: "Mọi đối tượng" }];
  }
};
const selectedGenres = ref([]);
const toggleGenre = (genre) => {
  const index = selectedGenres.value.findIndex(g => g.id === genre.id);
  if (index === -1) {
    selectedGenres.value.push(genre);
  } else {
    selectedGenres.value.splice(index, 1);
  }
};

const resetGenres = () => {
  selectedGenres.value = [];
};

const genreContainer = ref(null);
const scrollLeft = () => {
  if (genreContainer.value) {
    genreContainer.value.scrollBy({ left: -200, behavior: "smooth" });
  }
};
const scrollRight = () => {
  if (genreContainer.value) {
    genreContainer.value.scrollBy({ left: 200, behavior: "smooth" });
  }
};

const handleWheel = (e) => {
  if (genreContainer.value) {
    // Nếu người dùng cuộn dọc (deltaY), ta chuyển nó thành cuộn ngang
    // Nếu người dùng đang cuộn ngang (deltaX - dùng con lăn ngang/trackpad),
    // ta để trình duyệt tự xử lý để giữ độ mượt native của hệ điều hành.
    if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
      e.preventDefault();
      genreContainer.value.scrollLeft += e.deltaY;
    }
  }
};

const fileInput = ref(null);

const triggerFileInput = () => {
  fileInput.value.click();
};

const onFileChange = (e) => {
  const file = e.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (event) => {
      newMovie.value.posterUrl = event.target.result;
    };
    reader.readAsDataURL(file);
  }
};

// const availableFormats = ["2D", "3D", "IMAX", "4DX", "Dolby Atmos"];
const selectedFormats = ref([]);

const toggleFormat = (format) => {
  const index = selectedFormats.value.indexOf(format);
  if (index === -1) {
    selectedFormats.value.push(format);
  } else {
    selectedFormats.value.splice(index, 1);
  }
};

const isEditing = ref(false);
const currentMovieId = ref(null);

const newMovie = ref({
  title: "",
  titleVietnamese: "",
  duration: "",
  genres: [],
  country: "USA",
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
  trailerUrl: "",
  format: "",
  internalNotes: "",
});

const filteredMovies = computed(() => {
  return movies.value.filter((movie) => {
    const matchesSearch =
      movie.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      movie.titleVietnamese
        ?.toLowerCase()
        .includes(searchQuery.value.toLowerCase());

    const matchesCountry =
      filterCountry.value === "Tất cả quốc gia" ||
      movie.country === filterCountry.value;

    const matchesStatus =
      filterStatus.value === "Tất cả trạng thái" ||
      (filterStatus.value === "Đang chiếu" && movie.status === "active") ||
      (filterStatus.value === "Sắp chiếu" && movie.status === "upcoming") ||
      (filterStatus.value === "Lưu trữ" && movie.status === "archived");

    const matchesAge =
      filterAgeRating.value === "Tất cả độ tuổi" ||
      movie.ageRating === filterAgeRating.value;

    let matchesRating = true;
    if (filterRating.value === "4.5 - 5.0 sao")
      matchesRating = parseFloat(movie.rating) >= 4.5;
    else if (filterRating.value === "4.0 - 4.5 sao")
      matchesRating =
        parseFloat(movie.rating) >= 4.0 && parseFloat(movie.rating) < 4.5;
    else if (filterRating.value === "Dưới 4.0 sao")
      matchesRating = parseFloat(movie.rating) < 4.0;

    return (
      matchesSearch &&
      matchesCountry &&
      matchesStatus &&
      matchesAge &&
      matchesRating
    );
  });
});

const clearFilters = () => {
  filterCountry.value = "Tất cả quốc gia";
  filterStatus.value = "Tất cả trạng thái";
  filterRating.value = "Tất cả đánh giá";
  filterAgeRating.value = "Tất cả độ tuổi";
  searchQuery.value = "";
};

const fetchMovies = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/movies`);
    movies.value = response.data.map((m) => ({
      ...m,
      duration: m.durationMins ? m.durationMins + " Phút" : (m.duration ? m.duration + " Phút" : "N/A"),
      // Hiển thị tên thể loại nối nhau bằng dấu phẩy để dễ đọc trên bảng
      genreDisplay: m.genres?.map(g => g.name).join(", ") || "N/A",
      statusText:
        m.status === "active"
          ? "Đang chiếu"
          : m.status === "upcoming"
            ? "Sắp chiếu"
            : "Lưu trữ",
    }));
  } catch (error) {
    console.error("Lỗi khi tải danh sách phim:", error);
  }
};

const openDetailModal = (movie) => {
  selectedMovie.value = movie;
  isDetailModalOpen.value = true;
};

const openAddModal = () => {
  isEditing.value = false;
  currentMovieId.value = null;
  selectedGenres.value = [];
  selectedFormats.value = [];
  newMovie.value = {
    title: "",
    titleVietnamese: "",
    duration: "",
    genres: [],
    country: "USA",
    productionYear: new Date().getFullYear().toString(),
    language: "Phụ đề Tiếng Việt",
    status: "upcoming",
    rating: "5.0",
    ageRating: "P",
    releaseDate: "",
    startDate: "",
    endDate: "",
    basePrice: 85000,
    description: "",
    posterUrl: "",
    trailerUrl: "",
    format: "2D",
    originalLanguage: "Tiếng Anh",
    versionType: "Phụ đề Tiếng Việt",
    internalNotes: "",
  };
  isAddModalOpen.value = true;
};

const openEditModal = (movie) => {
  isEditing.value = true;
  currentMovieId.value = movie.id;
  selectedGenres.value = movie.genres ? [...movie.genres] : [];
  selectedFormats.value = movie.format ? movie.format.split(", ") : [];
  newMovie.value = { ...movie };
  isAddModalOpen.value = true;
};

const deleteMovie = async (id) => {
  if (!confirm("Bạn có chắc chắn muốn xóa bộ phim này?")) return;
  try {
    await axios.delete(`${API_BASE_URL}/movies/${id}`);
    await fetchMovies();
  } catch (error) {
    console.error("Lỗi khi xóa phim:", error);
  }
};

const saveMovie = async () => {
  if (!newMovie.value.title) return;

  // Validation: End Date cannot be before Start Date
  if (newMovie.value.startDate && newMovie.value.endDate) {
    if (new Date(newMovie.value.endDate) < new Date(newMovie.value.startDate)) {
      alert(
        "⚠️ Lỗi vận hành: Ngày kết thúc không được phép nhỏ hơn ngày khởi chiếu!",
      );
      return;
    }
  }

  try {
    const payload = {
      ...newMovie.value,
      durationMins: parseInt(newMovie.value.duration) || null,
      genres: selectedGenres.value,
      format: selectedFormats.value.join(", "),
      rating: newMovie.value.rating || "5.0",
      releaseDate:
        newMovie.value.releaseDate || new Date().toISOString().split("T")[0],
    };

    if (isEditing.value) {
      await axios.put(
        `${API_BASE_URL}/movies/${currentMovieId.value}`,
        payload,
      );
    } else {
      await axios.post(`${API_BASE_URL}/movies`, payload);
    }

    await fetchMovies();
    isAddModalOpen.value = false;
  } catch (error) {
    console.error("Lỗi khi lưu phim:", error);
    alert("Thao tác thất bại. Vui lòng kiểm tra lại kết nối!");
  }
};

const populateDemoData = async () => {
  const demoMovies = [
    {
      title: "Oppenheimer",
      genre: "Drama",
      duration: "180 Phút",
      country: "USA",
      status: "active",
      rating: "9.0",
    },
    {
      title: "Dune: Part Two",
      genre: "Sci-Fi",
      duration: "166 Phút",
      country: "USA",
      status: "active",
      rating: "8.8",
    },
    {
      title: "Spirited Away",
      genre: "Animation",
      duration: "125 Phút",
      country: "Japan",
      status: "active",
      rating: "8.6",
    },
    {
      title: "Interstellar",
      genre: "Sci-Fi",
      duration: "169 Phút",
      country: "USA",
      status: "active",
      rating: "8.7",
      duration: "148 Phút",
      country: "USA",
      status: "active",
      rating: "8.8",
    },
  ];

  for (const movie of demoMovies) {
    await axios.post(`${API_BASE_URL}/movies`, movie);
  }
  await fetchMovies();
};

onMounted(() => {
  fetchMovies();
  fetchCategories();
});
</script>

<template>
  <div class="p-10">
    <!-- Top Header -->
    <header class="flex justify-between items-center mb-12">
      <div>
        <h1
          class="text-3xl font-extrabold tracking-tight font-headline uppercase text-on-surface italic"
        >
          Nội dung <span class="text-primary">Kỹ thuật số</span>
        </h1>
        <p
          class="text-on-surface-variant text-sm mt-1 font-bold uppercase tracking-widest"
        >
          Quản lý Phim, Diễn viên & Bản quyền
        </p>
      </div>

      <div class="flex items-center gap-4">
        <div class="relative group">
          <span
            class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-sm"
            >search</span
          >
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm kiếm phim, đạo diễn..."
            class="bg-surface-container-high border-none text-xs pl-10 pr-4 py-3 rounded-sm w-72 focus:ring-1 focus:ring-primary transition-all text-on-surface"
          />
        </div>
        <button
          @click="populateDemoData"
          class="bg-surface-container-high text-on-surface font-headline font-bold text-xs uppercase tracking-widest px-6 py-3 rounded-sm hover:bg-white/5 transition-all flex items-center gap-2 border border-outline-variant/10"
        >
          <span class="material-symbols-outlined text-sm">database</span>
          Dữ liệu Mẫu
        </button>
        <button
          @click="openAddModal"
          class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-8 py-3 rounded-sm hover:brightness-110 active:scale-95 transition-all flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          Thêm Phim Mới
        </button>
      </div>
    </header>

    <!-- Advanced Filter Bar -->
    <section class="mb-8">
      <div
        class="bg-surface-container-low border border-outline-variant/10 p-4 rounded-lg flex items-center justify-between"
      >
        <div class="flex items-center gap-4">
          <button
            @click="isFilterOpen = !isFilterOpen"
            :class="
              isFilterOpen
                ? 'bg-primary text-on-primary shadow-lg shadow-primary/20'
                : 'bg-surface-container-high text-on-surface-variant'
            "
            class="px-6 py-2.5 rounded-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-3 transition-all hover:scale-[1.02] active:scale-95"
          >
            <span class="material-symbols-outlined text-sm">filter_list</span>
            Bộ lọc nâng cao
          </button>

          <div
            v-if="
              filterCountry !== 'Tất cả quốc gia' ||
              filterStatus !== 'Tất cả trạng thái' ||
              filterRating !== 'Tất cả đánh giá' ||
              filterAgeRating !== 'Tất cả độ tuổi' ||
              searchQuery
            "
            class="h-8 w-px bg-outline-variant/20 mx-2"
          ></div>

          <div class="flex flex-wrap gap-2">
            <!-- Active Chips -->
            <div
              v-if="filterStatus !== 'Tất cả trạng thái'"
              class="px-3 py-1.5 bg-surface-container-highest/50 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest text-on-surface flex items-center gap-3 rounded-sm group"
            >
              <span class="text-primary/60">Trạng thái:</span>
              {{ filterStatus }}
              <span
                @click="filterStatus = 'Tất cả trạng thái'"
                class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500 transition-colors"
                >close</span
              >
            </div>

            <div
              v-if="filterCountry !== 'Tất cả quốc gia'"
              class="px-3 py-1.5 bg-surface-container-highest/50 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest text-on-surface flex items-center gap-3 rounded-sm group"
            >
              <span class="text-primary/60">Quốc gia:</span> {{ filterCountry }}
              <span
                @click="filterCountry = 'Tất cả quốc gia'"
                class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500 transition-colors"
                >close</span
              >
            </div>

            <div
              v-if="filterAgeRating !== 'Tất cả độ tuổi'"
              class="px-3 py-1.5 bg-surface-container-highest/50 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest text-on-surface flex items-center gap-3 rounded-sm group"
            >
              <span class="text-primary/60">Độ tuổi:</span>
              {{ filterAgeRating }}
              <span
                @click="filterAgeRating = 'Tất cả độ tuổi'"
                class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500 transition-colors"
                >close</span
              >
            </div>

            <div
              v-if="filterRating !== 'Tất cả đánh giá'"
              class="px-3 py-1.5 bg-surface-container-highest/50 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest text-on-surface flex items-center gap-3 rounded-sm group"
            >
              <span class="text-primary/60">Đánh giá:</span> {{ filterRating }}
              <span
                @click="filterRating = 'Tất cả đánh giá'"
                class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500 transition-colors"
                >close</span
              >
            </div>

            <div
              v-if="searchQuery"
              class="px-3 py-1.5 bg-surface-container-highest/50 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest text-on-surface flex items-center gap-3 rounded-sm group"
            >
              <span class="text-primary/60">Từ khóa:</span> {{ searchQuery }}
              <span
                @click="searchQuery = ''"
                class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500 transition-colors"
                >close</span
              >
            </div>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <p
            class="text-[10px] font-black uppercase tracking-[0.2em] text-primary"
          >
            Hiển thị {{ filteredMovies.length }} bộ phim
          </p>
        </div>
      </div>

      <!-- Expandable Filters -->
      <transition name="fade">
        <div
          v-if="isFilterOpen"
          class="bg-surface-container-low border-x border-b border-outline-variant/10 p-10 grid grid-cols-1 md:grid-cols-5 gap-8 rounded-b-lg shadow-2xl relative z-20"
        >
          <div class="space-y-3">
            <label
              class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30"
              >Trạng thái vận hành</label
            >
            <select
              v-model="filterStatus"
              class="w-full bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold uppercase tracking-wider rounded-lg py-3 px-4 text-on-surface focus:border-primary transition-all outline-none appearance-none"
            >
              <option>Tất cả trạng thái</option>
              <option>Đang chiếu</option>
              <option>Sắp chiếu</option>
              <option>Lưu trữ</option>
            </select>
          </div>
          <div class="space-y-3">
            <label
              class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30"
              >Quốc gia sản xuất</label
            >
            <select
              v-model="filterCountry"
              class="w-full bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold uppercase tracking-wider rounded-lg py-3 px-4 text-on-surface focus:border-primary transition-all outline-none appearance-none"
            >
              <option>Tất cả quốc gia</option>
              <option>USA</option>
              <option>Japan</option>
              <option>UK</option>
              <option>South Korea</option>
              <option>Vietnam</option>
            </select>
          </div>
          <div class="space-y-3">
            <label
              class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30"
              >Đánh giá cộng đồng</label
            >
            <select
              v-model="filterRating"
              class="w-full bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold uppercase tracking-wider rounded-lg py-3 px-4 text-on-surface focus:border-primary transition-all outline-none appearance-none"
            >
              <option>Tất cả đánh giá</option>
              <option>4.5 - 5.0 sao</option>
              <option>4.0 - 4.5 sao</option>
              <option>Dưới 4.0 sao</option>
            </select>
          </div>
          <div class="space-y-3">
            <label
              class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30"
              >Phân loại độ tuổi</label
            >
            <select
              v-model="filterAgeRating"
              class="w-full bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold uppercase tracking-wider rounded-lg py-3 px-4 text-on-surface focus:border-primary transition-all outline-none appearance-none"
            >
              <option>Tất cả độ tuổi</option>
              <option v-for="rating in availableAgeRatings" :key="rating.code" :value="rating.code">{{ rating.code }}</option>
            </select>
          </div>
          <div class="flex items-end">
            <button
              @click="clearFilters"
              class="w-full bg-surface-container-highest text-on-surface py-3.5 text-[10px] font-black uppercase tracking-widest hover:bg-red-500 hover:text-white transition-all border border-outline-variant/10 rounded-lg shadow-lg flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-sm">restart_alt</span>
              Xóa tất cả bộ lọc
            </button>
          </div>
        </div>
      </transition>
    </section>

    <!-- Movie Table -->
    <section
      class="bg-surface-container-low border border-outline-variant/10 rounded-lg overflow-hidden"
    >
      <table class="w-full text-left border-collapse">
        <thead>
          <tr
            class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant border-b border-outline-variant/10 bg-surface-container-high/30"
          >
            <th class="px-8 py-6">Bộ phim</th>
            <th class="px-8 py-6">Phân loại</th>
            <th class="px-8 py-6">Đánh giá</th>
            <th class="px-8 py-6">Quốc gia</th>
            <th class="px-8 py-6">Trạng thái</th>
            <th class="px-8 py-6 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-outline-variant/10">
          <tr
            v-for="movie in filteredMovies"
            :key="movie.id"
            @click="openDetailModal(movie)"
            class="group hover:bg-white/5 transition-all text-on-surface cursor-pointer"
          >
            <td class="px-8 py-5">
              <div class="flex items-center gap-6">
                <div
                  class="w-14 h-20 bg-surface-container-highest overflow-hidden rounded-sm relative border border-outline-variant/10"
                >
                  <img
                    :src="movie.posterUrl || '/images/Hopper.webp'"
                    class="w-full h-full object-cover transition-all duration-700"
                  />
                  <div
                    class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"
                  ></div>
                </div>
                <div>
                  <p
                    class="font-bold text-base uppercase tracking-tight group-hover:text-primary transition-colors"
                  >
                    {{ movie.title }}
                  </p>
                  <p
                    class="text-[10px] text-on-surface-variant mt-1.5 uppercase tracking-widest font-bold flex items-center gap-4"
                  >
                    <span class="flex items-center gap-1"
                      ><span class="material-symbols-outlined text-[10px]"
                        >schedule</span
                      >
                      {{ movie.duration }}</span
                    >
                    <span v-if="movie.format" class="flex items-center gap-1"
                      ><span class="material-symbols-outlined text-[10px]"
                        >movie_edit</span
                      >
                      {{ movie.format }}</span
                    >
                  </p>
                </div>
              </div>
            </td>
            <td class="px-8 py-5">
              <span class="text-xs font-semibold text-on-surface-variant">{{
                movie.genreDisplay
              }}</span>
            </td>
            <td class="px-8 py-5">
              <div class="flex items-center gap-1">
                <span
                  class="material-symbols-outlined text-sm text-primary"
                  style="font-variation-settings: &quot;FILL&quot; 1"
                  >star</span
                >
                <span class="text-xs font-black">{{ movie.rating }}</span>
              </div>
            </td>
            <td class="px-8 py-5">
              <span
                class="text-xs font-bold uppercase tracking-widest text-on-surface-variant"
                >{{ movie.country }}</span
              >
            </td>
            <td class="px-8 py-5">
              <span
                :class="{
                  'px-3 py-1.5 text-[9px] font-black uppercase tracking-widest rounded-full': true,
                  'bg-green-500/10 text-green-500 border border-green-500/20':
                    movie.status === 'active',
                  'bg-blue-500/10 text-blue-400 border border-blue-500/20':
                    movie.status === 'upcoming',
                  'bg-surface-container-highest text-on-surface-variant border border-outline-variant/10':
                    movie.status === 'archived',
                }"
                >{{ movie.statusText }}</span
              >
            </td>
            <td class="px-8 py-5 text-right">
              <div class="flex justify-end gap-3">
                <button
                  @click.stop="openEditModal(movie)"
                  title="Chỉnh sửa"
                  class="w-10 h-10 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 hover:border-primary/50 hover:text-primary transition-all"
                >
                  <span class="material-symbols-outlined text-lg"
                    >edit_note</span
                  >
                </button>
                <button
                  @click.stop="deleteMovie(movie.id)"
                  title="Xóa"
                  class="w-10 h-10 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 hover:border-red-500/50 hover:text-red-500 transition-all"
                >
                  <span class="material-symbols-outlined text-lg"
                    >delete_sweep</span
                  >
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Add/Edit Movie Modal (Premium Redesign) -->
    <div
      v-if="isAddModalOpen"
      class="fixed inset-0 z-[100] flex items-center justify-center p-4"
    >
      <div
        class="absolute inset-0 bg-black/90 backdrop-blur-md"
        @click="isAddModalOpen = false"
      ></div>

      <div
        class="bg-surface-container-low w-full max-w-[1400px] rounded-[32px] border border-outline-variant/10 shadow-[0_40px_100px_rgba(0,0,0,0.6)] relative z-10 overflow-hidden grid grid-cols-1 md:grid-cols-3 h-[90vh]"
      >
        <!-- Left: Form Section (2/3) -->
        <div
          class="md:col-span-2 p-12 overflow-y-auto custom-scrollbar border-r border-outline-variant/10"
        >
          <header class="mb-8">
            <div class="flex items-center gap-3 mb-2">
              <span class="w-8 h-1 bg-primary rounded-full"></span>
              <h2
                class="font-headline font-black text-2xl uppercase tracking-tighter italic text-on-surface"
              >
                {{ isEditing ? "Hiệu chỉnh Phim" : "Khai báo Phim Mới" }}
              </h2>
            </div>
            <p
              class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant"
            >
              Thông tin chi tiết nội dung kỹ thuật số
            </p>
          </header>

          <div class="space-y-10">
            <!-- 1. Thông tin định danh & Nội dung -->
            <section class="space-y-6">
              <div
                class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3"
              >
                <span class="text-[10px] font-black uppercase tracking-[0.2em]"
                  >01. Định danh & Nội dung</span
                >
              </div>
              <div class="space-y-2">
                <label
                  class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                  >Tên phim</label
                >
                <input
                  v-model="newMovie.title"
                  type="text"
                  class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-6 text-on-surface transition-all outline-none rounded-t-lg"
                  placeholder="VD: Oppenheimer - Kẻ Hủy Diệt Thế Giới"
                />
              </div>
              <div class="grid grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Thời lượng (Phút)</label
                  >
                  <div
                    class="relative flex items-center bg-surface-container-high rounded-t-lg border-b border-outline-variant/20 focus-within:border-primary transition-all group/duration h-[44px]"
                  >
                    <input
                      v-model="newMovie.duration"
                      type="number"
                      min="1"
                      class="flex-grow bg-transparent text-sm py-2 px-6 text-on-surface outline-none appearance-none no-spin font-bold"
                      placeholder="120"
                    />
                    <div class="flex flex-col items-center justify-center h-full pr-5">
                      <button
                        @click="newMovie.duration = (parseInt(newMovie.duration) || 0) + 1"
                        type="button"
                        class="h-[14px] w-full text-on-surface-variant/30 hover:text-primary transition-all flex items-center justify-center leading-none"
                      >
                        <span class="material-symbols-outlined text-[10px] scale-75">expand_less</span>
                      </button>
                      <button
                        @click="newMovie.duration = Math.max(1, (parseInt(newMovie.duration) || 0) - 1)"
                        type="button"
                        class="h-[14px] w-full text-on-surface-variant/30 hover:text-primary transition-all flex items-center justify-center leading-none"
                      >
                        <span class="material-symbols-outlined text-[10px] scale-75">expand_more</span>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Trailer URL (YouTube)</label
                  >
                  <input
                    v-model="newMovie.trailerUrl"
                    type="text"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg"
                    placeholder="https://youtube.com/..."
                  />
                </div>
              </div>
            </section>

            <!-- 2. Thông tin kỹ thuật & Sản xuất -->
            <section class="space-y-6">
              <div
                class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3"
              >
                <span class="text-[10px] font-black uppercase tracking-[0.2em]"
                  >02. Kỹ thuật & Sản xuất</span
                >
              </div>
              <div class="grid grid-cols-3 gap-6">
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Quốc gia</label
                  >
                  <select
                    v-model="newMovie.country"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 pr-12 text-on-surface transition-all outline-none rounded-t-lg appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25rem_1.25rem] bg-[right_1rem_center] bg-no-repeat"
                  >
                    <option>Mỹ</option>
                    <option>Nhật Bản</option>
                    <option>Hàn Quốc</option>
                    <option>Việt Nam</option>
                    <option>Pháp</option>
                  </select>
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Năm sản xuất</label
                  >
                  <input
                    v-model="newMovie.productionYear"
                    type="text"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg"
                    placeholder="2024"
                  />
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Ngôn ngữ gốc</label
                  >
                  <select
                    v-model="newMovie.originalLanguage"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 pr-12 text-on-surface transition-all outline-none rounded-t-lg appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25rem_1.25rem] bg-[right_1rem_center] bg-no-repeat"
                  >
                    <option>Tiếng Anh</option>
                    <option>Tiếng Hàn</option>
                    <option>Tiếng Nhật</option>
                    <option>Tiếng Việt</option>
                    <option>Tiếng Pháp</option>
                  </select>
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Loại hình hiển thị</label
                  >
                  <select
                    v-model="newMovie.versionType"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 pr-12 text-on-surface transition-all outline-none rounded-t-lg appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25rem_1.25rem] bg-[right_1rem_center] bg-no-repeat"
                  >
                    <option>Phụ đề Tiếng Việt</option>
                    <option>Thuyết minh Tiếng Việt</option>
                    <option>Lồng tiếng Tiếng Việt</option>
                    <option>Bản gốc (No Sub)</option>
                  </select>
                </div>
              </div>

              <!-- Genre & Format Selectors -->
              <div class="space-y-4">
                <div class="flex justify-between items-center px-1">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant"
                    >Thể loại phim</label
                  >
                  <div class="flex gap-4">
                    <button
                      @click="resetGenres"
                      type="button"
                      class="text-on-surface-variant/40 hover:text-red-500 transition-all"
                    >
                      <span class="material-symbols-outlined text-base"
                        >restart_alt</span
                      >
                    </button>
                    <button
                      @click="scrollLeft"
                      type="button"
                      class="text-on-surface-variant/40 hover:text-primary transition-all"
                    >
                      <span class="material-symbols-outlined text-base"
                        >chevron_left</span
                      >
                    </button>
                    <button
                      @click="scrollRight"
                      type="button"
                      class="text-on-surface-variant/40 hover:text-primary transition-all"
                    >
                      <span class="material-symbols-outlined text-base"
                        >chevron_right</span
                      >
                    </button>
                  </div>
                </div>
                <div class="relative group/genre">
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
                      :class="
                        selectedGenres.some(g => g.id === genre.id)
                          ? 'bg-primary text-on-primary'
                          : 'bg-surface-container-high/50 text-on-surface-variant'
                      "
                      class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-bold uppercase tracking-widest transition-all whitespace-nowrap min-w-[100px]"
                    >
                      {{ genre.name }}
                    </button>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-6">
                <div class="space-y-4">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Định dạng hỗ trợ (Tick chọn nhiều)</label
                  >
                  <div class="flex flex-wrap gap-2">
                    <button
                      v-for="fmt in availableFormats"
                      :key="fmt"
                      type="button"
                      @click="toggleFormat(fmt)"
                      :class="
                        selectedFormats.includes(fmt)
                          ? 'bg-primary text-on-primary'
                          : 'bg-surface-container-high/50 text-on-surface-variant'
                      "
                      class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-bold uppercase tracking-widest transition-all min-w-[60px]"
                    >
                      {{ fmt }}
                    </button>
                  </div>
                </div>
                <div class="space-y-4">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Định dạng chiếu chính (Chọn một)</label
                  >
                  <div class="flex flex-wrap gap-2">
                    <button
                      v-for="fmt in availableFormats"
                      :key="fmt"
                      type="button"
                      @click="newMovie.format = fmt"
                      :class="
                        newMovie.format === fmt
                          ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
                          : 'bg-surface-container-high/50 text-on-surface-variant'
                      "
                      class="px-4 py-2 rounded-full border border-outline-variant/10 text-[9px] font-black uppercase tracking-widest transition-all min-w-[60px] hover:border-primary/50"
                    >
                      {{ fmt }}
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <!-- 3. Thông tin vận hành & Kiểm soát -->
            <section class="space-y-6">
              <div
                class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3"
              >
                <span class="text-[10px] font-black uppercase tracking-[0.2em]"
                  >03. Vận hành & Kiểm soát</span
                >
              </div>
              <div class="grid grid-cols-3 gap-6">
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Phân loại độ tuổi</label
                  >
                  <select
                    v-model="newMovie.ageRating"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 pr-12 text-on-surface transition-all outline-none rounded-t-lg appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25rem_1.25rem] bg-[right_1rem_center] bg-no-repeat"
                  >
                    <option v-for="rating in availableAgeRatings" :key="rating.code" :value="rating.code">
                      {{ rating.code }} ({{ rating.name }})
                    </option>
                  </select>
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Giá vé gốc (VNĐ)</label
                  >
                  <input
                    v-model="newMovie.basePrice"
                    type="number"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg"
                  />
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Trạng thái</label
                  >
                  <select
                    v-model="newMovie.status"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3.5 px-6 pr-12 text-on-surface transition-all outline-none rounded-t-lg appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25rem_1.25rem] bg-[right_1rem_center] bg-no-repeat"
                  >
                    <option value="active">Đang chiếu</option>
                    <option value="upcoming">Sắp chiếu</option>
                    <option value="archived">Ngừng chiếu</option>
                  </select>
                </div>
              </div>
              <div class="grid grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Ngày khởi chiếu</label
                  >
                  <input
                    v-model="newMovie.startDate"
                    type="date"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg"
                  />
                </div>
                <div class="space-y-2">
                  <label
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                    >Ngày kết thúc (Dự kiến)</label
                  >
                  <input
                    v-model="newMovie.endDate"
                    type="date"
                    class="w-full bg-surface-container-high border-b border-outline-variant/20 focus:border-primary text-sm py-3 px-4 text-on-surface transition-all outline-none rounded-t-lg"
                  />
                </div>
              </div>
              <div class="space-y-2">
                <label
                  class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1 italic"
                  >Ghi chú nội bộ cho Admin (Không hiển thị cho khách
                  hàng)</label
                >
                <textarea
                  v-model="newMovie.internalNotes"
                  rows="2"
                  class="w-full bg-surface-container-high border border-outline-variant/10 focus:border-primary/50 text-xs py-3 px-4 text-on-surface transition-all outline-none rounded-lg resize-none"
                  placeholder="VD: Ưu tiên suất chiếu tối, Kiểm tra kỹ độ tuổi..."
                ></textarea>
              </div>
            </section>

            <!-- 4. Media & Mô tả -->
            <section class="space-y-6">
              <div
                class="flex items-center gap-2 text-primary border-l-2 border-primary pl-3"
              >
                <span class="text-[10px] font-black uppercase tracking-[0.2em]"
                  >04. Media & Mô tả</span
                >
              </div>
              <div class="space-y-2">
                <label
                  class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                  >Hình ảnh Poster</label
                >
                <input
                  ref="fileInput"
                  type="file"
                  @change="onFileChange"
                  accept="image/*"
                  class="hidden"
                />
                <button
                  @click="triggerFileInput"
                  type="button"
                  class="w-full bg-surface-container-high border border-dashed border-outline-variant/30 hover:border-primary/50 hover:bg-primary/5 transition-all py-8 rounded-lg flex flex-col items-center justify-center gap-2 group"
                >
                  <span
                    class="material-symbols-outlined text-3xl text-on-surface-variant group-hover:text-primary transition-colors"
                    >add_photo_alternate</span
                  >
                  <span
                    class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant group-hover:text-primary transition-colors"
                    >Tải ảnh poster chính thức</span
                  >
                </button>
              </div>
              <div class="space-y-2">
                <label
                  class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant ml-1"
                  >Tóm tắt nội dung</label
                >
                <textarea
                  v-model="newMovie.description"
                  rows="5"
                  class="w-full bg-surface-container-high border border-outline-variant/10 focus:border-primary/50 text-sm py-4 px-4 text-on-surface transition-all outline-none rounded-lg resize-none"
                  placeholder="Viết mô tả ngắn về cốt truyện phim..."
                ></textarea>
              </div>
            </section>
          </div>
        </div>

        <!-- Right: Preview & Actions (1/3) -->
        <div
          class="md:col-span-1 bg-surface-container-high/30 p-12 flex flex-col justify-between relative overflow-hidden h-full"
        >
          <!-- Background Decoration -->
          <div
            class="absolute top-0 right-0 w-64 h-64 bg-primary/5 blur-[100px] -z-10"
          ></div>

          <div class="space-y-6">
            <p
              class="text-[10px] font-black uppercase tracking-widest text-center text-primary"
            >
              Xem trước hiển thị
            </p>

            <div
              @click="triggerFileInput"
              class="aspect-[2/3] max-w-[260px] mx-auto w-full rounded-2xl overflow-hidden border border-outline-variant/20 shadow-2xl bg-surface-container-highest relative group cursor-pointer"
            >
              <img
                v-if="newMovie.posterUrl"
                :src="newMovie.posterUrl"
                class="w-full h-full object-cover transition-transform group-hover:scale-105"
                @error="newMovie.posterUrl = ''"
              />
              <div
                v-else
                class="w-full h-full flex flex-col items-center justify-center p-6 text-center text-on-surface-variant/40"
              >
                <span class="material-symbols-outlined text-4xl mb-3"
                  >image_not_supported</span
                >
                <p class="text-[9px] font-bold uppercase tracking-widest">
                  Chưa có ảnh poster
                </p>
              </div>
              <div
                class="absolute inset-0 bg-gradient-to-t from-black/90 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-end p-6"
              >
                <p
                  class="text-white font-black text-xs uppercase tracking-tighter italic leading-none mb-1"
                >
                  {{ newMovie.title || "Tiêu đề phim" }}
                </p>
                <p
                  class="text-primary text-[8px] font-black uppercase tracking-widest"
                >
                  {{ selectedGenres.join(" • ") || "Thể loại" }}
                </p>
              </div>
            </div>

            <div
              class="bg-surface-container-lowest/50 p-4 rounded-lg border border-outline-variant/10"
            >
              <div class="flex justify-between items-center mb-2">
                <span
                  class="text-[9px] font-bold text-on-surface-variant uppercase"
                  >Rating Demo</span
                >
                <span class="text-primary font-black text-xs">5.0</span>
              </div>
              <div
                class="w-full h-1 bg-outline-variant/20 rounded-full overflow-hidden"
              >
                <div class="w-full h-full bg-primary"></div>
              </div>
            </div>
          </div>

          <div
            class="flex items-center gap-3 mt-8 pt-8 border-t border-outline-variant/10"
          >
            <button
              @click="isAddModalOpen = false"
              class="flex-grow py-4 bg-white/5 text-on-surface/40 font-black text-[9px] uppercase tracking-[0.2em] rounded-xl border border-white/5 hover:bg-white/10 hover:text-white transition-all"
            >
              Hủy bỏ
            </button>
            <button
              @click="saveMovie"
              class="flex-[2] py-4 bg-gradient-to-br from-primary to-primary-container text-on-primary font-black text-[9px] uppercase tracking-[0.2em] rounded-xl hover:scale-[1.02] active:scale-[0.98] transition-all shadow-xl shadow-primary/20 flex items-center justify-center gap-2 group"
            >
              <span
                class="material-symbols-outlined text-sm group-hover:rotate-12 transition-transform"
                >rocket_launch</span
              >
              {{ isEditing ? "Cập nhật" : "Xuất bản" }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Movie Detail Modal (Ultimate Aesthetic Masterpiece) -->
    <div
      v-if="isDetailModalOpen && selectedMovie"
      class="fixed inset-0 z-[100] flex items-center justify-center p-4 md:p-10"
    >
      <div
        class="absolute inset-0 bg-[#0a0a0a]/90 backdrop-blur-[100px]"
        @click="isDetailModalOpen = false"
      ></div>

      <!-- Immersive Background Glows -->
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div
          class="absolute -top-[20%] -left-[10%] w-[60%] h-[60%] bg-primary/10 blur-[150px] rounded-full animate-pulse"
        ></div>
        <div
          class="absolute -bottom-[20%] -right-[10%] w-[50%] h-[50%] bg-primary/5 blur-[120px] rounded-full"
        ></div>
      </div>

      <div
        class="bg-white/[0.02] w-full max-w-7xl h-full max-h-[900px] rounded-[48px] border border-white/10 shadow-[0_40px_100px_rgba(0,0,0,0.8)] relative z-10 overflow-hidden flex flex-col md:flex-row"
      >
        <!-- Sidebar: The Cinematic Anchor -->
        <div
          class="w-full md:w-[480px] h-full relative shrink-0 overflow-hidden"
        >
          <div class="absolute inset-0 z-0">
            <img
              :src="selectedMovie.posterUrl || '/images/Hopper.webp'"
              class="w-full h-full object-cover scale-110 blur-2xl opacity-30"
            />
          </div>

          <!-- Floating Poster Card -->
          <div class="relative z-10 h-full p-12 flex flex-col justify-between">
            <div
              class="aspect-[2/3] w-full rounded-[32px] overflow-hidden shadow-[0_30px_60px_rgba(0,0,0,0.6)] border border-white/10 group cursor-pointer relative"
            >
              <img
                :src="selectedMovie.posterUrl || '/images/Hopper.webp'"
                class="w-full h-full object-cover transition-transform duration-1000 group-hover:scale-110"
              />
              <div
                class="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 flex items-center justify-center"
              >
                <div
                  class="w-20 h-20 rounded-full bg-primary/20 backdrop-blur-2xl border border-primary/40 flex items-center justify-center scale-75 group-hover:scale-100 transition-transform duration-500"
                >
                  <span
                    class="material-symbols-outlined text-primary text-4xl translate-x-0.5"
                    >play_arrow</span
                  >
                </div>
              </div>
            </div>

            <div class="space-y-6 pt-10">
              <div class="flex items-center gap-3">
                <span
                  class="px-4 py-1.5 bg-red-600/20 text-red-500 border border-red-500/20 text-[10px] font-black rounded-lg uppercase tracking-widest"
                  >{{ selectedMovie.ageRating || "P" }}</span
                >
                <span
                  class="px-4 py-1.5 bg-primary/10 text-primary border border-primary/20 text-[10px] font-black rounded-lg uppercase tracking-widest"
                  >{{
                    selectedMovie.status === "active"
                      ? "Đang chiếu"
                      : "Sắp chiếu"
                  }}</span
                >
              </div>
              <div>
                <h2
                  class="text-5xl font-black text-white italic tracking-tighter uppercase leading-[0.85] mb-4"
                >
                  {{ selectedMovie.title }}
                </h2>
                <div class="flex items-center gap-4">
                  <div
                    class="flex items-center gap-2 py-1 px-3 bg-white/5 rounded-full border border-white/5"
                  >
                    <span
                      class="material-symbols-outlined text-primary text-sm"
                      style="font-variation-settings: &quot;FILL&quot; 1"
                      >star</span
                    >
                    <span class="text-sm font-black text-white">{{
                      selectedMovie.rating
                    }}</span>
                  </div>
                  <span
                    class="text-[10px] font-bold text-white/40 uppercase tracking-widest"
                    >{{ selectedMovie.duration }}</span
                  >
                  <span
                    class="text-[10px] font-bold text-white/40 uppercase tracking-widest"
                    >{{ selectedMovie.country }}</span
                  >
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Main Body: The Data Engine -->
        <div
          class="flex-grow flex flex-col h-full bg-black/20 backdrop-blur-md border-l border-white/10"
        >
          <!-- Top Bar: Quick Metadata -->
          <div
            class="px-16 py-10 flex justify-between items-center shrink-0 border-b border-white/5"
          >
            <div class="flex items-center gap-8">
              <div
                v-for="(info, i) in [
                  {
                    label: 'Ngôn ngữ',
                    val: selectedMovie.language || 'Tiếng Việt',
                  },
                  { label: 'Năm', val: selectedMovie.productionYear || '2024' },
                  { label: 'Hệ thống ID', val: '#' + selectedMovie.id },
                ]"
                :key="i"
                class="space-y-1"
              >
                <p
                  class="text-[8px] font-black text-white/30 uppercase tracking-[0.2em]"
                >
                  {{ info.label }}
                </p>
                <p class="text-xs font-bold text-white uppercase">
                  {{ info.val }}
                </p>
              </div>
            </div>
            <div class="flex gap-4">
              <button
                @click="
                  openEditModal(selectedMovie);
                  isDetailModalOpen = false;
                "
                class="px-8 py-3 bg-white/5 border border-white/10 text-white text-[10px] font-black uppercase tracking-widest rounded-xl hover:bg-primary hover:text-on-primary transition-all duration-500"
              >
                Edit Asset
              </button>
              <button
                @click="isDetailModalOpen = false"
                class="w-12 h-12 flex items-center justify-center rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-all"
              >
                <span class="material-symbols-outlined text-white">close</span>
              </button>
            </div>
          </div>

          <!-- Content Scrollable Area -->
          <div
            class="flex-grow overflow-y-auto custom-scrollbar p-16 space-y-16"
          >
            <!-- Quick Insights Grid -->
            <div class="grid grid-cols-4 gap-6">
              <div
                v-for="(stat, idx) in [
                  {
                    label: 'Suất chiếu',
                    val: '24',
                    icon: 'confirmation_number',
                    color: 'from-blue-500/20 to-transparent',
                  },
                  {
                    label: 'Tỷ lệ lấp đầy',
                    val: '68%',
                    icon: 'groups',
                    color: 'from-primary/20 to-transparent',
                  },
                  {
                    label: 'Doanh thu',
                    val: '4.2M',
                    icon: 'payments',
                    color: 'from-green-500/20 to-transparent',
                  },
                  {
                    label: 'Giá vé gốc',
                    val:
                      (selectedMovie.basePrice || 85000).toLocaleString() + 'đ',
                    icon: 'sell',
                    color: 'from-white/10 to-transparent',
                  },
                ]"
                :key="idx"
                class="relative group/stat p-8 bg-white/[0.03] border border-white/[0.08] rounded-[40px] overflow-hidden transition-all duration-500 hover:bg-white/[0.05] hover:-translate-y-1"
              >
                <div
                  class="absolute inset-0 bg-gradient-to-br opacity-0 group-hover/stat:opacity-100 transition-opacity duration-700"
                  :class="stat.color"
                ></div>
                <div class="relative z-10 space-y-4">
                  <span
                    class="material-symbols-outlined text-2xl text-white/20 group-hover/stat:text-white transition-colors duration-500"
                    >{{ stat.icon }}</span
                  >
                  <div>
                    <p
                      class="text-[8px] font-black text-white/30 uppercase tracking-[0.2em] mb-1"
                    >
                      {{ stat.label }}
                    </p>
                    <p
                      class="text-2xl font-black text-white italic tracking-tighter"
                    >
                      {{ stat.val }}
                    </p>
                  </div>
                </div>
              </div>

            </div>

            <div class="grid grid-cols-12 gap-16">
              <div class="col-span-7 space-y-12">
                <section class="space-y-6">
                  <div class="flex items-center gap-4">
                    <div class="w-12 h-px bg-primary/40"></div>
                    <p
                      class="text-[10px] font-black text-primary uppercase tracking-[0.4em]"
                    >
                      Storyline
                    </p>
                  </div>
                  <p
                    class="text-2xl text-on-surface/80 leading-relaxed font-medium italic text-justify"
                  >
                    {{
                      selectedMovie.description ||
                      "Chưa có tóm tắt nội dung chính thức cho bộ phim này. Thông tin sẽ sớm được cập nhật."
                    }}
                  </p>
                </section>

                <div class="grid grid-cols-2 gap-12">
                  <section class="space-y-6">
                    <p
                      class="text-[10px] font-black text-white/20 uppercase tracking-[0.2em]"
                    >
                      Release Timeline
                    </p>
                    <div class="space-y-6">
                      <div class="flex items-center gap-4 group/tm">
                        <div
                          class="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center group-hover/tm:bg-primary transition-all duration-500"
                        >
                          <span
                            class="material-symbols-outlined text-lg text-primary group-hover/tm:text-on-primary"
                            >rocket_launch</span
                          >
                        </div>
                        <div>
                          <p
                            class="text-[8px] font-black text-white/30 uppercase"
                          >
                            Khởi chiếu
                          </p>
                          <p class="text-sm font-bold text-white">
                            {{ selectedMovie.startDate || "TBA" }}
                          </p>
                        </div>
                      </div>
                      <div class="flex items-center gap-4 group/tm">
                        <div
                          class="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center group-hover/tm:bg-primary transition-all duration-500"
                        >
                          <span
                            class="material-symbols-outlined text-lg text-primary group-hover/tm:text-on-primary"
                            >timer_off</span
                          >
                        </div>
                        <div>
                          <p
                            class="text-[8px] font-black text-white/30 uppercase"
                          >
                            Kết thúc
                          </p>
                          <p class="text-sm font-bold text-white">
                            {{ selectedMovie.endDate || "TBA" }}
                          </p>
                        </div>
                      </div>
                    </div>
                  </section>
                </div>
              </div>

              <div class="col-span-5 space-y-12">
                <section
                  class="p-10 bg-white/[0.03] border border-white/[0.08] rounded-[48px] space-y-8 relative overflow-hidden group/box"
                >
                  <div
                    class="absolute -top-10 -right-10 w-32 h-32 bg-primary/10 blur-3xl rounded-full opacity-0 group-hover/box:opacity-100 transition-opacity duration-1000"
                  ></div>
                  <div>
                    <p
                      class="text-[9px] font-black text-white/20 uppercase tracking-[0.2em] mb-4"
                    >
                      Genres & Categorization
                    </p>
                    <div class="flex flex-wrap gap-2">
                      <span
                        v-for="g in selectedMovie.genre?.split(', ')"
                        :key="g"
                        class="px-4 py-2 bg-white/5 border border-white/5 text-[9px] font-black text-white uppercase tracking-widest rounded-xl hover:border-primary/50 transition-colors"
                        >{{ g }}</span
                      >
                    </div>
                  </div>
                  <div>
                    <p
                      class="text-[9px] font-black text-white/20 uppercase tracking-[0.2em] mb-4"
                    >
                      Screening Formats
                    </p>
                    <div class="flex flex-wrap gap-2">
                      <span
                        v-for="f in selectedMovie.format?.split(', ')"
                        :key="f"
                        class="px-4 py-2 bg-primary text-on-primary text-[9px] font-black uppercase tracking-widest rounded-xl shadow-lg shadow-primary/20"
                        >{{ f }}</span
                      >
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </div>

          <!-- Bottom Action Bar (Footer) -->
          <div
            class="px-16 py-8 shrink-0 bg-white/[0.02] border-t border-white/5 flex items-center justify-between"
          >
            <div class="flex items-center gap-4 text-white/20">
              <span class="material-symbols-outlined text-sm">security</span>
              <p class="text-[9px] font-bold uppercase tracking-widest">
                Administrative Access Only • Secure Data Portal
              </p>
            </div>
            <div class="flex gap-4">
              <button
                @click="
                  openEditModal(selectedMovie);
                  isDetailModalOpen = false;
                "
                class="px-10 py-4 bg-primary text-on-primary font-black text-[10px] uppercase tracking-[0.2em] rounded-2xl hover:brightness-110 hover:scale-[1.02] transition-all shadow-xl shadow-primary/20 flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-sm"
                  >edit_square</span
                >
                Cập nhật nội dung
              </button>
              <button
                class="px-10 py-4 bg-white/5 border border-white/10 text-white font-black text-[10px] uppercase tracking-[0.2em] rounded-2xl hover:bg-white/10 transition-all flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-sm"
                  >bar_chart_4_bars</span
                >
                Trích xuất báo cáo
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.no-spin::-webkit-outer-spin-button,
.no-spin::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.no-spin {
  -moz-appearance: textfield;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(var(--primary-rgb, 245, 197, 24), 0.3);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(var(--primary-rgb, 245, 197, 24), 0.5);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
