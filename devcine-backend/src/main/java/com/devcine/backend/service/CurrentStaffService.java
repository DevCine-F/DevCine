package com.devcine.backend.service;

import com.devcine.backend.entity.Staff;
import com.devcine.backend.repository.StaffRepository;
import com.devcine.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ nhân viên của tài khoản đang đăng nhập.
 *
 * <p>Trước khi phân hệ Phân ca bị gỡ, người thực hiện được suy ra từ ca làm việc, nên
 * ADMIN/MANAGER (không có ca) thao tác tại quầy đều không được ghi nhận. Lấy thẳng từ tài khoản
 * đăng nhập là đúng hơn và không phụ thuộc vào bất kỳ lịch làm việc nào.</p>
 */
@Service
@RequiredArgsConstructor
public class CurrentStaffService {

    private final StaffRepository staffRepository;

    /** Null nếu chưa đăng nhập hoặc tài khoản không phải nhân viên (vd khách hàng). */
    @Transactional(readOnly = true)
    public Staff current() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return userId != null ? staffRepository.findById(userId).orElse(null) : null;
    }
}
