# AGENTS.md

Single-module Android app (`:app`): Kotlin + Jetpack Compose, local-only Room DB, no network.
Package / applicationId: `com.github.altusea.thatday`. Remote: `github.com/altusea/thatday-android`.

## Source of truth

`DESIGN.md` is **gitignored (untracked) and stale**. Do not trust it for packages, versions,
or feature status — it names `com.szt.thatday`, older toolchain versions, and unimplemented
reminders. Trust `gradle/libs.versions.toml` and `app/build.gradle.kts`.

Actual baseline: AGP 9.4.1, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Gradle wrapper 9.6.0,
Compose BOM 2026.02.01, Room 2.8.5, minSdk 33 / compileSdk & targetSdk 37, Java 17.

## Commands

On Windows use `.\gradlew.bat` (paths below are Git Bash style). Config cache is on, so
repeated builds are fast.

- Build debug APK: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Single test class: `./gradlew :app:testDebugUnitTest --tests "com.github.altusea.thatday.time.RelativeTimeTest"`
- Lint: `./gradlew :app:lintDebug`
- Install / instrumented (need device or emulator): `./gradlew :app:installDebug`, `./gradlew :app:connectedDebugAndroidTest`

There is no CI, formatter, or static-analysis plugin configured.

## Build config quirks (AGP 9 — not the classic DSL)

- `compileSdk { version = release(37) }`, not `compileSdk = 37`.
- Release uses `buildTypes.release { optimization { enable = true; packageScope = ... } }`,
  not `isMinifyEnabled` / `proguardFiles`.
- R8 keep rules live in `app/src/main/keepRules/rules.keep` (AGP 9 location), not
  `proguard-rules.pro`. AGP combines every file in that directory.
- Gradle daemon runs on a **JVM toolchain 25** auto-provisioned by the foojay resolver
  (`gradle/gradle-daemon-jvm.properties`), independent of `JAVA_HOME` (locally JDK 27) and
  of `compileOptions` (Java 17). Don't bump the wrapper/toolchain casually.
- `android.disallowKotlinSourceSets=false` in `gradle.properties` is required for KSP; keep it.

## Architecture

- Manual DI via `di/ServiceLocator.kt` + `ThatDayApplication` (no Hilt/Koin). `MainActivity`
  pulls `repository` / `backupManager` from the Application.
- Navigation graph: `ui/ThatDayApp.kt` (`list` → `detail/{id}`, `edit?eventId={id}`; new id = `-1`).
- `EventRepository` is the **single write entry point** for events. Layering: UI → ViewModel →
  Repository. Keep DB and (future) alarm scheduling behind it.
- `time/` (`EventTimes`, `RelativeTime`, `RemindTime`) and `data/backup/` codecs are pure JVM,
  no Android deps — that is intentional so they stay unit-testable.

## Room / schema (easy to break)

- Schema is exported and committed at `app/schemas/com.github.altusea.thatday.data.ThatDayDb/`.
  Changing `Event` or `EventDao` requires bumping `@Database(version = ...)` and committing the
  regenerated schema.
- No migration exists and `fallbackToDestructiveMigration()` is **not** set. A version bump
  without a `Migration` will crash at runtime; add a migration.

## Reminders are NOT implemented

`remindAt` is stored, `RemindTime.compute(...)` is implemented and tested, and
`EventDao.getWithFutureReminder()` / `EventRepository.eventsWithFutureReminder()` exist — but
there is no `AlarmManager`, receiver, notification, or boot-rescheduling code, and
`AndroidManifest.xml` declares **zero permissions**. The edit screen's reminder switch only
persists `remindAt`; nothing fires yet. Don't assume end-to-end reminders work.

## Backup format

SAF-based JSON (`BackupManager`), no storage/INTERNET permission. Envelope has `APP_ID="thatday"`
and `FORMAT_VERSION=1` (`data/backup/EventBackup.kt`). Export omits DB ids; import is
non-destructive and idempotent — duplicates are detected by title + occurredAt + timeKind + note.
Adding `Event` fields means updating `EventDto`/codec and considering `FORMAT_VERSION`.

## i18n

Nine locales: default `en` plus `ar`, `es`, `fr`, `ja`, `ko`, `ru`, `zh-Hans`, `zh-Hant`
(see `res/xml/locales_config.xml`). Add every new string/plural to **all** `values-*/strings.xml`;
`app_name` is `translatable="false"`. Relative-time labels are plurals resolved in
`ui/RelativeText.kt`.

## UI constraints (enforced by platform, not style)

- Edge-to-edge is mandatory on target 37; `enableEdgeToEdge()` is called. Scrollable content must
  consume insets itself.
- Never implement `onBackPressed()` (dead on target 33+/Android 13+ predictive back). Use
  `BackHandler`, as the edit screen does for unsaved-change confirmation.
- Dynamic color is always on (minSdk 33), so brand colors are unused except static icon/splash.
- Content is width-capped at 640dp on large screens; no list-detail two-pane layout.
