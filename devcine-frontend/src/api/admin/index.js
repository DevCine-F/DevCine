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
