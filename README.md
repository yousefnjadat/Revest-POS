# Revest POS

A small offline-first point-of-sale app built with Kotlin Multiplatform and Compose Multiplatform.
A cashier browses a catalog, builds a cart, and checks out. **Every sale is written to the local
database before any network call**, so a completed order can never be lost to a bad connection.
Orders sync to the backend when the device is online, retry safely when a request fails, and can
never be accepted twice.

Android is the only launcher target; all logic and UI live in the `shared` module.

---

## 1. What it does

- Loads a product catalog over HTTP (Ktor + MockEngine).
- Add / remove products, change quantities, never exceeding available stock.
- Calculates subtotal, tax, discount and total in integer cents.
- An **Online / Offline** switch in the app bar simulates connectivity.
- Checkout stores the order in SQLite (SQLDelight) first, then syncs if online.
- Pending orders sync automatically when the connection returns, or manually via **Sync now**.
- The mock backend fails the first order it ever sees once, so the retry path is demonstrable.
- Retries reuse the order's UUID as an idempotency key, so nothing is duplicated.

## 2. Assignment requirements and where they live

| Requirement | Implementation |

| Product catalog | `data/catalog/`, `ui/screens/CatalogScreen.kt` |
| Add / remove from cart | `domain/Cart.kt`, `presentation/PosViewModel.kt` |
| Quantity changes respecting stock | `domain/Cart.kt`, `domain/CartLine.kt` |
| Subtotal + 10% tax − discount | `domain/CartCalculator.kt` |
| 5% discount at subtotal ≥ 50.00 | `domain/CartCalculator.kt` |
| Online / Offline toggle | `ui/components/ConnectionStatusControl.kt`, `PosViewModel.setOnline` |
| Local persistence of every checkout | `sqldelight/…/PendingOrders.sq`, `data/order/` |
| Sync via Ktor MockEngine | `data/remote/MockPosBackend.kt`, `data/order/OrderSyncApi.kt` |
| UUID idempotency keys | `PosViewModel.newOrderId`, `KtorOrderSyncApi.submit` |
| Simulated transient failure + retry | `MockPosBackend.shouldFailNow`, `data/sync/OrderSyncCoordinator.kt` |
| Coroutines + StateFlow | `PosViewModel.state`, SQLDelight `asFlow()` |
| Manual + automatic sync, with logging | `PosViewModel.syncNow` / `setOnline`, `domain/SyncTrigger.kt`, `PosLog.kt` |
| Compose Multiplatform UI | `ui/` |
| Koin dependency injection | `di/KoinSetup.kt` |

## 3. Technology stack

| | |
| --- | --- |
| Kotlin | 2.2.21 |
| Gradle / Android Gradle Plugin | 8.14.3 / 8.13.2 |
| Compose Multiplatform | 1.9.3 (Material 3) |
| SQLDelight | 2.0.2 |
| Ktor client | 3.0.0 (MockEngine, ContentNegotiation) |
| Koin | 4.1.1 |
| Coroutines / Serialization / DateTime | 1.10.2 / 1.7.3 / 0.7.1 |
| Lifecycle ViewModel (multiplatform) | 2.9.6 |
| JDK | JVM 17 bytecode, built with JDK 21 |
| SDK | compileSdk 36, targetSdk 36, minSdk 26 |

## 4. Structure

Two Gradle modules. Layers are packages, not modules.

```
pos-kmp/
├── app-android/                  Android launcher only
│   └── src/main/…                PosApplication, MainActivity, manifest, icons
└── shared/
    └── src/
        ├── commonMain/kotlin/com/example/pos/
        │   ├── domain/           Product, Cart, CartLine, CartTotals, CartCalculator,
        │   │                     Order, OrderLine, OrderSyncState, SyncTrigger, Money
        │   ├── data/
        │   │   ├── catalog/      CatalogApi, CatalogRepository, DTOs
        │   │   ├── order/        OrderSyncApi, OrderRepository, OrderLocalDataSource, mapping
        │   │   ├── remote/       MockPosBackend (Ktor MockEngine)
        │   │   └── sync/         OrderSyncCoordinator
        │   ├── presentation/     PosViewModel, PosUiState, AppDestination
        │   ├── ui/               PosApp, screens/, components/, theme/
        │   └── di/               Koin modules, ioDispatcher
        ├── commonMain/sqldelight/…/PendingOrders.sq
        ├── androidMain/          SQLDelight driver, Dispatchers.IO, @Preview composables
        ├── commonTest/           pure domain, mapping and repository-contract tests
        └── androidUnitTest/      tests needing a JVM SQLite database
```

