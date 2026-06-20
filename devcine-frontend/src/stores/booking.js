import { defineStore, acceptHMRUpdate } from 'pinia'
import { bookingApi, seatApi, fnbApi, showtimeApi } from '../api/customer'
import { useAuthStore } from './auth'

export const useBookingStore = defineStore('booking', {
  state: () => ({
    selectedMovie: null,
    selectedShowtime: null,
    selectedSeats: [], // Array of seat objects
    selectedFnbs: [], // Array of { fnbItem, quantity }
    selectedVoucher: null, // Selected voucher object
    totalPrice: 0, // Tạm tính (suất + bắp), CHƯA trừ voucher
    finalPrice: 0, // Giá phải trả sau khi trừ voucher (do backend tính ở holdSeats)
    bookingStep: 1, // 1: Select Seat, 2: F&B, 3: Payment, 4: Success
    bookingId: null,
    bookingCode: null,
    paymentMethod: null, // Phương thức thanh toán đã chọn (VNPAY/TRANSFER/WALLET)
    paidAt: null, // Thời điểm thanh toán thành công (ISO string)
    lastHoldError: '', // Thông điệp lỗi giữ ghế gần nhất (để hiển thị cho khách)
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
      // Bắt đầu phiên đặt vé mới: dọn sạch lựa chọn cũ để tránh áp voucher/ghế/combo còn sót từ lần trước
      this.selectedSeats = [];
      this.selectedFnbs = [];
      this.selectedVoucher = null;
      this.totalPrice = 0;
      this.finalPrice = 0;
      this.bookingId = null;
      this.bookingCode = null;
      this.paymentMethod = null;
      this.paidAt = null;
      this.lastHoldError = '';
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
    async holdSeatsAndProceed(paymentMethod) {
      if (this.selectedSeats.length === 0) return false;
      const authStore = useAuthStore()
      try {
        const payload = {
          customerId: authStore.user?.id || null,
          showtimeId: this.selectedShowtime.id,
          seatIds: this.selectedSeats.map(s => s.seatId),
          fnbs: this.selectedFnbs.map(f => ({ fnbItemId: f.fnbItem.id, quantity: f.quantity })),
          voucherId: this.selectedVoucher ? this.selectedVoucher.id : null,
          paymentMethod: paymentMethod
        };
        const { data } = await bookingApi.holdSeats(payload);
        this.bookingId = data.id;
        this.bookingCode = data.bookingCode;
        this.paymentMethod = paymentMethod;
        // Giá cuối do backend tính (đã trừ voucher) — dùng làm số tiền thanh toán chuẩn
        this.finalPrice = data.finalPrice;
        this.bookingStep = 3;
        return true;
      } catch (err) {
        console.error('Failed to hold seats', err);
        this.lastHoldError = err.response?.data?.error || err.response?.data?.message || '';
        return false;
      }
    },
    async confirmPayment(paymentMethod) {
      if (!this.bookingId) return false;
      try {
        await bookingApi.completePayment(this.bookingId, paymentMethod);
        this.paymentMethod = paymentMethod;
        this.paidAt = new Date().toISOString();
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
      this.selectedVoucher = null;
      this.totalPrice = 0;
      this.finalPrice = 0;
      this.bookingStep = 1;
      this.bookingId = null;
      this.bookingCode = null;
      this.paymentMethod = null;
      this.paidAt = null;
    }
  }
})

// Cho phép HMR cập nhật store khi dev (tránh giữ state/action cũ sau khi sửa)
if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useBookingStore, import.meta.hot))
}
