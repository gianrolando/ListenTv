# ListenTv 1.4.1 — Mediaset playback resilience

- Enable Widevine multi-session support for protected Mediaset live channels, allowing fresh license requests when keys change.
- Recover from Media3's behind-live-window error by resuming at the default live position.
- Keep DRM-free TGcom24 playback and other broadcasters' DRM configuration unchanged.
- Add offline regression coverage for Mediaset DRM setup, source types and license transport validation.
- Preserve the 51-channel catalog, favorites and release signing identity.

This update addresses missing handling found while investigating Mediaset playback stopping after a few seconds. The exact failure on the reporting device has not been reproduced; prolonged Widevine playback must still be checked on that device.

Broadcaster geographic and programme restrictions still apply.
