# -*- coding: utf-8 -*-
"""
Updater that integrates the rich 28-testcase suite for POS F&B into generate_final_perfect_workbook.py and builds the workbook.
"""

import sys
sys.stdout.reconfigure(encoding='utf-8')

from pos_fnb_suite import tc_pos_fnb

with open("generate_final_perfect_workbook.py", "r", encoding="utf-8") as f:
    content = f.read()

# Let's inspect where tc_pos_fnb is defined in generate_final_perfect_workbook.py
# and replace it with pos_fnb_suite
import re

# Replace tc_pos_fnb definition
pattern = r"tc_pos_fnb = \[[\s\S]*?modules\.append\(\{\s*\"code\": \"MOD_POS_FNB\"[\s\S]*?\"test_cases\": tc_pos_fnb\s*\}\)"

replacement = """from pos_fnb_suite import tc_pos_fnb
    modules.append({
        "code": "MOD_POS_FNB", "sheet": "POS Bán F&B tại quầy",
        "req": "Kiểm tra Bán bắp nước tại quầy POS, Tùy chọn vị FnbOptionModal, Tra cứu hội viên, Áp Voucher, Tiền mặt/VietQR và In phiếu nhận món",
        "tester": "Văn Minh Khôi", "role": "Thu ngân (STAFF)",
        "pre": "Thu ngân mở màn hình Bán F&B tại quầy Concession trên hệ thống POS DevCine",
        "test_cases": tc_pos_fnb
    })"""

new_content = re.sub(pattern, replacement, content)

with open("generate_final_perfect_workbook.py", "w", encoding="utf-8") as f:
    f.write(new_content)

print("Updated generate_final_perfect_workbook.py with 28 POS F&B test cases!")
