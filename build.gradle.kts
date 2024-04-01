import org.jetbrains.kotlin.config.JvmTarget

val projectVersion = "0.4.0"
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
val jvmTarget = JvmTarget.JVM_1_8
val javaVersion = 8

plugins {
    kotlin("multiplatform") version "1.9.23"
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    idea
}

repositories {
    mavenCentral()
}

group = projectGroup
version = projectVersion

kotlin {
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
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
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

val javaDocJar = tasks.register<Jar>("stubJavadoc") {
    archiveClassifier.set("javadoc")
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(javaDocJar)
            pom {
                name.set(projectName)
                description.set(projectDesc)
                url.set("https://github.com/konform-kt/konform")
                licenses {
                    license {
                        name.set(projectLicense)
                        url.set(projectLicenseUrl)
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set(projectDevelNick)
                        name.set(projectDevelName)
                    }
                }
                scm {
                    url.set(projectScmUrl)
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
