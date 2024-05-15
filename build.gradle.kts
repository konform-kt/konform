import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

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
val jvm = JvmTarget.JVM_10

/** The "CI" env var is a quasi-standard way to indicate that we're running on CI. */
val onCI: Boolean = System.getenv("CI")?.toBooleanLenient() ?: false

plugins {
    kotlin("multiplatform") version "2.0.0-RC3"
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    idea
}

repositories {
    mavenCentral()
}

group = projectGroup
val projectVersion = System.getenv("CI_VERSION") ?: "0.6.0-SNAPSHOT"
version = projectVersion

kotlin {
    // Since we are a library, prevent accidentally making things part of the public API
    explicitApi()

    sourceSets.all {
        languageSettings {
            languageVersion = kotlinApiTarget
            apiVersion = kotlinApiTarget
        }
    }
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
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
    linuxX64()
    linuxArm64()
    iosX64()
    iosArm64()
    macosX64()
    macosArm64()
    tvosArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi()
    mingwX64()
    sourceSets {
        commonMain {
            dependencies {
                api(kotlin("stdlib"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version = "1.2.1"
}

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
        val encryptedSigningKey = layout.projectDirectory.file(".github/workflows/publishing/github_actions.key.asc").asFile.readText()
        useInMemoryPgpKeys(encryptedSigningKey, System.getenv("PGP_PASSPHRASE"))
    } else {
        useGpgCmd()
    }
    sign(publishing.publications)
}
//region Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
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

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
