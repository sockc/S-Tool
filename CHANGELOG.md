# Changelog

## 1.4.3.1

### Critical Gaode splash fix
- Restore Gaode's original `SplashScreenServiceImpl` launch/callback methods instead of short-circuiting them.
- Stop forcing splash-state query methods to false.
- Stop suppressing splash AJX linkage getters during startup.
- Keep the 17.x semantic `BootBizDataPreloaderImpl.canShowSplash() = false` gate.
- Add 16.x exact-signature compatibility for `u96.g(int,String)` and `za6.g(int,String)`: call the original method first, then set the returned finish-reason field to NO_SPLASH when compatible.
- Observe `com.autonavi.minimap.g.e(...)` as the real splash-finish signal without altering it.
- Preserve Gaode R3 AJX UI cleanup and structural bottom-tab handling.

### Version
- Bump app version to `1.4.3.1` / versionCode `1431`.

## 1.4.3

### Gaode R3
- Replace text-only cleaning as the primary strategy with semantic Gaode business hooks.
- Force `BootBizDataPreloaderImpl.canShowSplash()` to false so Gaode follows its own no-splash path.
- Suppress realtime splash fetch, splash mask display, banner management/parsing, background operation messages, splash linkage data and search splash template sources.
- Add AJX text-anchor handling for Explore Local and operation badges.
- Discover and hook the actual AJX list adapter `onBindViewHolder` after a target item is observed, so future bound items are handled at item level rather than by whole-page polling.
- Add structural bottom-tab row recognition and equal-width redistribution after hiding Explore / AI Chat / Route.
- Keep V1.4.2 R2 fingerprints and text scanning as compatibility fallbacks.

### Compatibility
- Designed around publicly documented and independently reimplemented hook points verified for Gaode 16.23 / 17.00.
- No source files from MapAdKiller are vendored into S Tool.

### Version
- Bump app version to `1.4.3` / versionCode `143`.

## 1.4.2

### OTP auto-copy V2
- Add NotificationListenerService as the primary OTP capture channel.
- Keep Google Messages SMS broadcast / provider observation as a fallback channel.
- Share duplicate suppression across notification and SMS paths.
- Expand contextual OTP recognition while keeping loose arbitrary-number matching disabled.
- Add a manager status/action for Notification Listener access.

### Gaode R2
- Add targeted UI fingerprint diagnostics: Activity, View class, resource name, and parent chain.
- Add resource/class identity fallbacks for Explore Local, bottom tabs and operation badges.
- Keep text scanning as fallback rather than the only matching mechanism.
- Deduplicate diagnostic fingerprints to avoid log flooding.

### OPlus / ColorOS Game Assistant R2
- Remove the late second Application.attach Hook that could miss the current attach event.
- Try MMKV Hook directly from the package class loader.
- Retry from Application.onCreate only when MMKV is not ready yet.
- Add MMKV key-name diagnostics without logging stored values.
- Recognize com.oplus.games, com.coloros.gamespaceui and com.coloros.gamespace.

### Scope diagnostics
- Show dedicated-app LSPosed Scope state in app enhancement cards.
- Allow tapping missing-Scope status to issue a native LSPosed Scope Request.
- Refresh Scope snapshot on manager resume.

### Stability
- Remove Airvoy's late Application.attach Hook and track target Activity lifecycle directly.
- Preserve per-Hook failure isolation from previous releases.

### Version
- Bump app version to `1.4.2` / versionCode `142`.

## 1.4.0

### Universal privacy stage 2
- Add Universal Clipboard Privacy.
- Add Universal Device Identifier Privacy.
- Add Universal File / Media Privacy.
- Add Universal Installed-App List Privacy.
- Keep Location Privacy and Screenshot Privacy.
- All six capabilities are independently enabled and have independent arbitrary-app selections.

### Privacy engines
- Clipboard: hide primary clip state, clip data, descriptions, and legacy text reads.
- Device ID: protect common Telephony identifiers, Android ID, and Build serial.
- File / Media: hide common DCIM, Pictures, Download, Movies, and Screenshots directory listings through File APIs.
- App List: filter bulk installed-application and installed-package enumeration to the caller and Android core package.

