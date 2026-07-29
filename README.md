# Pulse POS

A small cross-platform point-of-sale app built with Kotlin Multiplatform and Compose Multiplatform.
Android is the only launcher target; everything else lives in `shared`.

## Modules

| Module        | Contents                                                                 |
| ------------- | ------------------------------------------------------------------------ |
| `app-android` | Android launcher: `Application`, `MainActivity`, manifest, icons, window theme |
| `shared`      | Domain, data, presentation, and Compose UI — layers are packages, not modules |

## Toolchain

| | |
| --- | --- |
| Gradle | 8.14.3 (wrapper) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 |
| Compose Multiplatform | 1.9.3 |
| JDK | 17 bytecode target, built with JDK 21 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |

Libraries: Coroutines 1.10.2, kotlinx-serialization 1.7.3, kotlinx-datetime 0.7.1,
Ktor 3.0.0 (client + MockEngine), SQLDelight 2.0.2, Koin 4.1.1,
Jetbrains Lifecycle ViewModel 2.9.6.

## Building

The JDK on `PATH` must be 17 or 21 — AGP 8.13 does not support JDK 20. The Android Studio
bundled runtime works:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app-android:assembleDebug
```

Run the shared unit tests (they execute on the JVM as Android unit tests, no device needed):

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :shared:testDebugUnitTest
```

`local.properties` must point at an Android SDK install (`sdk.dir`); it is not checked in.

## Conventions

- Money is stored and calculated as `Long` minor units — never `Double`.
- Order IDs are `kotlin.uuid.Uuid` values, used directly as sync idempotency keys.
- Opt-ins for `kotlin.uuid.ExperimentalUuidApi` and `kotlin.time.ExperimentalTime` are applied
  project-wide in `shared/build.gradle.kts` rather than annotated per call site.

Business-rule assumptions (tax base, discount threshold, rounding, stock handling) are documented
here as the corresponding features land.
