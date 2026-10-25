# listentv

Listen to live TV channels as audio, with background playback and Android Auto support.

**Version 0.2.2 · Android prototype · GPL-3.0-or-later**

listentv is a native Android app that plays the audio from live television streams.
It disables video and subtitle tracks and exposes a media catalog for Android Auto.
The interface follows the device language: English by default, Italian on Italian devices.

> This is an early prototype. Channel playback and Android Auto integration still
> require testing on real devices. Source code support does not guarantee that a
> broadcaster's live service is currently accessible.

## Features

- Media3/ExoPlayer playback hosted in a `MediaLibraryService`.
- Background and screen-off playback, media notification and playback controls.
- Audio focus handling and pause when the audio output is disconnected.
- All channels and persistent favorites in the phone app and Android Auto catalog.
- Stream resolution at playback time rather than storing temporary source URLs.
- Custom direct HTTPS HLS, DASH and audio sources; long-press to remove them.
- English and Italian interface resources, including Android Auto folder labels.
- GitHub Actions build and Android lint checks.

## Channel integrations

| Channels | Implemented source procedure | Playback validation |
| --- | --- | --- |
| Rai 1, Rai 2, Rai 3, Rai News 24 | RaiPlay catalog and relinker; Widevine configuration when supplied | Pending |
| Rete 4, Canale 5, Italia 1, TGcom24, 20 Mediaset, Iris, La5, Mediaset Extra, Focus, Top Crime, Italia 2, Cine34, 27 Twentyseven, Boing, Cartoonito | Mediaset guest authorization, playback check, DASH selection and Widevine license configuration | API resolution checked; device playback pending |
| Sky TG24, Cielo, TV8 | Sky live API | Pending |
| TV2000 | Meride/Play2000 XML configuration | Pending |
| La7 | Live page, preauthorization and Widevine license configuration | Experimental; pending |
| Custom sources | Direct HTTPS URLs supported by Media3 | Depends on source |

Mediaset uses a fresh anonymous guest session for each playback request. No account
credentials are requested and tokens are not saved. Its standard playback check
controls access; protected streams use the license service with Android Widevine.
Account-only or subscription content is not included.

Provider procedures are adapted from WorldLiveTV; Kodi is not required.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for attribution.
There is no DRM or geographic restriction bypass. Availability depends on each
broadcaster, its authorization service and the device. Sources can change.

## Audio only and mobile data

Disabling video playback does **not** guarantee audio-only network traffic.
When a source provides a separate audio rendition, the player may use it. When
video and audio share the same segments, both may still need to be downloaded.
Data use must be measured per source. No cloud transcoding service is included.

## Download a test APK

1. Open this repository's [Actions tab](https://github.com/gianrolando/listentv/actions).
2. Select **Build Android APK** and open a successful run. Pushes to `main` build
   automatically; **Run workflow** also starts a manual build.
3. Under **Artifacts**, download **listentv-0.2.2-debug** and extract it.
4. Transfer `app-debug.apk` to an Android phone and allow installation from the
   app used to open the file if Android asks.

The artifact is available only after a successful build and is retained for
30 days. GitHub access is required while the repository is private.
The APK is debug-signed for testing; it is not a Google Play release. Debug keys
may change across runners, which can require uninstalling a previous test build.
Stable updates need a persistent signing key. Never commit signing keys or secrets.

## Build locally

Requirements: JDK 17, Android SDK platform 35, and Gradle 8.11.1.
Pinned dependencies include Android Gradle Plugin 8.9.2 and Media3 1.8.0.
There is no Gradle Wrapper yet; install the specified Gradle version or generate
one using your local Gradle installation.

