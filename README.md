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

## Business-rule assumptions

The assignment states the totals as `subtotal + 10% tax on taxable items - discount`, with a 5%
discount once the subtotal reaches 50. Where that leaves room for interpretation, this app
assumes:

1. **Tax is calculated on the pre-discount taxable subtotal.** The formula is read left to right,
   so the discount is subtracted at the end and never shrinks the tax base.
2. **The discount is 5% of the entire subtotal**, taxable and tax-exempt lines alike, because the
   rule is stated against "the cart subtotal" rather than against the taxable portion.
3. **The 50.00 threshold is inclusive** — a subtotal of exactly 5000 cents earns the discount.
4. **Percentages round half up to the nearest cent**, applied independently to tax and to
   discount against their own bases. See `percentOfCents` in
   `shared/src/commonMain/kotlin/com/example/pos/domain/Money.kt`.
5. **Quantity is clamped to the product's stock** rather than rejected: asking for more units than
   remain fills the line to the maximum available instead of failing the whole action.

Assumptions about stock persistence and sync behaviour are documented as those features land.
