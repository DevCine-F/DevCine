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
  updatePermissions: (roleId, matrix) => api.put(`/admin/roles/${roleId}/permissions`, matrix),
};

export const marketingApi = {
  getPromotions: () => api.get('/marketing/promotions'),
  createPromotion: (data) => api.post('/marketing/promotions', data),
  updatePromotion: (id, data) => api.put(`/marketing/promotions/${id}`, data),
  deletePromotion: (id) => api.delete(`/marketing/promotions/${id}`),
  issueVoucher: (promoId, customerId) => api.post(`/marketing/promotions/${promoId}/issue-voucher`, { customerId }),
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
