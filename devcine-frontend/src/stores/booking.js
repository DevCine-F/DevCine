import { defineStore, acceptHMRUpdate } from 'pinia'
import { bookingApi, seatApi, fnbApi, showtimeApi, paymentApi } from '../api/customer'
import { useAuthStore } from './auth'

export const useBookingStore = defineStore('booking', {
  state: () => ({
    selectedMovie: null,
    selectedShowtime: null,
    selectedSeats: [], // Array of seat objects (mỗi ghế mang blockId của khối đã đặt)
    currentBlockSize: 0, // Kích thước khối ghế đang chọn (Block Selector kiểu Lotte)
    selectedFnbs: [], // Array of { fnbItem, quantity }
    selectedVoucher: null, // Selected voucher object
    totalPrice: 0, // Tạm tính (suất + bắp), CHƯA trừ voucher
    finalPrice: 0, // Giá phải trả sau khi trừ voucher (do backend tính ở holdSeats)
    bookingStep: 1, // 1: Select Seat, 2: F&B, 3: Payment, 4: Success
    bookingId: null,
    bookingCode: null,
    sessionStartedAt: null,
    heldAt: null, // Thời điểm server tạo đơn giữ ghế (ISO) — mốc bắt đầu đếm ngược
    paymentMethod: null, // Phương thức thanh toán đã chọn (VNPAY/TRANSFER)
    paidAt: null, // Thời điểm thanh toán thành công (ISO string)
    lastHoldError: '', // Thông điệp lỗi giữ ghế gần nhất (để hiển thị cho khách)
    availableSeats: [],
    priceTable: {}, // tên loại ghế -> (mã đối tượng -> giá)
    audienceLabels: {}, // mã đối tượng -> nhãn
    ticketQuantities: {}, // mã đối tượng -> số lượng vé (chọn TRƯỚC khi chọn ghế)
    autoAddedTickets: {}, // seatId -> { code, qty }
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
      const currentSelectedCapacity = state.selectedSeats.reduce((acc, s) => acc + (s.seatType === 'SWEETBOX' ? 2 : 1), 0);
      return arr.slice(0, currentSelectedCapacity);
    },
    // Tổng số chỗ đã đặt (SWEETBOX tính 2 chỗ).
    selectedCapacity: (state) => state.selectedSeats.reduce((a, s) => a + (s.seatType === 'SWEETBOX' ? 2 : 1), 0),
    // Số chỗ CÒN LẠI cần đặt.
    remainingCapacity() {
      return Math.max(0, this.totalTickets - this.selectedCapacity);
    },
    // Các kích thước khối hợp lệ cho phần CÒN LẠI (thuật toán chống ghế mồ côi kiểu Lotte):
    // với R chỗ còn lại, khối b hợp lệ khi b ≤ R và (R − b) ≠ 1; khối 1 chỉ khi R === 1.
    validBlockSizes() {
      const R = this.remainingCapacity;
      if (R <= 0) return [];
      if (R === 1) return [1];
      const sizes = [];
      for (let b = 2; b <= Math.min(4, R); b++) {
        if (R - b !== 1) sizes.push(b);
      }
      return sizes;
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
        this.availableFnbs = Array.isArray(data) ? data : (data.data || []);
      } catch (err) {
        console.error('Failed to fetch fnbs', err);
      }
    },
    // Đối soát danh sách combo/món đã chọn với menu F&B mới nhất từ server
    // ── THIẾT KẾ SNAPSHOT: chỉ GỠ món đã tạm ngưng/xóa, KHÔNG ghi đè giá đã snapshot ──
    // Giá (snapshotPrice) được "đóng băng" lúc user bấm chọn — bất biến trước mọi cập nhật
    // từ admin trong khi khách đang ở bước Combo. Đây là hành vi chuẩn CGV/Lotte Cinema.
    reconcileSelectedFnbs() {
      if (!this.selectedFnbs || this.selectedFnbs.length === 0) return [];
      const removedItems = [];
      const updatedFnbs = [];

      for (const sel of this.selectedFnbs) {
        const currentItem = (this.availableFnbs || []).find(i => i.id === sel.fnbItem.id && i.isActive !== false && !i.isDeleted);
        if (!currentItem) {
          // Món đã tạm ngưng phục vụ hoặc bị xóa → gỡ khỏi giỏ
          removedItems.push(sel.fnbItem.name || 'Món');
        } else {
          // ── Cập nhật metadata (tên, ảnh, slots...) nhưng GIỮ NGUYÊN snapshotPrice ──
          // KHÔNG gán sel.fnbItem = currentItem để tránh ghi đè giá đã snapshot.
          // Chỉ cập nhật các trường hiển thị (tên, ảnh, mô tả) để UI đồng bộ.
          sel.fnbItem = {
            ...currentItem,
            price: sel.fnbItem.price, // GIỮ NGUYÊN giá gốc của fnbItem (không đổi)
          };
          // snapshotPrice vẫn là nguồn sự thật duy nhất cho tính tiền

          // Đối soát lại các vị con đã chọn (nếu vị bị xóa khỏi kho)
          if (sel.options && sel.options.length > 0 && currentItem.slots) {
            const validOptions = [];
            for (const opt of sel.options) {
              const slot = currentItem.slots.find(s => s.id === opt.slotId || s.slotLabel === opt.slotLabel);
              const optItemExists = slot?.optionGroup?.items?.some(it => it.id === opt.optionItemId);
              if (optItemExists) {
                validOptions.push(opt);
              }
            }
            sel.options = validOptions;
          }
          updatedFnbs.push(sel);
        }
      }

      this.selectedFnbs = updatedFnbs;
      this.calculateTotal();
      return removedItems;
    },
    setMovie(movie) {
      this.selectedMovie = movie;
    },
    setShowtime(showtime, cinema) {
      this.selectedShowtime = { ...showtime, cinema };
      // Bắt đầu phiên đặt vé mới: dọn sạch lựa chọn cũ để tránh áp voucher/ghế/combo còn sót từ lần trước
      this.selectedSeats = [];
      this.ticketQuantities = {};
      this.autoAddedTickets = {};
      this.selectedFnbs = [];
      this.selectedVoucher = null;
      this.totalPrice = 0;
      this.finalPrice = 0;
      this.bookingId = null;
      this.bookingCode = null;
      this.sessionStartedAt = new Date().toISOString();
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
    // ── Block Selector (chọn theo khối ghế liền nhau) ──
    setBlockSize(b) { this.currentBlockSize = Number(b) || 0; },
    // Tự chọn khối mặc định = khối hợp lệ LỚN NHẤT cho phần còn lại (tối đa ngồi cùng nhau).
    autoPickBlockSize() {
      const sizes = this.validBlockSizes;
      this.currentBlockSize = sizes.length ? sizes[sizes.length - 1] : 0;
    },
    // Đặt cả một khối ghế (đã quét hợp lệ) vào lựa chọn; mỗi ghế gắn blockId để gỡ nguyên khối.
    addSeatBlock(seats, blockId) {
      seats.forEach(s => this.selectedSeats.push({ ...s, blockId }));
      this.calculateTotal();
    },
    // Gỡ toàn bộ ghế thuộc một khối.
    removeSeatBlock(blockId) {
      this.selectedSeats = this.selectedSeats.filter(s => s.blockId !== blockId);
      this.calculateTotal();
    },
    setTicketQuantity(code, qty) {
      let n = Math.max(0, Number(qty) || 0);
      // Không cho tổng số vé vượt giới hạn cấu hình (chống phe vé)
      const others = this.totalTickets - (Number(this.ticketQuantities[code]) || 0);
      const allowed = Math.max(0, this.maxTicketsPerBooking - others);
      if (n > allowed) n = allowed;
      this.ticketQuantities = { ...this.ticketQuantities, [code]: n };
      this.calculateTotal();
    },
    clearSeats() {
      this.selectedSeats = [];
      this.calculateTotal();
    },
    updateFnb(fnbItem, quantity, options = []) {
      const MAX_FNB_QTY = 99;
      let q = Math.max(0, Number(quantity) || 0);

      const isOptionsEqual = (a, b) => {
          if (!a && !b) return true;
          if (!a || !b) return false;
          if (a.length !== b.length) return false;
          const aIds = a.map(o => o.optionItemId).sort().join(',');
          const bIds = b.map(o => o.optionItemId).sort().join(',');
          return aIds === bIds;
      };
      
      const index = this.selectedFnbs.findIndex(f => f.fnbItem.id === fnbItem.id && isOptionsEqual(f.options, options));
      
      const otherQty = this.selectedFnbs
        .filter((f, i) => f.fnbItem.id === fnbItem.id && i !== index)
        .reduce((sum, f) => sum + f.quantity, 0);

      if (otherQty + q > MAX_FNB_QTY) {
        q = Math.max(0, MAX_FNB_QTY - otherQty);
      }

      if (q > 0) {
        if (index === -1) {
          // ── Snapshot giá tại thời điểm user bấm chọn (Price Lock at Selection) ──
          // Giá được "đóng băng" ngay lúc này, không bị ảnh hưởng nếu admin
          // cập nhật giá sau đó trong khi khách đang ở bước Combo.
          this.selectedFnbs.push({ fnbItem, quantity: q, options, snapshotPrice: fnbItem.price });
        } else {
          this.selectedFnbs[index].quantity = q;
          // snapshotPrice GIỮ NGUYÊN — không cập nhật lại khi tăng số lượng
        }
      } else if (index !== -1) {
        this.selectedFnbs.splice(index, 1);
      }
      this.calculateTotal();
    },
    calculateTotal() {
      let total = 0;
      const assign = this.audienceAssignment; // đối tượng gán cho từng ghế theo thứ tự
      let assignIdx = 0;
      this.selectedSeats.forEach((seat) => {
        const capacity = seat.seatType === 'SWEETBOX' ? 2 : 1;
        for (let j = 0; j < capacity; j++) {
          const aud = assign[assignIdx] || 'ADULT';
          const byAud = this.priceTable[seat.seatType];
          total += (byAud && byAud[aud] != null) ? Number(byAud[aud]) : (seat.price || 0);
          assignIdx++;
        }
      });
      for (const fnb of this.selectedFnbs) {
        let surcharge = 0;
        if (fnb.options) {
           for (const opt of fnb.options) {
               surcharge += (opt.surchargePrice || 0);
           }
        }
        // Dùng snapshotPrice (giá tại thời điểm user bấm chọn) thay vì giá DB hiện tại
        // → tránh tự động thay đổi tổng tiền khi admin cập nhật giá trong khi khách đặt vé
        const basePrice = fnb.snapshotPrice ?? fnb.fnbItem.price;
        total += (basePrice + surcharge) * fnb.quantity;
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
          seatSelections: (() => {
            const sels = [];
            let assignIdx = 0;
            this.selectedSeats.forEach(s => {
              const capacity = s.seatType === 'SWEETBOX' ? 2 : 1;
              for (let j = 0; j < capacity; j++) {
                const aud = this.audienceAssignment[assignIdx] || 'ADULT';
                const byAud = this.priceTable[s.seatType];
                const unitPrice = (byAud && byAud[aud] != null) ? Number(byAud[aud]) : (s.price || 0);
                sels.push({
                  seatId: s.seatId,
                  ticketType: aud,
                  unitPrice: unitPrice
                });
                assignIdx++;
              }
            });
            return sels;
          })(),
          totalTickets: this.totalTickets,
          fnbs: this.selectedFnbs.map(f => ({ 
             fnbItemId: f.fnbItem.id, 
             quantity: f.quantity,
             // Gửi snapshotPrice (giá lock lúc user bấm chọn) lên backend
             // Backend sẽ verify và dùng làm priceSnapshot thay vì luôn fetch DB price
             clientPrice: f.snapshotPrice ?? f.fnbItem.price,
             options: f.options ? f.options.map(o => ({ slotId: o.slotId, optionGroupId: o.optionGroupId, optionItemId: o.optionItemId })) : []
          })),
          voucherId: this.selectedVoucher ? this.selectedVoucher.id : null,
          paymentMethod: paymentMethod,
          sessionStartedAt: this.sessionStartedAt
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
