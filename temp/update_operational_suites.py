# -*- coding: utf-8 -*-
"""
Updater that integrates the 5 expanded operational suites into generate_final_perfect_workbook.py
"""

import sys
sys.stdout.reconfigure(encoding='utf-8')

with open("generate_final_perfect_workbook.py", "r", encoding="utf-8") as f:
    content = f.read()

import re

# Replace tc_pos_pending, tc_checkin, tc_void, tc_incident, tc_maint
pattern = r"# =========================================================================\s*# 10\. POS ĐƠN CHỜ \(MOD_POS_PENDING\)[\s\S]*?modules\.append\(\{\s*\"code\": \"MOD_MGR_SEAT_MAINTENANCE\"[\s\S]*?\"test_cases\": tc_maint\s*\}\)"

replacement = """from detailed_operational_suites import (
        tc_pos_pending, tc_checkin, tc_void, tc_incident, tc_maint
    )
    # =========================================================================
    # 10. POS ĐƠN CHỜ (MOD_POS_PENDING)
    # =========================================================================
    modules.append({
        "code": "MOD_POS_PENDING", "sheet": "POS Đơn chờ",
        "req": "Kiểm tra Tạo đơn chờ, Chuyển tab đơn hàng, Giới hạn tối đa 3 đơn chờ, Hủy đơn và Timeout 10 phút",
        "tester": "Văn Minh Khôi", "role": "Thu ngân (STAFF)",
        "pre": "Thu ngân mở màn hình TicketingPOS.vue tại quầy vé trên hệ thống DevCine",
        "test_cases": tc_pos_pending
    })

    # =========================================================================
    # 11. SOÁT VÉ & CHECK-IN (MOD_STAFF_CHECKIN)
    # =========================================================================
    modules.append({
        "code": "MOD_STAFF_CHECKIN", "sheet": "Soát vé & Check-in",
        "req": "Kiểm tra Quét mã QR qua Camera, Nhập mã thủ công, Chặn vé đã dùng/giả mạo/sai suất, Âm thanh BEEP và In vé nhiệt",
        "tester": "Văn Minh Khôi", "role": "Nhân viên soát vé (STAFF)",
        "pre": "Nhân viên soát vé mở màn hình StaffTicketCheckin.vue tại cửa phòng chiếu",
        "test_cases": tc_checkin
    })

    # =========================================================================
    # 12. PHÊ DUYỆT HỦY ĐƠN F&B (MOD_MGR_APPROVE_VOID)
    # =========================================================================
    modules.append({
        "code": "MOD_MGR_APPROVE_VOID", "sheet": "Phê duyệt hủy đơn F&B",
        "req": "Kiểm tra Danh sách yêu cầu hủy món F&B, Phê duyệt hoàn tiền, Từ chối kèm lý do, Phân quyền Quản lý và Tab Yêu cầu của tôi",
        "tester": "Nguyễn Quang Huy", "role": "Quản lý (MANAGER)",
        "pre": "Quản lý mở màn hình ApprovalQueue.vue trên Admin Portal",
        "test_cases": tc_void
    })

    # =========================================================================
    # 13. XỬ LÝ SỰ CỐ & ĐỔI GHẾ (MOD_STAFF_INCIDENT_RELOCATE)
    # =========================================================================
    modules.append({
        "code": "MOD_STAFF_INCIDENT_RELOCATE", "sheet": "Xử lý sự cố & Đổi ghế",
        "req": "Kiểm tra Tra cứu vé sự cố, Sơ đồ ghế phòng chiếu, Đổi ngang VIP, Nâng hạng miễn phí, Hủy chỗ hoàn tiền và Tặng Voucher đền bù CSKH",
        "tester": "Văn Minh Khôi", "role": "Nhân viên rạp (STAFF)",
        "pre": "Nhân viên mở màn hình IncidentManagement.vue trong ca trực",
        "test_cases": tc_incident
    })

    # =========================================================================
    # 14. KHÓA BẢO TRÌ GHẾ VẬT LÝ (MOD_MGR_SEAT_MAINTENANCE)
    # =========================================================================
    modules.append({
        "code": "MOD_MGR_SEAT_MAINTENANCE", "sheet": "Khóa bảo trì ghế vật lý",
        "req": "Kiểm tra Khóa bảo trì ghế vật lý theo lý do, Chặn chọn ghế trên Web/POS, Mở khóa sau bảo dưỡng, Đồng bộ WebSocket và Audit Logs",
        "tester": "Nguyễn Ngọc Hà Linh", "role": "Quản lý rạp (MANAGER)",
        "pre": "Quản lý mở sơ đồ ghế phòng chiếu trên Admin Portal",
        "test_cases": tc_maint
    })"""

new_content = re.sub(pattern, replacement, content)

with open("generate_final_perfect_workbook.py", "w", encoding="utf-8") as f:
    f.write(new_content)

print("Updated generate_final_perfect_workbook.py with 5 expanded operational test suites!")
