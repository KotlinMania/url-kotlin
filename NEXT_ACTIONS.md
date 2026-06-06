# High Priority

1. **Re-run `ast_distance --deep`** — After successful build + 42 tests green, re-measure to see which gaps closed and which remain.

2. **Port Parser.kt missing functions** — ~56 missing, but ~10 are error types moved to Errors.kt. Focus on: `check_ipv6_loopback`, URL-scheme-specific state machine parts.

3. **Fix remaining Port.kt gap** — Port missing `writeIpv6`-style serialization helpers if needed.

4. **Port Slicing.kt helper functions** — 3 missing functions: `afterPort`, `afterQuery`, `afterFragment` position helpers used by Rust `Position` indexing.

# Medium Priority

5. **Port Origin.kt missing functions** — 3 missing: `same_scheme`, `unicode_serialisation`, `from_url`.

6. **Handle embedded IPv4 in IPv6 parser** — New `parseIpv6Addr` in Host.kt handles this; needs test coverage.

7. **domainToUnicode with IDNA** — Currently a passthrough stub; real IDNA requires external dependency or minimal implementation.

8. **Port PathSegments.kt missing functions** — `pop_if_empty` edge cases, `clear` semantics.

# Low Priority

9. **Port FormUrlEncoded.kt** — Serializer (`form_urlencoded::Serializer`, buffer size hints, pair management).

10. **Platform-specific functions** — `socket_addrs`, `to_file_path`, `from_file_path` (skip unless needed).

11. **Serde support** — Feature-gated in Rust; skip for now.

12. **Documentation** — Add doc comments matching Rust source.

13. **`test_count_digits` test** — Create new test matching `unit.rs`.
