import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.config.JvmTarget

val projectVersion = "0.6.0-SNAPSHOT"
val projectName = "konform"
val projectGroup = "io.konform"
val projectDesc = "Konform: Portable validations for Kotlin"
val projectHost = "github"
val projectOrg = "konform-kt"
val projectLicense = "MIT"
val projectLicenseUrl = "https://opensource.org/licenses/MIT"
val projectScmUrl = "https://github.com/konform-kt/konform.git"
val projectDevelNick = "nlochschmidt"
val projectDevelName = "Niklas Lochschmidt"
val projectInceptionYear = 2018

val kotlinApiTarget = "1.7"
val jvmTarget = JvmTarget.JVM_11
val javaVersion = 11

/** The "CI" env var is a quasi-standard way to indicate that we're running on CI. */
val onCI: Boolean = System.getenv("CI")?.toBooleanLenient() ?: false

plugins {
    kotlin("multiplatform") version "1.9.24"
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
version = System.getenv("CI_VERSION") ?: projectVersion

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
        compilations.all {
            kotlinOptions.jvmTarget = jvmTarget.toString()
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
    js(IR) {
        browser {}
        nodejs {}
    }
    //    linuxX64()
    //    linuxArm64()
    //    linuxArm32Hfp()
    //    linuxMips32()
    //    linuxMipsel32()
    //    ios()
    //    iosX64()
    //    iosArm64()
    //    iosSimulatorArm64()
    //    macosX64()
    //    macosArm64()
    //    tvos()
    //    tvosArm64()
    //    tvosSimulatorArm64()
    //    tvosX64()
    //    watchos()
    //    watchosArm32()
    //    watchosSimulatorArm64()
    //    watchosArm64()
    //    watchosX86()
    //    watchosX64()
    //    wasm()
    //    wasm32()
    //    mingwX86()
    //    mingwX64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly(kotlin("stdlib"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly(kotlin("stdlib-jdk8"))
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
        create<MavenPublication>("mavenJava") {
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
                        id = projectDevelNick
                        name = projectDevelName
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
