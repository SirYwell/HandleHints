<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# HandleHints Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/SirYwell/HandleHints/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/SirYwell/HandleHints/compare/v0.2.1...v0.3.1
[0.2.1]: https://github.com/SirYwell/HandleHints/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/SirYwell/HandleHints/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/SirYwell/HandleHints/commits/v0.1.0
