-- =====================================================================
-- Giai đoạn 0 — VERIFY. Chạy SAU 01_backfill_sold_by.sql
-- =====================================================================
-- Điều kiện đi tiếp sang Giai đoạn 1: cột `missing` của CẢ HAI dòng = 0.
-- Nếu còn khác 0 → DỪNG, không được drop staff_schedule_id ở Giai đoạn 6.
-- =====================================================================

SELECT 'bookings' AS bang,
       COUNT(*) FILTER (WHERE staff_schedule_id IS NOT NULL)                       AS co_ca,
       COUNT(*) FILTER (WHERE sold_by IS NOT NULL)                                 AS da_co_sold_by,
       COUNT(*) FILTER (WHERE staff_schedule_id IS NOT NULL AND sold_by IS NULL)   AS missing
FROM bookings
UNION ALL
SELECT 'concession_sales',
       COUNT(*) FILTER (WHERE staff_schedule_id IS NOT NULL),
       COUNT(*) FILTER (WHERE sold_by IS NOT NULL),
       COUNT(*) FILTER (WHERE staff_schedule_id IS NOT NULL AND sold_by IS NULL)
FROM concession_sales;

-- Đối chiếu chéo: sold_by phải khớp đúng staff của ca tương ứng (kỳ vọng: 0 dòng).
SELECT b.id, b.booking_code, b.staff_schedule_id, b.sold_by, ss.staff_id AS staff_dung
FROM bookings b
JOIN staff_schedules ss ON ss.id = b.staff_schedule_id
WHERE b.sold_by IS DISTINCT FROM ss.staff_id;
