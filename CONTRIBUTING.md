# Contributing to KronoSync

Thank you for your interest in contributing to KronoSync! This document provides guidelines and instructions for contributing.

## Code of Conduct

Be respectful and constructive in all interactions.

## How to Contribute

### Reporting Bugs

1. Check existing issues to avoid duplicates
2. Use the bug report template
3. Include:
   - Platform (Android, iOS, Desktop, JS)
   - Kotlin version
   - Library version
   - Steps to reproduce
   - Expected vs actual behavior

### Suggesting Features

1. Check existing feature requests
2. Describe the use case
3. Explain the expected behavior

### Submitting Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Write tests for new functionality
4. Ensure all tests pass: `./gradlew allTests`
5. Update documentation as needed
6. Submit a PR with a clear description

## Development Setup

### Prerequisites

- JDK 17+
- Android SDK (for Android development)
- Xcode (for iOS development on macOS)

### Building

```bash
# Build all targets
./gradlew build

# Run tests
./gradlew allTests

# Run sample app (Desktop)
./gradlew :sample:composeApp:run

# Publish to local Maven
./gradlew publishToMavenLocal
```

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc for public APIs
- Keep functions small and focused

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
