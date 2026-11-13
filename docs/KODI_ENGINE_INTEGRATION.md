# Kodi engine integration — technical boundary

This branch keeps the Fermata application and the Kodi engine in separate processes. The current Java service only creates private staging directories and copies an imported add-on; it is deliberately not presented as an executable Kodi runtime.

## Integration stages

1. **Build the real upstream Android core.** The workflow `build-kodi-core-android.yml` follows Kodi's Android unified-depends and CMake build, using ARM64 and the NDK revision currently recommended by Kodi's Android build guide. It produces an upstream reference APK and extracts its packaged native libraries and assets for inspection.
2. **Integrate native libraries and runtime assets.** Before packaging anything into Fermata, inspect the upstream APK's ABI libraries, asset layout, Python runtime, linker dependencies, JNI entry points, and license notices. Do not copy only a guessed `libkodi.so`: Kodi's Android application bootstraps paths and environment variables before entering the native core.
3. **Provide Kodi's Python add-on API.** The actual Kodi build must supply the matching Python runtime and Kodi modules (including xbmc, xbmcaddon, xbmcgui, xbmcplugin, and xbmcvfs). A system Python interpreter or empty Java stubs are not compatible substitutes.
4. **Connect the add-on UI to the isolated runtime.** Implement a defined IPC contract between the Fermata UI and the engine process, covering start/stop, add-on identity, logs, lifecycle, permissions, and result/error reporting. Imported add-ons remain in Fermata's private storage. Never dispatch to the external Kodi package.

## Critical engineering constraint

Kodi's Android port is a complete native application with its own Android packaging, native activity/JNI bootstrap, assets, and runtime environment. Building its APK successfully does **not** by itself make the core loadable inside Fermata's Java Service. The extracted payload is an analysis/integration input, not a claim that add-ons already execute. The next implementation must identify and adapt Kodi's supported native entry point (or refactor the Android bootstrap into an embeddable library) before the UI can launch it.

## Safety and regression rules

- Work only on `kodi-auto-fermata-embedded-engine`.
- Keep Build 49 and the known-good UI/build branches untouched.
- Do not launch `org.xbmc.kodi` or require the original Kodi app.
- Do not mark add-ons executable until a real Python add-on has been launched and its logs/results verified on-device.
