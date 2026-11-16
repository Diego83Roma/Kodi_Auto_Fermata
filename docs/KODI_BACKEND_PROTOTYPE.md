# Kodi Backend prototype

## Scope of this first increment

This branch adds the first isolated transport component:

- `me.aap.fermata.kodi.KodiJsonRpcClient`
- loopback-only endpoint: `127.0.0.1:8080/jsonrpc`
- optional HTTP Basic authentication
- bounded connection/read timeouts and response size
- JSON-RPC helpers for API version, movies, TV shows, episodes, addons, and `Files.PrepareDownload`

All methods are blocking and explicitly reject calls from Android's main thread. Call them only from a worker thread. Credentials are held in memory by the client and must never be written to logs.

## Existing Fermata integration points

The current source already provides the integration seam:

- `DefaultMediaLib.getChildren("Root")` appends roots supplied by registered `MediaLibAddon` implementations.
- `DefaultMediaLib.getItem()` delegates unknown item IDs and schemes to `AddonManager`.
- `MediaLibAddon` defines `getRootItem`, `getItem`, and item ownership.
- `MediaLib.PlayableItem.getLocation()` resolves network resources through Fermata's VFS manager.

The next increment will implement the Kodi source as a `MediaLibAddon`, then add Kodi-backed browsable and playable items. The MediaBrowserService, CarService, ProjectionService, and existing media engines remain unchanged.

## Network configuration already present

The base manifest already declares `android.permission.INTERNET` and `android:usesCleartextTraffic="true"`. Therefore this prototype does not add a second network-security configuration or broaden cleartext access. The first connection test must still verify that Kodi's HTTP server accepts loopback requests from another app process on this device.

## Playback is deliberately not assumed

`Files.PrepareDownload` returns Kodi transport details, not a guaranteed Fermata-ready media URL. The playback increment must separately test Kodi's `/vfs/` endpoint, authentication handling, byte-range requests, seeking, and the active Fermata media engine.

## Addons

The transport exposes `Addons.GetAddons`. Addon invocation and stream resolution are a later compatibility layer: a Kodi `plugin://` path is not itself a stream URL and cannot be passed blindly to ExoPlayer or VLC.
