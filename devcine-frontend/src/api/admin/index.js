import api from '../axios';

export const movieApi = {
  getAll: () => api.get('/admin/movies'),
  getById: (id) => api.get(`/admin/movies/${id}`),
  create: (data) => api.post('/admin/movies', data),
  update: (id, data) => api.put(`/admin/movies/${id}`, data),
  delete: (id) => api.delete(`/admin/movies/${id}`),
};

export const cinemaApi = {
  getAll: () => api.get('/admin/cinemas'),
  // ... other cinema methods
};

export const settingsApi = {
  getAll: () => api.get('/settings'),
  getByKey: (key) => api.get(`/settings/${key}`),
  save: (data) => api.post('/settings', data),
};

export const bannerApi = {
  getAll: () => api.get('/banners'),
  create: (data) => api.post('/banners', data),
  update: (id, data) => api.put(`/banners/${id}`, data),
  delete: (id) => api.delete(`/banners/${id}`),
};

export const supportTicketApi = {
  getAll: () => api.get('/support-tickets'),
  create: (data) => api.post('/support-tickets', data),
  updateStatus: (id, status) => api.put(`/support-tickets/${id}/status`, null, { params: { status } }),
  delete: (id) => api.delete(`/support-tickets/${id}`),
};

export const auditLogApi = {
  getLogs: (params) => api.get('/admin/logs', { params }),
};

export const rolePermissionApi = {
  getRoles: () => api.get('/admin/roles'),
  getStaffUsers: () => api.get('/admin/roles/staff-users'),
  getUserOverrides: (userId) => api.get(`/admin/roles/users/${userId}/permission-overrides`),
  updatePermissions: (roleId, matrix) => api.put(`/admin/roles/${roleId}/permissions`, matrix),
  updateUserOverrides: (userId, data) => api.put(`/admin/roles/users/${userId}/permission-overrides`, data),
};

export const marketingApi = {
  getPromotions: () => api.get('/marketing/promotions'),
  createPromotion: (data) => api.post('/marketing/promotions', data),
  updatePromotion: (id, data) => api.put(`/marketing/promotions/${id}`, data),
  deletePromotion: (id) => api.delete(`/marketing/promotions/${id}`),
  issueVoucher: (promoId, customerId) => api.post(`/marketing/promotions/${promoId}/issue-voucher`, { customerId }),
  // Gửi email chiến dịch mã ưu đãi tới toàn bộ khách thuộc đối tượng áp dụng
  sendCampaign: (promoId) => api.post(`/marketing/promotions/${promoId}/send-campaign`),
};

export const promoArticleApi = {
  getAll: () => api.get('/promo-articles/all'),
  create: (data) => api.post('/promo-articles', data),
  update: (id, data) => api.put(`/promo-articles/${id}`, data),
  toggle: (id) => api.patch(`/promo-articles/${id}/toggle`),
  delete: (id) => api.delete(`/promo-articles/${id}`),
};

export const customerApi = {
  list: (q) => api.get('/customers', { params: q ? { q } : {} }),
};

export const fnbApi = {
  getAll: () => api.get('/fnbs/all'),
  create: (data) => api.post('/fnbs', data),
  update: (id, data) => api.put(`/fnbs/${id}`, data),
  delete: (id) => api.delete(`/fnbs/${id}`),
};

export const bookingAdminApi = {
  list: (params) => api.get('/admin/bookings', { params }),
  detail: (id) => api.get(`/admin/bookings/${id}`),
};

export const ticketingApi = {
  getShowtimes: () => api.get('/ticketing/showtimes'),
  getSeats: (showtimeId) => api.get(`/seats/showtime/${showtimeId}`),
  getCombos: () => api.get('/fnbs'),
  memberCard: (cardNumber) => api.get(`/ticketing/member-card/${cardNumber}`),
  pay: (payload) => api.post('/ticketing/pay', payload),
  concession: (payload) => api.post('/ticketing/concession', payload),
  hold: (payload) => api.post('/ticketing/hold', payload),
  releaseHold: (bookingId) => api.post(`/ticketing/hold/${bookingId}/release`),
  // Voucher tại quầy: danh sách voucher của khách + áp mã (tự claim nếu mã công khai hợp lệ)
  customerVouchers: (customerId) => api.get(`/vouchers/customer/${customerId}/all`),
  applyVoucher: (customerId, code) => api.post('/vouchers/apply', null, { params: { customerId, code } }),
};

export const reviewAdminApi = {
  list: () => api.get('/reviews/admin/list'),
  toggle: (id) => api.put(`/reviews/${id}/visibility`),
  delete: (id) => api.delete(`/reviews/${id}`),
};

export const staffApi = {
  list: (params) => api.get('/staff/list', { params }),
  create: (data) => api.post('/staff', data),
  update: (id, data) => api.put(`/staff/${id}`, data),
  toggle: (id) => api.put(`/staff/${id}/toggle`),
};

export const staffShiftApi = {
  list: (params) => api.get('/staff/shifts', { params }),
  current: () => api.get('/staff/shifts/current'),
  my: (params) => api.get('/staff/shifts/my', { params }),
  create: (data) => api.post('/staff/shifts', data),
  approve: (id) => api.put(`/staff/shifts/${id}/approve`),
  reject: (id) => api.put(`/staff/shifts/${id}/reject`),
  checkIn: (id) => api.put(`/staff/shifts/${id}/check-in`),
  checkOut: (id) => api.put(`/staff/shifts/${id}/check-out`),
};

export const shiftHandoverApi = {
  list: () => api.get('/staff/handovers'),
  mine: () => api.get('/staff/handovers/my'),
  summary: (staffScheduleId) => api.get('/staff/handovers/summary', { params: { staffScheduleId } }),
  currentSummary: () => api.get('/staff/shifts/current/handover-summary'),
  submit: (data) => api.post('/staff/handovers', data),
};

// Danh sách cơ sở (cụm rạp) cho dropdown lọc/gán nhân viên — endpoint công khai GET
export const cinemaListApi = {
  getAll: () => api.get('/v1/cinemas'),
};

// Luồng phê duyệt "sửa sai" của Trưởng ca (void hóa đơn F&B / đổi ghế sự cố)
export const approvalApi = {
  pending: () => api.get('/staff/approvals/pending'),
  mine: () => api.get('/staff/approvals/mine'),
  requestFnbVoid: (saleId, reason) => api.post('/staff/approvals/fnb-void', { saleId, reason }),
  requestSeatMove: (bookingSeatId, toSeatId, reason) =>
    api.post('/staff/approvals/seat-move', { bookingSeatId, toSeatId, reason }),
  seatMoveOptions: (showtimeId) => api.get('/staff/approvals/seat-move/options', { params: { showtimeId } }),
  approve: (id) => api.put(`/staff/approvals/${id}/approve`),
  reject: (id, note) => api.put(`/staff/approvals/${id}/reject`, { note }),
};

export const pricingApi = {
  getConfig: () => api.get('/pricing/config'),
  saveBaseMatrix: (rules) => api.put('/pricing/base-matrix', { rules }),
  saveSeatTypes: (items) => api.put('/pricing/seat-types', { items }),
  saveFormats: (items) => api.put('/pricing/formats', { items }),
  saveSpecialPrices: (items) => api.put('/pricing/special-prices', { items }),
  addHoliday: (holidayDate, name) => api.post('/pricing/holidays', { holidayDate, name }),
  deleteHoliday: (id) => api.delete(`/pricing/holidays/${id}`),
  simulate: (payload) => api.post('/pricing/simulate', payload),
};
