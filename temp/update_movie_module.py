# -*- coding: utf-8 -*-
"""
Updater that integrates the rich 106-testcase Movie Management Suite into build_senior_human_testreport.py
"""

import sys
sys.stdout.reconfigure(encoding='utf-8')

with open("build_senior_human_testreport.py", "r", encoding="utf-8") as f:
    content = f.read()

import re

# We will remove MOD_ADMIN_MOVIE_CRUD from all_specs and add it as a dedicated module right after MOD_POS_TICKETS
# or in the list of modules.

# Let's inspect where all_specs is defined:
# In build_senior_human_testreport.py:
# Find MOD_ADMIN_MOVIE_CRUD in all_specs and remove it, then add it as a dedicated module.

pattern_import = "from pos_fnb_suite import tc_pos_fnb"
replacement_import = "from pos_fnb_suite import tc_pos_fnb\nfrom movie_management_suite import full_movie_suite"

content = content.replace(pattern_import, replacement_import)

# Now replace all_specs definition of MOD_ADMIN_MOVIE_CRUD
pattern_movie_spec = r'\("MOD_ADMIN_MOVIE_CRUD", "Quản lý phim"[\s\S]*?\[\("MOV_FUNC_01"[\s\S]*?\)\],'

content = re.sub(pattern_movie_spec, '', content)

# Now add Quản lý phim as a dedicated module after MOD_POS_TICKETS or before all_specs loop
# Let's see: in build_senior_human_testreport.py, after all_specs loop or before it:
pattern_before_specs = r'all_specs = \['

replacement_before_specs = """# =========================================================================
    # 17. QUẢN LÝ PHIM (MOD_ADMIN_MOVIE_CRUD) - TÁCH BIỆT THÊM MỚI & CHỈNH SỬA
    # =========================================================================
    # (Được thêm vào sau MOD_POS_TICKETS)

    all_specs = ["""

# Let's add Quản lý phim right into the modules list in proper order.
# The order of modules currently:
# 1. Đăng nhập
# 2. Đăng ký
# 3. Chọn ghế & Giữ chỗ
# 4. Combo F&B online
# 5. Thanh toán VNPAY
# 6. Phân quyền hệ thống
# 7. Thống kê & Báo cáo
# 8. Nhật ký hệ thống
# 9. Quản lý khách hàng
# 10. Sơ đồ ghế
# 11. POS Đơn chờ
# 12. Soát vé & Check-in
# 13. Phê duyệt hủy đơn F&B
# 14. Xử lý sự cố & Đổi ghế
# 15. Khóa bảo trì ghế vật lý
# 16. POS Bán vé tại quầy (from all_specs)
# 17. Quản lý phim -> We will insert it right here!

# Let's do a clean replacement in build_senior_human_testreport.py
# Let's rewrite the module insertion part.
