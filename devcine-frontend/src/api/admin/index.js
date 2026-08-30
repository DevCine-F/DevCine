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
  reorder: (items) => api.put('/banners/reorder', items),
  delete: (id) => api.delete(`/banners/${id}`),
};

export const supportTicketApi = {
  getAll: () => api.get('/support-tickets'),
  create: (data) => api.post('/support-tickets', data),
  updateStatus: (id, status) => api.put(`/support-tickets/${id}/status`, null, { params: { status } }),
  reply: (id, data) => api.post(`/support-tickets/${id}/reply`, data),
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
  togglePromotion: (id) => api.patch(`/marketing/promotions/${id}/toggle`),
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
  getById: (id) => api.get(`/customers/${id}`),
  getOrders: (id) => api.get(`/customers/${id}/orders`),
  getVouchers: (id) => api.get(`/customers/${id}/vouchers`),
  getPointHistory: (id) => api.get(`/customers/${id}/point-history`),
  update: (id, data) => api.put(`/customers/${id}`, data),
  toggleStatus: (id, data) => api.put(`/customers/${id}/toggle-status`, data),
  sendResetPassword: (id) => api.post(`/customers/${id}/send-reset-password`),
};

export const fnbApi = {
  getAll: () => api.get('/fnbs/all'),
  create: (data) => api.post('/fnbs', data),
  update: (id, data) => api.put(`/fnbs/${id}`, data),
  delete: (id) => api.delete(`/fnbs/${id}`),
};

export const fnbGroupApi = {
  getAll: () => api.get('/fnbs/groups'),
  create: (data) => api.post('/fnbs/groups', data),
  update: (id, data) => api.put(`/fnbs/groups/${id}`, data),
  delete: (id) => api.delete(`/fnbs/groups/${id}`),
};

export const bookingAdminApi = {
  list: (params) => api.get('/admin/bookings', { params }),
  detail: (id, params) => api.get(`/admin/bookings/${id}`, { params }),
};

export const ticketingApi = {
  getShowtimes: () => api.get('/ticketing/showtimes'),
  getSeats: (showtimeId) => api.get(`/seats/showtime/${showtimeId}`, { params: { channel: 'POS' } }),
  getCombos: () => api.get('/fnbs'),
  memberCard: (cardNumber) => api.get(`/ticketing/member-card/${cardNumber}`),
  pay: (payload) => api.post('/ticketing/pay', payload),
  concession: (payload) => api.post('/ticketing/concession', payload),
  hold: (payload) => api.post('/ticketing/hold', payload),
  releaseHold: (bookingId) => api.post(`/ticketing/hold/${bookingId}/release`),
  mockWebhookSuccess: (bookingId) => api.post('/payment/mock-webhook-success', { bookingId }),
  // Voucher tại quầy: danh sách voucher của khách + áp mã (tự claim nếu mã công khai hợp lệ)
  customerVouchers: (customerId) => api.get(`/vouchers/customer/${customerId}/all`),
  applyVoucher: (customerId, code) => api.post('/vouchers/apply', null, { params: { customerId, code } }),
};

// Xử lý sự cố phòng chiếu / đổi ghế đền bù (Tạm ẩn)
// export const incidentApi = {
//   lookup: (query) => api.get('/staff/incidents/lookup', { params: { query } }),
//   lookupByPhone: (phone) => api.get('/staff/incidents/lookup-by-phone', { params: { phone } }),
//   seatOccupant: (showtimeId, seatId) => api.get('/staff/incidents/seat-occupant', { params: { showtimeId, seatId } }),
//   compensationOptions: () => api.get('/staff/incidents/compensation-options'),
//   seats: (showtimeId) => api.get(`/seats/showtime/${showtimeId}`, { params: { channel: 'POS' } }),
//   setSeatStatus: (seatId, payload) => api.patch(`/staff/incidents/seats/${seatId}/status`, payload),
//   relocate: (payload) => api.post('/staff/incidents/relocate', payload),
//   cancel: (payload) => api.post('/staff/incidents/cancel', payload),
//   history: (params) => api.get('/staff/incidents', { params }),
//   detail: (id) => api.get(`/staff/incidents/${id}`),
// };

export const posPendingOrderApi = {
  hold: (payload) => api.post('/admin/pos/bookings/hold', payload),
  getPending: (posTerminalId) => api.get('/admin/pos/bookings', { params: { posTerminalId } }),
  resume: (id, posTerminalId) => api.get(`/admin/pos/bookings/${id}/resume`, { params: { posTerminalId } }),
  payIntent: (id, posTerminalId) => api.post(`/admin/pos/bookings/${id}/pay-intent`, { posTerminalId }),
  pay: (id, payload) => api.post(`/admin/pos/bookings/${id}/pay`, payload),
  cancel: (id, posTerminalId) => api.post(`/admin/pos/bookings/${id}/cancel`, { posTerminalId }),
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
  getNextCode: () => api.get('/staff/next-code'),
};

// Danh sách cơ sở (cụm rạp) cho dropdown lọc/gán nhân viên — endpoint công khai GET
export const cinemaListApi = {
  getAll: () => api.get('/v1/cinemas'),
};

// Luồng phê duyệt "sửa sai" (chỉ còn void hóa đơn F&B) — duyệt bởi Quản lý/Quản trị viên
export const approvalApi = {
  pending: () => api.get('/staff/approvals/pending'),
  mine: () => api.get('/staff/approvals/mine'),
  requestFnbVoid: (saleId, reason) => api.post('/staff/approvals/fnb-void', { saleId, reason }),
  approve: (id) => api.put(`/staff/approvals/${id}/approve`),
  reject: (id, note) => api.put(`/staff/approvals/${id}/reject`, { note }),
};

export const pricingApi = {
  getConfig: () => api.get('/pricing/config'),
  saveBaseMatrix: (rules) => api.put('/pricing/base-matrix', { rules }),
  saveSeatTypes: (items) => api.put('/pricing/seat-types', { items }),
  saveFormats: (items) => api.put('/pricing/formats', { items }),
  addHoliday: (holidayDate, name) => api.post('/pricing/holidays', { holidayDate, name }),
  deleteHoliday: (id) => api.delete(`/pricing/holidays/${id}`),
  simulate: (payload) => api.post('/pricing/simulate', payload),
};
