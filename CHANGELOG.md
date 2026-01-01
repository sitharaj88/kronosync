# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-01

### Added

- Initial release of KronoSync
- SNTP client implementation following RFC 4330
- Support for Kotlin Multiplatform targets:
  - Android
  - iOS (x64, arm64, simulatorArm64)
  - JVM Desktop
  - JavaScript (Browser and Node.js)
  - WASM JavaScript
- `NtpClient` class for direct NTP operations
- `KronoSync` singleton for convenient global access
- `NtpConfig` with builder pattern for flexible configuration
- Default NTP server pools (Google, Apple, Cloudflare, pool.ntp.org, Microsoft)
- Configurable timeout, retry count, and retry delay
- Automatic fallback between multiple NTP servers
- Thread-safe implementation using atomic operations
- Comprehensive KDoc documentation
- Unit tests for packet encoding/decoding
- Sample Compose Multiplatform application
- Maven Central publishing configuration
- GitHub Actions CI/CD workflows
- Apache 2.0 License

### Platform-Specific Implementation Details

- **Android/JVM**: Uses `DatagramSocket` for UDP communication
- **iOS**: Uses Darwin POSIX sockets via Kotlin/Native cinterop
- **JavaScript/WASM**: Falls back to HTTP-based time APIs due to browser restrictions

## [Unreleased]

### Planned

- Background periodic sync
- Compose Multiplatform state integration
- Additional time format utilities
- Performance optimizations
- More comprehensive test coverage
