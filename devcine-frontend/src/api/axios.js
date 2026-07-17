import axios from 'axios';

const instance = axios.create({
  baseURL: (import.meta.env.VITE_API_URL || '') + '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add request interceptor for tokens
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: khi token hết hạn/không hợp lệ (401), tự đăng xuất để
// trạng thái UI (navbar, menu...) phản ánh đúng là CHƯA đăng nhập, tránh tình
// trạng "còn token cũ" hiển thị tên user trong khi server đã từ chối.
// Xử lý TẬP TRUNG một lần cho cả loạt 401 đồng thời: 1 toast "hết phiên" + điều hướng về đăng nhập,
// thay vì để từng view tự bung toast (gây nhiều toast giống hệt).
let handling401 = false;
instance.interceptors.response.use(
  (response) => {
    // Bóc envelope ApiResponse<T> thống nhất từ backend: { success, message, data, errors }.
    // Chỉ bóc khi ĐÚNG hình dạng envelope (có cờ boolean `success` VÀ khóa `data`) để không
    // đụng vào payload thô (mảng, object nghiệp vụ, hay { code, data } của VNPAY...).
    // Nhờ vậy mọi consumer đọc `response.data` / `data.data ?? data` đều nhận đúng payload,
    // FE không phải sửa từng call-site khi backend chuyển sang bọc data.
    const body = response.data;
    if (body && typeof body === 'object' && typeof body.success === 'boolean'
        && Object.prototype.hasOwnProperty.call(body, 'data')) {
      response.data = body.data;
    }
    return response;
  },
  async (error) => {
    if (error.response?.status === 401) {
      // import động để tránh phụ thuộc vòng khi module axios được nạp sớm
      const { useAuthStore } = await import('@/stores/auth');
      const auth = useAuthStore();
      const wasAuthenticated = auth.isAuthenticated;
      if (wasAuthenticated) auth.logout();

      // Chỉ báo + điều hướng MỘT lần cho cả loạt 401; và chỉ khi trước đó đang đăng nhập
      // (bỏ qua 401 do đăng nhập sai mật khẩu ở màn login).
      if (wasAuthenticated && !handling401) {
        handling401 = true;
        try {
          const { useToastStore } = await import('@/stores/toast');
          useToastStore().error('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.');
        } catch (_) { /* ignore */ }
        try {
          const { default: router } = await import('@/routers');
          const path = router.currentRoute.value.path;
          const target = path.startsWith('/admin') ? '/admin/login' : '/login';
          if (path !== target) router.replace(target);
        } catch (_) { /* ignore */ }
        setTimeout(() => { handling401 = false; }, 1500);
      }
    }
    return Promise.reject(error);
  }
);

export default instance;
