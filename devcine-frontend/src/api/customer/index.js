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
};

export const paymentApi = {
  createPayment: (amount, bookingId) => api.post('/payment/create_payment', null, { params: { amount, bookingId } }),
  vnpayReturn: (queryString) => api.get(`/payment/vnpay_return?${queryString}`),
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
};

export const customerApi = {
  getProfile: (customerId) => api.get(`/customers/${customerId}`),
  updateProfile: (customerId, data) => api.put(`/customers/${customerId}`, data),
};

export const reviewApi = {
  getForMovie: (movieId) => api.get(`/reviews/movie/${movieId}`),
  submit: (data) => api.post('/reviews', data),
};

export const promotionApi = {
  getActive: () => api.get('/marketing/promotions/active'),
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
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (data) => api.post('/auth/register', data),
  changePassword: (userId, oldPassword, newPassword) =>
    api.put('/auth/change-password', { userId, oldPassword, newPassword }),
  getProfile: (userId) => api.get(`/auth/profile/${userId}`),
  updateProfile: (data) => api.put('/auth/profile', data),
};

