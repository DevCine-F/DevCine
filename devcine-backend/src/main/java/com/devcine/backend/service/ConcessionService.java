package com.devcine.backend.service;

import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.ConcessionSale;
import com.devcine.backend.entity.ConcessionSaleItem;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.repository.ConcessionSaleItemRepository;
import com.devcine.backend.repository.ConcessionSaleRepository;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nghiệp vụ bán bắp nước/đồ ăn độc lập tại quầy (Concession Only).
 * Tách hoàn toàn khỏi {@code BookingService}: không suất chiếu, không ghế, không sinh vé QR.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConcessionService {

    private static final BigDecimal POINT_RATE = BigDecimal.valueOf(1000); // 1000đ = 1 điểm
    private static final int MAX_QTY_PER_ITEM = 99;

    private final ConcessionSaleRepository saleRepository;
    private final ConcessionSaleItemRepository itemRepository;
    private final FnbItemRepository fnbItemRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public ConcessionSale createSale(List<FnbSelectionDTO> items, Integer customerId, String paymentMethod) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 món.");
        }

        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;

        // Gom 1 query đọc toàn bộ món được chọn (tránh N+1)
        List<Integer> ids = items.stream().map(FnbSelectionDTO::getFnbItemId).toList();
        Map<Integer, FnbItem> fnbMap = new HashMap<>();
        fnbItemRepository.findAllById(ids).forEach(i -> fnbMap.put(i.getId(), i));

        ConcessionSale sale = ConcessionSale.builder()
                .customer(customer)
                .saleCode("CCS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(paymentMethod != null ? paymentMethod : "CASH")
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .totalPrice(BigDecimal.ZERO)
                .build();
        saleRepository.save(sale);

        // Tính tiền hoàn toàn ở server theo giá hiện tại của món (không tin giá client gửi lên)
        BigDecimal total = BigDecimal.ZERO;
        List<ConcessionSaleItem> rows = new ArrayList<>();
        for (FnbSelectionDTO dto : items) {
            FnbItem item = fnbMap.get(dto.getFnbItemId());
            if (item == null) {
                throw new RuntimeException("Không tìm thấy món F&B (id=" + dto.getFnbItemId() + ").");
            }
            int qty = dto.getQuantity() == null ? 0 : dto.getQuantity();
            if (qty < 1 || qty > MAX_QTY_PER_ITEM) {
                throw new RuntimeException("Số lượng mỗi món phải từ 1 đến " + MAX_QTY_PER_ITEM + ".");
            }
            rows.add(ConcessionSaleItem.builder()
                    .sale(sale)
                    .fnbItem(item)
                    .quantity(qty)
                    .priceSnapshot(item.getPrice())
                    .build());
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        itemRepository.saveAll(rows);

        sale.setTotalPrice(total);
        saleRepository.save(sale);

        // Tích điểm cho thành viên nếu có (khách vãng lai bỏ qua)
        if (customer != null) {
            int earned = total.divide(POINT_RATE, 0, RoundingMode.DOWN).intValue();
            if (earned > 0) {
                int current = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
                customer.setLoyaltyPoints(current + earned);
                customerRepository.save(customer);
            }
        }

        return sale;
    }
}
