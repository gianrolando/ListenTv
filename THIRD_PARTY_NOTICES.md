# Attribution and source provenance

The provider-specific procedures in `StreamResolver.java` and `MediasetResolver.java` are adapted from
WorldLiveTV, by WLTV Group, version 1.2.14 hotfix, licensed GPL-3.0-or-later.
The original authors retain all rights in their work.

Upstream repository: https://github.com/WorldLiveTV/WorldLiveTV.github.io

Package inspected on 2026-10-25:
`repo/plugin.video.wltvhelper.hotfix/hotfix-1.2.14.zip`

Adapted modules: `broadcaster/rai.py`, `broadcaster/sky.py`,
`broadcaster/tv2000.py`, `broadcaster/la7.py`, and `broadcaster/mediaset.py`.

The Mediaset module was inspected in the full release package
`repo/plugin.video.wltvhelper/plugin.video.wltvhelper-1.2.14.zip` on 2026-10-25
(it is not included in the separate hotfix archive). The adaptation keeps guest
authorization and standard Widevine licensing, requests DASH for Android,
validates the selector host and HTTPS redirects, rejects unsupported DRM, and
does not implement account-password login or persist authorization tokens.

Changes: translation to Java, replacement of Kodi APIs with native Android
Media3 APIs, bounded HTTPS network reads, removal of disabled TLS checks,
no persistent authentication tokens, no Kodi runtime dependency, audio-only
track configuration, and a new Android UI and media library service.
The upstream package itself is not bundled.

AndroidX Media3 is provided by Google/Android Open Source Project under
Apache-2.0; Guava by Google under Apache-2.0; desugar_jdk_libs uses its upstream
licenses. Gradle resolves these dependencies separately; no dependency binaries
are bundled in this source archive. See upstream distributions for notices.

Television content, channel names and services belong to their respective
owners. Source-code licensing grants no rights in television content.

## Channel branding

The PNG channel logos bundled in `app/src/main/res/drawable-nodpi/` are sourced
from the broadcasters' public catalogs and official websites. Exact source URLs
are listed in `CHANNEL_LOGOS.json`. La7 and TV2000 SVG originals were rasterized
for Android display. Logos are broadcaster trademarks/artwork and are not
relicensed under the source code GPL license. Their inclusion identifies the
channels and does not imply affiliation or endorsement.

## Mediaset artwork update — 1.0.0

The previous 100-pixel TV catalog thumbnails have been replaced with full-color
560-pixel originals from MFE/Mediaset's corporate channel gallery:
https://www.mfemediaforeurope.com/it/gruppo/tv-radio/
Exact image URLs remain in CHANNEL_LOGOS.json. Transparent padding was cropped
without altering the marks. Android Auto/media artwork is rendered at 512x512
with an appropriate background and lossless PNG encoding.

Copyright for original ListenTv code: © 2026 Gianrolando Scaringi.
Adapted WorldLiveTV code retains WLTV Group attribution and GPL-3.0-or-later.
Channel artwork is not relicensed under the GPL source-code license.
