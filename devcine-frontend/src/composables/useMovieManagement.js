import { ref, computed, watch } from "vue";
import axios from "@/api/axios";

/**
 * Trung tâm logic cho màn Quản lý Phim: tải dữ liệu, lọc, sắp xếp, phân trang
 * (client-side — dataset nhỏ), chọn nhiều + thao tác hàng loạt, CRUD và định dạng.
 * Khi catalog lớn lên có thể chuyển fetch sang endpoint phân trang server-side
 * mà không đổi giao diện component nhờ gói gọn ở đây.
 */
export function useMovieManagement() {
  // ===== State danh sách & danh mục =====
  const movies = ref([]);
  const isLoading = ref(false);
  const loadError = ref(false);

  const genres = ref([]);
  const availableFormats = ref([]);
  const availableAgeRatings = ref([]);

  // ===== Bộ lọc =====
  const searchQuery = ref("");
  const filterCountry = ref("Tất cả quốc gia");
  const filterStatus = ref("Tất cả trạng thái");
  const filterRating = ref("Tất cả đánh giá");
  const filterAgeRating = ref("Tất cả độ tuổi");
  const filterGenre = ref("Tất cả phân loại");
  const filterFormat = ref("Tất cả định dạng");

  // ===== Sắp xếp =====
  const sortKey = ref("id"); // id | title | releaseDate | rating | basePrice
  const sortDir = ref("desc"); // asc | desc

  // ===== Phân trang =====
  const page = ref(1);
  const pageSize = ref(10);

  // ===== Chọn nhiều =====
  const selectedIds = ref(new Set());

  // ---------- Helpers định dạng ----------
  const optimizeCloudinaryUrl = (url) => {
    if (!url) return "";
    if (url.includes("cloudinary.com") && url.includes("/upload/") && !url.includes("f_auto")) {
      return url.replace("/upload/", "/upload/f_auto,q_auto/");
    }
    return url;
  };

  const formatPrice = (value) => {
    if (value == null || value === "") return "—";
    return new Intl.NumberFormat("vi-VN").format(value) + "đ";
  };

  const formatDate = (value) => {
    if (!value) return "—";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "—";
    return `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`;
  };

  const statusToText = (status) =>
    status === "active" ? "Đang chiếu" : status === "upcoming" ? "Sắp chiếu" : "Lưu trữ";

  /**
   * Badge vòng đời phim dựa trên releaseDate/endDate so với hôm nay.
   * tone: future | live | ending | ended | none
   */
  const releaseInfo = (movie) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const release = movie.releaseDate ? new Date(movie.releaseDate) : null;
    const end = movie.endDate ? new Date(movie.endDate) : null;
    const dayMs = 86400000;

    if (release && release > today) {
      const days = Math.ceil((release - today) / dayMs);
      return { label: `Khởi chiếu sau ${days} ngày`, tone: "future" };
    }
    if (end) {
      const days = Math.ceil((end - today) / dayMs);
      if (days < 0) return { label: "Đã kết thúc", tone: "ended" };
      if (days === 0) return { label: "Kết thúc hôm nay", tone: "ending" };
      if (days <= 7) return { label: `Còn ${days} ngày`, tone: "ending" };
      return { label: `Còn ${days} ngày`, tone: "live" };
    }
    return { label: "", tone: "none" };
  };

  // ---------- Tải dữ liệu ----------
  const fetchCategories = async () => {
    try {
      const [genresRes, formatsRes, ageRatingsRes] = await Promise.all([
        axios.get("/categories/genres"),
        axios.get("/categories/formats"),
        axios.get("/categories/age-ratings"),
      ]);
      genres.value = genresRes.data;
      availableFormats.value = formatsRes.data.map((f) => f.name);
      availableAgeRatings.value = ageRatingsRes.data;
    } catch (error) {
      console.error("Lỗi khi tải danh mục:", error);
      genres.value = [{ id: 1, name: "Hành động" }];
      availableFormats.value = ["2D", "3D", "IMAX"];
      availableAgeRatings.value = [{ code: "P", name: "Mọi đối tượng" }];
    }
  };

  const fetchMovies = async () => {
    isLoading.value = true;
    loadError.value = false;
    try {
      const response = await axios.get("/movies");
      movies.value = response.data.map((m) => ({
        ...m,
        duration: m.durationMins ? m.durationMins + " Phút" : "N/A",
        genreDisplay: m.genres?.map((g) => g.name).join(", ") || "N/A",
        statusText: statusToText(m.status),
      }));
    } catch (error) {
      console.error("Lỗi khi tải danh sách phim:", error);
      loadError.value = true;
    } finally {
      isLoading.value = false;
    }
  };

  // ---------- Lọc + sắp xếp ----------
  const filteredMovies = computed(() => {
    const q = searchQuery.value.toLowerCase().trim();
    const result = movies.value.filter((movie) => {
      const matchesSearch =
        !q ||
        movie.title.toLowerCase().includes(q) ||
        movie.titleVietnamese?.toLowerCase().includes(q);

      const matchesCountry =
        filterCountry.value === "Tất cả quốc gia" || movie.country === filterCountry.value;

      const matchesStatus =
        filterStatus.value === "Tất cả trạng thái" ||
        (filterStatus.value === "Đang chiếu" && movie.status === "active") ||
        (filterStatus.value === "Sắp chiếu" && movie.status === "upcoming") ||
        (filterStatus.value === "Lưu trữ" && movie.status === "archived");

      const matchesAge =
        filterAgeRating.value === "Tất cả độ tuổi" || movie.ageRating === filterAgeRating.value;

      const matchesGenre =
        filterGenre.value === "Tất cả phân loại" ||
        (movie.genres && movie.genres.some((g) => g.name === filterGenre.value));

      const matchesFormat =
        filterFormat.value === "Tất cả định dạng" ||
        movie.format === filterFormat.value ||
        (movie.supportedFormats && movie.supportedFormats.split(", ").includes(filterFormat.value));

      let matchesRating = true;
      const r = parseFloat(movie.rating);
      if (filterRating.value === "4.5 - 5.0 sao") matchesRating = r >= 4.5;
      else if (filterRating.value === "4.0 - 4.5 sao") matchesRating = r >= 4.0 && r < 4.5;
      else if (filterRating.value === "Dưới 4.0 sao") matchesRating = r < 4.0;

      return (
        matchesSearch &&
        matchesCountry &&
        matchesStatus &&
        matchesAge &&
        matchesGenre &&
        matchesFormat &&
        matchesRating
      );
    });

    const dir = sortDir.value === "asc" ? 1 : -1;
    const key = sortKey.value;
    return [...result].sort((a, b) => {
      let av = a[key];
      let bv = b[key];
      if (key === "releaseDate") {
        av = av ? new Date(av).getTime() : 0;
        bv = bv ? new Date(bv).getTime() : 0;
      } else if (key === "rating" || key === "basePrice" || key === "id") {
        av = parseFloat(av) || 0;
        bv = parseFloat(bv) || 0;
      } else {
        av = (av ?? "").toString().toLowerCase();
        bv = (bv ?? "").toString().toLowerCase();
      }
      if (av < bv) return -1 * dir;
      if (av > bv) return 1 * dir;
      return 0;
    });
  });

  const totalFiltered = computed(() => filteredMovies.value.length);
  const totalPages = computed(() => Math.max(1, Math.ceil(totalFiltered.value / pageSize.value)));

  const pagedMovies = computed(() => {
    // Trang hiệu lực (không mutate state trong computed) — phòng khi filter làm giảm số trang
    const currentPage = Math.min(page.value, totalPages.value);
    const start = (currentPage - 1) * pageSize.value;
    return filteredMovies.value.slice(start, start + pageSize.value);
  });

  // Khi bộ lọc thu hẹp kết quả khiến trang hiện tại vượt quá tổng trang → kéo về trang cuối
  watch(totalPages, (total) => {
    if (page.value > total) page.value = total;
  });

  // Đổi bộ lọc/tìm kiếm → quay về trang 1
  watch(
    [searchQuery, filterCountry, filterStatus, filterRating, filterAgeRating, filterGenre, filterFormat],
    () => {
      page.value = 1;
    },
  );

  const setSort = (key) => {
    if (sortKey.value === key) {
      sortDir.value = sortDir.value === "asc" ? "desc" : "asc";
    } else {
      sortKey.value = key;
      sortDir.value = "asc";
    }
  };

  const goToPage = (n) => {
    page.value = Math.min(Math.max(1, n), totalPages.value);
  };

  const hasActiveFilters = computed(
    () =>
      filterCountry.value !== "Tất cả quốc gia" ||
      filterStatus.value !== "Tất cả trạng thái" ||
      filterRating.value !== "Tất cả đánh giá" ||
      filterAgeRating.value !== "Tất cả độ tuổi" ||
      filterGenre.value !== "Tất cả phân loại" ||
      filterFormat.value !== "Tất cả định dạng" ||
      !!searchQuery.value,
  );

  const clearFilters = () => {
    filterCountry.value = "Tất cả quốc gia";
    filterStatus.value = "Tất cả trạng thái";
    filterRating.value = "Tất cả đánh giá";
    filterAgeRating.value = "Tất cả độ tuổi";
    filterGenre.value = "Tất cả phân loại";
    filterFormat.value = "Tất cả định dạng";
    searchQuery.value = "";
    page.value = 1;
  };

  // ---------- Chọn nhiều ----------
  const isSelected = (id) => selectedIds.value.has(id);

  const toggleSelect = (id) => {
    const next = new Set(selectedIds.value);
    next.has(id) ? next.delete(id) : next.add(id);
    selectedIds.value = next;
  };

  const isPageAllSelected = computed(
    () => pagedMovies.value.length > 0 && pagedMovies.value.every((m) => selectedIds.value.has(m.id)),
  );

  const toggleSelectAllOnPage = () => {
    const next = new Set(selectedIds.value);
    if (isPageAllSelected.value) {
      pagedMovies.value.forEach((m) => next.delete(m.id));
    } else {
      pagedMovies.value.forEach((m) => next.add(m.id));
    }
    selectedIds.value = next;
  };

  const clearSelection = () => {
    selectedIds.value = new Set();
  };

  const selectionCount = computed(() => selectedIds.value.size);

  // ---------- CRUD + bulk ----------
  const getMovieDetail = async (id) => {
    const res = await axios.get(`/movies/${id}`);
    return res.data;
  };

  const getMovieStats = async (id) => {
    const res = await axios.get(`/movies/${id}/stats`);
    return res.data;
  };

  const deleteMovie = async (id) => {
    await axios.delete(`/movies/${id}`);
    await fetchMovies();
  };

  const saveMovie = async (payload, id) => {
    if (id) {
      await axios.put(`/movies/${id}`, payload);
    } else {
      await axios.post("/movies", payload);
    }
    await fetchMovies();
  };

  /** Đổi nhanh trạng thái 1 phim ngay trên dòng (optimistic). */
  const quickUpdateStatus = async (movie, newStatus) => {
    const prev = movie.status;
    movie.status = newStatus;
    movie.statusText = statusToText(newStatus);
    try {
      await axios.patch("/movies/bulk-status", { ids: [movie.id], status: newStatus });
    } catch (error) {
      console.error("Lỗi đổi trạng thái:", error);
      movie.status = prev;
      movie.statusText = statusToText(prev);
      alert("Đổi trạng thái thất bại. Vui lòng thử lại!");
    }
  };

  const bulkUpdateStatus = async (newStatus) => {
    const ids = [...selectedIds.value];
    if (!ids.length) return;
    await axios.patch("/movies/bulk-status", { ids, status: newStatus });
    clearSelection();
    await fetchMovies();
  };

  const bulkDelete = async () => {
    const ids = [...selectedIds.value];
    if (!ids.length) return;
    await axios.delete("/movies/bulk", { data: { ids } });
    clearSelection();
    await fetchMovies();
  };

  return {
    // state
    movies,
    isLoading,
    loadError,
    genres,
    availableFormats,
    availableAgeRatings,
    // filters
    searchQuery,
    filterCountry,
    filterStatus,
    filterRating,
    filterAgeRating,
    filterGenre,
    filterFormat,
    hasActiveFilters,
    clearFilters,
    // sort + pagination
    sortKey,
    sortDir,
    setSort,
    page,
    pageSize,
    totalPages,
    totalFiltered,
    pagedMovies,
    goToPage,
    // selection
    selectedIds,
    isSelected,
    toggleSelect,
    isPageAllSelected,
    toggleSelectAllOnPage,
    clearSelection,
    selectionCount,
    // data ops
    fetchCategories,
    fetchMovies,
    getMovieDetail,
    getMovieStats,
    deleteMovie,
    saveMovie,
    quickUpdateStatus,
    bulkUpdateStatus,
    bulkDelete,
    // helpers
    optimizeCloudinaryUrl,
    formatPrice,
    formatDate,
    statusToText,
    releaseInfo,
    filteredMovies,
  };
}
