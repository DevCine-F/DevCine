import api from '../axios';

export const movieApi = {
  getNowShowing: () => api.get('/movies/now-showing'),
  getUpcoming: () => api.get('/movies/upcoming'),
  getDetails: (id) => api.get(`/movies/${id}`),
};

export const bookingApi = {
  create: (data) => api.post('/bookings', data),
  getHistory: () => api.get('/bookings/history'),
};
