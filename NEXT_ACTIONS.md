# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 7/7 (100.0%)
- **Function parity:** 161/192 matched (target 222) — 83.9%
- **Class/type parity:** 15/26 matched (target 44) — 57.7%
- **Combined symbol parity:** 176/218 matched (target 266) — 80.7%
- **Average inline-code cosine:** 0.54 (function body across 7 matched files)
- **Average documentation cosine:** 0.00 (doc text across 7 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 4 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. host

- **Target:** `url.Host`
- **Similarity:** 0.42
- **Dependents:** 2
- **Priority Score:** 2031705.9
- **Functions:** 12/15 matched (target 29)
- **Missing functions:** `from`, `fmt`, `eq`
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_

### 2. lib

- **Target:** `url.Lib`
- **Similarity:** 0.40
- **Dependents:** 0
- **Priority Score:** 318906.0
- **Functions:** 55/80 matched (target 70)
- **Missing functions:** `socket_addrs`, `io_result`, `mutate`, `serialize_internal`, `deserialize_internal`, `from_str`, `try_from`, `fmt`, `from`, `eq`, `cmp`, `partial_cmp`, `hash`, `as_ref`, `slice_of`, `serialize`, `deserialize`, `expecting`, `visit_str`, `path_to_file_url_segments`, `path_to_file_url_segments_windows`, `file_url_segments_to_pathbuf`, `file_url_segments_to_pathbuf_windows`, `as_mut_string`, `drop`
- **Types:** 3/9 matched (target 3)
- **Missing types:** `Err`, `Error`, `RangeArg`, `UrlVisitor`, `Value`, `Finished`

### 3. parser

- **Target:** `url.Errors`
- **Similarity:** 0.61
- **Dependents:** 0
- **Priority Score:** 66603.9
- **Functions:** 55/57 matched (target 74)
- **Missing functions:** `fmt`, `size_hint`
- **Types:** 5/9 matched (target 25)
- **Missing types:** `Pattern`, `Item`, `QueryPartIter`, `FragmentPartIter`

### 4. path_segments

- **Target:** `url.PathSegments`
- **Similarity:** 0.45
- **Dependents:** 0
- **Priority Score:** 10805.5
- **Functions:** 6/7 matched (target 8)
- **Missing functions:** `drop`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 5. slicing

- **Target:** `url.Slicing`
- **Similarity:** 0.33
- **Dependents:** 0
- **Priority Score:** 10506.7
- **Functions:** 3/3 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/2 matched
- **Missing types:** `Output`
- **Tests:** 1/1 matched

### 6. quirks

- **Target:** `url.Quirks`
- **Similarity:** 0.83
- **Dependents:** 0
- **Priority Score:** 2601.7
- **Functions:** 25/25 matched (target 29)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_

### 7. origin

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

