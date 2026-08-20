# -*- coding: utf-8 -*-
"""
Perfect Graduation-Thesis Grade Test Report Generator for DevCine
All test titles formatted as:
- 'Kiểm tra hiển thị [tên phần tử cụ thể]' (GUI)
- 'Kiểm tra chức năng [Tên chức năng / Trường nhập] - Thành công'
- 'Kiểm tra chức năng [Tên chức năng / Trường nhập] - Thất bại khi [lý do cụ thể]'
- 'Kiểm tra chức năng Lọc / Tìm kiếm [tiêu chí] - Thành công'

ABSOLUTELY NO '#1', '#2', '#idx', 'phần tử #', 'kịch bản #' placeholders.
100% realistic, senior QA, graduation thesis standard.
"""

import os
import sys
import datetime
import shutil
import openpyxl
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side

sys.stdout.reconfigure(encoding='utf-8')

def build_gui_cases(prefix, screen_name, role, elements):
    """
    Builds realistic GUI test cases for concrete UI elements on a screen.
    elements: list of (element_name, visual_description, expected_appearance)
    """
    cases = []
    for i, (elem, desc, exp) in enumerate(elements, start=1):
        c_id = f"{prefix}_GUI_{i:02d}"
        c_title = f"Kiểm tra hiển thị {elem}"
        c_desc = f"Kiểm tra {desc} trên màn hình {screen_name}"
        c_steps = f"Bước 1: Đăng nhập thành công vào hệ thống DevCine với vai trò '{role}'\nBước 2: Điều hướng đến màn hình '{screen_name}'\nBước 3: Quan sát {elem}\nBước 4: Kiểm tra kết quả hiển thị"
        c_data = "N/A"
        c_exp = exp
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    return cases

