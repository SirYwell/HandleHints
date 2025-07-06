<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# HandleHints Changelog

## [Unreleased]

## [0.3.3] - 2025-07-06

- Improved type analysis and inspections for `MethodHandles#dropArguments`
- Fixed multi-threading issue

## [0.3.2] - 2025-04-26

- chore: Changelog update - `v0.3.1` by @github-actions in https://github.com/SirYwell/HandleHints/pull/147
- chore(deps): update gradle/actions action to v4 by @renovate in https://github.com/SirYwell/HandleHints/pull/144
- chore(deps): update dependency org.jetbrains.kotlin.jvm to v2 by @renovate in https://github.com/SirYwell/HandleHints/pull/79
- feature: improve exception handling to provide more context by @SirYwell in https://github.com/SirYwell/HandleHints/pull/150
- feature: support MethodHandles#byte(Array|Buffer)ViewVarHandle methods by @SirYwell in https://github.com/SirYwell/HandleHints/pull/148
- feature: support VarHandle#withInvoke(Exact)Behavior methods by @SirYwell in https://github.com/SirYwell/HandleHints/pull/153
- chore: support IJ 2025.1 early access by @SirYwell in https://github.com/SirYwell/HandleHints/pull/154
- feature: improve MethodHandles#filterReturnValue handling by @SirYwell in https://github.com/SirYwell/HandleHints/pull/152
- fix: avoid StackOverflowErrors and empty phi errors by @SirYwell in https://github.com/SirYwell/HandleHints/pull/155
- chore(deps): update dependency gradle to v8.12.1 by @renovate in https://github.com/SirYwell/HandleHints/pull/149
- chore(deps): update jetbrains/qodana-action action to v2024.3.4 by @renovate in https://github.com/SirYwell/HandleHints/pull/137
- chore(deps): update plugin org.gradle.toolchains.foojay-resolver-convention to v0.9.0 by @renovate in https://github.com/SirYwell/HandleHints/pull/151
- chore(deps): update dependency gradle to v8.13 by @renovate in https://github.com/SirYwell/HandleHints/pull/158
- chore(deps): update codecov/codecov-action action to v5 by @renovate in https://github.com/SirYwell/HandleHints/pull/146
- Fix MemoryLayout#varHandle missing leading long parameter by @SirYwell in https://github.com/SirYwell/HandleHints/pull/159

## [0.3.1] - 2024-11-14

- Add `FunctionDescriptor` type analysis
- Add `MethodHandle#withVarargs` type analysis
- Add inspection and quick fix for redundant `MethodHandles#dropReturn` calls
- Add inspection and quick fix for `MethodHandles#constant` calls that can be replaced by `MethodHandles#zero` calls
- Add `MethodHandle#asType` type analysis, including inspections and quick fixes
- Add `MemoryLayout#byteOffsetHandle` type analysis
- Add inspection for `void` parameter types
- Add `Linker#downcallHandle` type analysis
- Improve `MethodHandles#tryFinally` type analysis

## [0.2.1] - 2024-08-12

### Fixed

- Avoid usage of internal API for method icons

## [0.2.0] - 2024-08-11

### Added

- Support a wide range of `MemoryLayout` related methods 
- Contribute type information as documentation on variables
- Inspections for `PaddingLayout` and alignment issues
- Support for methods with `PathElement` parameters
- Contribute completions for methods with `PathElement` parameters

## [0.1.0] - 2024-06-25

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/SirYwell/HandleHints/compare/v0.3.3...HEAD
[0.3.3]: https://github.com/SirYwell/HandleHints/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/SirYwell/HandleHints/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/SirYwell/HandleHints/compare/v0.2.1...v0.3.1
[0.2.1]: https://github.com/SirYwell/HandleHints/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/SirYwell/HandleHints/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/SirYwell/HandleHints/commits/v0.1.0
