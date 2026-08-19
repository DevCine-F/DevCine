# -*- coding: utf-8 -*-
"""
Exports all 72 Use Cases in exact Markdown format to docs/Dac_Ta_Use_Case.md
"""
from generate_doc_data import use_cases

with open("docs/Dac_Ta_Use_Case.md", "w", encoding="utf-8") as f:
    f.write("# PHỤ LỤC A: ĐẶC TẢ USE CASE HỆ THỐNG DEVCINE\n\n")
    
    current_block = ""
    for uc in use_cases:
        if uc["num"] == 1:
            f.write("## A.1. ĐẶC TẢ CÁC USE CASE KHỐI KHÁCH HÀNG (CUSTOMER PORTAL)\n\n")
        elif uc["num"] == 23:
            f.write("## A.2. ĐẶC TẢ CÁC USE CASE KHỐI NHÂN VIÊN QUẦY & SOÁT VÉ (STAFF / POS / CHECK-IN)\n\n")
        elif uc["num"] == 42:
            f.write("## A.3. ĐẶC TẢ CÁC USE CASE KHỐI QUẢN TRỊ VIÊN & QUẢN LÝ (ADMIN / MANAGER PORTAL)\n\n")

        steps_md = " <br> ".join(uc["steps"])
        notes_md = " <br> ".join([f"- {n}" if not n.startswith("-") else n for n in uc["notes"]])
        
        f.write(f"| Mã Use case | {uc['id']} | Tên Use Case | {uc['name']} |\n")
        f.write("|---|---|---|---|\n")
        f.write(f"| Độ ưu tiên | {uc['priority']} | Tác nhân | {uc['actor']} |\n")
        f.write(f"| Mô tả | {uc['description']} <td colspan=3/> |\n")
        f.write(f"| Luồng chạy | {steps_md} <td colspan=3/> |\n")
        f.write(f"| Lưu ý | {notes_md} <td colspan=3/> |\n\n")
        f.write(f"Bảng A.{uc['num']}: Use case {uc['name'].lower()}.\n\n")
        f.write("---\n\n")

print("Generated docs/Dac_Ta_Use_Case.md successfully!")
