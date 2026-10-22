#!/usr/bin/env python3
"""Summarize real probe callbacks without printing coordinates or addresses."""
import collections
import json
import statistics
import sys
from pathlib import Path

records = [json.loads(line) for line in Path(sys.argv[1]).read_text().splitlines() if line.strip()]
counts = collections.Counter(record['type'] for record in records)
print(f'Records: {len(records)}')
print('Gaps include manual query responses; they are not a guaranteed automatic update cadence.')
for name in ('connected', 'connection_failed', 'location', 'driving', 'route_state', 'navi_error'):
    print(f'{name}: {counts[name]}')
for name in ('location', 'driving'):
    # Monotonic time resets after device restart; exclude negative cross-boot gaps.
    gaps = [record['gapMs'] for record in records
            if record['type'] == name and isinstance(record.get('gapMs'), (int, float))
            and record['gapMs'] >= 0]
    if gaps:
        print(f'{name} gaps (ms): min={min(gaps)}, median={statistics.median(gaps)}, max={max(gaps)}')
    else:
        print(f'{name} gaps: insufficient callbacks')
