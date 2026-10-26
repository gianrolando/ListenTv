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

### Recorded release checks — 2026-10-26

- Source build commit: `e6a07e4e26beb4f369dd95e1438f160a951f64bd`.
- Private Actions run: https://github.com/gianrolando/ListenTv/actions/runs/40352952651.
- Release unit tests, release/debug compilation and release lint passed.
- Lint reported 0 errors and 9 warnings (dependency/SDK updates, crash-log persistence and media-service/backup declarations).
- APK identity: `it.gianrolando.listentv`, version 1.0.0 (6), minimum Android API 23; debugging disabled.
- Dedicated RSA-3072 release signature verified successfully with APK signature schemes v1, v2 and v3.
- Signing certificate SHA-256: `8f473cc34b872ae99d4e630ded22ec544a0c6440981c8774318908e8ab887795`.
- Signed APK SHA-256: `49bc47997bf9638c54fcdf3dce75aee3b9e0df00f64536dcc4a7301f5d58234f`.
- Offline license/credits assets verified in the signed APK.
- The later signing-script/documentation commit does not change the compiled app source.

## Expanded catalog — 1.1.0 (2026-10-26)

- Compared WorldLiveTV 1.2.14 discovery.py with its 2026-04-19 hotfix.
- Queried current RaiPlay live catalog: 15 entries, including Italiana and Rai 4K.
- All 15 Rai relinkers and manifests returned HTTP 200.
- Anonymous Discovery authorization and HLS manifests returned HTTP 200 for NOVE, Real Time, DMAX, Giallo, Food Network, HGTV, Discovery, Discovery Turbo and Warner TV.
- K2 and Frisbee guest authorization returned HTTP 200, but the supplied HLS and DASH manifests returned HTTP 504. This upstream limitation is documented rather than described as working playback.
- No live guest tokens or signed stream URLs are stored in the repository.
- Added regression coverage for Discovery authorization endpoint trust and unrecognized channel keys. Existing MediaSet/reader/crash-text tests remain unchanged.
- Runtime audio/DRM and Android Auto playback on physical hardware remain device checks; successful HTTP manifests alone do not validate them.
- The 1.0.0 private signing key is reused for the 1.1.0 APK.

### Recorded 1.1.0 release artifact

- Application source commit: `275a3242a055336601dcc2e2a132b6a4e78201b4`.
- Private Actions run: https://github.com/gianrolando/ListenTv/actions/runs/40378692540.
- Release unit tests, release/debug compilation and release lint passed.
- APK: ListenTv-1.1.0.apk, application ID `it.gianrolando.listentv`, version code 7, debugging disabled.
- APK SHA-256: `5293c2b59e13173632719898d3ac61cb40b06294fb66bbcf60367e0ff5c30dc2`.
- APK signature schemes v1, v2 and v3 verified. Certificate SHA-256 is unchanged from 1.0.0: `8f473cc34b872ae99d4e630ded22ec544a0c6440981c8774318908e8ab887795`.
- Offline GPL and component-credit assets verified in the final signed APK.
- A later verification-only commit does not change the compiled application source.
- Phone and tablet emulator capture job passed; reviewed color Rai logos, catalog count (46), and Discovery filter. Runtime capture contains no Java fatal exception.

## Catalog corrections — 1.2.0 (2026-10-26)

- Removed Rai Radio 2; retained stable IDs for all other existing channels.
- UI grouping is separate from playback-provider selection; TV8 uses the Sky
  resolver but appears under the requested Discovery filter.
- Added La7 Cinema, San Marino RTV and RTV Sport (48 entries total).
- Compared La7's live-la7cinema integration with the WorldLiveTV hotfix.
- La7 Cinema's live page, pre-authorization and DASH manifest returned HTTP 200.
- Both official San Marino player scripts, HLS masters and media playlists
  returned HTTP 200. URLs are resolved at runtime, not copied into the catalog.
- Added parser regression tests for signed-path preservation, escaped player
  source URLs, and rejection of insecure/credential-bearing sources.
- Uses the same stable release signing key; Android Auto hardware playback
  and Widevine license/decode remain physical-device checks.

### Recorded 1.2.0 release artifact

- Application source commit: `00fe38036233b588d4b3a8fef440c44731db1c9f`.
- Private Actions run: https://github.com/gianrolando/ListenTv/actions/runs/40387678722.
- Release unit tests, release/debug compilation and release lint passed.
- Signed APK version 1.2.0 (8), debugging disabled; APK SHA-256: `4ed796f3e5ea7a9ee32671a15c80cb6ba29a939180ac1cca11d0f016c9e4a003`.
- Signature schemes v1/v2/v3 verified; certificate is unchanged from stable 1.0.0 and 1.1.0 (`8f473cc34b872ae99d4e630ded22ec544a0c6440981c8774318908e8ab887795`).
- Offline GPL and component notices verified in the signed APK.
- A later documentation-only commit does not change compiled application source.
- Phone/tablet emulator preview passed; reviewed a 48-channel catalog with Rai Radio 2 removed and no Java fatal exception in the captured runtime log.
