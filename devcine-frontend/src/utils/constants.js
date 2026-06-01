export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000/api';

export const BOOKING_STATUS = {
  PENDING: 'pending',
  CONFIRMED: 'confirmed',
  CANCELLED: 'cancelled',
  EXPIRED: 'expired',
};

export const USER_ROLES = {
  ADMIN: 'admin',
  CUSTOMER: 'customer',
  STAFF: 'staff',
};

export const MOVIE_RATINGS = {
  P: 'P',   // General
  K: 'K',   // Under 13 with parent
  T13: 'T13', // 13+
  T16: 'T16', // 16+
  T18: 'T18', // 18+
};
