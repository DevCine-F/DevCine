import { defineStore } from 'pinia'
import { bookingApi, seatApi, fnbApi, showtimeApi } from '../api/customer'

export const useBookingStore = defineStore('booking', {
  state: () => ({
    selectedMovie: null,
    selectedShowtime: null,
    selectedSeats: [], // Array of seat objects
    selectedFnbs: [], // Array of { fnbItem, quantity }
    totalPrice: 0,
    bookingStep: 1, // 1: Select Seat, 2: F&B, 3: Payment, 4: Success
    bookingId: null,
    bookingCode: null,
    availableSeats: [],
    matrixRow: 9,
    matrixCol: 10,
    availableFnbs: [],
    cities: [],
    selectedCity: '',
    cinemaShowtimes: [] // Grouped showtimes
  }),
  actions: {
    async fetchCities() {
      try {
        const { data } = await showtimeApi.getCities();
        this.cities = data;
      } catch (err) {
        console.error('Failed to fetch cities', err);
      }
    },
    async fetchShowtimes(movieId, city) {
      try {
        const { data } = await showtimeApi.getForMovie(movieId, city);
        this.cinemaShowtimes = data;
      } catch (err) {
        console.error('Failed to fetch showtimes', err);
      }
    },
    async fetchSeats() {
      if (!this.selectedShowtime) return;
      try {
        const { data } = await seatApi.getForShowtime(this.selectedShowtime.id);
        if (data && data.seats) {
          this.matrixRow = data.matrixRow;
          this.matrixCol = data.matrixCol;
          this.availableSeats = data.seats;
        } else {
          // Fallback if backend hasn't been updated yet or returned an array directly
          this.availableSeats = Array.isArray(data) ? data : [];
        }
      } catch (err) {
        console.error('Failed to fetch seats', err);
      }
    },
    async fetchFnbs() {
      try {
        const { data } = await fnbApi.getAll();
        this.availableFnbs = data;
      } catch (err) {
        console.error('Failed to fetch fnbs', err);
      }
    },
    setMovie(movie) {
      this.selectedMovie = movie;
    },
    setShowtime(showtime, cinema) {
      this.selectedShowtime = { ...showtime, cinema };
      this.bookingStep = 1;
    },
    toggleSeat(seat) {
      const index = this.selectedSeats.findIndex(s => s.seatId === seat.seatId);
      if (index === -1) {
        this.selectedSeats.push(seat);
      } else {
        this.selectedSeats.splice(index, 1);
      }
      this.calculateTotal();
    },
    updateFnb(fnbItem, quantity) {
      const index = this.selectedFnbs.findIndex(f => f.fnbItem.id === fnbItem.id);
      if (quantity > 0) {
        if (index === -1) {
          this.selectedFnbs.push({ fnbItem, quantity });
        } else {
          this.selectedFnbs[index].quantity = quantity;
        }
      } else if (index !== -1) {
        this.selectedFnbs.splice(index, 1);
      }
      this.calculateTotal();
    },
    calculateTotal() {
      let total = 0;
      for (const seat of this.selectedSeats) {
        total += seat.price;
      }
      for (const fnb of this.selectedFnbs) {
        total += fnb.fnbItem.price * fnb.quantity;
      }
      this.totalPrice = total;
    },
    async holdSeatsAndProceed() {
      if (this.selectedSeats.length === 0) return false;
      try {
        const payload = {
          customerId: null, // Replace with actual user ID if logged in
          showtimeId: this.selectedShowtime.id,
          seatIds: this.selectedSeats.map(s => s.seatId),
          fnbs: this.selectedFnbs.map(f => ({ fnbItemId: f.fnbItem.id, quantity: f.quantity })),
          paymentMethod: null
        };
        const { data } = await bookingApi.holdSeats(payload);
        this.bookingId = data.id;
        this.bookingCode = data.bookingCode;
        this.bookingStep = 2; // Move to F&B (if you want F&B before payment)
        // Wait, if hold is called after F&B? We should hold first, then add F&B, or hold both.
        // The API supports passing fnbs at hold time. So we hold right before payment.
        // Let's refine the flow: 1: Seats -> 2: F&B -> 3: Payment (Hold happens here)
        return true;
      } catch (err) {
        console.error('Failed to hold seats', err);
        return false;
      }
    },
    async confirmPayment(paymentMethod) {
      if (!this.bookingId) return false;
      try {
        await bookingApi.completePayment(this.bookingId, paymentMethod);
        this.bookingStep = 4; // Success
        return true;
      } catch (err) {
        console.error('Payment failed', err);
        return false;
      }
    },
    resetBooking() {
      this.selectedMovie = null;
      this.selectedShowtime = null;
      this.selectedSeats = [];
      this.selectedFnbs = [];
      this.totalPrice = 0;
      this.bookingStep = 1;
      this.bookingId = null;
      this.bookingCode = null;
    }
  }
})
