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
instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // import động để tránh phụ thuộc vòng khi module axios được nạp sớm
      const { useAuthStore } = await import('@/stores/auth');
      const auth = useAuthStore();
      if (auth.isAuthenticated) auth.logout();
    }
    return Promise.reject(error);
  }
);

export default instance;