```sh
git clone https://github.com/gianrolando/listentv.git
cd listentv
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Configure the Android SDK using `ANDROID_HOME` or a local `local.properties` file.
The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Minimum Android version: Android 6.0 (API 23).

## Use on a phone

Select a channel to start listening and use the star to add it to favorites.
**Pause** suspends playback, **Resume** continues the loaded stream, and **Stop**
stops the player. After a long pause, select the channel again to renew temporary
URLs or authorization. Custom sources must be direct HTTPS streams, not web pages.
Long-press a custom channel to remove it.

## Test with Android Auto

The app declares Android Auto media support and provides a browsable media
service. Actual compatibility has not yet been verified on a phone or head unit.

For a sideloaded media app, enable Android Auto developer settings by repeatedly
tapping its version entry, then enable **Unknown sources** in those settings.
Reconnect the phone and look for **listentv** in the launcher. Check launcher
customization if needed. Menus vary by Android Auto version and device.
Use a parked vehicle or Google's Desktop Head Unit for interactive testing.

Play Store distribution additionally requires release signing, a privacy policy
and compliance with Google's current car app review requirements.

## Known limitations and validation

No EPG, voice search, M3U import, recording, Mediaset account login, traffic meter or
custom long-outage reconnection logic is implemented. Normal player retries
are provided by ExoPlayer. Low-level provider diagnostics are in English.

See [VERIFICATION.md](VERIFICATION.md) for validation status and the device test
checklist. A successful APK build does not establish that individual channels or
Android Auto work on real devices.

## Development

Application ID and Java namespace: `it.gianrolando.listentv`.
Keep user-facing strings in `app/src/main/res/values/strings.xml` and provide
Italian translations in `app/src/main/res/values-it/strings.xml`.
Keep commits, documentation and issue descriptions in English.

## License and references

GPL-3.0-or-later. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Television content, channel
names and services belong to their respective owners. Code licensing does not
grant rights to television content. This project is not affiliated with
WorldLiveTV, Kodi, Google or the broadcasters.

- [WorldLiveTV](https://github.com/WorldLiveTV/WorldLiveTV.github.io)
- [Media3 media library service](https://developer.android.com/media/media3/session/serve-content)
- [Background playback](https://developer.android.com/media/media3/session/background-playback)
- [Track selection](https://developer.android.com/media/media3/exoplayer/track-selection)
- [Media apps for Android Auto](https://developer.android.com/training/cars/media/auto)
- [Car app testing](https://developer.android.com/training/cars/testing)

## Mediaset troubleshooting

Version 0.2.2 fixes a startup failure caused by reading the entire Mediaset
homepage, which exceeds the API response limit. Only its HTML head is now read
(up to 128 KiB), and the connection is closed before downloading the page body.
Other API responses retain their 2 MiB limit. Regression tests cover large page
bodies, split HTML head terminators and response limits.

If Mediaset resolution fails, the app shows a safe stage code:
M01 homepage metadata, M02 guest login, M03 playback authorization, M04 media
selector, M05 stream/DRM metadata. Tokens and source URLs are not included.
For player failures, report the displayed Media3 error code instead.

### Mediaset compatibility fixes in 0.2.2

The resolver now accepts Mediaset's `link-ott-prod.mediaset.net` selector as well
as the legacy thePlatform hosts. The new host was being rejected for most
channels, while Canale 5 and Italia 1 used a supported legacy host.

Encrypted channels explicitly prefer Widevine while preserving the provider's
quality and territory filters; PlayReady-only manifests are rejected before
creating an incompatible DRM session. TGcom24 retains its unencrypted source.
Media redirects (including the wrapper used by 20 Mediaset) are followed before
passing the final DASH URI and explicit MIME type to Media3. Resolution stage
M06 now covers manifest redirects and Widevine compatibility.

Use **Diagnostics → Copy diagnostics** after a failure. The report contains
app/Android/device versions, the most recent channel and error types/frames.
Java crashes are retained locally for the next launch; on Android 11+ the report
also includes the latest available system crash/ANR reason. Native crash traces
are not collected. Exception messages, stream URLs and tokens are excluded.
Nothing is sent automatically. Device playback and the reported 20 Mediaset
process crash still require confirmation on the affected phone.
