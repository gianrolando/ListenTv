# Validation status

## Initial import — 2026-10-25

This repository starts from the TV Ascolto 0.1.0 source prototype, renamed to
`listentv` with the Java namespace and application ID `it.gianrolando.listentv`.
English is the default interface language; Italian translations are included.

Local checks: XML parsing, matching translation keys, Java package references,
resource references and repository contents. No Android SDK or usable JDK is
available locally, so compilation and Android lint are delegated to Actions.
The Actions run is the authoritative build result; it is not a device test.

The original prototype reported review of provider procedures and Media3
callback signatures. Those checks are inherited provenance, not new playback tests.

## Device tests still required

- Install and launch on API 23 and a recent Android version, in English and Italian.
- Play every broadcaster, including protected and unavailable streams.
- Listen with the screen off for at least 20 minutes and test notification controls.
- Test calls, navigation audio, audio focus and audio output disconnection.
- Browse and play from Android Auto without first opening the phone activity.
- Add/remove favorites and custom sources; test catalog updates and pagination.
- Test network loss, Wi-Fi/mobile changes, expired URLs and long pauses.
- Measure bandwidth for separate audio renditions and multiplexed streams.

Run interactive tests while parked or with Desktop Head Unit.

## Mediaset integration — 0.2.0

- Added 15 free-to-air Mediaset channel entries with stable catalog IDs.
- Checked the official station catalog and anonymous login response.
- Checked playback authorization, SMIL resolution and manifest HTTP availability
  for each listed Mediaset channel. This does not test decrypted audio playback.
- Selected DASH rather than FairPlay HLS and configured standard Widevine licensing.
- Account-only streams, subscription content and geographic restriction bypass
  are not implemented.
- Compilation and Android lint are checked by the commit's GitHub Actions run.
- Widevine license exchange and actual playback still require an Android device.

## Mediaset startup fix — 0.2.1

The homepage retrieved with the app user agent was approximately 6.7 MB, beyond
the previous 2 MiB response limit. The application metadata appeared near byte
3422 and the HTML head ended near byte 6931. The resolver now reads only the
head (128 KiB maximum) and keeps the existing limit for complete API responses.
Five JVM regression tests exercise the production bounded reader. The Actions
workflow runs these tests, APK compilation and Android lint. Device playback
and Widevine license exchange are still not validated by these checks.

## Mediaset selector/DRM fixes — 0.2.2

User testing of 0.2.1 found only Canale 5 and Italia 1 working and a process
crash on 20 Mediaset. Live API inspection reproduced a matching routing split:
12 failing channels used a selector host rejected by the app; the two working
channels used an accepted legacy host. 20 Mediaset used a legacy selector and
an additional CDN redirect. The source of the process crash is not established.

For all 15 channels, corrected requests returned HTTP 200 manifests; 14
protected channels advertised Widevine without PlayReady, and TGcom24 was
unencrypted. These checks do not exercise licenses, decoding or Android Auto.
Regression tests cover both selector generations, untrusted host rejection,
preservation of territory/quality filters, DRM compatibility checks and token
omission in diagnostic text. All prior bounded-read tests remain enabled.

The new local diagnostic report is intended to distinguish a remaining source
failure from a device player/DRM error or process crash. No native crash fix is
claimed until the affected device can be tested.

## Interface refresh — 0.3.0

Bundled 24 logos from official sources. Updated the app display name to
ListenTv, with a native dark UI, responsive grid, search/filter controls and
persistent playback panel. Added channel artwork to media metadata.

Stream-resolution code was not changed by this UI update. Existing regression
tests, compilation and lint remain in Actions. A separate emulator job captures
phone/tablet screenshots for visual review. Hardware audio/DRM playback and
Android Auto remain dependent on device validation.

## Stable release — 1.0.0

The owner confirmed that the app works. Streaming resolvers are unchanged.
Mediaset logos now use official MFE 560x560 originals, with transparent padding
cropped. Media artwork is composed at 512x512 rather than 256x256.
The diagnostics menu is removed; About includes offline copyright, license and
component notices. Release builds disable debugging and keep code shrinking
off to avoid introducing playback regressions.

CI runs release unit tests, release APK compilation and lint, plus emulator
phone/tablet screenshots from a separate UI test build. The unsigned release
is signed locally using a dedicated private PKCS12 key kept outside GitHub.
The final APK must pass apksigner verification after signing.
Repository visibility remains private; no public publication is authorized.
