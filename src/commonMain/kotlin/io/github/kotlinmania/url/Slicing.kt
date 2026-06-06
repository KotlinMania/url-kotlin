// port-lint: source slicing.rs
package io.github.kotlinmania.url

public enum class Position {
    BeforeScheme,
    AfterScheme,
    BeforeUsername,
    AfterUsername,
    BeforePassword,
    AfterPassword,
    BeforeHost,
    AfterHost,
    BeforePort,
    AfterPort,
    BeforePath,
    AfterPath,
    BeforeQuery,
    AfterQuery,
    BeforeFragment,
    AfterFragment,
}
