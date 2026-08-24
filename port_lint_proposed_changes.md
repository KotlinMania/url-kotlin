# port-lint Proposed Changes

**Generated:** 2026-08-24
**Source:** tmp/url
**Target:** src

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `commonMain/kotlin/io/github/kotlinmania/url/Host.kt` | `// port-lint: source host.rs` | `// port-lint: source host.rs` | `host.rs` | `port-lint provenance header matched only after fallback normalization: 'host.rs' vs expected 'host.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/Quirks.kt` | `// port-lint: source quirks.rs` | `// port-lint: source quirks.rs` | `quirks.rs` | `port-lint provenance header matched only after fallback normalization: 'quirks.rs' vs expected 'quirks.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/Lib.kt` | `// port-lint: source lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'lib.rs' vs expected 'lib.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/Errors.kt` | `// port-lint: source parser.rs` | `// port-lint: source parser.rs` | `parser.rs` | `port-lint provenance header matched only after fallback normalization: 'parser.rs' vs expected 'parser.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/Parser.kt` | `// port-lint: source parser.rs` | `// port-lint: source parser.rs` | `parser.rs` | `port-lint provenance header matched only after fallback normalization: 'parser.rs' vs expected 'parser.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/PathSegments.kt` | `// port-lint: source path_segments.rs` | `// port-lint: source path_segments.rs` | `path_segments.rs` | `port-lint provenance header matched only after fallback normalization: 'path_segments.rs' vs expected 'path_segments.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/Slicing.kt` | `// port-lint: source slicing.rs` | `// port-lint: source slicing.rs` | `slicing.rs` | `port-lint provenance header matched only after fallback normalization: 'slicing.rs' vs expected 'slicing.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/url/Origin.kt` | `// port-lint: source origin.rs` | `// port-lint: source origin.rs` | `origin.rs` | `port-lint provenance header matched only after fallback normalization: 'origin.rs' vs expected 'origin.rs'` |
