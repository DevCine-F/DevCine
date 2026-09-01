import api from '../axios';

export const movieApi = {
  getNowShowing: () => api.get('/movies/now-showing'),
  getUpcoming: () => api.get('/movies/upcoming'),
  getDetails: (id) => api.get(`/movies/${id}`),
  search: (q) => api.get('/movies/search', { params: { q } }),
};

export const showtimeApi = {
  getCities: () => api.get('/showtimes/cities'),
  getForMovie: (movieId, city) => api.get(`/showtimes/movie/${movieId}`, { params: { city } }),
  // Trang Lịch chiếu (lọc phía server + phân trang)
  getCinemas: (city) => api.get('/showtimes/cinemas', { params: { city } }),
  getCinemasWithShowtimes: () => api.get('/showtimes/cinemas-with-showtimes'),
  getMovies: ({ city, date, q, page = 0, size = 12 }) => api.get('/showtimes/movies', { params: { city, date, q, page, size } }),
  getByMovie: (movieId, date, city) => api.get('/showtimes/by-movie', { params: { movieId, date, city } }),
  getByCinema: (cinemaId, date) => api.get('/showtimes/by-cinema', { params: { cinemaId, date } }),
  getUpcoming: () => api.get('/showtimes/upcoming'),
};

export const seatApi = {
  getForShowtime: (showtimeId) => api.get(`/seats/showtime/${showtimeId}`),
};

export const fnbApi = {
  getAll: () => api.get('/fnbs'),
};

export const bookingApi = {
  holdSeats: (data) => api.post('/bookings/hold', data),
  completePayment: (bookingId, paymentMethod) => api.post(`/bookings/${bookingId}/payment/complete`, null, { params: { paymentMethod } }),
  getHistory: (customerId) => api.get('/bookings/history', { params: { customerId } }),
  // Nhả đơn đang giữ ghế khi hết giờ giữ chỗ → mở ghế cho khách khác mua ngay
  releaseHold: (bookingId) => api.post(`/bookings/${bookingId}/release`, null),
};

export const paymentApi = {
  createPayment: (amount, bookingId) => api.post('/payment/create_payment', null, { params: { amount, bookingId } }),
  vnpayReturn: (queryString) => api.get(`/payment/vnpay_return?${queryString}`),
  mockWebhookSuccess: (bookingId) => api.post('/payment/mock-webhook-success', { bookingId })
};

export const voucherApi = {
  getActiveVouchers: (customerId) => api.get(`/vouchers/customer/${customerId}`),
  getAllVouchers: (customerId) => api.get(`/vouchers/customer/${customerId}/all`),
  validateVoucher: (code, customerId) => api.post('/vouchers/validate', null, { params: { code, customerId } }),
  getRedeemable: () => api.get('/marketing/promotions/redeemable'),
  redeem: (customerId, promoId) => api.post('/vouchers/redeem', null, { params: { customerId, promoId } }),
  lookup: (customerId, code) => api.get('/vouchers/lookup', { params: { customerId, code } }),
  claim: (customerId, code) => api.post('/vouchers/claim', null, { params: { customerId, code } }),
  applyCode: (customerId, code) => api.post('/vouchers/apply', null, { params: { customerId, code } }),
  // Chấm điều kiện + số giảm thực của voucher theo giỏ hàng hiện tại (làm mờ mã không đủ điều kiện)
  preview: (payload) => api.post('/vouchers/preview', payload),
};

export const customerApi = {
  getProfile: (customerId) => api.get(`/customers/${customerId}`),
  updateProfile: (customerId, data) => api.put(`/customers/${customerId}`, data),
  pointHistory: (customerId) => api.get(`/customers/${customerId}/point-history`),
};

export const reviewApi = {
  getForMovie: (movieId) => api.get(`/reviews/movie/${movieId}`),
  eligibility: (movieId, customerId) => api.get(`/reviews/movie/${movieId}/eligibility`, { params: { customerId } }),
  submit: (data) => api.post('/reviews', data),
};

export const promotionApi = {
  getActive: (customerId) => api.get('/marketing/promotions/active', { params: customerId ? { customerId } : {} }),
};

export const promoArticleApi = {
  getActive: () => api.get('/promo-articles'),
  getDetail: (id) => api.get(`/promo-articles/${id}`),
};

export const notificationApi = {
  getForCustomer: (customerId) => api.get(`/notifications/customer/${customerId}`),
  getUnreadCount: (customerId) => api.get(`/notifications/customer/${customerId}/unread-count`),
  markAsRead: (id) => api.put(`/notifications/${id}/read`),
  markAllAsRead: (customerId) => api.put(`/notifications/customer/${customerId}/read-all`),
};

export const supportApi = {
  createTicket: (data) => api.post('/support-tickets', data),
};

export const authApi = {
  login: (identifier, password) => api.post('/auth/login', { identifier, password }),
  register: (data) => api.post('/auth/register', data),
  changePassword: (userId, oldPassword, newPassword) =>
    api.put('/auth/change-password', { userId, oldPassword, newPassword }),
  getProfile: (userId) => api.get(`/auth/profile/${userId}`),
  updateProfile: (data) => api.put('/auth/profile', data),
  // Quên mật khẩu: nhập email → nếu email có trong hệ thống thì gửi OTP → xác minh → đặt mật khẩu mới
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  verifyOtp: (email, otp) => api.post('/auth/verify-otp', { email, otp }),
  resetPassword: (email, otp, newPassword) => api.post('/auth/reset-password', { email, otp, newPassword }),
};

