-- =====================================================================
-- Giai đoạn 0 — Gỡ phân hệ Phân ca: bảo toàn dữ liệu quy kết doanh thu POS
-- =====================================================================
-- Bối cảnh: `staff_schedule_id` trên bookings/concession_sales hiện là nơi
-- DUY NHẤT ghi nhận "đơn này do nhân viên nào bán". Trước khi gỡ phân hệ ca,
-- phải chuyển thông tin đó sang cột `sold_by` trỏ thẳng vào staffs(user_id).
--
-- ĐIỀU KIỆN CHẠY: backend đã khởi động ÍT NHẤT MỘT LẦN sau khi thêm field
-- `soldBy` vào entity Booking/ConcessionSale — Hibernate (ddl-auto=update)
-- sẽ tự tạo cột `sold_by`. Script này KHÔNG tạo cột.
--
-- audit_logs KHÔNG cần backfill: AuditLog.user đã lưu người thực hiện,
-- staff_schedule_id ở đó chỉ là thông tin bổ sung.
--
-- Script chỉ THÊM dữ liệu vào cột mới đang rỗng — không sửa, không xoá gì.
-- =====================================================================

BEGIN;

-- --- Bước 1: kiểm tra cột đã tồn tại (dừng sớm nếu chưa chạy backend) ---
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bookings' AND column_name = 'sold_by'
    ) THEN
        RAISE EXCEPTION 'Chưa có cột bookings.sold_by — hãy khởi động backend một lần trước khi chạy script này.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'concession_sales' AND column_name = 'sold_by'
    ) THEN
        RAISE EXCEPTION 'Chưa có cột concession_sales.sold_by — hãy khởi động backend một lần trước khi chạy script này.';
    END IF;
END $$;

-- --- Bước 2: backfill ---
UPDATE bookings b
SET sold_by = ss.staff_id
FROM staff_schedules ss
WHERE ss.id = b.staff_schedule_id
  AND b.staff_schedule_id IS NOT NULL
  AND b.sold_by IS NULL;

UPDATE concession_sales cs
SET sold_by = ss.staff_id
FROM staff_schedules ss
WHERE ss.id = cs.staff_schedule_id
  AND cs.staff_schedule_id IS NOT NULL
  AND cs.sold_by IS NULL;

COMMIT;
