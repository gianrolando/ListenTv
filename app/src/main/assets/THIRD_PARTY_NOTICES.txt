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

## Rai and Discovery catalog update — 1.1.0

`DiscoveryResolver.java` adapts `broadcaster/discovery.py` from WorldLiveTV
1.2.14 and its hotfix dated 2026-04-19, by WLTV Group, GPL-3.0-or-later.
Changes include Java/Media3 integration, validated authorization endpoints,
HTTPS-only requests, ephemeral guest tokens, HLS fallback and explicit MIME
types. No account login or subscription bypass is implemented.

Rai channel names and stream relinkers are taken from the official current
RaiPlay live catalog. Color Rai SVG marks and K2/Frisbee marks are sourced
from Wikimedia Commons (RAI and WBD remain the respective trademark owners);
Italiana uses its official RaiPlay mark. Discovery marks are retrieved from
the broadcaster's Aurora site configurations through its official image
service. SVGs are rasterized and transparent padding is trimmed, preserving
colors and proportions. Exact original/image-service URLs are recorded in
CHANNEL_LOGOS.json. No logo is relicensed under the source-code GPL.

## Additional television channels — 1.2.0

The La7 Cinema procedure follows WorldLiveTV's broadcaster/la7.py hotfix
(2026-04-19), by WLTV Group, GPL-3.0-or-later. The San Marino live-player
procedure follows broadcaster/smtv.py from WorldLiveTV 1.2.14. The Java
adaptation preserves HTTPS certificate validation and resolves fresh URLs
without persisting guest credentials or signed media URLs.

La7 Cinema and San Marino channel logos come from the broadcasters' official
websites, listed in CHANNEL_LOGOS.json. Marks retain their original ownership.
TV8's placement in the Discovery UI filter is a user-selected grouping, not
an assertion of broadcaster ownership; TV8's Sky source and attribution remain.

## Programme guide — 1.3.0

Programme titles and transmission times are downloaded at runtime from the
same public XMLTV feed configured by WorldLiveTV's lib/utils.py:
https://epg-guide.com/wltv.xz (epg-guide.com / EPGMaker by CrAcK75).
This feed is not bundled or redistributed with the app/source repository.
Only the selected channels' titles and start/stop times are kept in the app's
private cache. The guide data is not relicensed under the source-code GPL.
Italiana and Rai 4K use official RaiPlay schedule fallbacks.

XZ for Java 1.10, by the XZ for Java authors and contributors, decodes the feed
under the BSD Zero Clause License (0BSD). Full decoder notice:

Copyright (C) The XZ for Java authors and contributors

Permission to use, copy, modify, and/or distribute this
software for any purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
