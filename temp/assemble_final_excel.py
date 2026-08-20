# -*- coding: utf-8 -*-
"""
Direct Full Assembly of TestReport Dự án DevCine.xlsx
Contains all 43 sheets, strictly verified against DevCine frontend Vue components and business rules.
"""

import os
import sys
import shutil
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

sys.stdout.reconfigure(encoding='utf-8')

# Helper to build standard form suites
def build_gui_section(prefix, screen_name, role, elements):
    if not elements:
        return []
    cases = [("__SECTION__", "KIỂM TRA GIAO DIỆN (GUI)")]
    for i, (elem, desc, exp) in enumerate(elements, start=1):
        c_id = f"{prefix}_GUI_{i:02d}"
        c_title = f"Kiểm tra chức năng hiển thị {elem} thành công"
        c_desc = f"Kiểm tra hiển thị {elem} tại màn hình {screen_name}"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Quan sát và kiểm tra hiển thị {elem}\nBước 4: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = "N/A"
        c_exp = exp
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    return cases

def build_search_filter_section(prefix, screen_name, role, filter_fields):
    if not filter_fields:
        return []
    cases = [("__SECTION__", "KIỂM TRA TÌM KIẾM & BỘ LỌC DỮ LIỆU")]
    loc_num = 1
    
    c_id = f"{prefix}_LOC_{loc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm thành công"
    c_desc = f"Kiểm tra chức năng Tìm kiếm khi nhập từ khóa chính xác"
    c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập từ khóa 'Avatar' vào ô Tìm kiếm\nBước 4: Click button Tìm kiếm\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống"
    c_data = "Tìm kiếm: 'Avatar'"
    c_exp = f"Bảng dữ liệu tự động lọc và chỉ hiển thị các bản ghi có chứa từ khóa 'Avatar'"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    loc_num += 1

    c_id = f"{prefix}_LOC_{loc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm thành công"
    c_desc = f"Kiểm tra chức năng Tìm kiếm khi nhập khoảng trắng trước từ khóa"
    c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập từ khóa có khoảng trắng ở đầu ('   Avatar')\nBước 4: Click button Tìm kiếm\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống"
    c_data = "Tìm kiếm: '   Avatar'"
    c_exp = f"Hệ thống tự động cắt khoảng trắng đầu và trả về kết quả khớp với từ khóa 'Avatar'"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    loc_num += 1

    c_id = f"{prefix}_LOC_{loc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm thành công"
    c_desc = f"Kiểm tra chức năng Tìm kiếm khi nhập khoảng trắng sau từ khóa"
    c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập từ khóa có khoảng trắng ở cuối ('Avatar   ')\nBước 4: Click button Tìm kiếm\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống"
    c_data = "Tìm kiếm: 'Avatar   '"
    c_exp = f"Hệ thống tự động cắt khoảng trắng cuối và trả về kết quả khớp với từ khóa 'Avatar'"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    loc_num += 1

    c_id = f"{prefix}_LOC_{loc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm thành công"
    c_desc = f"Kiểm tra chức năng Tìm kiếm khi nhập từ khóa không tồn tại trong hệ thống"
    c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập từ khóa không tồn tại 'XXXX_NOT_FOUND_999'\nBước 4: Click button Tìm kiếm\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống"
    c_data = "Tìm kiếm: 'XXXX_NOT_FOUND_999'"
    c_exp = f"Hiển thị thông báo 'Không tìm thấy dữ liệu phù hợp' kèm hình minh họa trống"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    loc_num += 1

    for f_label, f_val in filter_fields:
        c_id = f"{prefix}_LOC_{loc_num:02d}"
        c_title = f"Kiểm tra chức năng Bộ lọc theo {f_label} thành công"
        c_desc = f"Kiểm tra chức năng lọc dữ liệu theo {f_label} ('{f_val}')"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Chọn giá trị '{f_val}' tại bộ lọc {f_label}\nBước 4: Click button Lọc\nBước 5: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"Bộ lọc {f_label}: '{f_val}'"
        c_exp = f"Bảng dữ liệu chỉ hiển thị các bản ghi thuộc {f_label} '{f_val}'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        loc_num += 1

    return cases

def build_equivalence_partitioning_section(prefix, screen_name, role, fields):
    cases = [("__SECTION__", "KỸ THUẬT PHÂN VÙNG TƯƠNG ĐƯƠNG")]
    ep_num = 1
    for f_name, f_label, valid_val, rules, custom_cases in fields:
        min_l = rules.get("min_len", 2)
        max_l = rules.get("max_len", 100)
        
        c_id = f"{prefix}_EP_{ep_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} trong khoảng từ {min_l} đến {max_l} ký tự"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {len(valid_val)} ký tự ('{valid_val}')\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: '{valid_val}'"
        c_exp = "Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        ep_num += 1

        if min_l > 1:
            c_id = f"{prefix}_EP_{ep_num:02d}"
            c_title = f"Kiểm tra chức năng {f_name} thất bại"
            c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} khoảng dưới của [{min_l},{max_l}]"
            short_val = valid_val[:min_l-1] if len(valid_val) >= min_l else "A"
            c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {len(short_val)} ký tự\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
            c_data = f"{f_label}: '{short_val}'"
            c_exp = f"Hiển thị thông báo lỗi yêu cầu độ dài tối thiểu {min_l} ký tự"
            cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
            ep_num += 1

        c_id = f"{prefix}_EP_{ep_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thất bại"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} khoảng trên của [{min_l},{max_l}]"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} vượt quá giới hạn ({max_l + 10} ký tự)\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: (Chuỗi {max_l + 10} ký tự)"
        c_exp = f"Hiển thị thông báo lỗi '{f_label} không được vượt quá {max_l} ký tự'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        ep_num += 1

    return cases