One `PosViewModel` backs all three destinations, because the cart, catalog and order history are
shared state — splitting them would only mean synchronising them again. Navigation is a three-value
`AppDestination` enum held in state; there is no navigation library.

## 5. Data flow

```
Catalog  ──add──▶  Cart  ──checkout──▶  Local order (PENDING)
   ▲                 │                        │
   │                 │                        ├── offline ──▶ stays PENDING, waits
 Ktor                │                        │
 MockEngine       CartCalculator              └── online ──▶ POST /orders
                  (pure totals)                                 │
                                                    ┌───────────┴───────────┐
                                               201/200 OK               503 error
                                                    │                       │
                                              synced_at set          last_error set
                                              → SYNCED               → FAILED (still local)
                                                                            │
                                                              manual sync / reconnect
                                                                            │
                                                                    retry same UUID
                                                                            └──▶ SYNCED
```

The database is the source of truth. The Orders screen renders a SQLDelight `Flow`, so it updates
itself whenever a row changes — nothing pushes state into it.

## 6. Offline-first behaviour — the key decision

**Every checkout is written to SQLite before any network call.**

- **Offline checkout**: persist, clear the cart, show the order as *Saved, pending*. No request is made.
- **Online checkout**: persist, clear the cart, *then* fire a sync as a separate coroutine.

The cart is cleared on successful persistence, not on a successful network round trip, so the
cashier is never blocked by the network and **a request failure can never lose a completed sale**.
If the local write itself fails, the cart is kept and the message says nothing was charged.

Sync is triggered by exactly three things, each logged with its reason:

| Trigger | Log line |
| Cashier presses Sync now | `sync requested (manual trigger)` |
| Offline → Online transition | `sync requested (changed to online)` |
| Checkout while already online | `sync requested (checkout while online)` |

`OrderSyncCoordinator` guards the loop with a `Mutex.tryLock()`: a trigger arriving while a sync is
in flight is skipped rather than queued, because the run already going covers the same backlog.
Orders are sent sequentially, oldest first, and one failure never stops the rest of the batch.

## 7. Idempotency

Each order gets a `kotlin.uuid.Uuid` at checkout, and that id is the idempotency key end to end:

- **Locally** — it is the primary key of `pending_orders`, and inserts use `INSERT OR IGNORE`, so
  replaying a checkout cannot create a second row.
- **On the wire** — it travels twice: in the `Idempotency-Key` header (what a gateway would
  deduplicate on) and as `order_id` in the body (what the service stores). The mock backend rejects
  a request where the two disagree.
- **On the backend** — the mock keeps a set of accepted ids. A replayed id is answered `200` with
  `"duplicate": true` instead of being recorded again, and the client treats that as success.

A failed request is deliberately **not** added to the accepted set, so a retry runs the full accept
path exactly once.

## 8. Transient failure simulation

`MockPosBackend(TransientFailureMode.FirstOrderOnly)` — the default — answers `503` to the first
order it ever sees, once. That order stays local and is shown as **Retry needed** with the reason
and a note that it is safe and cannot be charged twice. The next sync of the same UUID succeeds and
the card becomes **Synced … after 2 attempts**. No second list entry appears, and the backend's
accepted set still holds exactly one id.

## 9. Money

**All money is a `Long` count of cents.** Binary floating point cannot represent decimal cent values
exactly — `0.1 + 0.2 != 0.3` — and a POS that accumulates cent-level error across a shift is
unacceptable. Integers are exact, compare and sum reliably, and map straight onto SQLite `INTEGER`
and the JSON contract's `price_cents` / `total_cents`. `Double` appears nowhere in the money path.

Percentages use one documented rule, `percentOfCents` in `domain/Money.kt`: **half up to the nearest
cent**, using integer arithmetic only — `(amount * percent + 50) / 100`.

## 10. Tax and discount rules

```
tax      = 10% of the taxable line subtotal
discount = 5% of the FULL subtotal, when subtotal >= 50.00
total    = subtotal + tax - discount
```

Where the assignment leaves room for interpretation, this app assumes:

1. **Tax is calculated on the pre-discount taxable subtotal.** The formula is read left to right, so
   the discount is subtracted at the end and never shrinks the tax base.
2. **The discount base is the entire subtotal**, taxable and tax-exempt lines alike, because the rule
   is stated against "the cart subtotal".
