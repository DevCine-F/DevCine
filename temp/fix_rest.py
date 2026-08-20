# -*- coding: utf-8 -*-
import re

with open("generate_rest_modules.py", "r", encoding="utf-8") as f:
    content = f.read()

# Replace any single \n inside double quotes with \\n if it's not already \\n
# Or rewrite generate_rest_modules.py cleanly
