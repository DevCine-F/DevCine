package com.devcine.backend.enums;

import java.util.List;

/**
 * Vị trí (Position) làm việc trong ca — nguồn duy nhất cho mã vị trí dùng ở
 * {@code StaffSchedule.workPosition} và các điểm gate quyền theo ca (ShiftAccessService).
 *
 * <p>Mô hình: Quyền thực tế = Tier (Role) + Position (kích hoạt khi mở ca). SHIFT_LEAD là
 * vị trí đặc biệt được phép ở mọi quầy + có quyền phê duyệt (void hóa đơn F&B, đổi ghế sự cố).</p>
 */
public final class WorkPosition {

    /** Quầy vé (Ticket_Staff): bán vé, in vé, bàn giao ca vé. */
    public static final String POS_TICKETING = "POS_TICKETING";
    /** Quầy bắp nước (FNB_Staff): bán combo F&B, kiểm kê nhanh, bàn giao ca F&B. */
    public static final String FNB = "FNB";
    /** Kiểm vé & sảnh (Gate_Staff): quét QR, đánh dấu vé đã vào phòng. Chặn tiền/kho. */
    public static final String CHECK_IN = "CHECK_IN";
    /** Trưởng ca (Shift_Leader): chạy giữa các quầy, có quyền phê duyệt sửa sai. */
    public static final String SHIFT_LEAD = "SHIFT_LEAD";
    /** Phòng chiếu (giữ tương thích dữ liệu cũ; không thuộc 4 vị trí spec chính). */
    public static final String PROJECTION = "PROJECTION";

    /** Vị trí có quyền phê duyệt (duyệt void F&B / đổi ghế). */
    public static final List<String> APPROVERS = List.of(SHIFT_LEAD);

    private WorkPosition() {
    }

    /** allowedPositions cho một quầy luôn kèm SHIFT_LEAD (Trưởng ca chạy qua lại mọi quầy). */
    public static List<String> withLeader(String position) {
        return List.of(position, SHIFT_LEAD);
    }
}
