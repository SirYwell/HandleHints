import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Set the JVM language level used to compile sources and generate files - Java 11 is required since 2020.3
kotlin {
    @Suppress("UnstableApiUsage")
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty)
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = projectDir.resolve(".qodana").canonicalPath
    reportPath = projectDir.resolve("build/reports/inspections").canonicalPath
    saveReport = true
    showReport = System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        })

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").also { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = System.getenv("PUBLISH_TOKEN")
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
    }

    test {
        doFirst {
            // TODO can we use some gradle caching for this?
            val file = file(".testSdk")
            if (!file.exists()) {
                println("Downloading IntelliJ sources for Mock SDKs...")
                file.createNewFile()
            }
            val targetDir = file.readText()
            if (targetDir.isBlank() || !Files.exists(Path.of(targetDir))) {
                val path = Files.createTempDirectory("intellij-community").toAbsolutePath()
                downloadFromIntelliJRepo("java/mockJDK-11/jre/lib/annotations.jar", path)
                downloadFromIntelliJRepo("java/mockJDK-11/jre/lib/rt.jar", path)
                file.writeText(path.toString())
            }
            systemProperty("idea.home.path", file.readText())
        }
    }

    clean {
        doFirst {
            val testSdkFile = file(".testSdk").toPath()
            // TODO probably clean up unneeded files?
            Files.deleteIfExists(testSdkFile)
        }
    }
}

fun downloadFromIntelliJRepo(filePath: String, targetBase: Path) {
    val outputFile = targetBase.resolve(filePath)
    outputFile.parent.createDirectories()
    val res = exec {
        executable = "curl"
        args = listOf(
            "-o", outputFile.toString(),
            // hardcode last commit containing mockJDK-11.
            // This can probably be removed in future as IntelliJ should download it now automatically (uncertain which version)
            // see https://github.com/JetBrains/intellij-community/commit/77d1d0ab2e8bd5c56a6b9ce2b56adb0d348461b8
            // and https://github.com/JetBrains/intellij-community/commit/403065b8c35a68e3520f49bd48c4569aca0c8af1
            "https://raw.githubusercontent.com/JetBrains/intellij-community/da4c4b3da79fc528db443defccb2349f8250b47c/$filePath"
        )
    }
    res.rethrowFailure()
}
