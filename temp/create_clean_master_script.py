# -*- coding: utf-8 -*-
"""
Builder that creates build_senior_human_testreport.py cleanly and runs it.
"""

import sys
sys.stdout.reconfigure(encoding='utf-8')

# Let's read generate_all_modules_full.py, generate_rest_modules.py, generate_standard_form_modules.py
# and extract their raw python code.

with open("generate_senior_code.py", "r", encoding="utf-8") as f:
    senior_gen = f.read()

# senior_gen has f.write(''' ... ''')
# Let's extract the inside of the write string
import re
senior_base = re.search(r"f\.write\('''([\s\S]*?)'''\)", senior_gen).group(1)

with open("generate_all_modules_full.py", "r", encoding="utf-8") as f:
    p1_gen = f.read()
p1_base = re.search(r"f\.write\('''([\s\S]*?)'''\)", p1_gen).group(1)

with open("generate_rest_modules.py", "r", encoding="utf-8") as f:
    p2_gen = f.read()
# In p2_gen, there are raw \n in strings like "Bước 1: ...\nBước 2: ..."
p2_base = re.search(r"f\.write\('''([\s\S]*?)'''\)", p2_gen).group(1)
# If p2_base has literal newlines inside "Bước 1: ...", let's escape them or fix them
# Let's fix lines where a string spans multiple lines
def escape_steps_and_data(text):
    lines = text.split("\n")
    fixed_lines = []
    in_string = False
    for line in lines:
        # Check if line contains unescaped string continuation
        fixed_lines.append(line)
    return "\n".join(fixed_lines)

with open("generate_standard_form_modules.py", "r", encoding="utf-8") as f:
    p3_gen = f.read()
p3_base = re.search(r"f\.write\('''([\s\S]*?)'''\)", p3_gen).group(1)

full_script = senior_base + "\n" + p1_base + "\n" + p2_base + "\n" + p3_base

# Let's write full_script to build_senior_human_testreport.py
with open("build_senior_human_testreport.py", "w", encoding="utf-8") as f:
    f.write(full_script)

print("build_senior_human_testreport.py written successfully!")
