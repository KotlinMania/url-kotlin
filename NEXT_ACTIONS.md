# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 7/9 (77.8%)
- **Function parity:** 106/193 matched (target 152) — 54.9%
- **Class/type parity:** 12/25 matched (target 41) — 48.0%
- **Combined symbol parity:** 118/218 matched (target 193) — 54.1%
- **Average inline-code cosine:** 0.57 (function body across 6 matched files)
- **Average documentation cosine:** 0.00 (doc text across 6 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 4 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. url.host

- **Target:** `url.Host`
- **Similarity:** 0.42
- **Dependents:** 2
- **Priority Score:** 2031705.9
- **Functions:** 12/15 matched (target 29)
- **Missing functions:** `from`, `fmt`, `eq`
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_

### 2. url.quirks

- **Target:** `url.Quirks`
- **Similarity:** 0.83
- **Dependents:** 1
- **Priority Score:** 1002601.7
- **Functions:** 25/25 matched (target 29)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_

### 3. url.parser

- **Target:** `url.Errors`
- **Similarity:** 0.61
- **Dependents:** 0
- **Priority Score:** 66603.9
- **Functions:** 55/57 matched (target 74)
- **Missing functions:** `fmt`, `size_hint`
- **Types:** 5/9 matched (target 25)
- **Missing types:** `Pattern`, `Item`, `QueryPartIter`, `FragmentPartIter`

### 4. url.path_segments

- **Target:** `url.PathSegments`
- **Similarity:** 0.45
- **Dependents:** 0
- **Priority Score:** 10805.5
- **Functions:** 6/7 matched (target 8)
- **Missing functions:** `drop`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 5. url.slicing

- **Target:** `url.Slicing`
- **Similarity:** 0.33
- **Dependents:** 0
- **Priority Score:** 10506.7
- **Functions:** 3/3 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/2 matched
- **Missing types:** `Output`
- **Tests:** 1/1 matched

### 6. url.origin

- **Target:** `url.Origin`
- **Similarity:** 0.75
- **Dependents:** 0
- **Priority Score:** 702.5
- **Functions:** 5/5 matched (target 6)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 4)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Matched

| Source | Target | Path |
|--------|--------|------|
| `url.lib` | `url.Lib` | `url/src/lib` |

