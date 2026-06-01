import { defineStore } from 'pinia'

export const useBookingStore = defineStore('booking', {
  state: () => ({
    selectedMovie: null,
    selectedShowtime: null,
    selectedSeats: [],
    totalPrice: 0,
    bookingStep: 1, // 1: Select Seat, 2: Payment, 3: Success
  }),
  actions: {
    setMovie(movie) {
      this.selectedMovie = movie;
    },
    setShowtime(showtime) {
      this.selectedShowtime = showtime;
    },
    updateSeats(seats) {
      this.selectedSeats = seats;
      // Calculate price logic here
    },
    resetBooking() {
      this.selectedMovie = null;
      this.selectedShowtime = null;
      this.selectedSeats = [];
      this.totalPrice = 0;
      this.bookingStep = 1;
    }
  }
})
