# -*- coding: utf-8 -*-
"""
Build Complete Accurate TestReport Excel for DevCine
"""
import os
import sys
import shutil
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

sys.stdout.reconfigure(encoding='utf-8')

def create_full_testreport():
    out_dir = r"c:\Users\ADMIN\OneDrive\Desktop\DATN\devcine"
    out_file = os.path.join(out_dir, "TestReport Dự án DevCine.xlsx")
    
    # Import the modules definition
    import build_senior_human_testreport as bsh
    from build_accurate_devcine_testreport import build_accurate_workbook_file
    
    print("Generating full workbook...")
    bsh.export_human_testreport()
    print("Completed!")

if __name__ == '__main__':
    create_full_testreport()
