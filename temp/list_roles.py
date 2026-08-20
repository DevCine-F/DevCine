# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')
from build_senior_human_testreport import build_all_modules

mods = build_all_modules()
roles = set()
for m in mods:
    roles.add(m['role'])
    print(f"{m['sheet']}: role='{m['role']}', tester='{m['tester']}'")
print("\nUnique roles:", sorted(list(roles)))
