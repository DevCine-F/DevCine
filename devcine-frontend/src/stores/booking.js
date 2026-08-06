import { defineStore, acceptHMRUpdate } from 'pinia'
import { bookingApi, seatApi, fnbApi, showtimeApi, paymentApi } from '../api/customer'
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
    heldAt: null, // Thời điểm server tạo đơn giữ ghế (ISO) — mốc bắt đầu đếm ngược
    paymentMethod: null, // Phương thức thanh toán đã chọn (VNPAY/TRANSFER)
    paidAt: null, // Thời điểm thanh toán thành công (ISO string)
    lastHoldError: '', // Thông điệp lỗi giữ ghế gần nhất (để hiển thị cho khách)
    availableSeats: [],
    priceTable: {}, // tên loại ghế -> (mã đối tượng -> giá)
    audienceLabels: {}, // mã đối tượng -> nhãn
    ticketQuantities: {}, // mã đối tượng -> số lượng vé (chọn TRƯỚC khi chọn ghế)
    maxTicketsPerBooking: 8, // giới hạn số vé/lần đặt (lấy từ cấu hình admin)
    matrixRow: 9,
    matrixCol: 10,
    availableFnbs: [],
    cities: [],
    selectedCity: '',
    cinemaShowtimes: [] // Grouped showtimes
  }),
  getters: {
    // Tổng số vé = tổng số lượng các loại đối tượng
    totalTickets: (state) => Object.values(state.ticketQuantities).reduce((a, b) => a + (Number(b) || 0), 0),
    // Gán đối tượng cho từng ghế theo thứ tự (tổng tiền không đổi dù gán cách nào)
    audienceAssignment: (state) => {
      const arr = []
      for (const [code, qty] of Object.entries(state.ticketQuantities)) {
        for (let i = 0; i < (Number(qty) || 0); i++) arr.push(code)
      }
      return arr.slice(0, state.selectedSeats.length)
    }
  },
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
          this.priceTable = data.priceTable || {};
          this.audienceLabels = data.audienceLabels || {};
          // Khởi tạo số lượng vé theo các đối tượng (giữ giá trị cũ nếu có)
          const q = {};
          Object.keys(this.audienceLabels).forEach(k => { q[k] = this.ticketQuantities[k] || 0; });
          this.ticketQuantities = q;
        } else {
          // Fallback if backend hasn't been updated yet or returned an array directly
          this.availableSeats = Array.isArray(data) ? data : [];
          this.priceTable = {};
          this.audienceLabels = {};
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
      this.ticketQuantities = {};
      this.selectedFnbs = [];
      this.selectedVoucher = null;
      this.totalPrice = 0;
      this.finalPrice = 0;
      this.bookingId = null;
      this.bookingCode = null;
      this.heldAt = null;
      this.paymentMethod = null;
      this.paidAt = null;
      this.lastHoldError = '';
      this.bookingStep = 1;
    },
    toggleSeat(seat) {
      const index = this.selectedSeats.findIndex(s => s.seatId === seat.seatId);
      if (index === -1) {
        // Chỉ cho chọn tối đa = tổng số vé đã chọn ở bước "Loại vé"
        if (this.selectedSeats.length >= this.totalTickets) return;
        this.selectedSeats.push({ ...seat });
      } else {
        this.selectedSeats.splice(index, 1);
      }
      this.calculateTotal();
    },
    setTicketQuantity(code, qty) {
      let n = Math.max(0, Number(qty) || 0);
      // Không cho tổng số vé vượt giới hạn cấu hình (chống phe vé)
      const others = this.totalTickets - (Number(this.ticketQuantities[code]) || 0);
      const allowed = Math.max(0, this.maxTicketsPerBooking - others);
      if (n > allowed) n = allowed;
      this.ticketQuantities = { ...this.ticketQuantities, [code]: n };
      // Nếu giảm số vé xuống dưới số ghế đang chọn → bỏ bớt ghế thừa
      if (this.selectedSeats.length > this.totalTickets) {
        this.selectedSeats = this.selectedSeats.slice(0, this.totalTickets);
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
      const assign = this.audienceAssignment; // đối tượng gán cho từng ghế theo thứ tự
      this.selectedSeats.forEach((seat, i) => {
        const aud = assign[i] || 'ADULT';
        const byAud = this.priceTable[seat.seatType];
        total += (byAud && byAud[aud] != null) ? Number(byAud[aud]) : (seat.price || 0);
      });
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
          seatSelections: this.selectedSeats.map((s, i) => ({ seatId: s.seatId, ticketType: this.audienceAssignment[i] || 'ADULT' })),
          fnbs: this.selectedFnbs.map(f => ({ fnbItemId: f.fnbItem.id, quantity: f.quantity })),
          voucherId: this.selectedVoucher ? this.selectedVoucher.id : null,
          paymentMethod: paymentMethod
        };
        const { data } = await bookingApi.holdSeats(payload);
        this.bookingId = data.id;
        this.bookingCode = data.bookingCode;
        this.heldAt = data.createdAt || new Date().toISOString(); // mốc đếm ngược (ưu tiên giờ server)
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
        if (paymentMethod === 'TRANSFER' || paymentMethod === 'VNPAY_MOCK') {
          await paymentApi.mockWebhookSuccess(this.bookingId);
        } else {
          await bookingApi.completePayment(this.bookingId, paymentMethod);
        }
        this.paymentMethod = paymentMethod;
        this.paidAt = new Date().toISOString();
        this.bookingStep = 4; // Success
        return true;
      } catch (err) {
        console.error('Payment failed', err);
        throw err; // throw to let BookingView handle the 400 Expired Toast
      }
    },
    // Nhả đơn đang giữ ghế dưới DB (best-effort) — dùng khi hết giờ giữ chỗ
    async releaseHold() {
      if (!this.bookingId) return;
      try { await bookingApi.releaseHold(this.bookingId); } catch (e) { /* best-effort, không chặn UI */ }
    },
    // Xoá SẠCH toàn bộ state tạm của phiên đặt vé (ghế/vé/combo/voucher/giá) — GIỮ movie + showtime
    // để khách chọn lại từ bước 1 cho cùng suất. Chống rò rỉ state sang phiên/API lượt sau.
    resetSelections() {
      this.selectedSeats = [];
      this.ticketQuantities = {};
      this.selectedFnbs = [];
      this.selectedVoucher = null;
      this.totalPrice = 0;
      this.finalPrice = 0;
      this.bookingId = null;
      this.bookingCode = null;
      this.heldAt = null;
      this.paymentMethod = null;
      this.paidAt = null;
      this.lastHoldError = '';
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
      this.heldAt = null;
      this.paymentMethod = null;
      this.paidAt = null;
    }
  }
})

// Cho phép HMR cập nhật store khi dev (tránh giữ state/action cũ sau khi sửa)
if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useBookingStore, import.meta.hot))
}
