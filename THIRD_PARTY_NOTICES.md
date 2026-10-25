# Attribution and source provenance

The provider-specific procedures in `StreamResolver.java` are adapted from
WorldLiveTV, by WLTV Group, version 1.2.14 hotfix, licensed GPL-3.0-or-later.
The original authors retain all rights in their work.

Upstream repository: https://github.com/WorldLiveTV/WorldLiveTV.github.io

Package inspected on 2026-10-25:
`repo/plugin.video.wltvhelper.hotfix/hotfix-1.2.14.zip`

Adapted modules: `broadcaster/rai.py`, `broadcaster/sky.py`,
`broadcaster/tv2000.py`, `broadcaster/la7.py`.

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
