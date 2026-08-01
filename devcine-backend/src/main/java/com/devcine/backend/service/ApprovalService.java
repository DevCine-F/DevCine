package com.devcine.backend.service;

import com.devcine.backend.entity.ApprovalRequest;
import com.devcine.backend.entity.ConcessionSale;
import com.devcine.backend.repository.ApprovalRequestRepository;
import com.devcine.backend.repository.ConcessionSaleRepository;
import com.devcine.backend.repository.UserRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Luồng phê duyệt "sửa sai" nội bộ — chỉ còn FNB_VOID: hủy hóa đơn bắp nước bấm nhầm
 * (hoàn điểm thành viên). Nhân viên quầy tạo yêu cầu; Quản lý/Quản trị viên duyệt.
 *
 * <p>Không còn phụ thuộc Ca làm việc: quyền được gác theo Vai trò (tier) + cách ly cụm rạp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    public static final String TYPE_FNB_VOID = "FNB_VOID";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final ApprovalRequestRepository approvalRepository;
    private final ConcessionSaleRepository concessionSaleRepository;
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;

    // ----------------------------------------------------------------------------------
    // TẠO YÊU CẦU (nhân viên quầy)
    // ----------------------------------------------------------------------------------

    /** Nhân viên F&B (hoặc Quản lý) yêu cầu hủy một hóa đơn bắp nước bấm nhầm. */
    @Transactional
    public ApprovalRequest requestFnbVoid(Integer saleId, String reason) {
        ConcessionSale sale = concessionSaleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn F&B."));
        // Cách ly cụm rạp: chỉ thao tác trên hóa đơn thuộc cơ sở mình.
        SecurityUtils.assertCinemaAccess(sale.getCinema() != null ? sale.getCinema().getId() : null);

        if (!"CONFIRMED".equalsIgnoreCase(sale.getStatus())) {
            throw new IllegalArgumentException("Hóa đơn không ở trạng thái hợp lệ để hủy.");
        }
        if (approvalRepository.existsByTypeAndRefIdAndStatus(TYPE_FNB_VOID, saleId, STATUS_PENDING)) {
            throw new IllegalArgumentException("Hóa đơn này đã có yêu cầu hủy đang chờ duyệt.");
        }

        ApprovalRequest request = baseRequest();
        request.setType(TYPE_FNB_VOID);
        request.setRefId(saleId);
        request.setRefCode(sale.getSaleCode());
        request.setSummary("Hủy hóa đơn " + sale.getSaleCode()
                + " (" + sale.getTotalPrice().toBigInteger() + "đ)");
        request.setReason(reason);
        return approvalRepository.save(request);
    }

    // ----------------------------------------------------------------------------------
    // DUYỆT / TỪ CHỐI (Quản lý hoặc Quản trị viên)
    // ----------------------------------------------------------------------------------

    @Transactional
    public ApprovalRequest approve(Integer requestId) {
        requireApprover();
        ApprovalRequest request = loadPending(requestId);
        if (TYPE_FNB_VOID.equals(request.getType())) {
            executeFnbVoid(request);
        } else {
            throw new IllegalArgumentException("Loại yêu cầu không hỗ trợ: " + request.getType());
        }
        stampDecision(request, STATUS_APPROVED, null);
        return approvalRepository.save(request);
    }

    @Transactional
    public ApprovalRequest reject(Integer requestId, String note) {
        requireApprover();
        ApprovalRequest request = loadPending(requestId);
        stampDecision(request, STATUS_REJECTED, note);
        return approvalRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> listPending() {
        // Quản lý chỉ thấy yêu cầu của cơ sở mình; Admin thấy tất cả.
        Integer cinemaId = SecurityUtils.getCurrentUserCinemaId();
        if (cinemaId != null && !SecurityUtils.isAdmin()) {
            return approvalRepository.findByCinemaIdAndStatusOrderByCreatedAtDesc(cinemaId, STATUS_PENDING);
        }
        return approvalRepository.findByStatusOrderByCreatedAtDesc(STATUS_PENDING);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> listMine() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return List.of();
        return approvalRepository.findByRequestedByUserIdOrderByCreatedAtDesc(userId);
    }

    // ----------------------------------------------------------------------------------
    // Thực thi hành động khi duyệt
    // ----------------------------------------------------------------------------------

    private void executeFnbVoid(ApprovalRequest request) {
        ConcessionSale sale = concessionSaleRepository.findById(request.getRefId())
                .orElseThrow(() -> new IllegalArgumentException("Hóa đơn F&B không còn tồn tại."));
        if (!"CONFIRMED".equalsIgnoreCase(sale.getStatus())) {
            throw new IllegalArgumentException("Hóa đơn đã được xử lý trước đó.");
        }

        // Thu hồi điểm thưởng đã cộng cho thành viên (đảo cả ví lẫn tích lũy trọn đời, ghi sổ điểm).
        loyaltyService.reclaim(sale.getCustomer(), sale.getTotalPrice(), "VOID_FNB", sale.getSaleCode());

        sale.setStatus("VOIDED");
        concessionSaleRepository.save(sale);
        log.info("Đã hủy (void) hóa đơn F&B {} theo yêu cầu duyệt #{}", sale.getSaleCode(), request.getId());
    }

    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    /** Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) được duyệt/từ chối yêu cầu sửa sai. */
    private void requireApprover() {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isManager()) {
            throw new AccessDeniedException("Chỉ Quản lý hoặc Quản trị viên mới được duyệt yêu cầu sửa sai.");
        }
    }

    private ApprovalRequest baseRequest() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return ApprovalRequest.builder()
                .status(STATUS_PENDING)
                .cinemaId(SecurityUtils.getCurrentUserCinemaId())
                .requestedByUserId(userId)
                .requestedByName(resolveUserName(userId))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ApprovalRequest loadPending(Integer requestId) {
        ApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu phê duyệt."));
        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu đã được xử lý.");
        }
        return request;
    }

    private void stampDecision(ApprovalRequest request, String status, String note) {
        request.setStatus(status);
        request.setDecisionNote(note);
        request.setApprovedByUserId(SecurityUtils.getCurrentUserId());
        request.setApprovedByName(resolveUserName(SecurityUtils.getCurrentUserId()));
        request.setDecidedAt(LocalDateTime.now());
    }

    private String resolveUserName(Integer userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> u.getFullName()).orElse(null);
    }
}
