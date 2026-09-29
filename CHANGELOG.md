# Changelog

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
