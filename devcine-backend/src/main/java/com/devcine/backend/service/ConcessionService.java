package com.devcine.backend.service;

import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.ConcessionSale;
import com.devcine.backend.entity.ConcessionSaleItem;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.Staff;
import com.devcine.backend.entity.StaffSchedule;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ConcessionService {

    private static final int MAX_QTY_PER_ITEM = 99;

    private final ConcessionSaleRepository saleRepository;
    private final ConcessionSaleItemRepository itemRepository;
    private final FnbItemRepository fnbItemRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public ConcessionSale createSale(List<FnbSelectionDTO> items, Integer customerId, String paymentMethod) {
        return createSale(items, customerId, paymentMethod, null);
    }

    /** {@code soldBy} là nhân viên thực hiện — nguồn quy kết doanh thu quầy F&B. */
    @Transactional
    public ConcessionSale createSale(List<FnbSelectionDTO> items, Integer customerId,
                                     String paymentMethod, Staff soldBy) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Vui long chon it nhat 1 mon.");
        }

        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;

        List<Integer> ids = items.stream().map(FnbSelectionDTO::getFnbItemId).toList();
        Map<Integer, FnbItem> fnbMap = new HashMap<>();
        fnbItemRepository.findAllById(ids).forEach(i -> fnbMap.put(i.getId(), i));

        ConcessionSale sale = ConcessionSale.builder()
                .customer(customer)
                .soldBy(soldBy)
                .saleCode("CCS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(paymentMethod != null ? paymentMethod : "CASH")
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .totalPrice(BigDecimal.ZERO)
                .build();
        saleRepository.save(sale);

        BigDecimal total = BigDecimal.ZERO;
        List<ConcessionSaleItem> rows = new ArrayList<>();
        for (FnbSelectionDTO dto : items) {
            FnbItem item = fnbMap.get(dto.getFnbItemId());
            if (item == null) {
                throw new RuntimeException("Khong tim thay mon F&B (id=" + dto.getFnbItemId() + ").");
            }
            int qty = dto.getQuantity() == null ? 0 : dto.getQuantity();
            if (qty < 1 || qty > MAX_QTY_PER_ITEM) {
                throw new RuntimeException("So luong moi mon phai tu 1 den " + MAX_QTY_PER_ITEM + ".");
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

        // Tích điểm F&B qua LoyaltyService (đồng nhất với vé: cập nhật hạng + ghi sổ điểm).
        loyaltyService.award(customer, total, "FNB", sale.getSaleCode());

        return sale;
    }
}
