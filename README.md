# HandleHints

![Build](https://github.com/SirYwell/HandleHints/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/24637.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24637.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
<!--
## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Verify the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
-->

<!-- Plugin description -->
This IntelliJ plugin adds inspections and tools to make working with Java MethodHandles easier.

### Features

- Dataflow-sensitive type analysis
- Support for methods from the `MethodHandles` class
- Support for methods from the `MethodHandles.Lookup` class
- Support for methods from the `MethodType` class
- Support for methods from the `MethodHandle` class
- Precise type tracking for parameters and return types separately
- Inspections for supported creation/transformation/combination methods
- Inspections for `invoke` and `invokeExact` arguments and return type checks
- Inlay type hints
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "HandleHints"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/SirYwell/HandleHints/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
