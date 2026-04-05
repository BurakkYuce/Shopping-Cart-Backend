# Visualization Agent

This agent uses deterministic Python logic — no LLM call required.
See nodes/visualization.py for the decide_chart() function.
This file exists as documentation of the decision rules:

- date/time column + numeric column → "line" (time series trend)
- question contains "top/rank/best/worst/most/least" AND ≤20 rows → "bar" (ranking)
- categorical + exactly 1 numeric + ≤8 rows → "pie" (composition)
- >20 rows + 2 or more numeric columns → "scatter" (distribution/correlation)
- 1 row or aggregated single value → "table" (no chart needed)
- otherwise → "bar" (safe default)
- empty result → "none"
