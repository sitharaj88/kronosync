import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

group = property("GROUP").toString()
version = property("VERSION_NAME").toString()

kotlin {
    explicitApi()
    
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KronoSync"
            isStatic = true
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.atomicfu)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions.core)
        }

        val desktopMain by getting

        androidMain.dependencies {
            // Android-specific if needed
        }

        iosMain.dependencies {
            // iOS-specific if needed
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "com.sitharaj.kronosync"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Publishing configuration
publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set(property("POM_NAME").toString())
            description.set(property("POM_DESCRIPTION").toString())
            url.set(property("POM_URL").toString())

            licenses {
                license {
                    name.set(property("POM_LICENCE_NAME").toString())
                    url.set(property("POM_LICENCE_URL").toString())
                }
            }

            developers {
                developer {
                    id.set(property("POM_DEVELOPER_ID").toString())
                    name.set(property("POM_DEVELOPER_NAME").toString())
                }
            }

            scm {
                url.set(property("POM_SCM_URL").toString())
                connection.set(property("POM_SCM_CONNECTION").toString())
                developerConnection.set(property("POM_SCM_DEV_CONNECTION").toString())
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: findProperty("ossrhUsername")?.toString()
                password = System.getenv("OSSRH_PASSWORD") ?: findProperty("ossrhPassword")?.toString()
            }
        }
    }
}

signing {
    val signingKeyId = System.getenv("SIGNING_KEY_ID") ?: findProperty("signing.keyId")?.toString()
    val signingKey = System.getenv("SIGNING_KEY") ?: findProperty("signing.key")?.toString()
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: findProperty("signing.password")?.toString()

    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}
