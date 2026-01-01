# KronoSync

[![Maven Central](https://img.shields.io/maven-central/v/in.sitharaj/kronosync)](https://central.sonatype.com/artifact/in.sitharaj/kronosync)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Kotlin Multiplatform SNTP (Simple Network Time Protocol) client library for accurate network time synchronization.

## Features

- 🌍 **Multiplatform** - Android, iOS, JVM Desktop, JavaScript, WASM
- ⚡ **Coroutine-based** - Modern async API with Kotlin Coroutines
- 🔄 **Automatic Fallback** - Multiple NTP server pools with retry logic
- 🛠️ **Configurable** - Builder pattern for flexible configuration
- 📦 **Lightweight** - Minimal dependencies
- 📚 **Well Documented** - Comprehensive KDoc documentation

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
// For Kotlin Multiplatform projects
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("in.sitharaj:kronosync:1.0.0")
        }
    }
}

// For Android/JVM only projects
dependencies {
    implementation("in.sitharaj:kronosync:1.0.0")
}
```

## Quick Start

### Basic Usage

```kotlin
import `in`.sitharaj.kronosync.KronoSync
import `in`.sitharaj.kronosync.SyncResult

// Initialize once at app startup
KronoSync.initialize()

// Sync with NTP servers
val result = KronoSync.sync()
when (result) {
    is SyncResult.Success -> {
        println("Synced! Offset: ${result.offset}")
        println("Server: ${result.serverAddress}")
    }
    is SyncResult.Failure -> {
        println("Failed: ${result.error}")
    }
}

// Get network-synchronized time
val networkTime = KronoSync.now()
val timeMillis = KronoSync.currentTimeMillis()
```

### Custom Configuration

```kotlin
import `in`.sitharaj.kronosync.NtpConfig
import kotlin.time.Duration.Companion.seconds

KronoSync.initialize(
    NtpConfig.Builder()
        .ntpServers(listOf(
            "time.google.com",
            "time.apple.com",
            "time.cloudflare.com"
        ))
        .timeout(10.seconds)
        .retryCount(3)
        .retryDelay(1.seconds)
        .build()
)
```

### Using NtpClient Directly

For more control, use `NtpClient` directly instead of the singleton:

```kotlin
import `in`.sitharaj.kronosync.NtpClient
import `in`.sitharaj.kronosync.NtpConfig

val client = NtpClient(NtpConfig.DEFAULT)

// Sync
val result = client.sync()

// Get time
val now = client.now()           // Returns null if not synced
val nowOrSystem = client.nowOrSystem()  // Falls back to system time

// Get offset
val offset = client.offset()

// Check sync status
val isSynced = client.isSynchronized()

// Get full snapshot
val snapshot = client.snapshot()
```

## Platform-Specific Notes

### Android

Add Internet permission to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Initialize in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KronoSync.initialize()
        
        // Optional: sync in background
        lifecycleScope.launch {
            KronoSync.sync()
        }
    }
}
```

### iOS

Initialize in your app delegate or SwiftUI App:

```swift
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KronoSyncKt.doInitialize()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### JavaScript/WASM

Due to browser security restrictions, raw UDP sockets are not available. KronoSync uses HTTP-based time APIs as a fallback on these platforms.

## API Reference

### KronoSync (Singleton)

| Method | Description |
|--------|-------------|
| `initialize(config)` | Initialize with optional configuration |
| `sync()` | Sync with NTP servers (suspend) |
| `now()` | Get network time or null |
| `nowOrSystem()` | Get network time or system time |
| `currentTimeMillis()` | Get time in epoch milliseconds |
| `offset()` | Get clock offset duration |
| `isSynchronized()` | Check if synced |
| `snapshot()` | Get full time snapshot |
| `reset()` | Reset sync state |

### NtpConfig.Builder

| Method | Default | Description |
|--------|---------|-------------|
| `ntpServers(list)` | Google, Apple, Cloudflare, etc. | NTP server hostnames |
| `timeout(duration)` | 10 seconds | Request timeout |
| `retryCount(count)` | 3 | Retry attempts per server |
| `retryDelay(duration)` | 1 second | Delay between retries |
| `syncOnInit(bool)` | true | Sync automatically |
| `cacheDuration(duration)` | Infinite | Cache expiration |

## Sample App

Run the sample application:

```bash
# Desktop
./gradlew :sample:composeApp:run

# Android
./gradlew :sample:composeApp:installDebug

# iOS
open sample/iosApp/iosApp.xcworkspace
```

## Publishing

### Local Maven

```bash
./gradlew :kronosync:publishToMavenLocal
```

### Maven Central (Manual Bundle Upload)

1. Configure GPG signing in `~/.gradle/gradle.properties`:
```properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_KEY_PASSWORD
signing.secretKeyRingFile=/path/to/secring.gpg
```

2. Generate the bundle:
```bash
./gradlew :kronosync:zipBundle
```

3. Upload `kronosync/build/bundle/kronosync-bundle.zip` at [central.sonatype.com](https://central.sonatype.com/publishing/deployments)

## License

```
Copyright 2024 Sitharaj Seenivasan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) first.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Acknowledgments

- [RFC 4330](https://tools.ietf.org/html/rfc4330) - Simple Network Time Protocol (SNTP)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) - Multiplatform date/time library
- [kotlinx-coroutines](https://github.com/Kotlin/kotlinx.coroutines) - Coroutines library
