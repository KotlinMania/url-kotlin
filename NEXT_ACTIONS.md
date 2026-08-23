# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 7/7 (100.0%)
- **Function parity:** 99/192 matched (target 147) — 51.6%
- **Class/type parity:** 10/26 matched (target 31) — 38.5%
- **Combined symbol parity:** 109/218 matched (target 178) — 50.0%
- **Average inline-code cosine:** 0.39 (function body across 7 matched files)
- **Average documentation cosine:** 0.00 (doc text across 7 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 5 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. host

- **Target:** `url.Host`
- **Similarity:** 0.16
- **Dependents:** 2
- **Priority Score:** 2081708.4
- **Functions:** 7/15 matched (target 23)
- **Missing functions:** `from`, `to_owned`, `parse_cow`, `parse_opaque_cow`, `into_owned`, `fmt`, `eq`, `write_ipv6`
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_

### 2. parser

- **Target:** `url.Errors`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 656609.9
- **Functions:** 1/57 matched (target 11)
- **Missing functions:** `from`, `fmt`, `is_special`, `is_file`, `default_port`, `new_no_trim`, `new_trim_tab_and_newlines`, `new_trim_c0_control_and_space`, `is_empty`, `starts_with`, `split_prefix`, `split_first`, `count_matching`, `next_utf8`, `next`, `size_hint`, `log_violation`, `log_violation_if`, `for_setter`, `parse_url`, `parse_scheme`, `parse_with_scheme`, `parse_non_special`, `parse_file`, `parse_relative`, `after_double_slash`, `parse_userinfo`, `parse_host_and_port`, `parse_host`, `get_file_host`, `parse_file_host`, `file_host`, `parse_port`, `parse_path_start`, `parse_path`, `push_pending`, `last_slash_can_be_removed`, `pop_path`, `parse_cannot_be_a_base_path`, `with_query_and_fragment`, `parse_query_and_fragment`, `parse_query`, `fragment_only`, `parse_fragment`, `check_url_code_point`, `is_url_code_point`, `c0_control_or_space`, `ascii_tab_or_new_line`, `ascii_alpha`, `to_u32`, `is_normalized_windows_drive_letter`, `is_windows_drive_letter`, `path_starts_with_windows_drive_letter`, `starts_with_windows_drive_letter`, `starts_with_windows_drive_letter_segment`, `fast_u16_to_str`
- **Types:** 0/9 matched (target 14)
- **Missing types:** `ParseResult`, `SchemeType`, `Input`, `Pattern`, `Item`, `Parser`, `Context`, `QueryPartIter`, `FragmentPartIter`

### 3. lib

- **Target:** `url.Lib`
- **Similarity:** 0.40
- **Dependents:** 0
- **Priority Score:** 328906.0
- **Functions:** 54/80 matched (target 70)
- **Missing functions:** `into_string`, `socket_addrs`, `io_result`, `mutate`, `serialize_internal`, `deserialize_internal`, `from_str`, `try_from`, `fmt`, `from`, `eq`, `cmp`, `partial_cmp`, `hash`, `as_ref`, `slice_of`, `serialize`, `deserialize`, `expecting`, `visit_str`, `path_to_file_url_segments`, `path_to_file_url_segments_windows`, `file_url_segments_to_pathbuf`, `file_url_segments_to_pathbuf_windows`, `as_mut_string`, `drop`
- **Types:** 3/9 matched (target 3)
- **Missing types:** `Err`, `Error`, `RangeArg`, `UrlVisitor`, `Value`, `Finished`

### 4. path_segments

- **Target:** `url.PathSegments`
- **Similarity:** 0.34
- **Dependents:** 0
- **Priority Score:** 20806.6
- **Functions:** 5/7 matched
- **Missing functions:** `new`, `drop`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 5. slicing

- **Target:** `url.Slicing`
- **Similarity:** 0.24
- **Dependents:** 0
- **Priority Score:** 20507.6
- **Functions:** 2/3 matched
- **Missing functions:** `test_count_digits`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Output`
- **Tests:** 0/1 matched

### 6. quirks

- **Target:** `url.Quirks`
- **Similarity:** 0.83
- **Dependents:** 0
- **Priority Score:** 2601.7
- **Functions:** 25/25 matched (target 27)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
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

