# -*- coding: utf-8 -*-
"""
Exports the new Use Cases in exact Markdown format to docs/Dac_Ta_Use_Case_Phu_Luc_A_Moi.md
"""
from generate_macro_doc import macro_use_cases

with open("docs/Dac_Ta_Use_Case_Phu_Luc_A_Moi.md", "w", encoding="utf-8") as f:
    f.write("# PHỤ LỤC A: ĐẶC TẢ USE CASE HỆ THỐNG DEVCINE\n\n")
    
    for uc in macro_use_cases:
        if uc["num"] == 1:
            f.write("## A.1. ĐẶC TẢ CÁC USE CASE KHỐI KHÁCH HÀNG (CUSTOMER PORTAL)\n\n")
        elif uc["num"] == 6:
            f.write("## A.2. ĐẶC TẢ CÁC USE CASE KHỐI QUẢN TRỊ VIÊN & QUẢN LÝ (ADMIN / MANAGER PORTAL)\n\n")

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

print("Generated docs/Dac_Ta_Use_Case_Phu_Luc_A_Moi.md successfully!")
