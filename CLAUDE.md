# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Konform is a portable validation library for Kotlin with a type-safe DSL supporting JVM, JavaScript, Native, and WebAssembly platforms with zero dependencies. The library provides comprehensive validation for data structures with detailed error reporting and path tracking.

## Build System & Commands

**Gradle Kotlin DSL** - Uses `build.gradle.kts` with comprehensive multiplatform configuration.

### Essential Commands

```bash
# Build and test all platforms
./gradlew build

# Run tests only
./gradlew check

# Platform-specific tests
./gradlew jvmTest
./gradlew jsTest

# Code formatting and linting
./gradlew ktlintCheck
./gradlew ktlintFormat

# API compatibility validation
./gradlew apiCheck

# Run single test class (JVM)
./gradlew jvmTest --tests "io.konform.validation.ValidationTest"
```

## Architecture & Structure

### Multiplatform Source Organization

- `src/commonMain/` - Platform-agnostic core implementation
- `src/commonTest/` - Shared tests across all platforms  
- `src/{platform}Main/` - Platform-specific implementations (jvm, js, native, wasm variants)
- Platform differences handled via `expect`/`actual` declarations

### Core Components

- **`Validation.kt`** - Main validation interface and factory functions
- **`ValidationBuilder.kt`** - DSL builder for creating type-safe validations
- **`ValidationResult.kt`** - Sealed result types (`Valid`/`Invalid`)
- **`ValidationError.kt`** - Error representation with path tracking
- **`Constraint.kt`** - Individual validation constraint implementations

### Key Packages

- `constraints/` - Built-in validation constraints (String, Number, Iterable, Map, etc.)
- `path/` - Validation path tracking system for precise error location
- `types/` - Different validation implementation types 
- `platform/` - Platform-specific code abstractions

### Design Patterns

1. **DSL-Based API** - Leverages Kotlin DSL capabilities for fluent validation syntax
2. **Type Safety** - Compile-time validation safety using Kotlin's type system
3. **Immutable Design** - All validation results and constraints are immutable
4. **Composable Validations** - Validations can be combined and reused
5. **Zero Dependencies** - No external dependencies for maximum portability

## Platform Support

Extensive platform coverage including:
- **JVM** (Java 11+)
- **JavaScript** (Browser/Node.js)
- **Native** (Android, iOS, Linux, macOS, Windows, tvOS, watchOS variants)  
- **WebAssembly** (WASM-JS, WASM-WASI)

## Testing Strategy

- **Kotest** framework with custom Konform matchers
- Tests run on all supported platforms via commonTest
- API compatibility tracking with binary compatibility validator
- README examples tested to ensure documentation accuracy

## Code Quality

- **Explicit API mode** (`explicitApi()`) prevents accidental public API exposure
- **Ktlint** for consistent code formatting
- **API dumps** in `/api/` directory track binary compatibility
- Minimum Kotlin API version: 2.0

## Publishing & CI

- Automated publishing to Maven Central on `v*` git tags
- Cross-platform builds on GitHub Actions (Ubuntu + macOS)
- PGP signing for release artifacts
- Version managed via `CI_VERSION` environment variable