### Architecture
- Remove the old standalone global ClipboardHook dispatch from SMainHook.
- UniversalPrivacyHook becomes the single dispatcher for the six reusable privacy engines.
- Decouple universal capability effectiveness from the old parent switch.
- Keep UniversalPrivacyHook available in all LSPosed-scoped target processes; each engine activates only when its capability and per-app selection are enabled.
- Keep WebView core packages excluded from universal privacy injection.

### Migration and compatibility
- Preserve V1.3.x Location / Screenshot effective state.
- New Device ID, File / Media, and App List capabilities default off.
- Explicitly enabled legacy Clipboard preference migrates known S Tool target apps to the new per-app Clipboard protection.
- Unicom location/screenshot, Meituan file/device-id, and Pinduoduo file privacy yield to matching universal engines to avoid duplicate hooks.
- ConfigProvider protocol bumped to v6.

### UI
- Add six compact Universal Protection menu cards.
- Reuse the searchable arbitrary-app selector for every privacy capability.
- Universal module count reflects whether at least one privacy capability is enabled.

### Version
- Bump app version to `1.4.0` / versionCode `140`.

## 1.3.4

### Universal protection menu
- Replace the large inline Universal Privacy card with two compact menu entries: Location Privacy and Screenshot Privacy.
- Show enabled/disabled state and selected-app count directly on each menu card.
- Move app selection to a dedicated screen.

### Arbitrary app selector
- Scan installed applications and allow privacy protection for apps without a dedicated S Tool enhancement module.
- Add search by app label or package name.
- Hide system apps by default with an optional “显示系统应用” switch.
- Add “全选当前” and “清空” actions.
- Show per-app LSPosed injection history inside the selector.
- Permanently exclude Android core, SystemUI, Settings, and S Tool itself.
- Add `QUERY_ALL_PACKAGES` for complete installed-app discovery in the current sideload/self-use workflow.

### Dynamic configuration
- Universal privacy app keys are now dynamic instead of limited to the registered enhancement-app list.
- ConfigProvider publishes all saved dynamic app privacy keys.
- Preserve V1.3.3 selections during upgrade.
- Reset Defaults clears dynamic app selections.
- ConfigProvider protocol bumped to v5.

### Version
- Bump app version to `1.3.4` / versionCode `134`.

## 1.3.3

### App-scoped universal privacy
- Add per-app selectors for Universal Location Privacy and Universal Screenshot Privacy.
- Migrate the previous global Universal Privacy state to the registered target apps to preserve V1.3.2 behavior.
- Keep new/reset app selections disabled by default when Universal Privacy was not previously enabled.

### Hook dispatch
- Route app-specific Hook modules by package before class instantiation.
- Keep only global features such as Universal Privacy and Clipboard Privacy on the global dispatch path.
- Reduce unrelated class initialization and cross-app side effects.

### Diagnostics
- Replace misleading "injected N / total" wording with a historical injection record count.
- Show "尚无注入记录" for apps that have not been started since diagnostics were enabled.
- Add a Clear Injection Records action.
- Persist and display isolated top-level Hook initialization/execution failures per app/feature.
- Clear a previous failure automatically after that Hook executes successfully again.
- ConfigProvider protocol bumped to v4.

### Version
- Bump app version to `1.3.3` / versionCode `133`.

## 1.3.2

### Critical fix
- Fix `AirvoyHook.<clinit>` crash caused by constructing a `Handler` during LSPosed/Zygote class initialization before a thread Looper existed.
- Create the Airvoy main-thread Handler only when a real Activity exists.
- Change `SMainHook` from eager Hook object construction to lazy per-Hook class instantiation.
- Isolate Hook class initialization and execution failures so one broken Hook can no longer prevent the S Tool entry class from loading.
- Preserve V1.3.1 Application.attach dispatch and real injection diagnostics.

### Version
- Bump app version to `1.3.2` / versionCode `132`.

## 1.3.1

### Injection entry fix
- Move feature dispatch from the earliest `handleLoadPackage()` stage to `Application.attach(Context)`.
- Read ConfigProvider settings with the hooked target app's real Context instead of an early system Context.
- Cache that target-process configuration so feature Hooks that reload settings reuse the verified snapshot.
- Keep immediate default-config dispatch as a fallback if the Application.attach hook itself cannot be installed.

