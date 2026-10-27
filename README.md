# ListenTv

Listen to live television as audio, with background playback and Android Auto.

**Stable version 1.3.2 · Android 6.0+ · GPL-3.0-or-later**

Copyright © 2026 Gianrolando Scaringi.

## Features

- Audio-only Media3 playback, including screen-off listening and media controls.
- Native dark interface, channel logos, search and broadcaster/favorites filters.
- Android Auto media catalog with persistent favorites and 512-pixel artwork.
- 14 Rai television channels (Rai Radio 2 visual radio excluded), 15 free-to-air Mediaset
  channels, 11 Warner Bros. Discovery channels, Sky TG24, Cielo, TV8, La7,
  La7 Cinema, San Marino RTV, San Marino RTV Sport and TV2000.
- Custom direct HTTPS HLS, DASH and audio sources.
- English and Italian interface, selected from the device language.
- In-app copyright, GPL license text and third-party credits, available offline.

Mediaset logos use the full-color, high-resolution originals from the official
MFE/Mediaset corporate gallery rather than the old 100-pixel thumbnails.
All channel logos are bundled, so displaying them does not require a network
request. Logos and television content belong to their respective owners.

## On-air programme guide — 1.3.2

Channel cards show the current programme, start/end times and a progress bar.
Search also matches current programme titles. Media metadata carries the
programme into Android Auto and notifications while keeping channel names.

The guide downloads in the background and caches only relevant titles/times
for up to 48 hours. It refreshes at most every six hours, with a 30-minute
retry interval on failure; on-air selection updates as the clock advances.
Cached entries remain usable offline until their actual end time. Missing or
expired information shows **Programme guide unavailable** instead of an old
programme. Custom stream URLs have no automatic guide mapping.

Source: epg-guide.com's XMLTV feed, as configured by WorldLiveTV, with official
RaiPlay fallbacks for Italiana and Rai 4K. Some source entries contain no
programme information (notably Sky TG24, Warner TV and RTV Sport at inspection).
Programme schedules may differ from live broadcast changes.

Guide downloads are size-limited, XZ decoder memory is capped, document types
and external entities are rejected, and feed redirects are allowlisted.
The public guide's delivery redirects to HTTP on 116.202.210.205; Android 7+
allows cleartext only for that exact host. Android 6 uses a manifest exception
and the same explicit URL checks in code. Stream and authorization resolution
still requires HTTPS. No broadcaster tokens are sent to the guide service.

## Catalog changes — 1.2.0

Rai Radio 2 has been removed as requested. La7 Cinema, San Marino RTV and
San Marino RTV Sport have been added under **More** (Italian: **Altri**).
TV8 is shown in the **Discovery** filter as a user-selected grouping; its
actual broadcaster attribution and Sky playback resolver are preserved.
There are 48 television entries: 14 Rai, 15 Mediaset, 12 in the Discovery
filter (including TV8), and 7 under More.

La7 Cinema uses its official live page and standard Widevine authorization.
Both San Marino channels resolve fresh, signed HLS URLs from the broadcaster's
official ACD player each time playback starts. No expiring stream URL is bundled.

## Expanded channel catalog — 1.1.0

Rai: Rai 1, Rai 2, Rai 3, Rai News 24, Rai Sport,
Rai 4K, Rai 4, Rai 5, Rai Movie, Rai Premium, Rai Storia, Italiana, Rai Yoyo and
Rai Gulp. The 1.1.0 release also included Rai Radio 2 visual radio, removed in
1.2.0. Names follow the current official live catalog (including Italiana).
Rai marks use color vector originals rasterized at high resolution; the new
Italiana mark uses the official RaiPlay asset and its channel background color.

Discovery: NOVE, Real Time, DMAX, Giallo, Food Network, HGTV, Discovery,
Discovery Turbo, Warner TV, K2 and Frisbee. Discovery guest playback is adapted
from WorldLiveTV 1.2.14 and its April 2026 hotfix. Current Aurora configurations
supply channel IDs and endpoints; HLS-only sources are supported. No paid
channels, account credentials or subscription bypass is included.

At verification on 2026-10-26 all 15 Rai manifests and 9 Discovery manifests
responded successfully. K2 and Frisbee authorized successfully but their CDN
returned HTTP 504 for both HLS and DASH; they remain in the catalog for retry
when the broadcaster restores service. This is not a decrypted hardware audio
or Android Auto playback test.

Updates from stable 1.0.0 retain favorites and custom sources because the same
private release signing key is used.

## Install the stable app

Install the signed `ListenTv-1.3.2.apk` from the private release package supplied
by the project owner. The stable release is signed with a dedicated release key,
and is not a debug build. Keep using the same release key for subsequent updates.

Earlier 0.x development APKs have a different debug signature. Android requires
uninstalling that development app before installing 1.0.0; that clears local
favorites and custom sources. Later releases signed with the same release key
can update the stable app normally.

The repository remains private. No public release or Play Store publication is
part of this delivery. GitHub Actions' **unsigned release build** is a developer
input to signing and cannot be installed directly.

## Use

Tap a channel card to listen. The player remains at the bottom of the screen.
Use the star for favorites, search by channel/broadcaster and use the filter
chips to narrow the catalog. The top-right menu offers custom sources,
**About ListenTv** and audio playback information. About includes copyright,
GPL license text and component credits. The diagnostics menu has been removed.
Long-press a custom source to remove it.

## Android Auto

The app exposes a browsable media library and media artwork. For a manually
installed media app, enable Android Auto developer settings and **Unknown
sources**, then reconnect and check launcher customization. Test while parked.
Android Auto chooses its own layout and may scale or cache channel artwork.

## Streaming and data usage

The player disables video and subtitle tracks. Data usage depends on each
source: separate audio renditions can save data, while multiplexed streams can
still carry video bytes. No conversion server or cloud transcoder is used.
Mediaset uses guest authorization and standard Widevine licenses; passwords,
subscription access and geographic restriction bypass are not implemented.
Broadcasters can change their endpoints or availability.

## Build and sign

Use JDK 17, Android SDK 35 and Gradle 8.11.1. Android Gradle Plugin 8.9.2 and
Media3 1.8.0 remain pinned. This project does not yet include a Gradle Wrapper.

```sh
gradle :app:testReleaseUnitTest :app:assembleRelease :app:lintRelease
```

The release APK is unsigned at `app/build/outputs/apk/release/app-release-unsigned.apk`.
Sign it locally using `scripts/sign-release.sh`, Android SDK's `apksigner.jar`
and the owner's private PKCS12 release keystore. Follow the script's usage.
The signing key and passwords must never be committed or uploaded to this repo.
No signing credentials are stored in GitHub Actions or its artifacts.

Actions runs regression tests, compiles the release and captures the UI on a
phone/tablet emulator using a separate debug-only UI test APK. The signing
runtime/tools are not included in the app. See VERIFICATION.md for validation.

## License and credits

GNU General Public License, version 3 or any later version (GPL-3.0-or-later).
See LICENSE for the full terms and disclaimer of warranty. This choice preserves
the license of the adapted WorldLiveTV provider procedures by WLTV Group.

AndroidX Media3 and Guava are Apache-2.0 components; other dependencies retain
their licenses. See THIRD_PARTY_NOTICES.md and CHANNEL_LOGOS.json. Broadcaster
marks are not covered by the GPL and their use does not imply endorsement.

Application ID: `it.gianrolando.listentv`.