def build_field_validation_cases(prefix, screen_name, role, field_name, field_label, normal_val, boundaries, special_cases=None):
    """
    Builds human-like field validation test cases:
    - Để trống
    - Chứa khoảng trắng ở đầu / cuối
    - Quá ngắn / Quá dài
    - Ký tự đặc biệt / SQL / XSS / Định dạng
    - Trùng lặp
    """
    cases = []
    tc_num = 1
    
    # 1. Trống
    c_id = f"{prefix}_VAL_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng {field_label} - Thất bại khi để trống"
    c_desc = f"Kiểm tra thông báo lỗi bắt buộc nhập đối với trường {field_name}"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Để trống trường '{field_name}'\nBước 3: Nhập đầy đủ các trường còn lại hợp lệ\nBước 4: Click button thực thi\nBước 5: Kiểm tra thông báo lỗi hiển thị"
    c_data = f"{field_name}: '' (Để trống)"
    c_exp = f"Hiển thị thông báo lỗi 'Vui lòng nhập {field_name}' màu đỏ bên dưới ô nhập liệu"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 2. Toàn khoảng trắng
    c_id = f"{prefix}_VAL_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng {field_label} - Thất bại khi nhập toàn khoảng trắng"
    c_desc = f"Kiểm tra validate không chấp nhận chuỗi chỉ chứa space"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi khoảng trắng '     ' vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra thông báo lỗi"
    c_data = f"{field_name}: '     '"
    c_exp = f"Hiển thị thông báo lỗi yêu cầu nhập nội dung hợp lệ cho trường {field_name}"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 3. Khoảng trắng ở đầu (Trim)
    c_id = f"{prefix}_VAL_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng {field_label} - Thành công khi nhập có khoảng trắng ở đầu"
    c_desc = f"Kiểm tra hệ thống tự động cắt bỏ khoảng trắng thừa phía trước"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập '   {normal_val}' vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra dữ liệu được lưu"
    c_data = f"{field_name}: '   {normal_val}'"
    c_exp = f"Hệ thống tự động trim khoảng trắng đầu, lưu dữ liệu '{normal_val}' thành công"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 4. Khoảng trắng ở cuối (Trim)
    c_id = f"{prefix}_VAL_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng {field_label} - Thành công khi nhập có khoảng trắng ở cuối"
    c_desc = f"Kiểm tra hệ thống tự động cắt bỏ khoảng trắng thừa phía sau"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập '{normal_val}   ' vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra dữ liệu được lưu"
    c_data = f"{field_name}: '{normal_val}   '"
    c_exp = f"Hệ thống tự động trim khoảng trắng cuối, lưu dữ liệu '{normal_val}' thành công"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # Boundaries: min_len, max_len
    if "min_len" in boundaries:
        min_l = boundaries["min_len"]
        # Min - 1 (Fail)
        c_id = f"{prefix}_VAL_{tc_num:02d}"
        c_title = f"Kiểm tra chức năng {field_label} - Thất bại khi nhập dưới {min_l} ký tự"
        c_desc = f"Kiểm tra chặn độ dài tối thiểu của trường {field_name}"
        c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi có độ dài {min_l - 1} ký tự vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra thông báo lỗi"
        c_data = f"{field_name}: '{'A' * (min_l - 1)}' ({min_l - 1} ký tự)"
        c_exp = f"Hiển thị thông báo lỗi '{field_name} phải chứa ít nhất {min_l} ký tự'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        tc_num += 1

        # Min exact (Pass)
        c_id = f"{prefix}_VAL_{tc_num:02d}"
        c_title = f"Kiểm tra chức năng {field_label} - Thành công khi nhập đúng {min_l} ký tự"
        c_desc = f"Kiểm tra biên độ dài tối thiểu"
        c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi có độ dài đúng {min_l} ký tự vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra kết quả"
        c_data = f"{field_name}: '{'A' * min_l}' ({min_l} ký tự)"
        c_exp = f"Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        tc_num += 1

    if "max_len" in boundaries:
        max_l = boundaries["max_len"]
        # Max exact (Pass)
        c_id = f"{prefix}_VAL_{tc_num:02d}"
        c_title = f"Kiểm tra chức năng {field_label} - Thành công khi nhập đúng {max_l} ký tự"
        c_desc = f"Kiểm tra biên độ dài tối đa"
        c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi có độ dài đúng {max_l} ký tự vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra kết quả"
        c_data = f"{field_name}: (Chuỗi {max_l} ký tự)"
        c_exp = f"Hệ thống chấp nhận dữ liệu và thực hiện thành công"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        tc_num += 1

        # Max + 1 (Fail)
        c_id = f"{prefix}_VAL_{tc_num:02d}"
        c_title = f"Kiểm tra chức năng {field_label} - Thất bại khi nhập vượt quá {max_l} ký tự"
        c_desc = f"Kiểm tra chặn độ dài tối đa"
        c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi có độ dài {max_l + 1} ký tự vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra thông báo lỗi"
        c_data = f"{field_name}: (Chuỗi {max_l + 1} ký tự)"
        c_exp = f"Hiển thị thông báo lỗi '{field_name} không được vượt quá {max_l} ký tự'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        tc_num += 1

    # Security: SQL & XSS
    c_id = f"{prefix}_VAL_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng {field_label} - Thất bại khi nhập mã SQL Injection"
    c_desc = f"Kiểm tra bảo mật chống tấn công SQL Injection"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi \"' OR '1'='1\" vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra phản hồi hệ thống"
    c_data = f"{field_name}: \"' OR '1'='1\""
    c_exp = f"Hệ thống lọc chuỗi an toàn, không gây lỗi cú pháp cơ sở dữ liệu và báo lỗi dữ liệu không hợp lệ"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    c_id = f"{prefix}_VAL_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng {field_label} - Thất bại khi nhập mã XSS Script"
    c_desc = f"Kiểm tra bảo mật chống tấn công Cross-Site Scripting"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập chuỗi '<script>alert(\"XSS\")</script>' vào trường '{field_name}'\nBước 3: Click button thực thi\nBước 4: Kiểm tra phản hồi"
    c_data = f"{field_name}: '<script>alert(\"XSS\")</script>'"
    c_exp = f"Hệ thống tự động HTML-encode hoặc từ chối chuỗi script độc hại, đảm bảo an toàn"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # Extra special cases if provided
    if special_cases:
        for sc_title, sc_desc, sc_steps, sc_data, sc_exp in special_cases:
            c_id = f"{prefix}_VAL_{tc_num:02d}"
            cases.append((c_id, sc_title, sc_desc, sc_steps, sc_data, sc_exp))
            tc_num += 1

    return cases

