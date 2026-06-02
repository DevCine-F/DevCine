import api from '../axios';

export const movieApi = {
  getNowShowing: () => api.get('/movies/now-showing'),
  getUpcoming: () => api.get('/movies/upcoming'),
  getDetails: (id) => api.get(`/movies/${id}`),
};

export const showtimeApi = {
  getCities: () => api.get('/showtimes/cities'),
  getForMovie: (movieId, city) => api.get(`/showtimes/movie/${movieId}`, { params: { city } }),
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
  getHistory: () => api.get('/bookings/history'),
};
