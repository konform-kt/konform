import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val projectName = "konform"
val projectGroup = "io.konform"
val projectDesc = "Konform: Portable validations for Kotlin"
val projectHost = "github"
val projectOrg = "konform-kt"
val projectLicense = "MIT"
val projectLicenseUrl = "https://opensource.org/licenses/MIT"
val projectScmUrl = "https://github.com/konform-kt/konform.git"
val projectInceptionYear = 2018

val kotlinApiTarget = "1.7"
val jvm = JvmTarget.JVM_1_8

/** The "CI" env var is a quasi-standard way to indicate that we're running on CI. */
val onCI: Boolean = System.getenv("CI")?.toBooleanLenient() ?: false

plugins {
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
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
val projectVersion = System.getenv("CI_VERSION") ?: "0.6.2-SNAPSHOT"
version = projectVersion

//region Kotlin and test configuration
kotlin {
    // Since we are a library, prevent accidentally making things part of the public API
    explicitApi()

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
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            // note lang toolchain cannot be used here
            // because gradle no longer supports running on java 8
            jvmTarget = jvm
        }
    }
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
        val kotestSupported = listOf(
            appleTest,
            jsTest,
            jvmTest,
            nativeTest,
            wasmJsTest,
        )
        // Shared dependencies
        commonMain.dependencies {
            api(kotlin("stdlib"))
        }
        // Shared test dependencies
        commonTest.dependencies {
            implementation(kotlin("test"))
            //            implementation(kotlin("test-annotations-common"))
            //            implementation(kotlin("test-common"))
        }
        kotestSupported.forEach {
            it.dependencies {
                implementation(libs.kotest.assertions.core)
                //            implementation(libs.kotest.framework.datatest)
                //            implementation(libs.kotest.framework.engine)
            }
        }
        jvmTest.dependencies {
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
            // Fallback to empty for local and CI builds with no access to the secrets
            // They should not need to publish anyway
            username = System.getenv("MAVEN_CENTRAL_TOKEN_USER") ?: ""
            password = System.getenv("MAVEN_CENTRAL_TOKEN_PW") ?: ""
        }
    }
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
