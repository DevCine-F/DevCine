# -*- coding: utf-8 -*-
"""
Script to standardize all roles to standard 4 system roles:
- Quản trị viên
- Quản lý
- Nhân viên
- Khách hàng (và Khách vãng lai)
No 'Thu ngân', 'Nhân viên soát vé', 'Nhân viên rạp', 'Quản lý rạp', etc.
"""

import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

def clean_roles_in_text(text):
    # Specific compound terms first
    replacements = [
        ("Thu ngân (STAFF)", "Nhân viên"),
        ("Nhân viên soát vé (STAFF)", "Nhân viên"),
        ("Nhân viên rạp (STAFF)", "Nhân viên"),
        ("Quản lý rạp (MANAGER)", "Quản lý"),
        ("Quản lý (MANAGER)", "Quản lý"),
        ("Quản trị viên (ADMIN)", "Quản trị viên"),
        ("Nhân viên soát vé", "Nhân viên"),
        ("Nhân viên rạp", "Nhân viên"),
        ("Quản lý rạp", "Quản lý"),
        ("Thu ngân", "Nhân viên"),
        ("thu ngân", "nhân viên"),
        ("nhân viên soát vé", "nhân viên"),
        ("nhân viên rạp", "nhân viên"),
        ("quản lý rạp", "quản lý"),
    ]
    for old, new in replacements:
        text = text.replace(old, new)
    return text

# Process detailed_operational_suites.py
with open("detailed_operational_suites.py", "r", encoding="utf-8") as f:
    c = f.read()
c_new = clean_roles_in_text(c)
with open("detailed_operational_suites.py", "w", encoding="utf-8") as f:
    f.write(c_new)
print("Updated detailed_operational_suites.py")

# Process pos_fnb_suite.py
with open("pos_fnb_suite.py", "r", encoding="utf-8") as f:
    c = f.read()
c_new = clean_roles_in_text(c)
with open("pos_fnb_suite.py", "w", encoding="utf-8") as f:
    f.write(c_new)
print("Updated pos_fnb_suite.py")

# Process build_senior_human_testreport.py
with open("build_senior_human_testreport.py", "r", encoding="utf-8") as f:
    c = f.read()
c_new = clean_roles_in_text(c)
with open("build_senior_human_testreport.py", "w", encoding="utf-8") as f:
    f.write(c_new)
print("Updated build_senior_human_testreport.py")

# Process generate_final_perfect_workbook.py
with open("generate_final_perfect_workbook.py", "r", encoding="utf-8") as f:
    c = f.read()
c_new = clean_roles_in_text(c)
with open("generate_final_perfect_workbook.py", "w", encoding="utf-8") as f:
    f.write(c_new)
print("Updated generate_final_perfect_workbook.py")