3. **The 50.00 threshold is inclusive** — a subtotal of exactly 5000 cents earns the discount.
4. **Tax and discount round independently**, each half up against its own base.
5. **Quantity is clamped to stock, not rejected** — asking for more units than remain fills the line
   to the maximum available.
6. **Catalog stock is a snapshot** of what the backend served; the app does not decrement inventory
   after a sale, since there is no inventory service in scope.
7. **A single currency**, formatted as `$0.00`; there is no locale-aware currency formatting.
8. **Order times use the device time zone** and a 24-hour clock, labelled "Today" when applicable.

Worked example (the manual scenario below): subtotal `$60.00`, of which `$35.00` is taxable →
tax `$3.50`, discount `$3.00`, total **`$60.50`**.

## 11. Running the Android app

The JDK used by Gradle must be 17 or 21 — AGP 8.13 does not support JDK 20. Android Studio's
bundled runtime (`<Android Studio>/jbr`) satisfies this:

```bash
./gradlew :app-android:assembleDebug
```

If the default `java` on your `PATH` is a different major version, point `JAVA_HOME` at a JDK 17 or
21 install for the command above.

Install and launch on a connected device or emulator:

```bash
./gradlew :app-android:installDebug
```

`local.properties` must point at an Android SDK (`sdk.dir`); it is not checked in. Opening the
project in Android Studio and pressing Run works too — the `@Preview` composables under
`shared/src/androidMain` cover every screen state.

## 12. Running tests

Everything runs on the JVM as Android unit tests. No device, no emulator:

```bash
./gradlew :shared:testDebugUnitTest
```

114 tests: pure domain and calculator tests in `commonTest`, plus tests that need a real SQLite
database (`JdbcSqliteDriver(IN_MEMORY)`) in `androidUnitTest`. `CheckoutPersistenceTest` drives the
view model over the real database, the real sync coordinator and the MockEngine backend.

Lint:

```bash
./gradlew :app-android:lintDebug
```

## 13. Manual demonstration

1. Launch the app — the catalog loads eight products.
2. Switch the app bar control to **Offline**.
3. Add 2 × *Espresso Beans* (tax exempt, $12.50) and 2 × *Ceramic Mug* (taxable, $17.50).
4. Open **Cart**: subtotal `$60.00`, tax `$3.50`, discount `−$3.00`, total `$60.50`.
5. Tap **Save Offline**. Logcat shows `order … stored offline, sync deferred`; the app moves to
   **Orders** and the sale appears once as *Saved, pending*.
6. Kill and relaunch the app — the order is still there.
7. Switch to **Online**. Logcat shows `sync requested (changed to online)` followed by
   `order … failed: Server responded 503`; the card becomes *Retry needed* and explains the sale is
   safe locally.
8. Tap **Sync now**. Logcat shows `order … synced`; the same card becomes *Synced … after 2
   attempts*. No second entry appears.
9. Toggle Offline → Online again: `sync finished (changed to online): nothing pending` — nothing is
   re-sent and no duplicate is created.

Filter the log with `adb logcat -s System.out:I | grep POS`.

## 14. Known limitations

- **Pending orders do not sync automatically at startup.** The app initialises as Online, so the
  offline→online edge never fires on launch; a backlog waits for **Sync now**. The Orders screen
  shows the button enabled with "N order(s) ready to send". Adding an app-start trigger would be a
  small change, but the brief specified manual and reconnect triggers only.
- **The connection mode is not persisted** and resets to Online on relaunch — it simulates
  connectivity rather than observing it. There is no real network monitoring.
- **The backend is a `MockEngine` fake** running in-process. There is no server, no authentication.
- **The catalog is not cached**; if the fetch fails, the screen shows an error with a retry.
- **Sync retries only when triggered** — no backoff, no scheduler, no background work.
- **The per-order `SYNCING` chip appears only in previews.** An in-flight attempt is runtime state
  and is deliberately not persisted, so progress is shown app-wide (progress bar, button spinner).
- **No instrumented or screenshot tests**; UI states are covered by 22 `@Preview` composables and
  manual verification.
- Compose previews and large-font behaviour were checked in Android Studio and via preview
  parameters, not on a device font-scale sweep.

## 15. Reasonable future improvements

- Sync pending orders once at app start when online, and observe real connectivity instead of a
  manual switch.
- Bounded retry with backoff, and a "stuck order" state after repeated failures.
- Cache the catalog locally so the app is fully usable on a cold offline start.
- Persist the cart across process death.
- Receipt detail per order, with line items and a reprint action.
- Locale-aware currency and date formatting.
- Instrumented UI tests for the checkout and retry flows.
