import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport

val projectName = "konform"
val projectGroup = "io.konform"
val projectDesc = "Konform: Portable validations for Kotlin"
val projectLicense = "MIT"
val projectLicenseUrl = "https://opensource.org/licenses/MIT"
val projectScmUrl = "https://github.com/konform-kt/konform.git"
val projectInceptionYear = 2018

val kotlinApiTarget = "1.9"
val jvmTarget = JavaLanguageVersion.of(11)

/** The "CI" env var is a quasi-standard way to indicate that we're running on CI. */
val onCI: Boolean = System.getenv("CI") == "true"

plugins {
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.powerassert)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.nexuspublish)
    id("maven-publish")
    id("signing")
    idea
}

repositories {
    mavenCentral()
}

group = projectGroup
val projectVersion = System.getenv("CI_VERSION") ?: "0.11.0-SNAPSHOT"
version = projectVersion

//region Kotlin and test configuration
kotlin {
    // Since we are a library, prevent accidentally making things part of the public API
    explicitApi()

    // Binary compatibility validation (built into Kotlin Gradle Plugin)
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    sourceSets.all {
        languageSettings {
            languageVersion = kotlinApiTarget
            apiVersion = kotlinApiTarget
        }
    }

    //region kotlin targets
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    jvmToolchain {
        languageVersion.set(jvmTarget)
    }
    jvm {}
    js(IR) {
        browser {}
        nodejs {}
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    linuxArm64()
    linuxX64()
    macosArm64()
    macosX64()
    mingwX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        d8()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }
    //endregion

    sourceSets {
        // Shared dependencies
        commonMain.dependencies {
            api(kotlin("stdlib"))
        }
        // Shared test dependencies
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }
        jvmTest.dependencies {
            implementation(libs.kotlincompiletesting)
            //            implementation(libs.kotest.runner.junit5)
        }
    }
}
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version = libs.versions.ktlint.get()
}
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = true
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events =
            setOf(
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf("io.kotest.matchers.shouldBe")
}

// WASM does not have tier 1 test support yet, and this fails with error
// "Module parse failed: Identifier 'startUnitTests' has already been declared (14:0)"
listOf("wasmJsNodeTest", "wasmJsD8Test", "wasmJsBrowserTest").forEach {
    tasks.named<KotlinJsTest>(it) {
        enabled = false
    }
}

// Make ABI validation run as part of the check task (which is part of build)
tasks.named("check") {
    dependsOn("checkLegacyAbi")
}

// Automatically update ABI dumps after jvmTest, but only locally (not on CI)
if (!onCI) {
    tasks.named("jvmTest") {
        finalizedBy("updateLegacyAbi")
    }
}

//endregion

//region Publishing configuration
val javaDocJar =
    tasks.register<Jar>("stubJavadoc") {
        archiveClassifier = "javadoc"
    }

publishing {
    publications {
        publications.withType(MavenPublication::class) {
            artifact(javaDocJar)
            pom {
                name = projectName
                description = projectDesc
                inceptionYear = projectInceptionYear.toString()
                url = "https://github.com/konform-kt/konform"
                licenses {
                    license {
                        name = projectLicense
                        url = projectLicenseUrl
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "nlochschmidt"
                        name = "Niklas Lochschmidt"
                    }
                    developer {
                        id = "dhoepelman"
                        name = "David Hoepelman"
                    }
                }
                scm {
                    url = projectScmUrl
                }
            }
        }
    }
}

signing {
    if (onCI) {
        val encryptedSigningKey =
            layout.projectDirectory
                .file(".github/workflows/publishing/github_actions.key.asc")
                .asFile
                .readText()
        useInMemoryPgpKeys(encryptedSigningKey, System.getenv("PGP_PASSPHRASE"))
    } else {
        useGpgCmd()
    }
    sign(publishing.publications)
}

// Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
// https://github.com/gradle/gradle/issues/26091
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            // Fallback to empty for local and CI builds with no access to the secrets
            // They should not need to publish anyway
            username = System.getenv("MAVEN_CENTRAL_TOKEN_USER") ?: ""
            password = System.getenv("MAVEN_CENTRAL_TOKEN_PW") ?: ""
        }
    }
}

// Disable configuration cache for nexus publishing tasks due to incompatibility
tasks.matching { it.name.contains("Sonatype", ignoreCase = true) }.configureEach {
    notCompatibleWithConfigurationCache("Nexus publishing plugin tasks are not compatible with configuration cache")
}
//endregion

//region IDE configuration
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
//endregion
