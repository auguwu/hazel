/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.floofy.utils.gradle.*
import java.text.SimpleDateFormat
import java.util.Date

buildscript {
    repositories {
        maven("https://maven.floofy.dev/repo/releases")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.3")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.7.2")
        classpath(kotlin("gradle-plugin", version = "1.6.21"))
        classpath(kotlin("serialization", version = "1.6.21"))
        classpath("dev.floofy.commons:commons-gradle:2.0.1")
        classpath("io.kotest:kotest-gradle-plugin:0.3.9")
    }
}

plugins {
    kotlin("plugin.serialization") version "1.7.10"
    id("com.diffplug.spotless") version "6.7.2"
    kotlin("jvm") version "1.7.10"
    id("io.kotest") version "0.3.9"
    application
}

// I can't add this to the plugins block due to it being "not found"
// So, this was the only solution. :(
apply(plugin = "kotlinx-atomicfu")

val JAVA_VERSION = JavaVersion.VERSION_17
val VERSION = Version(1, 2, 1, 0, ReleaseType.None)
val COMMIT_HASH by lazy {
    val cmd = "git rev-parse --short HEAD".split("\\s".toRegex())
    val proc = ProcessBuilder(cmd)
        .directory(File("."))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText().trim()
}

println("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")
println("|> Kotlin: v${KotlinVersion.CURRENT}")
println("|> Gradle: v${GradleVersion.current().toString().replace("Gradle", "").trim()}") // pipe operator in kotlin when :woeme:
println("|> hazel: $VERSION ($COMMIT_HASH)")
println("|> Java: $JAVA_VERSION (JVM: ${System.getProperty("java.version", "Unknown")} [${System.getProperty("java.vendor", "Unknown")}] | JRE: ${Runtime.version()})")
println("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")

group = "dev.floofy.hazel"
version = "$VERSION"

repositories {
    noelware(snapshots = true)
    mavenCentral()
    mavenLocal()
    noelware()
    noel()
}

dependencies {
    // Kotlin libraries
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // BOMs
    api(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.3.3"))
    api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4"))
    api(platform("org.noelware.ktor:ktor-routing-bom:0.1-beta"))
    testImplementation(platform("io.kotest:kotest-bom:5.4.1"))
    api(platform("org.noelware.remi:remi-bom:0.1.4-beta.3"))
    api(platform("dev.floofy.commons:commons-bom:2.1.1"))
    api(platform("io.ktor:ktor-bom:2.0.3"))

    // kotlinx.coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core")

    // kotlinx.datetime libraries
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Noel Utilities
    implementation("dev.floofy.commons:extensions-kotlin")
    implementation("dev.floofy.commons:extensions-koin")
    implementation("dev.floofy.commons:slf4j")

    // Apache Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Ktor Server
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-default-headers")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-mustache")
    implementation("io.ktor:ktor-serialization")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-cors")

    // Koin
    implementation("io.insert-koin:koin-core:3.2.0")

    // Logging with logback
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("ch.qos.logback:logback-core:1.2.11")
    api("org.slf4j:slf4j-api:1.7.36")

    // Conditional logic for logback
    implementation("org.codehaus.janino:janino:3.1.7")

    // Sentry
    implementation("io.sentry:sentry:6.3.1")
    implementation("io.sentry:sentry-logback:6.3.0")
    implementation("io.sentry:sentry-kotlin-extensions:6.3.1")

    // Prometheus (for metrics)
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")
    implementation("io.prometheus:simpleclient_common:0.16.0")
    implementation("io.prometheus:simpleclient:0.16.0")

    // Remi
    implementation("org.noelware.remi:remi-support-minio")
    implementation("org.noelware.remi:remi-support-s3")
    implementation("org.noelware.remi:remi-support-fs")
    api("org.noelware.remi:remi-core")

    // Ktor Routing
    implementation("org.noelware.ktor:core")
    implementation("org.noelware.ktor:loader-koin")

    // TOML
    implementation("com.akuleshov7:ktoml-core:0.2.13")
    implementation("com.akuleshov7:ktoml-file:0.2.13")
}

spotless {
    kotlin {
        trimTrailingWhitespace()
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        endWithNewline()

        // We can't use the .editorconfig file, so we'll have to specify it here
        // issue: https://github.com/diffplug/spotless/issues/142
        // ktlint 0.35.0 (default for Spotless) doesn't support trailing commas
        ktlint("0.43.0")
            .userData(
                mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                )
            )
    }

    kotlinGradle {
        trimTrailingWhitespace()
        endWithNewline()

        // We can't use the .editorconfig file, so we'll have to specify it here
        // issue: https://github.com/diffplug/spotless/issues/142
        // ktlint 0.35.0 (default for Spotless) doesn't support trailing commas
        ktlint("0.43.0")
            .userData(
                mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                )
            )
    }
}

application {
    mainClass.set("dev.floofy.hazel.Bootstrap")
}

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        kotlinOptions.javaParameters = true
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    processResources {
        filesMatching("build-info.json") {
            val date = Date()
            val formatter = SimpleDateFormat("EEE, MMM d, YYYY - HH:mm:ss a")

            expand(
                mapOf(
                    "version" to "$VERSION",
                    "commit_sha" to COMMIT_HASH,
                    "build_date" to formatter.format(date)
                )
            )
        }
    }
}
