package com.devcine.backend.controller;

import com.devcine.backend.dto.ApiResponse;
import com.devcine.backend.entity.Booking;
import com.devcine.backend.entity.Seat;
import com.devcine.backend.repository.BookingFnbRepository;
import com.devcine.backend.repository.BookingRepository;
import com.devcine.backend.repository.BookingSeatRepository;
import com.devcine.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFnbRepository bookingFnbRepository;
    private final TicketRepository ticketRepository;
    private final com.devcine.backend.repository.ConcessionSaleRepository concessionSaleRepository;
    private final com.devcine.backend.repository.ConcessionSaleItemRepository concessionSaleItemRepository;

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(2000, 1, 1, 0, 0);

    @GetMapping
    @PreAuthorize("@perm.can('bookings', 'view')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String method,
            @RequestParam(required = false, defaultValue = "") String hasFnb,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Integer staffUserId = isAdmin ? null : (Integer) auth.getPrincipal();

        LocalDateTime fromDt = parseStart(from, MIN_DATE);
        LocalDateTime toDt = parseEnd(to, LocalDateTime.now().plusYears(10));

        // hasFnb: "" (tất cả) | "YES" (có F&B) | "NO" (chỉ vé)
        String hasFnbFilter = hasFnb == null ? "" : hasFnb.trim().toUpperCase();

        // 1. Lấy danh sách Bookings (Vé & Vé+F&B)
        Page<Booking> bookingResult = bookingRepository.searchForAdmin(
                q.trim(), status.trim().toUpperCase(), method.trim().toUpperCase(), staffUserId,
                fromDt, toDt, hasFnbFilter, PageRequest.of(0, 2000));

        List<Integer> bookingIds = bookingResult.getContent().stream().map(Booking::getId).collect(Collectors.toList());
        Map<Integer, Long> seatCounts = new HashMap<>();
        Map<Integer, Long> fnbCounts = new HashMap<>();
        if (!bookingIds.isEmpty()) {
            for (Object[] row : bookingRepository.countSeatsByBookingIds(bookingIds)) {
                seatCounts.put((Integer) row[0], (Long) row[1]);
            }
            for (Object[] row : bookingFnbRepository.countFnbByBookingIds(bookingIds)) {
                fnbCounts.put((Integer) row[0], (Long) row[1]);
            }
        }

        List<Map<String, Object>> allRows = new java.util.ArrayList<>();

        // Map Bookings
        for (Booking b : bookingResult.getContent()) {
            boolean hasCustomer = b.getCustomer() != null && b.getCustomer().getUser() != null;
            Map<String, Object> m = new HashMap<>();
            m.put("bookingId", b.getId());
            m.put("bookingCode", nn(b.getBookingCode()));
            m.put("isConcession", false);
            m.put("orderType", "TICKET");
            m.put("status", nn(b.getStatus()));
            m.put("paymentMethod", nn(b.getPaymentMethod()));
            m.put("totalPrice", b.getTotalPrice());
            m.put("finalPrice", b.getFinalPrice());
            m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            m.put("createdAtRaw", b.getCreatedAt() != null ? b.getCreatedAt() : MIN_DATE);
            m.put("customerName", hasCustomer ? b.getCustomer().getUser().getFullName() : "Khách tại quầy");
            m.put("channel", channelOf(b.getPaymentMethod()));
            m.put("movieTitle", b.getShowtime().getMovie().getTitle());
            m.put("roomName", b.getShowtime().getRoom().getName());
            m.put("showtimeStart", b.getShowtime().getStartTime().toString());
            m.put("seatCount", seatCounts.getOrDefault(b.getId(), 0L));
            long fnbCount = fnbCounts.getOrDefault(b.getId(), 0L);
            m.put("fnbItemCount", fnbCount);
            m.put("hasFnb", fnbCount > 0);
            allRows.add(m);
        }

        // 2. Lấy danh sách ConcessionSale (Bán nhanh F&B độc lập) nếu không lọc "chỉ vé"
        if (!hasFnbFilter.equals("NO")) {
            List<com.devcine.backend.entity.ConcessionSale> concessionList = concessionSaleRepository.searchForAdmin(
                    q.trim(), status.trim().toUpperCase(), method.trim().toUpperCase(), staffUserId,
                    fromDt, toDt);

            List<Integer> saleIds = concessionList.stream().map(com.devcine.backend.entity.ConcessionSale::getId).collect(Collectors.toList());
            Map<Integer, Long> concessionItemCounts = new HashMap<>();
            if (!saleIds.isEmpty()) {
                for (Object[] row : concessionSaleItemRepository.countItemsBySaleIds(saleIds)) {
                    concessionItemCounts.put((Integer) row[0], ((Number) row[1]).longValue());
                }
            }

            for (com.devcine.backend.entity.ConcessionSale s : concessionList) {
                boolean hasCustomer = s.getCustomer() != null && s.getCustomer().getUser() != null;
                Map<String, Object> m = new HashMap<>();
                m.put("bookingId", s.getId());
                m.put("bookingCode", nn(s.getSaleCode()));
                m.put("isConcession", true);
                m.put("orderType", "CONCESSION");
                m.put("status", nn(s.getStatus()));
                m.put("paymentMethod", nn(s.getPaymentMethod()));
                m.put("totalPrice", s.getTotalPrice());
                m.put("finalPrice", s.getTotalPrice());
                m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
                m.put("createdAtRaw", s.getCreatedAt() != null ? s.getCreatedAt() : MIN_DATE);
                m.put("customerName", hasCustomer ? s.getCustomer().getUser().getFullName() : "Khách tại quầy");
                m.put("channel", "Quầy (POS)");
                m.put("movieTitle", "Bán nhanh bắp nước (F&B)");
                m.put("roomName", s.getCinema() != null ? s.getCinema().getName() : "Quầy Concession");
                m.put("showtimeStart", null);
                m.put("seatCount", 0L);
                long fnbCount = concessionItemCounts.getOrDefault(s.getId(), 1L);
                m.put("fnbItemCount", fnbCount);
                m.put("hasFnb", true);
                allRows.add(m);
            }
        }

        // 3. Sắp xếp đơn mới nhất lên đầu tiên theo thời gian tạo
        allRows.sort((a, b) -> ((LocalDateTime) b.get("createdAtRaw")).compareTo((LocalDateTime) a.get("createdAtRaw")));

        // 4. Phân trang
        int totalElements = allRows.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Map<String, Object>> pagedContent = allRows.subList(fromIndex, toIndex);
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / size);

        // Xoá trường phụ createdAtRaw trước khi trả về FE
        for (Map<String, Object> row : pagedContent) {
            row.remove("createdAtRaw");
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "content", pagedContent,
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
        )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('bookings', 'view')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Integer id, @RequestParam(required = false) String type) {
        if ("CONCESSION".equalsIgnoreCase(type)) {
            return getConcessionDetail(id);
        }

        Booking b = bookingRepository.findDetailById(id).orElse(null);
        if (b == null) {
            // Thử tìm theo ConcessionSale
            return getConcessionDetail(id);
        }

        List<Map<String, Object>> seats = bookingSeatRepository.findAllByBookingIdWithSeat(id).stream().map(bs -> {
            Seat seat = bs.getSeat();
            Map<String, Object> s = new HashMap<>();
            s.put("label", seat.displayLabel());
            s.put("seatType", seat.getSeatType() != null ? seat.getSeatType().getName() : "");
            s.put("ticketType", nn(bs.getTicketType()));
            s.put("price", bs.getPriceSnapshot());
            return s;
        }).collect(Collectors.toList());

        List<Map<String, Object>> fnbs = bookingFnbRepository.findByBookingIdWithFnb(id).stream().map(bf -> {
            Map<String, Object> f = new HashMap<>();
            java.math.BigDecimal snapshot = bf.getPriceSnapshot() != null ? bf.getPriceSnapshot() : java.math.BigDecimal.ZERO;
            int qty = bf.getQuantity() != null ? bf.getQuantity() : 0;
            f.put("fnbItemId", bf.getFnbItem() != null ? bf.getFnbItem().getId() : null);
            f.put("name", bf.getItemNameSnapshot() != null ? bf.getItemNameSnapshot() : (bf.getFnbItem() != null ? bf.getFnbItem().getName() : ""));
            f.put("quantity", qty);

            List<Map<String, Object>> options = bf.getOptions().stream().map(o -> {
                Map<String, Object> om = new HashMap<>();
                om.put("slotLabel", o.getSlotLabelSnapshot());
                om.put("optionName", o.getOptionNameSnapshot());
                om.put("surcharge", o.getSurchargeSnapshot());
                om.put("optionItemId", o.getOptionItem() != null ? o.getOptionItem().getId() : null);
                return om;
            }).collect(Collectors.toList());
            f.put("options", options);

            // Tổng phụ thu các tùy chọn
            java.math.BigDecimal totalSurcharge = options.stream()
                    .map(o -> o.get("surcharge") instanceof java.math.BigDecimal
                            ? (java.math.BigDecimal) o.get("surcharge")
                            : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal catalogPrice = (bf.getFnbItem() != null && bf.getFnbItem().getPrice() != null)
                    ? bf.getFnbItem().getPrice() : null;

            java.math.BigDecimal basePrice;
            java.math.BigDecimal finalUnitPrice;

            if (catalogPrice != null && snapshot.compareTo(catalogPrice) == 0 && totalSurcharge.compareTo(java.math.BigDecimal.ZERO) > 0) {
                basePrice = catalogPrice;
                finalUnitPrice = basePrice.add(totalSurcharge);
            } else if (snapshot.compareTo(totalSurcharge) >= 0 && totalSurcharge.compareTo(java.math.BigDecimal.ZERO) > 0) {
                basePrice = snapshot.subtract(totalSurcharge);
                finalUnitPrice = snapshot;
            } else {
                basePrice = catalogPrice != null ? catalogPrice : snapshot;
                finalUnitPrice = basePrice.add(totalSurcharge);
            }

            f.put("basePrice", basePrice);
            f.put("totalSurcharge", totalSurcharge);
            f.put("unitPrice", finalUnitPrice);
            f.put("price", finalUnitPrice);
            f.put("lineTotal", finalUnitPrice.multiply(java.math.BigDecimal.valueOf(qty)));

            return f;
        }).collect(Collectors.toList());

        List<Map<String, Object>> tickets = ticketRepository.findAllByBookingIdWithSeat(id).stream().map(t -> {
            Seat seat = t.getBookingSeat().getSeat();
            Map<String, Object> tk = new HashMap<>();
            tk.put("seatLabel", seat.displayLabel());
            tk.put("qrCode", t.getQrCode());
            tk.put("isCheckedIn", Boolean.TRUE.equals(t.getIsCheckedIn()));
            return tk;
        }).collect(Collectors.toList());

        boolean hasCustomer = b.getCustomer() != null && b.getCustomer().getUser() != null;
        String cinemaName = (b.getShowtime().getRoom() != null && b.getShowtime().getRoom().getCinema() != null)
                ? b.getShowtime().getRoom().getCinema().getName() : "DevCine Landmark 81";
        String cashier = (b.getSoldBy() != null && b.getSoldBy().getUser() != null)
                ? b.getSoldBy().getUser().getFullName()
                : ((b.getPrintedBy() != null && b.getPrintedBy().getUser() != null)
                ? b.getPrintedBy().getUser().getFullName() : null);

        Map<String, Object> dto = new HashMap<>();
        dto.put("bookingId", b.getId());
        dto.put("bookingCode", nn(b.getBookingCode()));
        dto.put("isConcession", false);
        dto.put("status", nn(b.getStatus()));
        dto.put("paymentMethod", nn(b.getPaymentMethod()));
        dto.put("channel", b.getChannel() != null ? b.getChannel() : channelOf(b.getPaymentMethod()));
        dto.put("totalPrice", b.getTotalPrice());
        dto.put("finalPrice", b.getFinalPrice());
        dto.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        dto.put("customerName", hasCustomer ? b.getCustomer().getUser().getFullName() : "Khách tại quầy");
        dto.put("customerPhone", hasCustomer ? b.getCustomer().getUser().getPhone() : null);
        dto.put("membershipTier", hasCustomer ? b.getCustomer().getMembershipTier() : null);
        dto.put("movieTitle", b.getShowtime().getMovie().getTitle());
        dto.put("roomName", b.getShowtime().getRoom().getName());
        dto.put("formatName", b.getShowtime().getFormat().getName());
        dto.put("showtimeStart", b.getShowtime().getStartTime().toString());
        dto.put("showtimeEnd", b.getShowtime().getEndTime() != null ? b.getShowtime().getEndTime().toString() : null);
        dto.put("cinemaName", cinemaName);
        dto.put("voucherCode", b.getVoucher() != null && b.getVoucher().getPromotion() != null
                ? b.getVoucher().getPromotion().getCode() : null);
        dto.put("discountAmount", b.getTotalPrice() != null && b.getFinalPrice() != null
                ? b.getTotalPrice().subtract(b.getFinalPrice()).max(java.math.BigDecimal.ZERO) : java.math.BigDecimal.ZERO);
        dto.put("paymentRef", b.getPaymentRef());
        dto.put("showtimeId", b.getShowtime().getId());
        dto.put("movieId", b.getShowtime().getMovie().getId());
        dto.put("checkedInAt", b.getPrintedAt() != null ? b.getPrintedAt().toString() : null);
        dto.put("checkedInBy", cashier != null ? cashier : (b.getPrintedBy() != null && b.getPrintedBy().getUser() != null ? b.getPrintedBy().getUser().getFullName() : "Đỗ Hoàng Minh"));
        dto.put("seats", seats);
        dto.put("fnbs", fnbs);
        dto.put("tickets", tickets);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    private ResponseEntity<?> getConcessionDetail(Integer saleId) {
        com.devcine.backend.entity.ConcessionSale s = concessionSaleRepository.findDetailById(saleId).orElse(null);
        if (s == null) {
            return ResponseEntity.status(404).body(ApiResponse.fail("Không tìm thấy hoá đơn."));
        }

        List<com.devcine.backend.entity.ConcessionSaleItem> items = concessionSaleItemRepository.findBySaleIdWithOptions(saleId);
        List<Map<String, Object>> fnbs = items.stream().map(ci -> {
            Map<String, Object> f = new HashMap<>();
            java.math.BigDecimal snapshot = ci.getPriceSnapshot() != null ? ci.getPriceSnapshot() : java.math.BigDecimal.ZERO;
            int qty = ci.getQuantity() != null ? ci.getQuantity() : 0;
            f.put("fnbItemId", ci.getFnbItem() != null ? ci.getFnbItem().getId() : null);
            f.put("name", ci.getItemNameSnapshot() != null ? ci.getItemNameSnapshot() : (ci.getFnbItem() != null ? ci.getFnbItem().getName() : ""));
            f.put("quantity", qty);

            List<Map<String, Object>> options = ci.getOptions() == null ? List.of() : ci.getOptions().stream().map(o -> {
                Map<String, Object> om = new HashMap<>();
                om.put("slotLabel", o.getSlotLabelSnapshot());
                om.put("optionName", o.getOptionNameSnapshot());
                om.put("surcharge", o.getSurchargeSnapshot());
                om.put("optionItemId", o.getOptionItem() != null ? o.getOptionItem().getId() : null);
                return om;
            }).collect(Collectors.toList());
            f.put("options", options);

            java.math.BigDecimal totalSurcharge = options.stream()
                    .map(o -> o.get("surcharge") instanceof java.math.BigDecimal
                            ? (java.math.BigDecimal) o.get("surcharge")
                            : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal catalogPrice = (ci.getFnbItem() != null && ci.getFnbItem().getPrice() != null)
                    ? ci.getFnbItem().getPrice() : null;

            java.math.BigDecimal basePrice;
            java.math.BigDecimal finalUnitPrice;

            if (catalogPrice != null && snapshot.compareTo(catalogPrice) == 0 && totalSurcharge.compareTo(java.math.BigDecimal.ZERO) > 0) {
                basePrice = catalogPrice;
                finalUnitPrice = basePrice.add(totalSurcharge);
            } else if (snapshot.compareTo(totalSurcharge) >= 0 && totalSurcharge.compareTo(java.math.BigDecimal.ZERO) > 0) {
                basePrice = snapshot.subtract(totalSurcharge);
                finalUnitPrice = snapshot;
            } else {
                basePrice = catalogPrice != null ? catalogPrice : snapshot;
                finalUnitPrice = basePrice.add(totalSurcharge);
            }

            f.put("basePrice", basePrice);
            f.put("totalSurcharge", totalSurcharge);
            f.put("unitPrice", finalUnitPrice);
            f.put("price", finalUnitPrice);
            f.put("lineTotal", finalUnitPrice.multiply(java.math.BigDecimal.valueOf(qty)));
            return f;
        }).collect(Collectors.toList());

        boolean hasCustomer = s.getCustomer() != null && s.getCustomer().getUser() != null;
        Map<String, Object> dto = new HashMap<>();
        dto.put("bookingId", s.getId());
        dto.put("bookingCode", nn(s.getSaleCode()));
        dto.put("isConcession", true);
        dto.put("orderType", "CONCESSION");
        dto.put("status", nn(s.getStatus()));
        dto.put("paymentMethod", nn(s.getPaymentMethod()));
        dto.put("channel", "Quầy (POS)");
        dto.put("totalPrice", s.getTotalPrice());
        dto.put("finalPrice", s.getTotalPrice());
        dto.put("discountAmount", java.math.BigDecimal.ZERO);
        dto.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        dto.put("customerName", hasCustomer ? s.getCustomer().getUser().getFullName() : "Khách tại quầy");
        dto.put("customerPhone", hasCustomer ? s.getCustomer().getUser().getPhone() : null);
        dto.put("membershipTier", hasCustomer ? s.getCustomer().getMembershipTier() : null);
        dto.put("movieTitle", "Bán nhanh bắp nước (F&B)");
        dto.put("roomName", s.getCinema() != null ? s.getCinema().getName() : "Quầy bắp nước");
        dto.put("formatName", "F&B");
        dto.put("showtimeStart", null);
        dto.put("voucherCode", null);
        dto.put("paymentRef", null);
        dto.put("showtimeId", null);
        dto.put("movieId", null);
        dto.put("checkedInAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        dto.put("checkedInBy", s.getSoldBy() != null && s.getSoldBy().getUser() != null ? s.getSoldBy().getUser().getFullName() : "Thu ngân");
        dto.put("cinemaName", s.getCinema() != null ? s.getCinema().getName() : "DEVCINE CINEMA");
        dto.put("cinemaAddress", s.getCinema() != null ? s.getCinema().getAddress() : "Tầng 3, TTTM DevCine Plaza, Hà Nội");
        dto.put("seats", List.of());
        dto.put("tickets", List.of());
        dto.put("fnbs", fnbs);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // ---- helpers ----
    private String channelOf(String method) {
        if (method == null) return "—";
        switch (method.toUpperCase()) {
            case "CASH": case "CARD": case "TRANSFER": return "Quầy (POS)";
            case "VNPAY": return "Online";
            default: return method;
        }
    }

    private String nn(String s) {
        return s != null ? s : "";
    }

    private LocalDateTime parseStart(String s, LocalDateTime def) {
        if (s == null || s.isBlank()) return def;
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (Exception e) {
            return def;
        }
    }

    private LocalDateTime parseEnd(String s, LocalDateTime def) {
        if (s == null || s.isBlank()) return def;
        try {
            return LocalDate.parse(s).atTime(LocalTime.MAX);
        } catch (Exception e) {
            return def;
        }
    }
}
