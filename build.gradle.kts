val projectVersion = "0.4.0"
val projectName = "konform"
val projectGroup = "io.konform"
val projectDesc = "Konform: Portable validations for Kotlin"
val projectHost = "github"
val projectOrg = "konform-kt"
val projectLicense = "MIT"
val projectLicenseUrl = "http://opensource.org/licenses/MIT"
val projectDevelNick = "nlochschmidt"
val projectDevelName = "Niklas Lochschmidt"
val projectInceptionYear = 2018

plugins {
    kotlin("multiplatform") version "1.7.10"
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

repositories {
    mavenCentral()
}

group = projectGroup
version = projectVersion

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "1.5" // Change to 1.6 when switching to Kotlin 1.8
            apiVersion = "1.5"      // Change to 1.6 when switching to Kotlin 1.8
        }
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js {
        browser {}
        nodejs {}
    }
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

tasks.create<Jar>("stubJavadoc") {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["stubJavadoc"])
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
                        url.set("https://github.com/konform-kt/konform.git")
                    }
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
