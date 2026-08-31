# port-lint Proposed Changes

**Generated:** 2026-08-31
**Source:** tmp/url/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/url

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/url/Host.kt` | `// port-lint: source url/src/host.rs` | `// port-lint: source host.rs` | `host.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/host.rs' vs expected 'host.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/Lib.kt` | `// port-lint: source url/src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/Errors.kt` | `// port-lint: source url/src/parser.rs` | `// port-lint: source parser.rs` | `parser.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/parser.rs' vs expected 'parser.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/Parser.kt` | `// port-lint: source url/src/parser.rs` | `// port-lint: source parser.rs` | `parser.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/parser.rs' vs expected 'parser.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/PathSegments.kt` | `// port-lint: source url/src/path_segments.rs` | `// port-lint: source path_segments.rs` | `path_segments.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/path_segments.rs' vs expected 'path_segments.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/Slicing.kt` | `// port-lint: source url/src/slicing.rs` | `// port-lint: source slicing.rs` | `slicing.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/slicing.rs' vs expected 'slicing.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/url/SlicingTest.kt` | `// port-lint: tests url/src/slicing.rs` | `// port-lint: tests slicing.rs` | `slicing.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:url/src/slicing.rs' vs expected 'slicing.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/Quirks.kt` | `// port-lint: source url/src/quirks.rs` | `// port-lint: source quirks.rs` | `quirks.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/quirks.rs' vs expected 'quirks.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/url/QuirksTest.kt` | `// port-lint: tests url/src/quirks.rs` | `// port-lint: tests quirks.rs` | `quirks.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:url/src/quirks.rs' vs expected 'quirks.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/url/Origin.kt` | `// port-lint: source url/src/origin.rs` | `// port-lint: source origin.rs` | `origin.rs` | `port-lint provenance header matched only after fallback normalization: 'url/src/origin.rs' vs expected 'origin.rs'` |