### Real injection diagnostics
- Add a validated `report_injection` ConfigProvider call.
- Verify the reported package belongs to the Binder caller UID before recording it.
- Persist last injection timestamps separately from feature configuration.
- Show real injection coverage and per-target last injection time in the manager.
- Refresh diagnostics whenever the manager returns to the foreground.
- ConfigProvider protocol bumped to v3.

### Version
- Bump app version to `1.3.1` / versionCode `131`.

## 1.3.0

### Universal privacy
- Add a new top-level Universal Privacy module, disabled by default.
- Add reusable `LocationPrivacyEngine`.
- Protect Android `LocationManager` last/current/continuous/single location entry points.
- Add reusable `ScreenshotPrivacyEngine`.
- Block Android 14+ screen-capture callback registration.
- Block common MediaStore screenshot/image observers.
- Skip Android core, SystemUI, Settings and S Tool itself.
- Apply universal privacy only to apps/processes already selected in LSPosed scope.

### Compatibility
- Reuse the screenshot engine for the existing China Unicom screenshot privacy feature.
- Avoid duplicate China Unicom screenshot/location hooks when the corresponding universal protection is active.
- Add per-feature default state support so new broad protections can safely default off.
- ConfigProvider protocol bumped to v2.

### UI
- Split the manager into Universal Protection and App Enhancements sections.
- Restore Defaults now respects each feature's declared default state.

### Version
- Bump app version to `1.3.0` / versionCode `130`.

## 1.2.2

### Cross-process configuration
- Replace New XSharedPreferences as the primary configuration transport with a read-only exported ConfigProvider.
- Store module settings in private SharedPreferences.
- Read Hook configuration through Binder from target app processes.
- Keep legacy XSharedPreferences as a fallback for older environments.
- Remove the `xposedsharedprefs` declaration and lower legacy minimum API metadata to 82.
- Update the manager status to report ConfigProvider bridge availability.

### Signing
- Allow Debug APKs to use the same fixed signing key as Release APKs when GitHub Secrets are configured.
- Warn clearly in CI when stable signing secrets are missing and the temporary runner debug certificate is used.
- Bump app version to `1.2.2` / versionCode `122`.

## 1.2.1

### Sub-feature controls
- Add expandable child controls under selected app modules.
- China Unicom: cold-start ad, warm-start ad, permission privacy, location privacy, screenshot privacy.
- Amap/Gaode: ad skip, Explore Local section, bottom tabs, floating campaign badges.
- Taobao: cold-start ad and warm-start ad controls.
- Meituan: file privacy, splash ad, permission privacy, device identifier privacy.
- Pinduoduo: file privacy, permission privacy, splash ad.
- Parent switches disable the whole module while preserving child preference states.
- Move the legacy standalone Unicom screenshot privacy switch under China Unicom and migrate its saved value.

### Stability
- Bound Taobao, Meituan and Pinduoduo temporary UI scanners with 5-second cleanup timeouts.
- Do not register disabled child hooks in target app processes.

### Version
- Bump app version to `1.2.1` / versionCode `121`.

## 1.2.0

### Management
- Add a launcher management panel for S Tool.
- Add independent enable/disable switches for all 19 Hook modules.
- Add enable-all, disable-all and reset-default actions.
- Show target app installation/version information.
- Show enabled-module count and cross-process preference status.
- Show last configuration change time.

### Hook configuration
- Add LSPosed XSharedPreferences based cross-process configuration.
- Explicitly declare `xposedsharedprefs`.
- Keep all Hook modules enabled by default if shared configuration is unavailable.
- Apply feature switches during target process startup.

### Version
- Bump app version to `1.2.0` / versionCode `120`.

## 1.1.0

### Security
- Remove committed APK signing material from the active branch.
- Remove hard-coded keystore passwords from Gradle configuration.
- Prepare GitHub Secrets based release signing.

### Architecture
- Add a single Xposed entry point: `SMainHook`.
- Keep individual Hook modules isolated behind the main dispatcher.

### Stability & Privacy
- Stop Unicom and Firsty global-layout scanners after success or timeout.
- Cancel pending Airvoy close attempts when a close session finishes or its Activity changes.
- Redact SMS sender/body/code data from Xposed logs.
- Do not mark copied OTP messages as read by default.
- Disable loose "any 4-8 digit number" OTP matching by default.

### CI
- Build debug APKs on pushes and pull requests.
- Build signed release APKs for `v*` tags.
- Generate SHA256 checksums and publish GitHub Releases.