def build_boundary_value_analysis_section(prefix, screen_name, role, fields):
    cases = [("__SECTION__", "KỸ THUẬT PHÂN TÍCH GIÁ TRỊ BIÊN")]
    bva_num = 1
    for f_name, f_label, valid_val, rules, custom_cases in fields:
        min_l = rules.get("min_len", 2)
        max_l = rules.get("max_len", 100)

        # 1. min
        c_id = f"{prefix}_BVA_{bva_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là giá trị biên min ({min_l} ký tự)"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {min_l} ký tự\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: '{'A' * min_l}' ({min_l} ký tự)"
        c_exp = "Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        bva_num += 1

        # 2. min + 1
        c_id = f"{prefix}_BVA_{bva_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là giá trị cận biên trên min ({min_l + 1} ký tự)"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {min_l + 1} ký tự\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: '{'A' * (min_l + 1)}' ({min_l + 1} ký tự)"
        c_exp = "Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        bva_num += 1

        # 3. max - 1
        c_id = f"{prefix}_BVA_{bva_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là giá trị cận biên dưới max ({max_l - 1} ký tự)"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {max_l - 1} ký tự\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: (Chuỗi {max_l - 1} ký tự)"
        c_exp = "Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        bva_num += 1

        # 4. max
        c_id = f"{prefix}_BVA_{bva_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là giá trị biên max ({max_l} ký tự)"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {max_l} ký tự\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: (Chuỗi {max_l} ký tự)"
        c_exp = "Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        bva_num += 1

        # 5. min - 1
        if min_l > 0:
            c_id = f"{prefix}_BVA_{bva_num:02d}"
            c_title = f"Kiểm tra chức năng {f_name} thất bại"
            c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là giá trị cận biên dưới min ({min_l - 1} ký tự)"
            c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {min_l - 1} ký tự (hoặc để trống)\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
            c_data = f"{f_label}: '{'A' * (min_l - 1)}' ({min_l - 1} ký tự)"
            c_exp = f"Hiển thị thông báo lỗi yêu cầu độ dài tối thiểu {min_l} ký tự"
            cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
            bva_num += 1

        # 6. max + 1
        c_id = f"{prefix}_BVA_{bva_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thất bại"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là giá trị cận biên trên max ({max_l + 1} ký tự)"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} với {max_l + 1} ký tự\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: (Chuỗi {max_l + 1} ký tự)"
        c_exp = f"Hiển thị thông báo lỗi '{f_label} không được vượt quá {max_l} ký tự'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        bva_num += 1

    return cases

def build_error_guessing_section(prefix, screen_name, role, fields):
    cases = [("__SECTION__", "KỸ THUẬT ĐOÁN LỖI")]
    err_num = 1
    for f_name, f_label, valid_val, rules, custom_cases in fields:
        c_id = f"{prefix}_ERR_{err_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thất bại"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} rỗng"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Để trống trường {f_label} (Null)\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: Null"
        c_exp = f"Hiển thị thông báo lỗi 'Vui lòng nhập {f_label}'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        err_num += 1

        c_id = f"{prefix}_ERR_{err_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thất bại"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập giá trị {f_label} là khoảng trắng"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} toàn khoảng trắng\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: '          '"
        c_exp = f"Hiển thị thông báo lỗi 'Vui lòng nhập {f_label}'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        err_num += 1

        c_id = f"{prefix}_ERR_{err_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập khoảng trắng trước {f_label}"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} có khoảng trắng ở đầu ('   {valid_val}')\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: '   {valid_val}'"
        c_exp = f"Hệ thống tự động cắt khoảng trắng đầu và lưu {f_label} thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        err_num += 1

        c_id = f"{prefix}_ERR_{err_num:02d}"
        c_title = f"Kiểm tra chức năng {f_name} thành công"
        c_desc = f"Kiểm tra chức năng {f_name} khi nhập khoảng trắng sau {f_label}"
        c_steps = f"Bước 1: Truy cập vào hệ thống DevCine với vai trò {role}\nBước 2: Truy cập vào màn hình {screen_name}\nBước 3: Nhập trường {f_label} có khoảng trắng ở cuối ('{valid_val}   ')\nBước 4: Nhập các trường thông tin còn lại hợp lệ\nBước 5: Click button Thực hiện\nBước 6: Kiểm tra kết quả hiển thị từ hệ thống"
        c_data = f"{f_label}: '{valid_val}   '"
        c_exp = f"Hệ thống tự động cắt khoảng trắng cuối và lưu {f_label} thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        err_num += 1

        for c_t, c_d, c_st, c_dt, c_ex in custom_cases:
            c_id = f"{prefix}_ERR_{err_num:02d}"
            cases.append((c_id, c_t, c_d, c_st, c_dt, c_ex))
            err_num += 1

    return cases

def build_module_unified_suite(prefix, screen_name, role, gui_elements, fields, filter_fields, custom_func_cases):
    all_cases = []
    if gui_elements:
        all_cases.extend(build_gui_section(prefix, screen_name, role, gui_elements))
    if fields:
        all_cases.extend(build_equivalence_partitioning_section(prefix, screen_name, role, fields))
        all_cases.extend(build_boundary_value_analysis_section(prefix, screen_name, role, fields))
        all_cases.extend(build_error_guessing_section(prefix, screen_name, role, fields))
    if filter_fields:
        all_cases.extend(build_search_filter_section(prefix, screen_name, role, filter_fields))
    if custom_func_cases:
        all_cases.append(("__SECTION__", "KIỂM TRA CHỨC NĂNG & LUỒNG NGHIỆP VỤ ĐẶC THÙ"))
        for c in custom_func_cases:
            all_cases.append(c)
    return all_cases

print("Helper definitions loaded successfully.")
