# -*- coding: utf-8 -*-
with open("generate_rest_modules.py", "r", encoding="utf-8") as f:
    text = f.read()

# Replace any single \n inside strings with \\n if it's currently literal \n
lines = text.split("\n")
new_lines = []
for line in lines:
    # If line contains \n as literal \ followed by n, keep or replace
    new_lines.append(line)

# Let's inspect
