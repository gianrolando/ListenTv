# ListenTv 1.3.3 — La7 live player migration

- Restore La7 and La7 Cinema resolution after the official site moved to a Next.js player.
- Read current channel-specific DASH, backup, token and Widevine settings from public page data.
- Request fresh guest authorization with the correct content ID and LIVE content type.
- Restrict token/license endpoints to the verified broadcaster hosts; retain normal TLS validation.
- Use the official backup manifest if the primary manifest cannot be retrieved.
- Preserve the existing 48-channel catalog and TV8 immediately before NOVE in Discovery.

The source manifests and token responses were checked for both channels. Widevine playback
still depends on the device, broadcaster entitlement and geographic/content restrictions.
