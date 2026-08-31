package com.devcine.backend.service;

import com.devcine.backend.dto.request.FnbSelectionDTO;
import com.devcine.backend.entity.Cinema;
import com.devcine.backend.entity.ConcessionSale;
import com.devcine.backend.entity.ConcessionSaleItem;
import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.ConcessionSaleItemOption;
import com.devcine.backend.entity.Staff;
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
    private final FnbOptionValidator fnbOptionValidator;

    @Transactional
    public ConcessionSale createSale(List<FnbSelectionDTO> items, Integer customerId, String paymentMethod) {
        return createSale(items, customerId, paymentMethod, null, null);
    }

    @Transactional
    public ConcessionSale createSale(List<FnbSelectionDTO> items, Integer customerId,
                                     String paymentMethod, Staff soldBy, Cinema cinema) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Vui long chon it nhat 1 mon.");
        }

        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;

        List<Integer> ids = items.stream().map(FnbSelectionDTO::getFnbItemId).toList();
        Map<Integer, FnbItem> fnbMap = new HashMap<>();
        // Nạp kèm slots.optionGroup.items để FnbOptionValidator xác thực server-side.
        fnbItemRepository.findByIdIn(ids).forEach(i -> fnbMap.put(i.getId(), i));

        ConcessionSale sale = ConcessionSale.builder()
                .customer(customer)
                .soldBy(soldBy)
                .cinema(cinema)
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
            // Re-validate lúc checkout: chặn món đã ngưng bán / đã xoá (giỏ hàng kẹt).
            if (item == null || Boolean.TRUE.equals(item.getIsDeleted())
                    || Boolean.FALSE.equals(item.getIsActive())) {
                throw new RuntimeException("Món '"
                        + (item != null ? item.getName() : "#" + dto.getFnbItemId())
                        + "' đã ngưng bán hoặc không tồn tại.");
            }
            int qty = dto.getQuantity() == null ? 0 : dto.getQuantity();
            if (qty < 1 || qty > MAX_QTY_PER_ITEM) {
                throw new RuntimeException("So luong moi mon phai tu 1 den " + MAX_QTY_PER_ITEM + ".");
            }
            
            BigDecimal lineSurcharge = BigDecimal.ZERO;
            List<ConcessionSaleItemOption> mappedOptions = new ArrayList<>();
            ConcessionSaleItem saleItem = ConcessionSaleItem.builder()
                    .sale(sale)
                    .fnbItem(item)
                    .itemNameSnapshot(item.getName()) // chốt cứng tên món cho lịch sử
                    .quantity(qty)
                    .build();

            // Xác thực server-side (membership + min/max + required) và lấy phụ thu TỪ DB.
            for (FnbOptionValidator.ResolvedOption ro : fnbOptionValidator.validateAndResolve(item, dto.getOptions())) {
                lineSurcharge = lineSurcharge.add(ro.surcharge());
                mappedOptions.add(ConcessionSaleItemOption.builder()
                        .saleItem(saleItem)
                        .optionGroup(ro.slot().getOptionGroup())
                        .optionItem(ro.item())
                        .slotLabelSnapshot(ro.slotLabel())
                        .optionNameSnapshot(ro.optionName())
                        .surchargeSnapshot(ro.surcharge())
                        .build());
            }
            BigDecimal basePrice = (dto.getClientPrice() != null && dto.getClientPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? dto.getClientPrice()
                    : item.getPrice();
            BigDecimal finalItemPrice = basePrice.add(lineSurcharge);
            saleItem.setPriceSnapshot(finalItemPrice);
            if (!mappedOptions.isEmpty()) {
                saleItem.setOptions(mappedOptions);
            }
            rows.add(saleItem);
            total = total.add(finalItemPrice.multiply(BigDecimal.valueOf(qty)));
        }
        itemRepository.saveAll(rows);

        sale.setTotalPrice(total);
        saleRepository.save(sale);

        // Tích điểm F&B qua LoyaltyService (đồng nhất với vé: cập nhật hạng + ghi sổ điểm).
        loyaltyService.award(customer, total, "FNB", sale.getSaleCode());

        return sale;
    }
}
