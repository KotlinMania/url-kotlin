# url-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Furl--kotlin-blue.svg)](https://github.com/KotlinMania/url-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/url-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/url-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/url-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/url-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`servo/rust-url`](https://github.com/servo/rust-url).

**Original Project:** This port is based on [`servo/rust-url`](https://github.com/servo/rust-url). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `servo/rust-url`

> The text below is reproduced and lightly edited from [`https://github.com/servo/rust-url`](https://github.com/servo/rust-url). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## rust-url


[![Build status](https://github.com/servo/rust-url/workflows/CI/badge.svg)](https://github.com/servo/rust-url/actions?query=workflow%3ACI)
[![Coverage](https://codecov.io/gh/servo/rust-url/branch/master/graph/badge.svg)](https://codecov.io/gh/servo/rust-url)
[![Chat](https://img.shields.io/badge/chat-%23rust--url:mozilla.org-%2346BC99?logo=Matrix)](https://matrix.to/#/#rust-url:mozilla.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE-MIT)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE-APACHE)

URL library for Rust, based on the [URL Standard](https://url.spec.whatwg.org/).

[Documentation](https://docs.rs/url)

Please see [UPGRADING.md](https://github.com/servo/rust-url/blob/main/UPGRADING.md) if you are upgrading from a previous version.

## Alternative Unicode back ends

`url` depends on the `idna` crate. By default, `idna` uses [ICU4X](https://github.com/unicode-org/icu4x/) as its Unicode back end. If you wish to opt for different tradeoffs between correctness, run-time performance, binary size, compile time, and MSRV, please see the [README of the latest version of the `idna_adapter` crate](https://docs.rs/crate/idna_adapter/latest) for how to opt into a different Unicode back end.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:url-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`servo/rust-url`](https://github.com/servo/rust-url). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the rust-url authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`servo/rust-url`](https://github.com/servo/rust-url) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
