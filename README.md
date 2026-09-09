# Loki VPN

A production-ready Android VPN architecture supporting V2Ray/Xray-based VPN configurations.

## Architecture

This project is structured into:
- `ui`: Jetpack Compose UI (MVVM)
- `data`: Room Database for caching, Retrofit for remote fetching
- `vpn`: `VpnService` implementation and `VpnEngine` interface

## V2Ray / Xray Core Integration

This project provides the **architectural skeleton** for Xray integration. Because Xray is written in Go, it must be compiled into a native library (`.so`) using `gomobile` or a similar toolchain to be used in Android. 

### How to integrate the native library:
1. Compile the Xray core for Android (e.g., using `libv2ray` or `xray-core`).
2. Place the resulting `libxray.so` inside `app/src/main/jniLibs/arm64-v8a/` (and other ABIs).
3. Update `LokiVpnEngine.kt` to load the library: `System.loadLibrary("xray")`.
4. Replace the placeholder start/stop methods with actual JNI calls.

## Remote Configuration & Premium System

Configuration is fetched via GitHub. 
To update configurations:
1. Edit `servers.json` and push it to your GitHub repository.
2. Edit `premium.txt` to add or remove authorized HWIDs.
3. Update `configUrl` and `premiumUrl` in `RemoteConfigRepository.kt` to point to your raw GitHub file URLs.

> **Security Limitation**: The client-side HWID premium check against a public text file is convenient for a quick release, but it is NOT highly secure. An attacker can repackage the APK or mock the HTTP response. For production, consider moving this check to a secure backend API with user authentication.

## Build Instructions

- **Android Studio**: Ladybug or newer
- **JDK**: 17
- **Min SDK**: 24
- **Target SDK**: 36
- **Gradle**: 8.x

### Build Debug APK
`./gradlew assembleDebug`

### Build Release APK
`./gradlew assembleRelease`
Set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` in your environment or `local.properties` (not recommended for CI).

### Testing
Use `testDebugUnitTest` for local JVM tests. 
For VLESS/VMess protocols, test by dropping the actual valid URIs into `servers.json` and verifying connection via logcat or a packet capture tool.
