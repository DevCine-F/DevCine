# -*- coding: utf-8 -*-
"""
Adapter module that re-exports the 5 operational suites with _expanded names.
"""

from detailed_operational_suites import (
    tc_pos_pending as tc_pos_pending_expanded,
    tc_checkin as tc_checkin_expanded,
    tc_void as tc_void_expanded,
    tc_incident as tc_incident_expanded,
    tc_maint as tc_maint_expanded
)
