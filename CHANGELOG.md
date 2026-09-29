# Changelog

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
