# ListenTv

Listen to live television as audio, with background playback and Android Auto.

**Stable version 1.0.0 · Android 6.0+ · GPL-3.0-or-later**

Copyright © 2026 Gianrolando Scaringi.

## Features

- Audio-only Media3 playback, including screen-off listening and media controls.
- Native dark interface, channel logos, search and broadcaster/favorites filters.
- Android Auto media catalog with persistent favorites and 512-pixel artwork.
- Rai 1, Rai 2, Rai 3, Rai News 24, 15 free-to-air Mediaset channels, Sky TG24,
  Cielo, TV8, La7 and TV2000 integrations.
- Custom direct HTTPS HLS, DASH and audio sources.
- English and Italian interface, selected from the device language.
- In-app copyright, GPL license text and third-party credits, available offline.

Mediaset logos use the full-color, high-resolution originals from the official
MFE/Mediaset corporate gallery rather than the old 100-pixel thumbnails.
All channel logos are bundled, so displaying them does not require a network
request. Logos and television content belong to their respective owners.

## Install the stable app

Install the signed `ListenTv-1.0.0.apk` from the private release package supplied
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