def build_search_filter_cases(prefix, screen_name, role, filter_fields):
    """
    Builds human-like search and filter test cases.
    """
    cases = []
    tc_num = 1
    
    # 1. Search keyword
    c_id = f"{prefix}_LOC_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm theo từ khóa chính xác - Thành công"
    c_desc = f"Kiểm tra tìm kiếm đúng từ khóa trên màn hình {screen_name}"
    c_steps = f"Bước 1: Đăng nhập vai trò '{role}'\nBước 2: Mở màn hình '{screen_name}'\nBước 3: Nhập từ khóa 'Avatar' vào ô Tìm kiếm\nBước 4: Kiểm tra kết quả hiển thị trên bảng"
    c_data = "Tìm kiếm: 'Avatar'"
    c_exp = f"Bảng dữ liệu tự động lọc và chỉ hiển thị các bản ghi có chứa từ khóa 'Avatar'"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 2. Search with leading spaces
    c_id = f"{prefix}_LOC_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm khi chứa khoảng trắng ở đầu - Thành công"
    c_desc = f"Kiểm tra tự động trim khoảng trắng khi tìm kiếm"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập '   Avatar' vào ô Tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị"
    c_data = "Tìm kiếm: '   Avatar'"
    c_exp = f"Hệ thống tự động cắt khoảng trắng đầu và trả về kết quả khớp với từ khóa 'Avatar'"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 3. Search with trailing spaces
    c_id = f"{prefix}_LOC_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm khi chứa khoảng trắng ở cuối - Thành công"
    c_desc = f"Kiểm tra tự động trim khoảng trắng sau khi tìm kiếm"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập 'Avatar   ' vào ô Tìm kiếm\nBước 3: Kiểm tra kết quả hiển thị"
    c_data = "Tìm kiếm: 'Avatar   '"
    c_exp = f"Hệ thống tự động cắt khoảng trắng cuối và trả về kết quả khớp với từ khóa 'Avatar'"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 4. Search no result
    c_id = f"{prefix}_LOC_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng Tìm kiếm khi không tìm thấy kết quả - Thành công"
    c_desc = f"Kiểm tra hiển thị trạng thái trống (Empty State)"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập từ khóa không tồn tại 'XXXX_NOT_FOUND_999'\nBước 3: Kiểm tra kết quả hiển thị"
    c_data = "Tìm kiếm: 'XXXX_NOT_FOUND_999'"
    c_exp = f"Hiển thị thông báo 'Không tìm thấy dữ liệu phù hợp' kèm hình minh họa trống"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 5. Dropdown filters
    for f_name, f_val in filter_fields:
        c_id = f"{prefix}_LOC_{tc_num:02d}"
        c_title = f"Kiểm tra chức năng Lọc theo {f_name} - Thành công"
        c_desc = f"Kiểm tra lọc dữ liệu theo combobox {f_name}"
        c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Click vào combobox '{f_name}'\nBước 3: Chọn giá trị '{f_val}'\nBước 4: Kiểm tra kết quả hiển thị"
        c_data = f"{f_name}: '{f_val}'"
        c_exp = f"Bảng dữ liệu lọc chính xác toàn bộ các bản ghi có {f_name} là '{f_val}'"
        cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
        tc_num += 1

    # 6. Combined filter
    c_id = f"{prefix}_LOC_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng Lọc kết hợp nhiều điều kiện - Thành công"
    c_desc = f"Kiểm tra lọc đồng thời từ khóa và các combobox"
    c_steps = f"Bước 1: Mở màn hình '{screen_name}'\nBước 2: Nhập từ khóa tìm kiếm và chọn đồng thời các giá trị trên các combobox bộ lọc\nBước 3: Kiểm tra kết quả hiển thị"
    c_data = "Kết hợp đa tiêu chí lọc"
    c_exp = f"Bảng dữ liệu hiển thị các bản ghi thỏa mãn đồng thời tất cả các tiêu chí đã chọn"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    # 7. Reset filters
    c_id = f"{prefix}_LOC_{tc_num:02d}"
    c_title = f"Kiểm tra chức năng Xóa bộ lọc (Reset) - Thành công"
    c_desc = f"Kiểm tra quay về danh sách mặc định khi xóa bộ lọc"
    c_steps = f"Bước 1: Đang ở trạng thái có áp dụng bộ lọc\nBước 2: Click button 'Xóa bộ lọc / Đặt lại'\nBước 3: Kiểm tra danh sách hiển thị"
    c_data = "Click Reset"
    c_exp = f"Toàn bộ các ô tìm kiếm và combobox quay về trạng thái mặc định, hiển thị lại toàn bộ danh sách ban đầu"
    cases.append((c_id, c_title, c_desc, c_steps, c_data, c_exp))
    tc_num += 1

    return cases
